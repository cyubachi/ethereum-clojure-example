(ns clojurescript-ethereum-example.core
  (:require
   [cljs-time.extend]
   [cljsjs.material-ui]
   [cljsjs.react-flexbox-grid]
   [cljsjs.web3]
   [clojurescript-ethereum-example.handlers]
   [clojurescript-ethereum-example.h-dev]
   [clojurescript-ethereum-example.h-login]
   [clojurescript-ethereum-example.h-list]
   [clojurescript-ethereum-example.h-users]
   [clojurescript-ethereum-example.h-encrypt]
   [clojurescript-ethereum-example.subs]
   [clojurescript-ethereum-example.views :as views]
   [print.foo.preloads.devtools]
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [dirac.runtime :as dirac]
   ))

(enable-console-print!)
(dirac/install!)

(defn mount-root []
  (reagent/render [views/main-panel]
                  (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize])
  (mount-root))

;; Contracts
;; https://gist.github.com/anonymous/8ceb0933af253aa2d5c0b267eedaf0ec
