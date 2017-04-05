(ns clojurescript-ethereum-example.v-list
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [reagent.core :as r]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [clojurescript-ethereum-example.utils :as u]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))

(defn enquiry-component []
  (let [enquiry (subscribe [:db/enquiry])]
    (fn []
      [ui/dialog {;; :title "test dialog"
                  :open  (if-not (nil? (:open @enquiry))
                           (:open @enquiry)
                           false)
                  ;; :open false
                  :modal true}
       (:lead-text @enquiry)
       [ui/text-field {:default-value       "test" ;;(:name @new-tweet)
                       :on-change           #(dispatch [:enquiry/update (u/evt-val %)])
                       :name                "name"
                       ;; :max-length          (:max-name-length @settings)
                       :floating-label-text "Message to dealer"
                       :style               {:width "100%"}}]
       [:div {:style {:float "right"}}
        [ui/flat-button {:label        "Submit"
                         :primary      true
                         :on-touch-tap #(dispatch [:enquiry/send (:dealer @enquiry)])}]]
       [:div {:style {:float "right"}}
        [ui/flat-button {:label        "Close"
                         :primary      false
                         :on-touch-tap #(dispatch [:enquiry/close])}]]])))

(defn list-component []
  (let [cars (subscribe [:db/cars])]
    (fn []
      [row
       [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3}
        [ui/paper {:style {:padding 20 :margin-top 15}}
         [:h1 "Cars"]
         (for [{:keys [id name price image dealer]} @cars]
           [:div {:style {:margin-top 20
                          :height     120
                          :font-size  "0.8em"}
                  :key   id}
            [:img {:src    image
                   :height 110}]
            [ui/raised-button
             {:secondary    true
              :label        "Enquiry"
              :style        {:margin-top 15
                             :float      "right"}
              :on-touch-tap #(dispatch [:ui/enquiry id name price dealer])
              }]
            [:div {:style {:float         "right"
                           :padding-right 80}}
             [:h3 "CAR_ID: " id]
             [:h3 "CAR_NAME: " name]
             [:h3 "PRICE: " price]
             [:h3 "DEALER: " dealer]]
            [ui/divider]])]]])))
