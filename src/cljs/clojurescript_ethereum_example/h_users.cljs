(ns clojurescript-ethereum-example.h-users
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
   [hodgepodge.core :refer [session-storage remove-item]]
   [re-frame.core :refer [reg-event-db reg-event-fx path trim-v after debug reg-fx console dispatch]]
   [clojurescript-ethereum-example.utils :as u]
   )
  )

(def interceptors [#_(when ^boolean js/goog.DEBUG debug)
                   trim-v])

(reg-event-fx
 :dev/get-users
 interceptors
 (fn [{:keys [db]}]
   {:http-xhrio {:method          :get
                 :uri             "/users/"
                 :timeout         6000
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success      [:dev/set-users]
                 :on-failure      [:log-error]}
    }
   ))

(reg-event-db
 :dev/set-users
 interceptors
 (fn [db [res]]
   (let [result (js->clj res) ;; ":keywordize-keys true"
         addresses (into [] (map #(:address %) result))]
     (console :log "hendler:dev/set-users" result addresses)
     (if (not (nil? (:web3 db)))
       (dispatch [:dev/get-balances addresses]) )
     (assoc-in db [:users] result)))
 )

(reg-event-fx
 :dev/get-balances
 interceptors
 (fn [{:keys [db]} [x]]
   (console :log "get-balances: " x)
   {;; :db (-> db
    ;;         (assoc-in [:balances :address] (first (:my-addresses db))))
    :web3-fx.blockchain/balances
    {:web3                   (:web3 db)
     :addresses              x
     :watch?                 true
     :blockchain-filter-opts "latest"
     :dispatches             [:dev/set-balances :log-error]}}))

(reg-event-db
 :dev/set-balances
 interceptors
 (fn [db [bbalance address]]
   (let [balance (js/parseFloat (str (web3/from-wei bbalance :ether)))]
     (dispatch [:dev/get-dealerinfo address])     
     (console :log "set-balances:" address balance)
     (assoc-in db [:users-balances address] balance))))

(reg-event-db
 :dev/get-dealerinfo
 interceptors
 (fn [db [address]]
   (console :log "get-dealerinfo:" address)
   (.getDealer (get-in db [:contract :instance])
               address
               (fn [err [cntb name addr paid pvalb]]
                 (let [cnt (js/parseFloat (str (web3/from-wei cntb :ether)))
                       pval (js/parseFloat (str (web3/from-wei pvalb :ether)))]
                   (dispatch [:dev/set-dealerinfo addr paid pval]) )))
   db))

(reg-event-db
 :dev/set-dealerinfo
 interceptors
 (fn [db [address paid pval]]
   (console :log address "-->" paid pval)
   (assoc-in db [:users-status address] {:paid paid
                                         :pval pval})
   ) )
