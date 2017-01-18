(ns clojurescript-ethereum-example.h-encrypt
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
   [cljs-react-material-ui.reagent :as ui]
   [goog.string :as gstring]
   [goog.string.format]
   [madvas.re-frame.web3-fx]
   [hodgepodge.core :refer [session-storage get-item set-item]]
   [re-frame.core :refer [reg-event-db reg-event-fx path trim-v after debug reg-fx console dispatch subscribe]]
   [clojurescript-ethereum-example.utils :as u]))

(def interceptors [#_(when ^boolean js/goog.DEBUG debug)
                   trim-v])

(def tweet-gas-limit 2000000)

(reg-event-fx
 :server/fetch-key
 interceptors
 (fn [{:keys [db]} [dealer id customer? index]]
   (console :log "fetch:" dealer)
   (console :log "id:" id)
   (console :log "customer?: " customer?)
   {:http-xhrio {:method          :get
                 :uri             (str "/key/" dealer)
                 :timeout         6000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      (if customer?
                                    [:fetch-dealer-key-result]
                                    [:decrypt-customer-message index])
                 :on-failure      [:log-error]}}))

(reg-event-db
 :fetch-dealer-key-result
 interceptors
 (fn [db [result]]
   (console :log ":fetch-key-result db:" (clj->js db))
   (console :log ":fetch-key-result result:" (clj->js result))
   (assoc-in db [:enquiry :key] (:pubkey result))))

(reg-event-db
 :decrypt-customer-message
 interceptors
 (fn [db [index result]]
   (let [keystore        (:keystore db)
         hd-path         "m/0'/0'/1'"
         encryption      (.-encryption js/lightwallet)
         dealer-pubkey   (first (.getPubKeys keystore hd-path))
         customer-pubkey (:pubkey result)
         message         (:message (nth (:tweets db) index))
         password        (get-item session-storage "password")]
     (console :log ":decrypt-customer-message db:" (clj->js db))
     (console :log ":decrypt-customer-message index:" (clj->js index))
     (console :log ":decrypt-customer-message result:" (clj->js result))
     (console :log ":decrypt-customer-message dealer-pubkey:" (clj->js dealer-pubkey))
     (console :log ":decrypt-customer-message customer-pubkey:" (clj->js customer-pubkey))
     (console :log ":decrypt-customer-message message:" (clj->js (.parse js/JSON (js/atob message))))
     (console :log ":decrypt-customer-message password:" password)
     (.keyFromPassword keystore password
                       (fn [err pw-derived-key]
                         (if-not (nil? err)
                           (throw err))
                         (let [decrypted-message (.multiDecryptString encryption keystore
                                                                      pw-derived-key
                                                                      (clj->js (.parse js/JSON (js/atob message)))
                                                                      customer-pubkey
                                                                      dealer-pubkey
                                                                      hd-path)]
                           (console :log "decrypted message:" decrypted-message)
                           (if-not (false? decrypted-message)
                             (do
                               (console :log "quoted decrypted message:" (reader/read-string decrypted-message))
                               (dispatch [:update-decrypted-message index decrypted-message]))
                             (console :log "tweets[" index "]: cannot decrypt message.")))))
     db)))

(reg-event-db
 :update-decrypted-message
 interceptors
 (fn [db [index decrypted-message]]
   (let [parsed-message  (reader/read-string decrypted-message)
         display-message [ui/paper {:style {:padding "5px 10px 10px"}}
                          [:div "CAR_NAME: " (:name parsed-message)]
                          [:div "PRICE: " (:price parsed-message)]
                          [:div "MESSAGE: "  (:text parsed-message)]]]
     (assoc-in db [:tweets index :message] display-message))))

(reg-event-db
 :decrypt/messages
 interceptors
 (fn [db]
   (let [tweets     (:tweets db)
         tweets-num (count tweets)]
     (console :log ":decrypt/message db:" (clj->js db))
     (console :log ":decrypt/message tweets:" (clj->js tweets))
     (console :log ":decrypt/message tweets-num:" tweets-num)
     (doseq [i (range 0 tweets-num)]
       (console :log (str "tweets[" i "]")  (clj->js (nth tweets i)))
       (dispatch [:server/fetch-key (:from (nth tweets i)) nil false i]))
     db)))
