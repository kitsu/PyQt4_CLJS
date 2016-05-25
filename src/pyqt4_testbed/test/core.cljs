(ns pyqt4-testbed.test.core
  (:require [pyqt4-testbed.core :as testbed]
            [devcards.core :as devcards]
            [cljs.test :as t :refer [report] :include-macros true]
            [cljs.core.async :as ca
             :refer [<! >! timeout chan take! put! poll!
                     sliding-buffer close!]])
  (:require-macros [devcards.core :as dc :refer [defcard]]
                   [cljs.test :refer [is testing async]]
                   [cljs.core.async.macros :as async-macros
                    :refer [go go-loop do-alt alt!]]))

(enable-console-print!)

;; Build some fake evt->target->id hierarchies on random input
(defn row [r] (clj->js {:id (str "row" r)}))
(defn col [r] (clj->js {:id "col1" :parentElement (row r)}))
(defn row-evt [r] (clj->js {:target (row r)}))
(defn col-evt [r] (clj->js {:target (col r)}))

(dc/deftest get-row-num
  "Test row extraction from event using target & id"
  (testing "Returns correct number given row event"
    (let [r (rand-int 999)]
      (is (= (testbed/get-row-num (row-evt r)) r)
          "Row number should be number after row prefix")))
  (testing "Returns correct number given col event"
    (let [r (rand-int 999)]
      (is (= (testbed/get-row-num (col-evt r)) r)
          "Row number should be from column parent"))))

(defn get-input-chan
  "Build fake mouse event stream channel."
  []
  (let [input-chan  (chan 12)
        data [[:mousemove  0]
              [:mousemove  1]
              [:mousedown  2]
              [:mousemove  3]
              [:mousemove  4]
              [:mousemove  5]
              [:mouseleave 6]
              [:mousemove  7]
              [:mouseup    8]
              [:mousedown  9]
              [:mousemove 10]
              [:mousemove 11]
              [:mouseup   12]]]
    (go-loop [head (first data) tail (rest data)]
      (when head
        (>! input-chan head)
        (recur (first tail) (rest tail))))
    input-chan))

(def expected-drags [[2 3 4 5 6 nil] [9 10 11 12 nil]])

(defn consume-channel
  "Try to get a channel from source, return vector of channels values."
  [source-chan]
  (go
    (let [ch (poll! source-chan)]
      (when ch
        (loop [val (<! ch) acc []]
          (if val
            (recur (<! ch) (conj acc val))
            (conj acc val)))))))

(dc/deftest build-drag-chan
  "Test drag event grouping"
  (testing "Drag event stream behaves correctly"
    (async done
           (go (let [drag-chan (testbed/build-drag-chan (get-input-chan))
                     drag1 (<! (consume-channel drag-chan))
                     drag2 (<! (consume-channel drag-chan))
                     drag3 (<! (consume-channel drag-chan))]
                 (is (= drag1 (first expected-drags)))
                 (is (= drag2 (second expected-drags)))
                 (is (= drag3 nil))
                 (done))))))

(devcards.core/start-devcard-ui!)
