(ns sormilla.core
  (:require [sormilla.gui :as gui]
            [sormilla.leap :as leap]
            [sormilla.math :as math])
  (:import [java.awt Color Graphics2D RenderingHints Rectangle BasicStroke]
           [java.awt.geom Ellipse2D$Double]))

(set! *warn-on-reflection* true)

(def background-color     (Color.   32   32  32   255))
(def hud-color            (Color.   64  192  64    92))
(def hud-hi-color         (Color.   64  255  64   192))
(def hud-lo-color         (Color.   64  192  64    32))
(def hand-hi              (Color.   64  255  64   192))
(def hand-lo              (Color.   64  255  64    64))

(def quality-colors [(Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  192   64  64   128)
                     (Color.   64  192  64   128)
                     (Color.   64  192  64   255)])

(def pitcher (comp
               (math/lin-scale [-0.3 +0.3] [-100.0 +100.0])
               (math/averager 10)
               (math/clip-to-zero 0.25)
               (fn [v] (- v 0.1))
               :pitch))

(def yawer (comp
             (math/lin-scale [-0.4 +0.4] [-100.0 +100.0])
             (math/averager 10)
             (math/clip-to-zero 0.15)
             :yaw))

(def roller (comp
              -
              (math/averager 10)
              (math/clip-to-zero 0.15)
              :roll))

(defn render [^Graphics2D g ^long w ^long h hand]
  (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
  (.setColor g background-color)
  (.fillRect g 0 0 w h)
  (let [w2     (/ w 2.0)
        h2     (/ h 2.0)]

    ; clear background
    (.setColor g background-color)
    (.fillRect g 0 0 w h)
    
    ; draw grid
    (.setColor g hud-lo-color)
    (doseq [x (range (/ w 10) w (/ w 10))] (.drawLine g x 0 x h))
    (doseq [y (range (/ h 10) h (/ h 10))] (.drawLine g 0 y w y))
    
    ; draw zero axis
    (.setColor g hud-color)
    (.drawLine g w2 0 w2 h)
    (.drawLine g 0 h2 w h2)
    (doseq [x (range (/ w 50) w (/ w 50))] (.drawLine g x (- h2 5) x (+ h2 5)))
    (doseq [y (range (/ h 50) h (/ h 50))] (.drawLine g (- w2 5) y (+ w2 5) y))
    
    ; draw quality boxes
    (let [quality (:quality hand 0)]
      (.setColor g (nth quality-colors quality))
      (doseq [y (range 5)]
        (when (>= y quality) (.setColor g hud-lo-color))
        (.fillRect g 5 (- h (* y 10) 10) 30 5)))
    
    ; draw "aim"
    (when hand
      (let [pitch  (* (pitcher hand) (/ h2 100.0) -1.0)
            yaw    (* (yawer hand) (/ w2 100.0))
            roll   (roller hand)
            aim-w  (/ w 2.0)
            aim-h  (/ h 10.0)
            aim-x  (/ aim-w -2.0)
            aim-y  (/ aim-h -2.0)]
        (.translate g (double (+ w2 yaw)) (double (+ h2 pitch)))
        (.rotate g roll)
        (.setColor g hand-lo)
        (.fillOval g aim-x aim-y aim-w aim-h)
        (.drawOval g aim-x aim-y aim-w aim-h)
        (.drawLine g -2000 0 2000 0)
        (.drawLine g 0 -2000 0 2000)))))

(defn dummy-source []
 
  {:quality     5
    :pitch      0.1
    :yaw        0.0
    :roll       0.3}
  
  (leap/hand)
  
  )

(comment
  
  (def f (gui/make-frame #'dummy-source #'render :safe true :top true :max-size false :interval 50))
  (gui/close-frame! f)

)
