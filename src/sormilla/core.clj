(ns sormilla.core
  (:require [sormilla.gui :as gui]
            [sormilla.leap :as leap]
            [sormilla.math :as math])
  (:import [java.awt Color Graphics2D RenderingHints Rectangle]
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
                     (Color.  192   64  64    64)
                     (Color.  192   64  64    32)
                     (Color.  192  192  64    32)
                     (Color.   64  192  64    32)
                     (Color.   64  192  64    64)])

(defmacro with-trans [^Graphics2D g & body]
  `(let [t# (.getTransform ~g)]
     ~@body
     (.setTransform ~g t#)))

(defn pitcher []
  (comp
    (math/lin-scale -0.6 +0.6 100.0 -100.0)
    (math/averager 10)
    (math/clip-to-zero 0.15)
    :pitch))

(defn yawer []
  (comp
    (math/lin-scale -0.6 +0.6 -150.0 150.0)
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
    (fn [^Graphics2D g w h hand]
      (when hand
        (.setColor g (nth quality-colors (:quality hand)))
        (doseq [n (range 5 (* 10 (:quality hand)) 10)]
          (.fillRect g 5 n 23 8))
        (.setColor g hud-color)
        (doseq [n (range 5 50 10)]
          (.drawRect g 5 n 23 5))
        (.setColor g norm)
        (.setClip g (Rectangle. 0 0 (/ w 2) h))
        (with-trans g
          (.translate g (double (/ w 4)) (double (/ h 2)))
          (.scale g (double (/ (/ w 4) 100.0)) (double (/ (/ h 2) 100.0)))
          (.translate g (double (yaw hand)) (double (pitch hand)))
          (.rotate g (roll hand))
          (.setColor g lo)
          (.fillOval g -75 -10 150 20)
          (.setColor g norm)
          (.drawOval g -75 -10 150 20)
          (.drawLine g -200 0 200 0)
          (.drawLine g 0 -200 0 200))))))

(def draw-left-hand (hand-drawer (:left hand-colors)))
(def draw-right-hand (hand-drawer (:right hand-colors)))

(defn render [^Graphics2D g ^long w ^long h frame]
  (.setColor g background-color)
  (.fillRect g 0 0 w h)
  (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
  (let [[left-hand right-hand] frame]
    (.setColor g hud-color)
    (.drawLine g (/ w 2 ) 0 (/ w 2) h)
    (.setColor g hud-lo-color)
    (doseq [x (map (fn [v] (int (* v (/ w 20.0)))) (range 20))]
      (.drawLine g x 0 x h))
    (doseq [x (map (fn [v] (int (* v (/ w 100.0)))) (range 100))]
      (.drawLine g x (- (/ h 2) 5) x (+ (/ h 2) 5)))
    (doseq [y (map (fn [v] (int (* v (/ h 10.0)))) (range 10))]
      (.drawLine g 0 y w y))
    (doseq [y (map (fn [v] (int (* v (/ h 50.0)))) (range 50))]
      (.drawLine g (- (/ w 4) 5) y (+ (/ w 4) 5) y)
      (.drawLine g (- (* 3 (/ w 4)) 5) y (+ (* 3 (/ w 4)) 5) y))
    (draw-left-hand g w h (nth frame 0))
    (.setClip g (Rectangle. 0 0 (inc w) (inc h)))
    (.translate g (double (/ w 2)) (double 0.0))
    (draw-right-hand g w h (nth frame 1))))


(defn dummy-source []
 
  [{:quality    3
    :pitch      0.0
    :yaw        0.0
    :roll       0.0}

   {:quality   0
    :pitch     0.2
    :yaw       0.2
    :roll     -0.0}]
  
  (leap/frame)
  
  )

(comment
  
  (def f (gui/make-frame #'dummy-source #'render :safe true :top true :max-size false :interval 50))
  (gui/close-frame! f)

)
