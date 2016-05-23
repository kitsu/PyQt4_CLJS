(ns pyqt4-testbed.core
  (:require [cljs.core.async :as async
             :refer [<! >! timeout chan take! put!
                     sliding-buffer close!]]
            [dommy.core :as dommy :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :as async-macros
                    :refer [go go-loop do-alt alt!]]))

(defn evt-chan [target kind btn]
  ;; The sliding buffer probably isn't needed anymore?
  (let [out (chan (sliding-buffer 1))]
    (dommy/listen! target kind
                  (fn [evt]
                    (when (= (.-button evt) btn)
                      (put! out [kind evt]))))
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

(defn build-drag-chan [target]
  ;; Setup event source channels and start producing drag sub-channels
  (let [output-chan (chan)
        input-chan (async/merge [(evt-chan target :mousedown 0)
                                 (evt-chan target :mousemove 0)
                                 (evt-chan target :mouseup 0)
                                 (evt-chan target :mouseleave 0)])
        terminals #{:mouseup :mouseleave}]
    (go-loop [[tag evt] (<! input-chan)]
      (when (= tag :mousedown)
        (let [drag-chan (chan)]
          ;; Give new drag channel to consumers
          (>! output-chan drag-chan)
          (>! drag-chan evt)
          ;; Consume move events until we get an up event
          (loop [[tag evt] (<! input-chan)]
            ;; Output events regardless of type
            (>! drag-chan evt)
            ;; Close drag on terminal event else recur
            (if (contains? terminals tag)
              (close! drag-chan)
              (recur (<! input-chan))))))
      ;; Wait for next LMB down
      (recur (<! input-chan)))
    output-chan))

(defn main []
  ;; Get handle of target element and create drag channel
  (let [target (sel1 :#doclist)
        drag-chan (build-drag-chan target)]
    ;; Loop forever pulling channels of drag events from drag-chan
    (go-loop [drag-set (<! drag-chan)]
      ;; Store the initial row and all highlight states
      (let [row (get-row-num (<! drag-set))
            highlight (get-highlight target)]
        ;; Consume events from drag-set until it is closed
        (loop [evt (<! drag-set)]
         (when evt
           ;; update highlighting...
           (toggle-highlight highlight row (get-row-num evt))
           (recur (<! drag-set)))))
      (recur (<! drag-chan)))))

(dommy/listen! js/window :load main)
