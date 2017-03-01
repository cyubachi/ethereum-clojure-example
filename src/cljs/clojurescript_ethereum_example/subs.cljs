(ns clojurescript-ethereum-example.subs
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [reg-sub console]]
   ) ) 

(reg-sub
 :db/my-addresses
 (fn [db]
   (:my-addresses db)))

(reg-sub
 :db/tweets
 (fn [db]
   (sort-by :date #(compare %2 %1) (:tweets db))))

(reg-sub
 :db/new-tweet
 (fn [db]
   (:new-tweet db)))

(reg-sub
  :db/settings
  (fn [db]
    (:settings db)))

(reg-sub
 :new-tweet/selected-address-balance
 (fn [db]
   (get-in db [:accounts (:address (:new-tweet db)) :balance])))

(reg-sub
 :db/drawer
 (fn [db]
   (:drawer db)))

(reg-sub
 :db/page
 (fn [db]
   (:page db)))

(reg-sub
 :db/tweetsNum
 (fn [db]
   (:tweetsNum db)))

(reg-sub
 :db/contractAddr
 (fn [db]
   (get-in db [:contract :address])
   ))

(reg-sub
 :db/contractAbi
 (fn [db]
   (get-in db [:contract :abi])
   ))

(reg-sub
 :db/devAddr
 (fn [db]
   (get-in db [:dev :address])
   ))

(reg-sub
 :db/devAmount
 (fn [db]
   (get-in db [:dev :amount]) 
   ))

(reg-sub
 :db/devEnc
 (fn [db]
   (get-in db [:dev :enc]) 
   ))

(reg-sub
 :db/cars
 (fn [db]
   (:cars db)))


(reg-sub
 :db/enquiry
 (fn [db]
   (:enquiry db)))


(reg-sub
 :db/login
 (fn [db]
   (:login db)))

(reg-sub
 :db/keystore
 (fn [db]
   (:keystore db)))

(reg-sub
 :db/type
 (fn [db]
   (:type db)))

(reg-sub
 :db/payed
 (fn [db]
   (:payed db)))

(reg-sub
 :db/limit
 (fn [db]
   (:limit db)))

(reg-sub
 :db/registered
 (fn [db]
   (:registered db)))

(reg-sub
 :db/users
 (fn [db]
   (let [users (:users db)]
     ;;(console :log "xxx:" users)
     (map #(-> %
               (assoc-in [:balance] (get-in db [:users-balances (:address %)]))
               (assoc-in [:paid] (get-in db [:users-status (:address %) :paid]))
               ) users)
     )
   ))
