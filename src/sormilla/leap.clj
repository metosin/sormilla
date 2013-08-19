(ns sormilla.leap
  (:import [com.leapmotion.leap Listener Controller Hand Frame Finger FingerList Vector Gesture GestureList Gesture$Type Pointable])
  (:require [sormilla.math :refer [avg]]))

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

(defn hand []
  (let [hands         (-> connection .frame .hands) 
        hand-count    (.count hands)]
    (when (pos? (.count hands)) (->hand (.leftmost hands)))))

(comment
  
  (hand)
  
  (require '[clojure.pprint :refer [pprint]])
  
  (dotimes [n 500]
    (Thread/sleep 100)
    (when-let [h (hand)]
      (let [[quality pitch yaw roll] ((juxt :quality :pitch :yaw :roll) h)]
        (printf "%3d: %15.3f %15.3f %15.3f\n" quality pitch yaw roll)
        (flush))))

)
