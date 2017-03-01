(ns clojurescript-ethereum-example.v-twitter
  (:require
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [clojurescript-ethereum-example.utils :as u]))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))

(defn new-tweet-component []
  (let [abi          (subscribe [:db/contractAbi])
        caddr        (subscribe [:db/contractAddr])
        settings     (subscribe [:db/settings])
        new-tweet    (subscribe [:db/new-tweet])
        my-addresses (subscribe [:db/my-addresses])
        balance      (subscribe [:new-tweet/selected-address-balance])
        payed        (subscribe [:db/payed])
        limit        (subscribe [:db/limit])
        registered   (subscribe [:db/registered])
        login        (subscribe [:db/login])
        type         (subscribe [:db/type])]
    (fn []
      (let [is-paied (and @payed (< (.getTime (js/Date.)) (* @limit 1000)))]
        [row
         [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3}
          [ui/paper {:style {:margin-top "15px"
                             :padding    20}}
           [:h2 "Connection"]
           [ui/text-field {:value               (:name @login)
                           :disabled            true
                           :name                "Name"
                           :floating-label-text "Your name"
                           :style               {:width "100%"}}]
           [ui/text-field {:value               (if-not (nil? (:address @new-tweet))
                                                  (:address @new-tweet)
                                                  "")
                           :disabled            true
                           :name                "MyAddr"
                           :floating-label-text "Your account (address)"
                           :style               {:width "100%"}}]
           [:br]
           [:h3 "Balance: " (u/eth @balance)]
           (if (not (= @type "customer"))
             [:div
              [:h3 "Payment status: "
               (if is-paied
                 "paied."
                 [:span {:style {:color       "red"
                                 :font-weight "bold"}} "not paied."])]
              [:h3 "Register status: "
               (if @registered
                 "registered."
                 [:span {:style {:color       "red"
                                 :font-weight "bold"}} "not registered."])]])
           [ui/text-field {:default-value       @caddr
                           :on-change           #(dispatch [:ui/cAddrUpdate (u/evt-val %)])
                           :name                "ContractAddr"
                           :floating-label-text "Where is your contract at?"
                           :style               {:width "100%"}}]
           [:br]
           [ui/raised-button
            {:secondary    true
             :label        "Update addr"
             :style        {:margin-top 15}
             :on-touch-tap #(dispatch [:contract/abi-loaded @abi])}]
           (if (not (= @type "customer"))
             [:span
              (if (not @registered)
                [ui/raised-button
                 {:primary      true
                  :label        "Register dealer to blockchain"
                  :style        {:margin-top  15
                                 :margin-left 15}
                  :on-touch-tap #(dispatch [:dealer/register])
                  }])
              (if (not is-paied)
                [ui/raised-button
                 {:primary      true
                  :label        "Pay the publication fee"
                  :style        {:margin-top  15
                                 :margin-left 15}
                  :on-touch-tap #(dispatch [:publication-fee/pay])}])])]]]))))

(defn tweets-component []
  (let [tweets  (subscribe [:db/tweets])
        myaddrs (subscribe [:db/my-addresses])
        type    (subscribe [:db/type])]
    (fn []
      [row
       [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3}
        (if (not (= @type "customer"))
          (do
            [ui/paper {:style {:padding 20 :margin-top 20}}
             [:h2 "Messages"]
             (for [{:keys [tweet-key to message date from]} (filter #(not (nil? %)) @tweets)]
               [:div {:style {:margin-top 20}
                      :key   date}
                [:h5 [:i "Date: "(u/format-date date)]]
                [:h5 [:i "Tx: " from " -> " to]]
                [:div {:style {:margin-top 5
                               :word-break "break-all"}
                       :width 500}
                 message]
                [ui/divider {:style {:margin-top 5}}]])
             [ui/raised-button
              {:secondary    true
               :label        "decode msg"
               :style        {:margin-top 15}
               :on-touch-tap #(dispatch [:decrypt/messages])}]]))]])))
