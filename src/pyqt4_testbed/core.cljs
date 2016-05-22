(ns pyqt4-testbed.core
  (:require [cljs.core.async :as async
             :refer [<! >! timeout chan take! put!
                     sliding-buffer alts!]]
            [dommy.core :as dommy :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :as async-macros
                    :refer [go go-loop do-alt alt!]]))

(defn evt-chan [target kind btn]
  (let [out (chan (sliding-buffer 1))]
    (dommy/listen! target kind
                  (fn [evt]
                    (when (= (.-button evt) btn)
                      (put! out evt))))
    out))

(defn get-row-num [evt]
  ;; Extract the row number from a mouse event
  (let [target (.-target evt)
        id (.-id target)
        kind (subs id 0 3)
        num (int (subs id 3))]
    (if-not (= kind "row")
      (int (subs (.-id (.-parentElement target)) 3))
      num)))

(defn get-highlight [target]
  ;; Return vector of bools where true means highlighted
  (mapv #(dommy/has-class? % "marked") (sel target "div.row")))

(defn toggle-highlight [initial start end]
  ;; Ensure all rows have correct highlight.
  (let [[start end] (if (> start end) [end start] [start end])]
    (dorun
     (map-indexed (fn [id high]
                    (let [row (sel1 (str "#row" id))
                          marked (dommy/has-class? row "marked")]
                      (if (and (>= id start) (<= id end))
                        ;; This row is in selected range
                        (when (= marked high)
                          ;;(js/proxy.log "In range and highlight matches - toggle")
                          (dommy/toggle-class! row "marked"))
                        ;; This row is outside selected range
                        (when-not (= marked high)
                          ;;(js/proxy.log "Out of range and highlight doesn't match - toggle")
                          (dommy/toggle-class! row "marked")))))
                  initial))))

(defn main []
  ;; Setup some event input channels and start a process to watch them.
  (let [target (sel1 :#doclist)
        lmb-down-chan (evt-chan target :mousedown 0)
        lmb-move-chan (evt-chan target :mousemove 0)
        lmb-up-chan (evt-chan target :mouseup 0)]
    ;; Wait for initial LMB down
    (go-loop [evt (<! lmb-down-chan)]
      (let [row (get-row-num evt)
            highlight (get-highlight target)]
        ;; Consume move events until we get an up event
        (loop [[evt ch] (alts! [lmb-up-chan lmb-move-chan] {:priority true})]
          ;; update highlighting...
          (toggle-highlight highlight row (get-row-num evt))
          (when-not (= ch lmb-up-chan)
            (recur (alts! [lmb-up-chan lmb-move-chan] {:priority true})))))
      ;; Wait for next LMB down
      (recur (<! lmb-down-chan)))))

(dommy/listen! js/window :load main)
