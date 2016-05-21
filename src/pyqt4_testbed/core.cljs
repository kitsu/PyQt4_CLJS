(ns pyqt4-testbed.core
  (:require [cljs.core.async :as async
             :refer [<! >! timeout chan take! put!]]
            [dommy.core :as dommy :refer-macros [sel sel1]])
  (:require-macros [cljs.core.async.macros :as async-macros
                    :refer [go go-loop do-alt alt!]]))

(comment go-loop [count 0]
  (js/proxy.log (str "First count " count))
  (<! (timeout 1000))
  (recur (inc count)))

(comment go-loop [count 0]
  (js/proxy.log (str "Second count " count))
  (<! (timeout 2000))
  (recur (inc count)))

(defn get-click-chan [target]
  ;; Connect an event stream to a channel, return channel
  (let [out (chan)]
    (dommy/listen! target :mousedown
                  (fn [evt]
                    (put! out evt)))
    out))

(defn main []
  ;; Setup some event input channels and start a process to watch them.
  (let [target (sel1 :#doclist)
        click-chan (get-click-chan target)]
    (go-loop [evt (<! click-chan)]
      (js/proxy.log (str "User clicked: " (.-button evt)))
      (recur (<! click-chan)))))

(dommy/listen! js/window :load main)
