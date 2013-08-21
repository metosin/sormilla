(ns sormilla.leap
  (:require [sormilla.math :as math])
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

(def pitch (comp
             (math/lin-scale [-0.3 +0.3] [-100.0 +100.0])
             (math/averager 10)
             (math/clip-to-zero 0.15)
             (fn [v] (- v 0.1))))

(def yaw (comp
           (math/lin-scale [-0.4 +0.4] [-100.0 +100.0])
           (math/averager 10)
           (math/clip-to-zero 0.15)))

(def roll (comp
            -
            (math/averager 10)
            (math/clip-to-zero 0.15)))

(defn ->hand [^Hand hand]
  (let [fingers  (.fingers hand)
        aim      (-> fingers middle-finger direction)]
    (when aim
      {:quality       (.count fingers)
       :pitch         (-> aim .pitch pitch)
       :yaw           (-> aim .yaw yaw)
       :roll          (-> hand .palmNormal .roll roll)})))

(defn connect ^Controller []
  (Controller.))

(defonce ^Controller connection (connect))

(defn leap [_]
  (let [hands         (-> connection .frame .hands) 
        hand-count    (.count hands)]
    (when (pos? (.count hands)) (->hand (.leftmost hands))))
  #_{:quality 4
   :pitch 0.30
   :yaw 0.20
   :roll 0.0})
