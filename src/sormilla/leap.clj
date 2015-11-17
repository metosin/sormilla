(ns sormilla.leap
  (:require [sormilla.task :as task]
            [sormilla.math :as math]
            [com.stuartsierra.component :as component])
  (:import [com.leapmotion.leap Controller Hand Finger FingerList Vector]))

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
             (math/averager 5)
             (math/clip-to-zero 0.15)))

(def yaw (comp
           (math/lin-scale [-0.4 +0.4] [-100.0 +100.0])
           (math/averager 5)
           (math/clip-to-zero 0.15)))

(def roll (comp
            -
            (partial * 0.8)
            (math/averager 5)
            (math/clip-to-zero 0.15)))

(defn aim [^Hand hand]
  (let [fingers  (.fingers hand)
        dir      (-> fingers middle-finger direction)]
    (when dir
      {:quality       (.count fingers)
       :pitch         (-> dir .pitch pitch)
       :yaw           (-> dir .yaw yaw)
       :roll          (-> hand .palmNormal .roll roll)})))

(defn get-hand [^Controller controller]
  (some-> controller
    (as-> ^Controller c (when (.isConnected c) c))
    (.frame)
    (.hands)
    (.leftmost)
    (aim)))

(defn leap-task [world controller]
  (swap! world assoc :leap (get-hand controller)))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(defrecord Leap [world controller task]
  component/Lifecycle
  (start [this]
    (if-not controller
      (let [controller (Controller.)]
        (task/schedule task :leap 50 leap-task (:state world) controller)
        (assoc this :controller controller))
      this))
  (stop [this]
    (task/cancel task :leap)
    (if controller
      (.delete controller))
    (assoc this :controller nil)))

(defn create []
  (map->Leap {}))
