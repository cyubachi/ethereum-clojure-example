(ns clojurescript-ethereum-example.h-login
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]
   [ajax.core :as ajax]
   [cljs-web3.core :as web3]
   [cljs-web3.eth :as web3-eth]
   [cljs-web3.personal :as web3-personal]
   [cljsjs.web3]
   [clojurescript-ethereum-example.db :as db]
   [day8.re-frame.http-fx]
   [re-frame.core :refer [dispatch subscribe]]
   [cljs-react-material-ui.reagent :as ui]
   [goog.string :as gstring]
   [goog.string.format]
   [madvas.re-frame.web3-fx]
   [hodgepodge.core :refer [session-storage set-item remove-item]]
   [re-frame.core :refer [reg-event-db reg-event-fx path trim-v after debug reg-fx console dispatch]]
   [clojurescript-ethereum-example.utils :as u]
   )
  )

(def interceptors [#_(when ^boolean js/goog.DEBUG debug)
                   trim-v])

(defn enter-password
  [callback]
  (let [pw (js/prompt "Please Enter Password" "password")]
    (callback nil pw)))

(reg-event-db
 :ui/cAddrUpdate
 interceptors
 (fn [db [x]]
   (assoc-in db [:contract :address] x)))

(reg-event-db
 :ui/loginEmailUpdate
 interceptors
 (fn [db [x]]
   (assoc-in db [:login :email] x)))

(reg-event-db
 :ui/loginPasswordUpdate
 interceptors
 (fn [db [x]]
   (assoc-in db [:login :password] x)))

(reg-event-fx
 :ui/login
 interceptors
 (fn [{:keys [db]}]
   (let [login (:login db)]
     (console :log ":ui/login db:" (clj->js db))
     {:db         db
      :http-xhrio {:method          :post
                   :uri             "/login"
                   :timeout         6000
                   :format          (ajax/transit-request-format)
                   :params          {:email    (:email login)
                                     :password (:password login)}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:ui/logined]
                   :on-failure      [:log-error]}})))

(reg-event-fx
 :ui/logined
 interceptors
 (fn [{:keys [db]} [{success :success user :user}]]
   (console :log ":ui/logined db:" (clj->js db))
   (console :log ":ui/logined success:" (clj->js success))
   (console :log ":ui/logined user:" (clj->js user))
   (if (true? success)
     (let [login    (:login db)
           keystore (.-keystore js/lightwallet)
           ks       (.deserialize keystore (:keystore user))]
       (set! (.-passwordProvider ks) #(%1 nil (:password login)))
       (set-item session-storage "keystore" (:keystore user))
       (console :log ":ui/logined password:" (:password login))
       (set-item session-storage "password" (:password login))
       (set-item session-storage "type" (:type user))
       (set-item session-storage "name" (:name user))
       (dispatch [:ui/web3 ks])
       (dispatch [:blockchain/my-addresses-loaded])
       (dispatch [:reload])
       {:db (-> db
                (assoc-in [:login :name] (:name user))
                (assoc :type (:type user))
                (assoc :page 0))})
     (do
       (js/alert "login failed. please check email or password.")
       {:db db}))))

(reg-event-db
 :ui/logout
 interceptors
 (fn [db]
   (remove-item session-storage "keystore")
   (-> db
       (assoc :type "customer")
       (dissoc :keystore)
       (assoc :page 3))))

(reg-event-db
 :ui/register-type
 interceptors
 (fn [db [type]]
   (assoc db :register-type type)))

(reg-event-fx
 :ui/register-create-vault
 interceptors
 (fn [{:keys [db]} [type]]
   (let [login (:login db)
         keystore (.-keystore js/lightwallet)
         keystore-params (clj->js {:password (:password login)})]
     (console :log ":ui/register-user db:" (clj->js db))
     (console :log ":ui/register-user type:" type)
     (dispatch [:ui/register-type type])
     (.createVault keystore keystore-params #(dispatch [:ui/register-key-from-password %1 %2])))
   {:db db}))

(reg-event-fx
 :ui/register-key-from-password
 interceptors
 (fn [{:keys [db]} [err ks]]
   (console :log ":ui/register-key-from-password db:" (clj->js db))
   (console :log ":ui/register-key-from-password err:" (clj->js err))
   (console :log ":ui/register-key-from-password ks:" (clj->js ks))
   (if-not (nil? err) (throw err))
   (let [login (:login db)]
     (.keyFromPassword ks (:password login) #(dispatch [:ui/register-init-keystore %1 %2 ks]))
     {:db db})))

(reg-event-fx
 :ui/register-init-keystore
 interceptors
 (fn [{:keys [db]} [err pw-derived-key ks]]
   (console :log ":ui/register-init-keystore db:" (clj->js db))
   (console :log ":ui/register-init-keystore err:" (clj->js err))
   (console :log ":ui/register-init-keystore pw-derived-key:" pw-derived-key)
   (let [encrypt-hd-path           "m/0'/0'/1'"
         hd-derivation-path-params (clj->js {:curve "curve25519", :purpose "asymEncrypt"})
         login                     (:login db)]
     (if-not (nil? err) (throw err))
     (.generateNewAddress ks pw-derived-key 3)
     (.addHdDerivationPath ks encrypt-hd-path pw-derived-key hd-derivation-path-params)
     (.generateNewEncryptionKeys ks pw-derived-key 1 encrypt-hd-path)
     (console :log ":ui/register-init-keystore keystore addresses:" (.getAddresses ks))
     (console :log ":ui/register-init-keystore serialized keystore:" (.serialize ks))
     (console :log ":ui/register-init-keystore public key:" (first (.getPubKeys ks encrypt-hd-path)))
     (set! (.-passwordProvider ks) enter-password)
     {:db         db
      :http-xhrio {:method          :post
                   :uri             "/register"
                   :timeout         6000
                   :format          (ajax/transit-request-format)
                   :params          {:email    (:email login)
                                     :password (:password login)
                                     :keystore (.serialize ks)
                                     :pubkey   (first (.getPubKeys ks "m/0'/0'/1'"))
                                     :type     (:register-type db)
                                     :name     (first (clojure.string/split (:email login) "@"))
                                     :address  (str "0x" (first (.getAddresses ks)))}
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success      [:ui/registered]
                   :on-failure      [:log-error]}})))

(reg-event-fx
 :ui/registered
 interceptors
 (fn [{:keys [db]} [res]]
   (console :log ":ui/registered db:" (clj->js db))
   (console :log ":ui/registered res:" (clj->js res))
   {:db db}))

(reg-event-db
 :ui/web3
 interceptors
 (fn [db [ks]]
   (let [ks        ks
         provider  (js/HookedWeb3Provider. (clj->js {:host db/rpc-url :transaction_signer ks}))
         web3      (js/Web3.)
         addresses (map #(str "0x" %) (js->clj (.getAddresses ks)))]
     (web3/set-provider web3 provider)
     (set! (.-accounts (.-eth web3)) (.getAddresses ks))
     (set! (.-getAccounts (.-eth web3)) #(.getAddresses ks))
     (-> db
         (assoc :keystore ks)
         (assoc :my-addresses addresses)
         (assoc :web3 web3)
         (assoc :provides-web3? true)))))
