(ns pyqt4-testbed.core
  (:require [cljs.core.async :as async
             :refer [<! >! timeout chan]])
  (:require-macros [cljs.core.async.macros :as async-macros
                    :refer [go go-loop do-alt alt!]]))

(enable-console-print!)

(go-loop [count 0]
  (println "First count " count)
  (<! (timeout 1000))
  (recur (inc count)))

(go-loop [count 0]
  (println "Second count " count)
  (<! (timeout 2000))
  (recur (inc count)))
