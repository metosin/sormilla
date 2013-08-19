(ns sormilla.leap
  (:require [sormilla.system :refer [task]])
  (:import [com.leapmotion.leap Listener Controller Hand Frame Finger FingerList Vector Gesture GestureList Gesture$Type Pointable]))

(set! *warn-on-reflection* true)

(defn x [^Finger finger]
  (-> finger .tipPosition .getX))

(defn middle-finger ^Finger [^FingerList fingers]
  (when (pos? (.count fingers))
    (nth (sort-by x (seq fingers)) (/ (.count fingers) 2))))

(defn direction ^Vector [^Finger finger]
  (when finger
    (.direction finger)))

(defn ->hand [^Hand hand]
  (let [fingers  (.fingers hand)
        aim      (-> fingers middle-finger direction)]
    (when aim
      {:quality       (.count fingers)
       :pitch         (-> aim .pitch double)
       :yaw           (-> aim .yaw double)
       :roll          (-> hand .palmNormal .roll double)})))

(defn connect ^Controller []
  (Controller.))

(defonce ^Controller connection (connect))

(defn leap []
  (let [hands         (-> connection .frame .hands) 
        hand-count    (.count hands)]
    (when (pos? (.count hands)) (->hand (.leftmost hands)))
    #_{:quality 4
     :pitch 0.0
     :yaw 0.0
     :roll 0.0}))

(task 50 leap)
