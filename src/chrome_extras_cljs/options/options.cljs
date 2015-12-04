(ns chrome-extras-cljs.options.options
  (:require [chrome-extras-cljs.background.events :refer [constants]]
            [chrome-extras-cljs.background.utils :refer [stringify logging]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn- user-feedback
  [message]
  (fn []
    (let [user-feedback-elem (.getElementById js/document "user-feedback")
          last-error (.-lastError js/chrome.runtime)]
      (if (nil? last-error)
        (do
          (set! (.-textContent user-feedback-elem) message)
          (.setTimeout js/window #(set! (.-textContent user-feedback-elem) "") 750))
        (set! (.-textContent user-feedback-elem) last-error)))))


(defn- restore-options
  []
  (.get js/chrome.storage.sync
        (clj->js ["history" "historyItems"])
        (fn [items]
          (set! (.-checked (.getElementById js/document "history"))
                (aget items "history"))
          (let [history-items (aget items "historyItems")
                doc js/document
                ul (.createElement doc "ul")
                dom-saved-items (.getElementById doc "saved-items")
                li-items (map (fn [item]
                                (let [li (.createElement doc "li")]
                                  (.appendChild li (.createTextNode doc item))
                                  li))
                              history-items)]
            (logging "li-items" (stringify li-items))
            (doseq [li-item li-items]
              (.appendChild ul li-item))
            (.appendChild dom-saved-items ul)))))

(defn- save-options
  []
  (.set js/chrome.storage.sync
        #js {:history (-> js/document
                          (.getElementById "history")
                          (.-checked))}
        (user-feedback "Options saved")))

(defn- clear-history
  []
  (.remove js/chrome.storage.sync
           #js ["history" "historyItems"]
           (user-feedback "History removed"))
  (let [doc js/document]
    (-> doc
        (.getElementById "history")
        (.-checked)
        (set! false))
    (-> doc
        (.getElementById "history")
        (.-innerHTML)
        (set! ""))))

(defn widget [data]
  (reify
    om/IRender
    (render [_]
      (let [classes {:button "btn btn-default"}]
        (html [:div {:class "main"}
               ;; Tabs in the header of the HTML.
               [:ul {:class "nav nav-tabs" :role "tablist"}
                [:li {:role "presentation" :class "active"}
                 [:a {:href "#home-tab" :aria-controls "home-tab" :role "tab" :data-toggle "tab"} "Home"]]
                [:li {:role "presentation"}
                 [:a {:href "#history-tab" :aria-controls "history-tab" :role "tab" :data-toggle "tab"} "History"]]
                [:li {:role "presentation"}
                 [:a {:href "#danger-tab" :aria-controls "danger-tab" :role "tab" :data-toggle "tab"} "Danger"]]]

               ;; Div with the content of the tabs.
               [:div {:class "tab-content"}
                [:div {:role "tabpanel" :class "tab-pane active" :id "home-tab"}
                 [:p "Chrome Extras!"]
                 [:div
                  [:p "What can you do with this?"]
                  [:ul
                   [:li "Search things in Google Maps by selecting the text, right clicking it and choosing
                         'Open in Google Maps'"]
                   [:li "Check the history of all searches you did with the extension"]
                   [:li "Choose whether you want to keep your history saved"]
                   [:li "Clear all your history"]
                   [:li "Detach the current tab from the current window just by pressing 'alt+shift+d'
                         and attach it back in the same position pressing the shortcut again"]]]]
                [:div {:role "tabpanel" :class "tab-pane" :id "history-tab"}
                 [:p "Extension options:"]
                 [:div {:class "input-group"}
                  [:span
                   [:input {:type "checkbox" :id "history"} "Save history of searches"]]
                  [:p
                   [:button {:id "save" :class (:button classes)} "Save"]]]
                 [:div
                  [:p "Saved items"]
                  [:div {:id "saved-items"}]]]
                [:div {:role "tabpanel" :class "tab-pane" :id "danger-tab"}
                 [:div
                  [:p "Danger zone!"]
                  [:button {:id "clear-history" :class (:button classes)} "Clear all history"]]]]

               ;; Show user feedback here.
               [:div {:id "user-feedback"}]])))))

(om/root
  widget
  {}
  {:target (. js/document (getElementById "app"))})

(let [doc js/document]
  (.addEventListener doc "DOMContentLoaded" restore-options)
  (-> doc
      (.getElementById "save")
      (.addEventListener "click" save-options))
  (-> doc
      (.getElementById "clear-history")
      (.addEventListener "click" clear-history)))


