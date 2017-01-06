(ns clojurescript-ethereum-example.utils
  (:require [cljs-time.coerce :refer [to-date-time to-long to-local-date-time]]
            [cljs-time.core :refer [date-time to-default-time-zone]]
            [cljs-time.format :as time-format]
            [cljs-web3.core :as web3]
            [goog.crypt.base64 :as b64]
            [cljs.reader :as reader]
            ))

(defn evt-val [e]
  (aget e "target" "value"))

(defn eth [big-num]
  (str (web3/from-wei big-num :ether) " ETH"))

(defn format-date [date]
  (time-format/unparse-local (time-format/formatters :rfc822) (to-default-time-zone (to-date-time date))))
