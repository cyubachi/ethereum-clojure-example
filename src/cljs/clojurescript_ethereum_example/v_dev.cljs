(ns clojurescript-ethereum-example.v-dev
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [clojurescript-ethereum-example.utils :as u]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))

(defn dev-component0 []
  (let [num (subscribe [:db/tweetsNum])]
    (fn []
      [row
       [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3}
        [ui/paper {:style {:padding "0 20px 20px"}}
         [:br]
         @num
         [:br]
         [ui/raised-button
          {:secondary    true
           :label        "Get Num"
           :style        {:margin-top 15}
           :on-touch-tap #(dispatch [:tf0054/getTweetsNum])
           }]
         ]]]
      )))

(defn dev-component1 []
  (let [aaddr   (subscribe [:db/devAddr])
        aamount (subscribe [:db/devAmount])
        enc     (subscribe [:db/devEnc])
        ]
    (fn []
      [row
       [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3}
        [ui/paper {:style {:padding "0 20px 20px"}}
         [ui/text-field {:default-value       (:name @aaddr)
                         :on-change           #(dispatch [:ui/AAupdate (u/evt-val %)])
                         :name                "addr"
                         :floating-label-text "Who's amount is interested?"
                         :style               {:width "70%"}}]
         [:br]
         [:h3 "ENC/OUT: \"" @enc "\""]
         [:br]
         [ui/raised-button
          {:secondary    true
           :disabled     (empty? @aaddr)
           :label        "Get ammount"
           :style        {:margin-top 15}
           :on-touch-tap #(dispatch [:tf0054/getAmount @aaddr])
           }]
         [:br]
         [:h3 "Balance: " @aamount] 
         ]]])))

(comment
  (defn tweets-component []
    (let [tweets (subscribe [:db/tweets])]
      (fn []
        [row
         [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3}
          [ui/paper {:style {:padding 20 :margin-top 20}}
           [:h1 "Tweets"]
           (for [{:keys [tweet-key name text date author-address]} @tweets]
             [:div {:style {:margin-top 20}
                    :key   tweet-key}
              [:h3 name]
              [:h5 (u/format-date date)]
              [:div {:style {:margin-top 5}}
               text]
              [:h3 {:style {:margin "5px 0 10px"}}
               author-address]
              [ui/divider]])]]]))))
