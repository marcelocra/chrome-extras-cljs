(ns chrome-extras-cljs.options.options
  (:require [chrome-extras-cljs.background.events :refer [constants]]
            [chrome-extras-cljs.background.utils :refer [stringify logging]]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [clojure.string :as s]))

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

(defn headers [{:keys [name class] :as data} owner]
  (reify
    om/IRender
    (render [this]
      (html
        (let [base-option {:role "presentation"}]
          [:li (if (not class)
                 base-option
                 (assoc base-option :class class))
           [:a
            {:href          (str "#" name "-tab")
             :aria-controls (str name "-tab")
             :role          "tab"
             :data-toggle   "tab"}
            (s/capitalize name)]])))))

(defn home-tab [data owner]
  (reify
    om/IRender
    (render [this]
      (html
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
                         and attach it back in the same position pressing the shortcut again"]]]]))))

(defn history-tab [data owner]
  (reify
    om/IRender
    (render [this]
      (html
        [:div {:role "tabpanel" :class "tab-pane" :id "history-tab"}
         [:p "Extension options:"]
         [:div {:class "input-group"}
          [:span
           [:input {:type "checkbox" :id "history"} "Save history of searches"]]
          [:p
           [:button {:id "save" :class "btn btn-default"} "Save"]]]
         [:div
          [:p "Saved items"]
          [:div {:id "saved-items"}]]]))))

(defn danger-tab [data owner]
  (reify
    om/IRender
    (render [this]
      (html
        [:div {:role "tabpanel" :class "tab-pane" :id "danger-tab"}
         [:div
          [:p "Danger zone!"]
          [:button {:id "clear-history" :class "btn btn-default"} "Clear all history"]]]))))

(defn widget [data owner]
  (reify
    om/IRender
    (render [_]
      (html
        (let [show-home true]
          [:div {:class "main"}
           ;; Tabs in the header of the HTML.
           [:ul {:class "nav nav-tabs" :role "tablist"}
            (om/build-all headers [{:name "home" :class "active"}
                                   {:name "history"}
                                   {:name "danger"}])]

           ;; Div with the content of the tabs.
           [:div {:class "tab-content"}
            (om/build home-tab {})
            (om/build history-tab {})
            (om/build danger-tab {})]

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


