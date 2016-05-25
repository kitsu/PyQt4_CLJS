(ns pyqt4-testbed.test.core
  (:require [pyqt4-testbed.core :as testbed]
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
  "###Testing row extraction from event using target & id"
  (testing "Returns correct number given row event"
    (let [r (rand-int 999)]
      (is (= (testbed/get-row-num (row-evt r)) r)
          "Row number should be number after row prefix")))
  (testing "Returns correct number given col event"
    (let [r (rand-int 999)]
      (is (= (testbed/get-row-num (col-evt r)) r)
          "Row number should be from column parent"))))

(defn input-filler
  "Generate some number of pre-events chosen from #{move leave up}."
  [size init]
  (loop [cnt 0 acc []]
    (if (< cnt size)
      (recur (inc cnt)
             (conj acc (vector (rand-nth [:mousemove
                                          :mouseleave
                                          :mouseup])
                               (+ init cnt))))
      acc)))

(defn input-drag
  "Generate events starting with down and ending with #{up leave}"
  [size init]
  (loop [cnt 1 acc [[:mousedown init]]]
    (if (< cnt (dec size))
      (recur (inc cnt)
             (conj acc (vector :mousemove (+ init cnt))))
      (conj acc (vector (rand-nth [:mouseup :mouseleave])
                        (+ init cnt))))))

(defn input-data
  "Generate random data simulating an input stream."
  [evt-num evt-size]
  (loop [num evt-num
         id 0
         events []
         drags []]
    (if (> num 0)
      (let [pre (input-filler (rand-int 4) id)
            id (+ id (count pre))
            drag (input-drag evt-size id)
            id (+ id evt-size)
            post (input-filler (rand-int 4) id)]
        ;; repeat start/end generation evt-num times
        (recur (dec num)
               (+ id (count post))
               (concat events pre drag post)
               (conj drags (conj (mapv second drag) nil))))
      [events drags])))

(dc/deftest build-test-data
  "###Testing random test data generation
   These are *meta-tests*, test to confirm the testing apparatus
   are working correctly."
  (testing "Test input-filler bounds"
    (let [pairs (input-filler 4 0)]
      (is (= (map second pairs) [0 1 2 3])
          "Event ids should be sequential starting from provided (0).")
      (is (empty? (clojure.set/difference (into #{} (map first pairs))
                                          #{:mousemove :mouseup :mouseleave}))
          "Filler events should be subset of expected."))
    (let [pairs (input-filler 4 5)]
      (is (= (map second pairs) [5 6 7 8])
          "Event ids should be sequential starting from provided (5).")
      (is (empty? (clojure.set/difference (into #{} (map first pairs))
                                          #{:mousemove :mouseup :mouseleave}))
          "Filler events should be subset of expected.")))
  (testing "Testing input-drag has correct shape"
    (let [pairs (input-drag 5 0)]
      (is (= (map second pairs) [0 1 2 3 4])
          "Event ids should be sequential starting from provided (0).")
      (is (= (first (first pairs)) :mousedown)
          "Drag must start with mousedown.")
      (is (contains? #{:mouseup :mouseleave} (first (last pairs)))
          "Drag must end with mouseup or mouseleave.")
      (is (= (set (map first (butlast (rest pairs)))) #{:mousemove})
          "Every event between first and last is a mousemove."))
    (let [pairs (input-drag 5 5)]
      (is (= (map second pairs) [5 6 7 8 9])
          "Event ids should be sequential starting from provided (5).")
      (is (= (first (first pairs)) :mousedown)
          "Drag must start with mousedown.")
      (is (contains? #{:mouseup :mouseleave} (first (last pairs)))
          "Drag must end with mouseup/mouseleave.")))
  (testing "Testing input-data is shaped correctly"
    (let [[events drags] (input-data 2 5)
          fdrag (first drags)
          sdrag (second drags)]
      (is (= (count drags) 2)
          "Produced drag count should match provided (2).")
      (is (= (map second events) (range 0 (count events)))
          "Event ids should be sequential starting from 0.")
      (is (= (first (nth events (first fdrag))) :mousedown)
          "Drag should start with mousedown.")
      (is (contains? #{:mouseup :mouseleave}
                     ;; event key of event at last non-nil index in first drag
                     (first (nth events (nth fdrag 4))))
          "Drag should end with mouseup/mouseleave.")
      (is (and (= (count fdrag) 6)
               (= (count sdrag) 6))
          "Drags length should match provided (5)."))))

(defn get-input-chan
  "Build fake mouse event stream channel."
  [data]
  (let [input-chan  (chan 12)]
    (go-loop [head (first data) tail (rest data)]
      (when head
        (>! input-chan head)
        (recur (first tail) (rest tail))))
    input-chan))

(defn consume-channel
  "Try to get a channel from source, return vector of channel's values."
  [source-chan]
  (go
    (let [ch (poll! source-chan)]
      (when ch
        (loop [val (<! ch) acc []]
          (if val
            (recur (<! ch) (conj acc val))
            (conj acc val)))))))

(dc/deftest build-drag-chan
  "###Testing drag event grouping"
  (testing "Drag event stream behaves correctly"
    (async done
           (go (let [[events expected-drags] (input-data 2 5)
                     drag-chan (testbed/build-drag-chan (get-input-chan events))
                     drag1 (<! (consume-channel drag-chan))
                     drag2 (<! (consume-channel drag-chan))
                     drag3 (<! (consume-channel drag-chan))]
                 (is (= drag1 (first expected-drags))
                     "First set of drag events should match expected.")
                 (is (= drag2 (second expected-drags))
                     "First set of drag events should match expected.")
                 (is (= drag3 nil)
                     "There should be no more drag events.")
                 (done))))))

