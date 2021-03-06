(ns clojurescript-ethereum-example.h-list
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

(reg-event-db
 :ui/enquiry
 (fn [db [_ id name price dealer]]
   (console :log "hendler:ui/enquiry" (get-in db [:enquiry :open]) id name price dealer)
   (dispatch [:server/fetch-key dealer id true])
   (-> db
       (assoc-in [:enquiry :open] true)
       (assoc-in [:enquiry :id] id)
       (assoc-in [:enquiry :name] name)
       (assoc-in [:enquiry :price] price)
       (assoc-in [:enquiry :dealer] dealer))
   ))

(reg-event-db
 :enquiry/update
 interceptors
 (fn [db [value]]
   (assoc-in db [:enquiry :text] value)))

(reg-event-fx
 :enquiry/send-encrypt-message
 interceptors
 (fn [{:keys [db]} [from to encrypted-message]]
   (console :log ":enquiry/send-encrypted-message db:" (clj->js db))
   (console :log "from:" from)
   (console :log "to:" to)
   (console :log "encrypted-message:" (js/btoa (.stringify js/JSON encrypted-message)))
   {:db db
    :web3-fx.contract/state-fn
      {:instance (:instance (:contract db))
       :web3     (:web3 db)
       :db-path  [:contract :send-tweet]
       :fn       [:add-enquiries from to (js/btoa (.stringify js/JSON encrypted-message)) (.getTime (js/Date.))
                  {:from     from
                   :gas      tweet-gas-limit
                   :gasPrice (web3-eth/gas-price (:web3 db))}
                  :enquiry/received
                  :log-error
                  :enquiry/transaction-receipt-loaded]}}))

(reg-event-fx
 :enquiry/close
 interceptors
 (fn [{:keys [db]}]
   {:db (assoc-in db [:enquiry :open] false)}))

(reg-event-fx
 :enquiry/send
 interceptors
 (fn [{:keys [db]} []]
   (console :log "handler:enquiry/send"
            (get-in db [:enquiry :id]))
   (console :log "send db: " db)
   (let [from            (get-in db [:new-tweet :address])
         to              (:dealer (:enquiry db))
         message         (pr-str (dissoc (:enquiry db) :open :lead-text :dealer :key))
         ks              (:keystore db)
         dealer-pubkey   (get-in db [:enquiry :key])
         encrypt-hd-path "m/0'/0'/1'"
         my-pubkey       (first (.getPubKeys ks encrypt-hd-path))
         password        (get-item session-storage "password")]
     (.keyFromPassword ks password
                       (fn [err pw-derived-key]
                         (console :log "err:" err)
                         (console :log "pw-derived-key-1:" pw-derived-key)
                         (console :log "dealer-pubkey:" dealer-pubkey)
                         (console :log "my-pubkey:" my-pubkey)
                         (let [encryption        (.-encryption js/lightwallet)
                               encrypted-message (.multiEncryptString encryption ks pw-derived-key message my-pubkey (clj->js [dealer-pubkey]) encrypt-hd-path)]
                           (console :log "encrypted-message:" encrypted-message)
                           (dispatch [:enquiry/send-encrypt-message from to encrypted-message]))))
     {:db (assoc-in db [:enquiry :open] false)})))

(reg-event-db
 :enquiry/received
 interceptors
 (fn [db [transaction-hash]]
   (console :log "Enquiry was confirmed! on" transaction-hash)
   (assoc-in db [:enquiry :text] "")))

(reg-event-db
 :enquiry/transaction-receipt-loaded
 interceptors
 (fn [db [{:keys [gas-used] :as transaction-receipt}]]
   (console :log "Enquiry was mined! like" transaction-receipt)
   (when (= gas-used tweet-gas-limit)
     (console :error "All gas used"))
   db))
