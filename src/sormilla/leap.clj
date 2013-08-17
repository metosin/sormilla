(ns sormilla.leap
  (:import [com.leapmotion.leap Listener Controller Hand Frame Finger FingerList Vector Gesture GestureList Gesture$Type Pointable])
  (:require [sormilla.math :refer [avg]]))

(set! *warn-on-reflection* true)

(defn ->hand [^Hand hand]
  (when (and hand (.isValid hand))
    (let [fingers     (.fingers hand)
          direction   (-> fingers .frontmost .direction)]
      {:quality       (.count fingers)
       :pitch         (-> direction .pitch double)
       :yaw           (-> direction .yaw double)
       :roll          (-> hand .palmNormal .roll double)})))

(defn connect ^Controller []
  (Controller.))

(defonce ^Controller connection (connect))

(defn frame []
  (let [hands         (-> connection .frame .hands) 
        hand-count    (.count hands)]
    [(when (> hand-count 1) (->hand (.leftmost hands)))
     (when (pos? hand-count) (->hand (.rightmost hands)))]))

(comment
  
  (:right (frame))
  
  (require '[clojure.pprint :refer [pprint]])
  
  (dotimes [n 500]
    (Thread/sleep 100)
    (when-let [hand (:right (frame))]
      (let [[quality pitch yaw roll] ((juxt :quality :pitch :yaw :roll) hand)]
        (printf "%3d: %15.3f %15.3f %15.3f\n" quality pitch yaw roll)
        (flush))))

)
