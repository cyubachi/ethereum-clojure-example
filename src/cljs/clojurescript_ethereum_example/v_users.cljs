(ns clojurescript-ethereum-example.v-users
  (:require
   [clojure.string :as str]
   [re-frame.core :refer [dispatch subscribe]]
   [reagent.core :as r]
   [cljs-react-material-ui.reagent :as ui]
   [cljs-react-material-ui.core :refer [get-mui-theme color]]
   [clojurescript-ethereum-example.utils :as u]
   [goog.string :as gstring]
   [goog.string.format]
   ))

(def col (r/adapt-react-class js/ReactFlexboxGrid.Col))
(def row (r/adapt-react-class js/ReactFlexboxGrid.Row))

(defn- mkAHref [uri x]
  [:a {:href (str uri x)
       :target "_blank"} x] )

(defn- mkUserTable [role users]
  (let [data (filter #(= (:type %) role) users)]
    [ui/table
     [ui/table-header {:adjust-for-checkbox false
                       :display-select-all false}
      (if (empty? data)
        [ui/table-row
         [ui/table-header-column {:col-span 4
                                  :style {:padding-left 0
                                          :font-size 14
                                          :text-align "center"}}
          (str "NO ITEM FOUND."
               data)]
         ]
        [ui/table-row
         [ui/table-header-column {:style {:text-align "center"}} "email"]
         [ui/table-header-column {:style {:text-align "center"}} "name"]
         [ui/table-header-column {:style {:text-align "center"}} "balance"]
         [ui/table-header-column {:style {:text-align "center"}} "address"]
         [ui/table-header-column {:style {:text-align "center"}} "paid (To RTC)"]
         ]        
        )]
     [ui/table-body {:display-row-checkbox false}
      (if (not (empty? data))
        (doall (map (fn [x]
                      [ui/table-row {:key (:address x)}
                       [ui/table-row-column ;;{:style {:font-family ["Lekton" "monospace"]}}
                        (:email x)]
                       [ui/table-row-column ;;{:style {:font-family ["Lekton" "monospace"]}}
                        (:name x)]
                       [ui/table-row-column {:style {:text-align "center"}}
                        (if (nil? (:balance x))
                          "-"
                          (gstring/format "%.8f" (:balance x)))]
                       [ui/table-row-column {:style {:font-family ["Lekton" "monospace"]}}
                        (mkAHref  "https://testnet.etherscan.io/address/" (:address x))]
                       [ui/table-row-column {:style {:text-align "center"}}
                        (if (:paid x)
                          "YES"
                          "NO") ]
                       ]
                      )
                    data))
        )]
     ]
    ))

(defn component0 []
  (let [users (subscribe [:db/users])]
    (fn []
      [row 
       [col {:xs 12 :sm 12 :md 10 :lg 6 :md-offset 1 :lg-offset 3
             :style {:margin-left "10%"
                     :max-width "80%"
                     :flex-basis "80%"} }
        [ui/paper {:style {:padding 20 :margin-top 15  :margin-bottom 15}}
         [:div "CUSTOMER"]
         (mkUserTable "customer" @users)
         [:div "DEALER"]
         (mkUserTable "dealer" @users)
         [:div "ADMIN"]
         (mkUserTable "admin" @users)

         [ui/raised-button
          {:secondary    true
           :label        "GET USERS"
           :style        {:margin-top 15 :margin-left 15}
           :on-touch-tap #(dispatch [:dev/get-users])
           }]
         
         ;; FOOTER
         [:div {:style {:overflow "auto"}}
          
          
          [:div {:style {:float "right"}} "Curious Technologies GmbH."]]
         
         ]]]
      )))
