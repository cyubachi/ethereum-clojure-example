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
   (let [result (js->clj res)]
     (console :log "hendler:dev/set-users" result)
     (assoc-in db [:users] result)))
 )
