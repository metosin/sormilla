(ns sormilla.core
  (:require [sormilla.gui :as gui]
            [sormilla.leap :as leap])
  (:import [java.awt Color Graphics2D]
           [java.awt.geom Ellipse2D$Double]))

(defn normalizer [[f1 f2] [t1 t2]]
  (let [r (/ (- t2 t1) (- f2 f1))]
    (fn [v]
      (+ (* (- v f1) r) t1))))

(defn render [^Graphics2D g ^long w ^long h data]
  (.setColor g Color/DARK_GRAY)
  (.fillRect g 0 0 w h)
  (let [s (min (/ w 1000.0) (/ h 1000.0))]
    (.translate g (/ w 2.0) (/ h 2.0))
    (.scale g s s))
  (doseq [[cx cy r ^Color c] data]
    (let [d (* 2 r)
          x (- cx r)
          y (- cy r)]
      (.setColor g c)
      (.fill g (Ellipse2D$Double. (double x) (double y) (double d) (double d))))))

(def pos (normalizer  [-300.0 300.0] [-900.0 +900.0]))
(def size (normalizer [-300.0 300.0] [70.0 15.0]))
(def sphere-size (normalizer [50.0 100.0] [10.0 250.0]))

(defn ->finger [color [x y z]]
  [(pos x) (pos z) (size y) color])

(defn ->hand [color [x y z r]]
  (when (and x y z r)
    [(pos x) (pos z) (sphere-size r) color]))

(def color-left-finger Color/RED)
(def color-left-hand (Color. 255 0 0 64))
(def color-right-finger Color/GREEN)
(def color-right-hand (Color. 0 255 0 64))

(defn source [c]
  (let [[lh ls rh rs] (leap/frame c)]
    (filter identity
      (concat
        [(->hand color-left-hand ls)
         (->hand color-right-hand rs)]
        (map (partial ->finger color-left-finger) lh)
        (map (partial ->finger color-right-finger) rh)))))

(defn -main []
  (let [c (leap/connect)]
    (gui/make-frame (partial #'source c) #'render :safe true :top true)))
