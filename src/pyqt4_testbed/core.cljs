(ns pyqt4-testbed.core
  (:require [cljs.core.async :as async
             :refer [<! >! timeout chan take! put!
                     sliding-buffer close!]]
            [dommy.core :as dommy :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :as async-macros
                    :refer [go go-loop do-alt alt!]]))

(defn evt-chan
  "Create channel of tagged events from target element."
  [target kind btn]
  (let [out (chan (sliding-buffer 1))]
    (dommy/listen! target kind
                  (fn [evt]
                    (when (= (.-button evt) btn)
                      (put! out [kind evt]))))
    out))

(defn get-row-num
  "Extract the row number from a mouse event."
  [evt]
  (let [target (.-target evt)
        id (.-id target)
        kind (subs id 0 3)
        num (int (subs id 3))]
    (if-not (= kind "row")
      (int (subs (.-id (.-parentElement target)) 3))
      num)))

(defn get-highlight
  "Return vector of bools where true means highlighted."
  [target]
  (mapv #(dommy/has-class? % "marked") (sel target "div.row")))

(defn toggle-highlight
  "Ensure all rows have correct highlight."
  [initial start end]
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

(defn mouse-input-chan
  "Build aggregate channel of mouse events."
  [target]
  (async/merge [(evt-chan target :mousedown 0)
                (evt-chan target :mousemove 0)
                (evt-chan target :mouseup 0)
                (evt-chan target :mouseleave 0)]))

(defn build-drag-chan
  "Setup event source channels and start producing drag sub-channels."
  [input-chan]
  (let [output-chan (chan)
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
  ;; Get handle of target element, if not found exit 
  (when-let [target (sel1 :#doclist)]
    ;; Create drag channel and start mainloop
    (let [input-chan (mouse-input-chan target)
          drag-chan (build-drag-chan input-chan)]
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
       (recur (<! drag-chan))))))

(dommy/listen! js/window :load main)
