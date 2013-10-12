(ns sormilla.leap
  (:require [metosin.system :as system]
            [sormilla.task :as task]
            [sormilla.world :refer [world]]
            [sormilla.math :as math])
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

(def controller (atom nil))

(defn get-hand []
  (some-> controller
    (deref)
    (as-> ^Controller c (when (.isConnected c) c))
    (.frame)
    (.hands)
    (.leftmost)
    (aim)))

(defn leap-task []
  (swap! world assoc :leap (get-hand)))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(def service (reify system/Service
               (start! [this config]
                 (reset! controller (Controller.))
                 (task/schedule :leap #'leap-task :interval 50)
                 config)
               (stop! [this]
                 (task/cancel :leap)
                 (when-let [c ^Controller @controller]
                   (reset! controller nil)
                   (.delete c)))))

