(ns sormilla.leap
  (:import [com.leapmotion.leap Listener Controller Hand Frame FingerList Vector Gesture GestureList Gesture$Type Pointable]))

(set! *warn-on-reflection* true)

(defn connect []
  (Controller.))

(defn ->hand [^Hand hand]
  (when (and hand (.isValid hand))
    (let [direction  (.direction hand)
          normal     (.palmNormal hand)]
      {:finger-count  (->> hand .fingers .count)
       :pitch         (.pitch direction)
       :yaw           (.yaw direction)
       :roll          (.roll normal)})))

(defn frame [^Controller c]
  (let [f             (.frame c)
        hands         (.hands f)
        hand-count    (.count hands)
        left-hand     (when (pos? hand-count) (->hand (.rightmost hands)))
        right-hand    (when (> hand-count 1) (->hand (.leftmost hands)))]
    {:left left-hand
     :right right-hand}))

(comment
  
  (def c (connect))
  (frame c)
  
  (require '[clojure.pprint :refer [pprint]])
  
  (dotimes [n 10]
    (Thread/sleep 100)
    (pprint (frame c)))
  
  (dotimes [n 100]
    (Thread/sleep 100)
    (let [[pitch yaw roll] ((juxt :pitch :yaw :roll) (:left (frame c)))]
      (printf "%15.3f %15.3f %15.3f\n" pitch yaw roll)
      (flush)))

)
