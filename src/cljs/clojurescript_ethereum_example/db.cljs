(ns clojurescript-ethereum-example.db
  (:require
   [clojure.string :as str]
   [hodgepodge.core :refer [session-storage get-item set-item]]
   [cljs-web3.core :as web3-core]))

(def serialized-ks (get-item session-storage "keystore"))


(def keystore (.-keystore js/lightwallet))
(def deserialized-ks (if-not (nil? serialized-ks)
                       (let [ks (.deserialize keystore serialized-ks)
                             pw (get-item session-storage "password")]
                         (set! (.-passwordProvider ks) #(%1 nil pw))
                         ks)
                       nil))
(defn logined?
  []
  (not (nil? deserialized-ks)))

(def addresses (map #(str "0x" %) (if (logined?)
                                    (js->clj (.getAddresses deserialized-ks))
                                    [])))

;; (def rpc-url "https://localhost:8545")
(def rpc-url "https://ropsten.infura.io/qHfzDE5q6dQQ1SR2tW1M")

#_(def expired-term (* 86400 30))
(def expired-term 500)

(defn generate-web3
  [ks]
  (let [provider  (js/HookedWeb3Provider. (clj->js {:host rpc-url :transaction_signer ks}))
        web3      (js/Web3.)]
    (web3-core/set-provider web3 provider)
    ;; for ropsten.infura.io
    (set! (.-accounts (.-eth web3)) (.getAddresses ks))
    (set! (.-getAccounts (.-eth web3)) #(.getAddresses ks))
    web3))


(def web3 (if (logined?)
            (generate-web3 deserialized-ks)
            nil))

(def new-tweet-address (if (logined?)
                         (first addresses)
                         ""))

(def my-addresses (if (logined?)
                    addresses
                    []))

(def default-db
  {:tweets         []
   :filteredTweets []
   :settings       {}
   :my-addresses   my-addresses
   :accounts       {}
   :new-tweet      {:text     ""
                    :name     ""
                    :address  new-tweet-address
                    :sending? false}
   :web3           web3
   :provides-web3? (not (nil? web3))
   :contract       {:name     "carsensor"
                    :abi      nil
                    :bin      nil
                    :instance nil
                    ;; :address  "0x1a962b69f59b6879a0c25874aa86f8f2658aa368"
                    ;; :address  "0x7B51E82Cbeed5732845CFDFe58CFE9099a61d5De"

                    ;; Recruit  0x3c26ab7c9795d0d0507c05f7d6b1f13fe1f56827
                    ;; Dealer   0x1c90b30e6fa89fe54801989b17dc8d985f2a2e81
                    ;; Dealer2  0x56fcc45350aaf6abbb555ee32cc4324f25485e32
                    ;; Customer 0xabc24d2e0db6b3b1b548488d6d4d4264e652bc11

                    ;; :address "0x9015fe7338a4f2dfa7f066348e8e13b3fbcf51b4"
                    ;; :address "0x84a9241efd3203cc399b3c9c3e6dc75dd1b2d1a4"
                    ;; :address "0x3b0b1b5269d3da33161fec864c6a60ae7c93181e"
                    ;; :address "0xe146f1c2d557b98b23f7ca339d51e154639dee9e"
                    :address "0x46ff08c24e9c747e50974a41d8e031a3141e577a"
                    ;; :address  "0x717579347713f18c2e874b2679bb48625626a554"
                    ;; :address  "0xa330C8Ca0e63e95ec56012aF375EDc24999b4c00"
                    }
   :drawer     {:open false}
   :page       (if (logined?) 0 3)
   :tweetsNum  0
   :dev        {:address nil
                :amount  0
                :enc     nil
                }
   ;;
   :cars       [{:id     "car001"
                 :name   "Fiat punto"
                 :price  1000000
                 :image  "/images/car001.jpg"
                 :dealer (str/lower-case "0xfff3f282061c2add91d8850e4eb824a02f063f16")}
                {:id     "car002"
                 :name   "Fiat punto"
                 :price  2000000
                 :image  "/images/car002.jpg"
                 :dealer (str/lower-case "0xfff3f282061c2add91d8850e4eb824a02f063f16")}
                {:id     "car003"
                 :name   "Fiat punto"
                 :price  3000000
                 :image  "/images/car003.jpg"
                 :dealer (str/lower-case "0x4829028a81a3379074cd72cb2bb598339a5dc71c")}
                {:id     "car004"
                 :name   "Fiat punto"
                 :price  4000000
                 :image  "/images/car004.jpg"
                 :dealer (str/lower-case "0x4829028a81a3379074cd72cb2bb598339a5dc71c")}]
   :user       {:id       "0x"
                :name     "Geroge"
                :location "Tokyo"}
   :enquiry    {:open      false
                :lead-text "test text"
                :id        nil
                :name      nil
                :price     nil
                :dealer    nil
                :text      "test"
                :key       nil}
   :login      {:email    ""
                :password ""
                :name     (if (nil? (get-item session-storage "name"))
                            ""
                            (get-item session-storage "name"))}
   :keystore   deserialized-ks
   :type       (if (nil? (get-item session-storage "type"))
                 "customer"
                 (get-item session-storage "type"))
   :payed      false
   :limit      0
   :registered false
   :put-dealer {:from nil
                :name nil
                :tx   nil}
   })

