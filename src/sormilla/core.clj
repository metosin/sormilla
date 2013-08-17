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

(def font (.deriveFont gui/cutive (float 20.0)))

(def ^String message-fmt  "  %+5.2f    %+5.2f    %+5.2f")
(def ^String message      "r        p        y      ")

(defmacro with-trans [^Graphics2D g & body]
  `(let [t# (.getTransform ~g)]
     ~@body
     (.setTransform ~g t#)))

(defn draw-hand [^Graphics2D g w h tw th td {:keys [quality roll pitch yaw]} {:keys [norm lo]}]
  (.setColor g hud-hi-color)
  (.drawString g
    (format "  %+5.2f    %+5.2f    %+5.2f" roll pitch yaw)
    (int (- (/ w 4) (/ tw 2)))
    (int (- h td)))
  (.setColor g hud-lo-color)
  (doseq [n (range 5 (* 10 quality) 10)]
    (.fillRect g 5 n 23 8))
  (.setColor g hud-color)
  (doseq [n (range 5 50 10)]
    (.drawRect g 5 n 23 5))
  (.setColor g norm)
  (.setClip g (Rectangle. 0 0 (/ w 2) (- h th)))
  (with-trans g
    (.translate g (double (/ w 4)) (double (/ (- h th) 2)))
    (.translate g
      (double (math/lin-scale yaw -0.6 +0.6 (/ w -4) (/ w 4)))
      (double (math/lin-scale pitch -1.5 +1.5 (/ w 4) (/ w -4))))
    (.rotate g (- roll))
    (.setColor g lo)
    (.fillOval g -150 -20 300 40)
    (.setColor g norm)
    (.drawOval g -150 -20 300 40)
    (.drawLine g -1000 0 1000 0)
    (.drawLine g 0 -1000 0 1000)))

(defn render [^Graphics2D g ^long w ^long h frame]
  (.setColor g background-color)
  (.fillRect g 0 0 w h)
  (let [[left-hand right-hand] frame
        fm         (.getFontMetrics g font)
        tw         (.stringWidth fm message)
        th         (.getHeight fm)
        td         (.getDescent fm)]
    (.setRenderingHint g RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
    (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
    (.setColor g hud-color)
    (.setFont g font)
    (.drawLine g (/ w 2 ) 0 (/ w 2) h)
    (.drawLine g 0 (- h th) w (- h th))
    (.drawString g message (int (- (/ w 4) (/ tw 2))) (int (- h td)))
    (.drawString g message (int (- (* 3 (/ w 4)) (/ tw 2))) (int (- h td)))
    (.setColor g hud-lo-color)
    (doseq [x (map (fn [v] (int (* v (/ w 20.0)))) (range 20))]
      (.drawLine g x 0 x (- h th)))
    (doseq [x (map (fn [v] (int (* v (/ w 100.0)))) (range 100))]
      (.drawLine g x (- (/ (- h th) 2) 5) x (+ (/ (- h th) 2) 5)))
    (doseq [y (map (fn [v] (int (* v (/ (- h th) 10.0)))) (range 10))]
      (.drawLine g 0 y w y))
    (doseq [y (map (fn [v] (int (* v (/ (- h th) 50.0)))) (range 50))]
      (.drawLine g (- (/ w 4) 5) y (+ (/ w 4) 5) y)
      (.drawLine g (- (* 3 (/ w 4)) 5) y (+ (* 3 (/ w 4)) 5) y))
    (when left-hand (draw-hand g w h tw th td left-hand (:left hand-colors)))
    (.setClip g (Rectangle. 0 0 (inc w) (inc h)))
    (.translate g (double (/ w 2)) (double 0.0))
    (when right-hand (draw-hand g w h tw th td right-hand (:right hand-colors)))))

(def pitch-bounds (partial math/bound -0.5 0.5))
(def yaw-bounds (partial math/bound -0.4 0.4))
(def roll-bounds (partial math/bound -1.2 1.2))

(defn tune [{:keys [pitch yaw roll] :as hand}]
  (when hand
    (assoc hand
      :pitch (-> pitch math/clip-to-zero)
      :yaw (-> yaw math/clip-to-zero)
      :roll (-> roll math/clip-to-zero))))

(defn source []
 
  (let [[left-hand right-hand] (leap/frame)]
    [(tune left-hand) (tune right-hand)])
  
  #_[{:quality    3
    :pitch      0.2
    :yaw        0.2
    :roll       0.2}
   {:quality   3
    :pitch     0.0
    :yaw       0.0
    :roll     -0.2}])

(comment
  
  (def c (leap/connect))
  (def f (gui/make-frame (partial leap/frame c) render :safe true :top true :max-size false))
  (gui/close-frame! f)
  
  (def f (gui/make-frame #'dymmy-source #'render :safe true :top true :max-size false :interval 50))
  (gui/close-frame! f)

  (dotimes [n 100]
    (Thread/sleep 100)
    (printf "%15.3f\n" (-> (leap/frame c) :left :fingers first :pos (nth 1)))
    (flush))

  (defn draw-roll [^Graphics2D g hand side]
    (with-transforms g
      (.translate g (+ (yaw (:yaw hand)) (if (= side :left) -400 400)) (pitch (:pitch hand)))
      (.rotate g (double (- (:roll hand))))
      (.fill g (Ellipse2D$Double. -350 -30 750 60))))
  
  (defn draw-details [^Graphics2D g hand side]
    (let [font     (.deriveFont gui/cutive (float 30.0))
          fm       (.getFontMetrics g font)
          message  (format "r: %5.2f  p: %5.2f y: %5.2f" (:roll hand) (:pitch hand) (:yaw hand))
          w        (.stringWidth fm message)]
      (.setFont g font)
      (.setRenderingHint g RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
      (.drawString g message (- (if (= side :left) -500 500) (/ w 2)) 490)))
  

  
  (defn normalizer [[f1 f2] [t1 t2]]
  (let [r (/ (- t2 t1) (- f2 f1))]
    (fn [v]
      (+ (* (- v f1) r) t1))))

(def pos (normalizer  [-300.0 300.0] [-900.0 +900.0]))
(def finger-size (normalizer [50.0 500.0] [150.0 10.0]))
(def palm-size (normalizer [50.0 100.0] [10.0 150.0]))
(def yaw (normalizer [-0.75 +0.75] [-500 500]))
(def pitch (normalizer [-1.5 +1.5] [+500 -500]))

)
