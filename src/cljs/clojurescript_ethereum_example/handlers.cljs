(ns clojurescript-ethereum-example.handlers
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
   [clojure.string :as str]
   [cljs.reader :as reader]
   [cljs.core.async :as async :refer [>! <! put! timeout chan]]
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
   [re-frame.core :refer [reg-event-db reg-event-fx path trim-v after debug reg-fx console dispatch subscribe]]
   [clojurescript-ethereum-example.utils :as u]
   )
  )

(def interceptors [#_(when ^boolean js/goog.DEBUG debug)
                   trim-v])

(def tweet-gas-limit 2000000)

(reg-event-fx
 :initialize
 (fn [_ _]
   (console :log "initialize")
   (console :log "db/default-db" (clj->js db/default-db))
   {:db                          db/default-db
    :http-xhrio {:method          :get
                 :uri             (gstring/format "./contracts/build/%s.abi"
                                                  (get-in db/default-db [:contract :name]))
                 :timeout         6000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:contract/abi-loaded]
                 :on-failure      [:log-error]}
    :dispatch   [:blockchain/my-addresses-loaded]}))

(reg-event-fx
 :reload
 (fn [{:keys [db]} _]
   (console :log "db:" db)
   (let [ks       (:keystore db)
         web3     (:web3 db)
         address  (:address (:new-tweet db))]
     (console :log "reload:" (clj->js db))
     (merge
      {:db         db
       :http-xhrio {:method          :get
                    :uri             (gstring/format "./contracts/build/%s.abi"
                                                     (get-in db/default-db [:contract :name]))
                    :timeout         6000
                    :response-format (ajax/json-response-format {:keywords? true})
                    :on-success      [:contract/abi-loaded]
                    :on-failure      [:log-error]}}
      {:web3-fx.blockchain/fns {:web3 web3
                                :fns  [[#(:my-addresses db)
                                        :blockchain/my-addresses-loaded
                                        :log-error]]}}))))

(reg-event-fx
 :dealer/register
 interceptors
 (fn [{:keys [db]} []]
   (console :log ":dealer/register")
   (console :log ":dealer/register db: " db)
   (let [web3         (:web3 db)
         from         (get-in db [:new-tweet :address])
         name         (get-in db [:login :name])
         tx           {:from from
                       :gasPrice (web3-eth/gas-price web3)}]
     (console :log ":dealer/register transaction:" tx)
     {:db (-> db
              (assoc-in [:put-dealer :from] from)
              (assoc-in [:put-dealer :name] name)
              (assoc-in [:put-dealer :tx] tx))
      :dispatch [:dealer/put-dealer-estmate-gas from name tx :dealer/put-dealer :log-error]})))

(reg-event-fx
 :dealer/put-dealer
 interceptors
 (fn [{:keys [db]} [err gas]]
   (if-not (nil? err) (throw err))
   (console :log ":dealer/put-dealer db:" (clj->js db))
   (console :log ":dealer/put-dealer gas:" gas)
   (let [from (get-in db [:put-dealer :from])
         name (get-in db [:put-dealer :name])
         tx   (get-in db [:put-dealer :tx])]
     {:db db
      :web3-fx.contract/state-fn
      {:instance (:instance (:contract db))
       :web3     (:web3 db)
       :db-path  [:contract :put-dealer]
       :fn       [:put-dealer from name (assoc tx :gas gas)
                  :dealer/put-dealer-received
                  :log-error
                  :dealer/put-dealer-receipt-loaded]}})))

(reg-event-fx
 :dealer/put-dealer-estmate-gas
 interceptors
 (fn [{:keys [db]} [from name tx]]
   (let [contract (get-in db [:contract :instance])]
     (console :log ":dealer/put-dealer-estmate-gas from:" from)
     (console :log ":dealer/put-dealer-estmate-gas name:" name)
     (console :log ":dealer/put-dealer-estmate-gas tx:" (clj->js tx))
     (.estimateGas (.-putDealer contract) from name tx #(dispatch [:dealer/put-dealer %1 %2]))
     {:db db})))


(reg-event-db
 :dealer/put-dealer-received
 interceptors
 (fn [db [transaction-hash]]
   (console :log "putDealer tx registered." transaction-hash)
   db))

(reg-event-db
 :dealer/put-dealer-receipt-loaded
 interceptors
 (fn [db [{:keys [gas-used] :as transaction-receipt}]]
   (console :log "Dealer was mined! like" transaction-receipt)
   (when (= gas-used tweet-gas-limit)
     (console :error "All gas used"))
   db))


(reg-event-fx
 :publication-fee/pay
 interceptors
 (fn [{:keys [db]} []]
   (console :log ":publication-fee/pay")
   (console :log ":publication-fee/pay db: " db)
   (let [web3         (:web3 db)
         from         (get-in db [:new-tweet :address])
         to           (:address (:contract db))
         value        (.toWei (:web3 db) 0.01)
         tx           {:from from
                       :to to
                       :value value
                       :gasPrice (web3-eth/gas-price web3)}
         gas          (+ (web3-eth/estimate-gas web3 tx) 1500000)]
     (console :log ":publication-fee/pay send transaction:" (clj->js (assoc tx :gas gas)))
     (web3-eth/send-transaction! web3 (assoc tx :gas gas) (fn [err tx]
                                                            (console :log "err:" err)
                                                            (console :log "tx:" tx)
                                                            (dispatch [:reload])))
     {:db db})))

(reg-event-fx
 :contract/get-dealer
 interceptors
 (fn [{:keys [db]} [[enquiry-count name address is-payed payed-amount]]]
   (let [contract (get-in db [:contract :instance])
         empty-address "0x0000000000000000000000000000000000000000"]
     (console :log ":contract/get-dealer enquiry-count:" (.toNumber enquiry-count))
     (console :log ":contract/get-dealer name:" name)
     (console :log ":contract/get-dealer address:" address)
     (console :log ":contract/get-dealer is-payed:" is-payed)
     {:db       (-> db
                    (assoc :payed  is-payed)
                    (assoc :registered (not (= address empty-address)))
                    (assoc :tweets (into [] (for [x (range 0 (.toNumber enquiry-count))] nil)))
                    (assoc :tweetsNum 0))
      :dispatch [:contract/get-enquiries [address (.toNumber enquiry-count)]]})))

(reg-event-db
 :contract/get-enquiries
 interceptors
 (fn [db [[address enquiry-count]]]
   (console :log ":contract/get-enquiries")
   (console :log "address:" address)
   (console :log "enquiry-count:" enquiry-count)
   (console :log ":contract/get-enquiries db" (clj->js db))
   (doseq [x (range 0 enquiry-count)]
     (console :log "loop:" x)
     (.getDealerEnquiry (get-in db [:contract :instance])
                        address
                        x
                        (fn [err [from to message date]]
                          (let [enquiry {:from from
                                         :to to
                                         :message message
                                         :date (js/Date. (.toNumber date))}]
                            (console :log "enquiry" (clj->js enquiry))
                            (dispatch [:contract/get-enquiry x enquiry])))))
   db))

(reg-event-db
 :contract/get-enquiry
 interceptors
 (fn [db [index enquiry]]
   (console :log ":contract/get-enquiry")
   (console :log (assoc-in db [:tweets index] enquiry))
   (assoc-in db [:tweets index] enquiry)))

(reg-event-fx
 :blockchain/my-addresses-loaded
 interceptors
 (fn [{:keys [db]}]
   (console :log "my-addresses-loaded: " db)
   {:db (-> db
            (assoc-in [:new-tweet :address] (first (:my-addresses db))))
    :web3-fx.blockchain/balances
    {:web3                   (:web3 db)
     :addresses              (:my-addresses db)
     :watch?                 true
     :blockchain-filter-opts "latest"
     :dispatches             [:blockchain/balance-loaded :log-error]}}))

(reg-event-fx
 :contract/abi-loaded
 interceptors
 (fn [{:keys [db]} [abi]]
   (console :log "abi-loaded" db)
   (if-not (nil? (:web3 db))
     (let [web3              (:web3 db)
           contract-instance (web3-eth/contract-at web3 abi (:address (:contract db)))
           db                (-> db
                                 (assoc-in [:contract :abi] abi)
                                 (assoc-in [:contract :instance] contract-instance)
                                 (assoc-in [:tweets] nil))]
       {:db db
        :web3-fx.contract/constant-fns
        {:instance contract-instance
         :fns      [[:get-dealer (:address (:new-tweet db)) :contract/get-dealer :log-error]]}}))))

(reg-event-db
 :contract/settings-loaded
 interceptors
 (fn [db [[max-name-length max-tweet-length]]]
   (assoc db :settings {:max-name-length  (.toNumber max-name-length)
                        :max-tweet-length (.toNumber max-tweet-length)})))

(reg-event-db
 :blockchain/balance-loaded
 interceptors
 (fn [db [balance address]]
   (assoc-in db [:accounts address :balance] balance)))

(reg-event-fx
 :contract/fetch-compiled-code
 interceptors
 (fn [{:keys [db]} [on-success]]
   {:http-xhrio {:method          :get
                 :uri             (gstring/format "/contracts/build/%s.json"
                                                  (get-in db [:contract :name]))
                 :timeout         6000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      on-success
                 :on-failure      [:log-error]}}))

(reg-event-fx
 :contract/deploy-compiled-code
 interceptors
 (fn [{:keys [db]} [contracts]]
   (let [{:keys [abi bin]} (get-in contracts [:contracts (keyword (:name (:contract db)))])]
     {:web3-fx.blockchain/fns
      {:web3 (:web3 db)
       :fns  [[web3-eth/contract-new
               (js/JSON.parse abi)
               {:gas  4500000
                :gasPrice (web3-eth/gas-price (:web3 db))
                :data bin
                :from (first (:my-addresses db))}
               :contract/deployed
               :log-error]]}})))

(reg-event-fx
 :blockchain/unlock-account
 interceptors
 (fn [{:keys [db]} [address password]]
   {:web3-fx.blockchain/fns
    {:web3 (:web3 db)
     :fns  [[web3-personal/unlock-account address password 999999
             :blockchain/account-unlocked
             :log-error]]}}))

(reg-event-fx
 :blockchain/account-unlocked
 interceptors
 (fn [{:keys [db]}]
   (console :log "Account was unlocked.")
   {}))

(reg-event-fx
 :contract/deployed
 interceptors
 (fn [_ [contract-instance]]
   (when-let [address (aget contract-instance "address")]
     (console :log "Contract deployed at" address))))

(reg-event-fx
 :log-error
 interceptors
 (fn [_ [err]]
   (console :error err)
   {}))

;; - - - - -

(reg-event-db
 :ui/drawer
 (fn [db]
   (console :log "hendler:ui/drawer" (get-in db [:drawer :open]))
   (if (get-in db [:drawer :open])
     (assoc-in db [:drawer :open] false)
     (assoc-in db [:drawer :open] true) )
   ))

(reg-event-db
 :ui/page
 interceptors
 (fn [db [x]]
   (console :log "hendler:ui/page" (get-in db [:page]) "->" x)
   (if-not (nil? (:keystore db))
     (do
       (dispatch [:reload])
       (assoc-in db [:page] x))
     (assoc-in db [:page] 3))))

(reg-event-fx
 :ui/send
 interceptors
 (fn [{:keys [db]}]
   (let [balance (subscribe [:new-tweet/selected-address-balance])
         payable (> 0.01 (web3/from-wei @balance :ether))]
     (console :log ":ui/send")
     (console :log "[:new-tweet :address]: " (get-in db [:new-tweet :address]))
     (console :log "payable: " payable)
     (console :log "db:" (clj->js db))
     (if payable
       {:http-xhrio {:method          :get
                     :uri             "/send"
                     :timeout         100000
                     :params          {:address (get-in db [:new-tweet :address])}
                     :response-format (ajax/json-response-format {:keywords? true})
                     :on-success      [:ui/send-log]
                     :on-failure      [:log-error]}}
       {:db db}))))

(reg-event-db
 :ui/send-log
 interceptors
 (fn [db [res]]
   (let [result (js->clj res)]
     (console :log "hendler :ui/send-log : " result)
     db)))
