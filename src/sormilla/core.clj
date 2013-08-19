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

(def hand-colors {:left   {:norm     (Color.   64  255  64   192)
                           :lo       (Color.   64  255  64    64)}
                  :right  {:norm     (Color.  255   64  64   192)
                           :lo       (Color.  255   64  64    64)}})

(def quality-colors [(Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  255   64  64   192)
                     (Color.  192   64  64   128)
                     (Color.   64  192  64   128)
                     (Color.   64  192  64   255)])

(def stroke-0 (BasicStroke. 0.0))
(def stroke-1 (BasicStroke. 1.0))

(defmacro with-trans [^Graphics2D g & body]
  `(let [t# (.getTransform ~g)
         c# (.getClip ~g)]
     ~@body
     (.setTransform ~g t#)
     (.setClip ~g c#)))

(defn pitcher []
  (comp
    (math/lin-scale [-0.6 +0.6] [60.0 -60.0])
    (math/averager 10)
    (math/clip-to-zero 0.25)
    (fn [v] (- v 0.1))
    :pitch))

(defn yawer []
  (comp
    (math/lin-scale [-0.6 +0.6] [-100.0 100.0])
    (math/averager 10)
    (math/clip-to-zero 0.15)
    :yaw))

(defn roller []
  (comp
    -
    (math/averager 10)
    (math/clip-to-zero 0.15)
    :roll))

(defn hand-drawer [{:keys [norm lo]}]
  (let [pitch (pitcher)
        yaw (yawer)
        roll (roller)]
    (fn [^Graphics2D g hand]
      
      ; clear background
      (.setColor g background-color)
      (.fillRect g 0 0 100 100)
      
      ; draw grid
      (.setColor g hud-color)
      (.setStroke g stroke-0)
      (doseq [n (range -40 50 10)]
        (.drawLine g -50 n 50 n)
        (.drawLine g n -50 n 50))

      ; draw zero axis
      (.setColor g hud-hi-color)
      (.drawLine g -50 0 50 0)
      (.drawLine g 0 -50 0 50)
      
      ; draw quality boxes
      (let [quality (:quality hand 0)]
        (.setColor g (nth quality-colors quality))
        (doseq [y (range 5)]
          (when (>= y quality) (.setColor g hud-lo-color))
          (.fillRect g -48 (- 48 (* y 3)) 8 2)))

      ; draw "aim"
      (when hand
        (.translate g ^double (yaw hand) ^double (pitch hand))
        (.rotate g (roll hand))
        (.setStroke g stroke-1)
        (.setColor g lo)
        (.fillOval g -40 -6 80 12)
        (.drawOval g -40 -6 80 12)
        (.drawLine g -200 0 200 0)
        (.drawLine g 0 -200 0 200)))))

(def draw-left-hand (hand-drawer (:left hand-colors)))
(def draw-right-hand (hand-drawer (:right hand-colors)))

(defn render [^Graphics2D g ^long w ^long h frame]
  (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
  (.setColor g background-color)
  (.fillRect g 0 0 w h)
  (let [w2 (/ w 2.0)
        w4 (/ w 4.0)
        h2 (/ h 2.0)]
    (with-trans g
      (.translate g w4 h2)
      (.scale g (/ w2 100.0) (/ h 100.0))
      (.setClip g (Rectangle. -50 -50 100 100))
      (draw-left-hand g (first frame)))
    (with-trans g
      (.translate g (+ w2 w4) h2)
      (.scale g (/ w2 100.0) (/ h 100.0))
      (.setClip g (Rectangle. -50 -50 100 100))
      (draw-right-hand g (second frame)))
    (.setColor g hud-color)
    (.setStroke g (BasicStroke. 1.0))
    (.drawLine g w2 0 w2 h)))


(defn dummy-source []
 
  [{:quality    5
    :pitch      0.0
    :yaw        0.0
    :roll       0.0}

   {:quality   5
    :pitch     0.4
    :yaw       0.2
    :roll     -0.2}]
  
  (leap/frame)
  
  )

(comment
  
  (def f (gui/make-frame #'dummy-source #'render :safe true :top true :max-size false :interval 50))
  (gui/close-frame! f)

)
