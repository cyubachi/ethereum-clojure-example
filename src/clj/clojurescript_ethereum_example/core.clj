(ns clojurescript-ethereum-example.core
  (:require [clojure.core.async :refer [<! >! put! close! go]]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as sql]
            [compojure.core :refer [ANY GET PUT POST DELETE defroutes]]
            [compojure.route :refer [resources]]
            [ring.middleware.defaults :refer [wrap-defaults site-defaults]]
            [ring.middleware.gzip :refer [wrap-gzip]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.json :refer [wrap-json-params]]
            [ring.middleware.transit :refer [wrap-transit-params]]
            [com.jakemccrary.middleware.reload :as reload]
            [clj-time.core :as c]
            [clj-time.coerce :as cr]
            [ring.logger.timbre :as logger.timbre]
            [environ.core :refer [env]]
            [cheshire.core :as json]
            [org.httpkit.server :refer [run-server]]
            [me.raynes.fs :as fs]
            [cloth.keys :as k])
  (:import org.web3j.protocol.Web3j
           org.web3j.protocol.infura.InfuraHttpService
           org.web3j.crypto.Credentials
           org.web3j.utils.Numeric
           org.web3j.utils.Convert
           org.web3j.tx.Transfer
           org.web3j.protocol.core.DefaultBlockParameterName
           org.web3j.protocol.core.methods.request.RawTransaction
           org.web3j.crypto.TransactionEncoder
           org.web3j.protocol.core.methods.response.Transaction
           java.math.BigDecimal
           java.math.BigInteger)
  (:gen-class))

(def ^:dynamic *server*)

(def send-price "0.01")
(def balance-threshold 0.001)
(def conn  (InfuraHttpService. (str "https://ropsten.infura.io/" (env :infuraiokey))))
(def web3j (Web3j/build conn))
(def cred  (Credentials/create (env :senderprivkey)))
(def db-spec {:classname "org.sqlite.JDBC", :subprotocol "sqlite", :subname "test.db"})

(defn get-user [email]
  (first (sql/query db-spec ["SELECT * FROM users WHERE email = ?" email])))

(defn get-user-from-address [address]
  (first (sql/query db-spec ["SELECT * FROM users WHERE address = ? " address])))

(defn generate-private-key
  [{dealer-address :dealer-address expired :expired}]
  (let [user   (get-user-from-address dealer-address)
        email  (:email user)
        now    (cr/to-long (c/now))
        expire (+ now 300000)]
    (println now)
    (println (str "dealer-address: " dealer-address))
    (println (:private_key_expire user))
    (cond
      (and (nil? (:private_key_expire user)) (nil? (:private_key user)))              (let [private-key (:private-key (k/create-keypair))]
                                                                                        (println "both nil.")
                                                                                        (sql/update! db-spec :users {:private_key private-key} ["email = ?" email])
                                                                                        private-key)
      (and (not (nil? (:private_key_expire user)))
           (and (< (:private_key_expire user) now) (not (nil? (:private_key user))))) (let [private-key (:private-key (k/create-keypair))]
                                                                                        (println "expire over.")
                                                                                        (sql/update! db-spec
                                                                                                     :users {:private_key        private-key
                                                                                                             :private_key_expire nil}
                                                                                                     ["email = ?" email])
                                                                                        private-key)
      :else                                                                           (:private_key user))))
#_{:private-key "0xf7324b22730a5ad5202bd056ffdabac67fdbb1d8b21407819b8c507fafac24cd", :address "0xfb786196af2cf2333e3c1268e01cd7b8c2c4d649"}


(defn update-private-key-expire
  [email]
  (let [now   (cr/to-long (c/now))
        expire (+ now 300000)]
    (sql/update! db-spec :users {:private_key_expire expire} ["email = ?" email])))

(defn get-private-key
  [{tx-hash :hash} session]
  (let [transaction (.orElse (.getTransaction (.send (.ethGetTransactionByHash web3j tx-hash))) (Transaction.))]
    (when-not (nil? transaction)
      (let [user (get-user-from-address (.getFrom transaction))
            private-key (:private_key user)]
        (println (str "tx-hash: " tx-hash))
        (println (str "private-key: " private-key))
        (println (str "transaction: " transaction))
        (println (str "From: " (.getFrom transaction)))
        (println (str "To: " (.getTo transaction)))
        (println (str "Value: " (.getValue transaction)))
        (when (and
               (= (.getTo transaction) "0x46ff08c24e9c747e50974a41d8e031a3141e577a")
               (and
                (= (.getFrom transaction) (:address user))
                (= (.getValue transaction) 10000000000000000)))
          (do
            (generate-private-key {:dealer-address (:address user)})
            (update-private-key-expire (:email user))
            (println (str "user: " user))
            (println (str "private-key: " (:private_key (get-user-from-address (.getFrom transaction)))))
            (:private_key (get-user-from-address (.getFrom transaction)))))))))

(defn get-balance-ether
  [address]
  (Convert/fromWei (.toString (.getBalance (.send (.ethGetBalance web3j address (org.web3j.protocol.core.DefaultBlockParameterName/LATEST)))))
                   (org.web3j.utils.Convert$Unit/ETHER)))

(defn sendable? [email]
  (let [user    (get-user email)
        hash    (:pending_transaction user)
        address (:address user)]
    (if (> (get-balance-ether address) balance-threshold)
      false
      (if (nil? hash)
        true
        (let [transaction (.orElse (.getTransaction (.send (.ethGetTransactionByHash web3j hash))) (Transaction.))]
          (if (nil? (.getBlockNumberRaw transaction))
            false
            (do
              (sql/update! db-spec :users {:pending_transaction nil} ["email = ?" email])
              true)))))))

(defn- send-ether [to-address eth-val]
  (let [email (:email (get-user-from-address to-address))]
    (println "clientVer: " (.getWeb3ClientVersion (.send (.web3ClientVersion web3j))))
    (println "senderAdr: " (.getAddress cred))
    (println "email: " email)
    (if (sendable? email)
      (let [eth-cnt     (.get (.sendAsync (.ethGetTransactionCount web3j (.getAddress cred) (org.web3j.protocol.core.DefaultBlockParameterName/PENDING))))
            nonce       (.getTransactionCount eth-cnt)
            gas-limit   (BigInteger/valueOf 21000)
            gas-price   (.getGasPrice (.get (.sendAsync (.ethGasPrice web3j))))
            value       (.toBigInteger (Convert/toWei eth-val (org.web3j.utils.Convert$Unit/ETHER)))
            raw-tx      (RawTransaction/createEtherTransaction nonce gas-price gas-limit to-address value)
            signed-msg  (TransactionEncoder/signMessage raw-tx cred)
            hex         (Numeric/toHexString signed-msg)
            send-tx     (.get (.sendAsync (.ethSendRawTransaction web3j hex)))
            hash        (.getTransactionHash send-tx)
            transaction (.orElse (.getTransaction (.send (.ethGetTransactionByHash web3j hash))) (Transaction.))]
        (println "Transfer: registered for" to-address " " hash)
        (println "Transaction Hash: " (.getHash transaction))
        (println "Transaction BlockNumber: " (.getBlockNumberRaw transaction))
        (println "Transaction TransactionIndex: " (.getTransactionIndexRaw transaction))
        (println "Transaction From: " (.getFrom transaction))
        (println "Transaction To: " (.getTo transaction))
        (println "Transaction Value: " (.getValueRaw transaction))
        (println "Transaction GasPrice: " (.getGasPriceRaw transaction))
        (println "Transaction Gas: " (.getGasRaw transaction))
        (sql/update! db-spec :users {:pending_transaction hash} ["email = ?" email])
        {:success true :message nil})
      {:success false :message "balance is enough or now waiting transaction."})))

(defn json-response
  [body & more]
  (let [response {:status  200
                  :headers {"Content-Type" "text/html; charset=utf-8"}
                  :body    (json/generate-string body)}
        session  (first more)]
    (if-not (nil? session)
      (assoc response :session session)
      response)))


(defn login-ok?
  [email password]
  (if-not (= 0 (:count (sql/query db-spec ["SELECT count(*) as count FROM users WHERE email = ? AND password = ?" email password])))
    true
    false))

(defn login [session {email :email password :password  :as params}]
  (if (login-ok? email password)
    (json-response {:success true :user (get-user email)} (assoc session :email email))
    (json-response {:success false})))

(defn reg-keystore
  [session {email :email keystore :keystore}]
  (sql/update! db-spec :users {:keystore keystore} ["email = ?" email]))

(defn register
  [session {email :email password :password keystore :keystore :as params}]
  (sql/insert! db-spec :users params)
  {:success true :user params})

(defn send-api
  [session {address :address :as params}]
  (send-ether address send-price))


(defroutes routes

  (resources "/browser-solidity/" {:root "browser-solidity"})

  (resources "/images/" {:root "images"})

  (POST "/login" {session :session params :params} (login session params))
  (POST "/register" {session :session params :params} (json-response (register session params)))
  (POST "/update-keystore" {session :session params :params} (json-response (reg-keystore session params)))
  (GET "/send" {session :session params :params} (json-response (send-api session params)))
  (POST "/generate-private-key" {params :params} (json-response (generate-private-key params)))
  (GET "/get-private-key" {params :params session :session} (json-response (get-private-key params session)))
  ;; DEALER KEY
  (GET "/key/:address" [address];; "/dealers/" isnt dealt with.
       (println "id:" address)
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (json/generate-string
                  (if (= 0 (count (filter #(= address (:address %)) (vals @users))))
                    {}
                    (dissoc (first (filter #(= address (:address %)) (vals @users))) :keystore)))})

  ;; DEALER INFO
  (GET "/dealers/:address" [address];; "/dealers/" isnt dealt with.
       (println "dealers: " address)
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (json/generate-string
                  (if (= 0 (count (filter #(= address (:address %)) (vals @users))))
                    {}
                    (dissoc (first (filter #(= address (:address %)) (vals @users))) :keystore)))})

  (GET "/users/" []
       (println "users: all")
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (json/generate-string (sql/query db-spec ["SELECT * FROM users"]))})

  (GET "/js/*" _
       {:status 404})

  (GET "/" _
       {:status  200
        :headers {"Content-Type" "text/html; charset=utf-8"}
        :body    (io/input-stream (io/resource "public/index.html"))}))

(def http-handler
  (-> routes
      ;; (wrap-defaults site-defaults)
      reload/wrap-reload
      logger.timbre/wrap-with-logger
      (wrap-defaults (assoc-in site-defaults [:security :anti-forgery] false))
      wrap-with-logger
      wrap-json-params
      (wrap-transit-params {:opts{}})
      wrap-gzip))

(defn -main [& [port]]
  ;;
  (let [port (Integer. (or port (env :port) 6655))]
    (alter-var-root (var *server*)
                    (constantly (run-server http-handler {:port port :join? false})))))

(defn stop-server []
  (*server*)
  (alter-var-root (var *server*) (constantly nil)))

(defn restart-server []
  (stop-server)
  (-main))

(comment
  (restart-server)
  (-main)
  (stop-server))
