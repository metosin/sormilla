(ns sormilla.core
  (:require [sormilla.gui :as gui]
            [sormilla.leap :as leap])
  (:import [java.awt Color Graphics2D RenderingHints Rectangle]
           [java.awt.geom Ellipse2D$Double]))

(set! *warn-on-reflection* true)

(def background-color     (Color.   32   32  32   255))
(def hud-color            (Color.   64  192  64    92))
(def hud-hi-color         (Color.   64  255  64   192))
(def hud-lo-color         (Color.   64  192  64    32))
(def left-hand-color      (Color.   64  255  64   192))
(def left-hand-lo-color   (Color.   64  255  64    64))
(def right-hand-color     (Color.  255   64  64   192))
(def right-hand-lo-color  (Color.  255   64  64    64))

(def hand-colors {:left   {:palm     (Color.  192    0    0   16)
                           :roll     (Color.  192   32   32  192)
                           :details  (Color.  255    0    0  192)}
                  :right  {:palm     (Color.    0  192    0   16)
                           :roll     (Color.   64  192   64  192)
                           :details  (Color.    0  192    0  192)}})

(def font (.deriveFont gui/cutive (float 20.0)))

(def message-fmt  "  %+5.2f    %+5.2f    %+5.2f")
(def message      "r        p        y      ")

(defmacro with-trans [^Graphics2D g & body]
  `(let [t# (.getTransform ~g)]
     ~@body
     (.setTransform ~g t#)))

(defn normalizer [[f1 f2] [t1 t2]]
  (let [r (/ (- t2 t1) (- f2 f1))]
    (fn [v]
      (+ (* (- v f1) r) t1))))

(defn scale [v fmin fmax tmin tmax]
  (let [r (/ (- tmax tmin) (- fmax fmin))]
    (+ (* (- v fmin) r) tmin)))

(defn render [^Graphics2D g ^long w ^long h frame]
  (.setColor g background-color)
  (.fillRect g 0 0 w h)
  (let [left-hand  (:left frame)
        right-hand (:right frame)
        fm         (.getFontMetrics g font)
        tw         (.stringWidth fm message)
        th         (.getHeight fm)
        td         (.getDescent fm)]
    (.setColor g hud-color)
    (.setFont g font)
    (.setRenderingHint g RenderingHints/KEY_TEXT_ANTIALIASING RenderingHints/VALUE_TEXT_ANTIALIAS_ON)
    (.setRenderingHint g RenderingHints/KEY_ANTIALIASING RenderingHints/VALUE_ANTIALIAS_ON)
    (.drawLine g (/ w 2 ) 0 (/ w 2) h)
    (.drawLine g 0 (- h th) w (- h th))
    (.drawString g message (int (- (/ w 4) (/ tw 2))) (- h td))
    (.drawString g message (int (- (* 3 (/ w 4)) (/ tw 2))) (- h td))
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
    (when left-hand
      (.setColor g hud-hi-color)
      (.drawString g
        (format "  %+5.2f    %+5.2f    %+5.2f" (:roll left-hand) (:pitch left-hand) (:yaw left-hand))
        (int (- (/ w 4) (/ tw 2)))
        (- h td))
      (.setColor g left-hand-color)
      (.setClip g (Rectangle. 0 0 (dec (/ w 2)) (dec (- h th))))
      (with-trans g
        (.translate g (double (/ w 4)) (double (/ (- h th) 2)))
        (.translate g
          (scale (:yaw left-hand) -0.6 +0.6 (/ w -4) (/ w 4))
          (scale (:pitch left-hand) -1.5 +1.5 (/ w 4) (/ w -4)))
        (.rotate g (- (:roll left-hand)))
        (.setColor g left-hand-lo-color)
        (.fillOval g -150 -20 300 40)
        (.setColor g left-hand-color)
        (.drawOval g -150 -20 300 40)
        (.drawLine g -1000 0 1000 0)
        (.drawLine g 0 -1000 0 1000)))
    (.setColor g hud-color)
    (.setClip g (Rectangle. 0 0 (inc w) (inc h)))
    (when right-hand
      (.setColor g hud-hi-color)
      (.drawString g
        (format "  %+5.2f    %+5.2f   %+5.2f" (:roll right-hand) (:pitch right-hand) (:yaw right-hand))
        (int (- (* 3 (/ w 4)) (/ tw 2)))
        (- h td))
      (.setColor g right-hand-color)
      (.setClip g (Rectangle. (inc (/ w 2)) 0 (inc (/ w 2)) (dec (- h th))))
      (with-trans g
        (.translate g (double (* 3 (/ w 4))) (double (/ (- h th) 2)))
        (.translate g 20 -10)
        (.rotate g -0.2)
        (.setColor g right-hand-lo-color)
        (.fillOval g -150 -20 300 40)
        (.setColor g right-hand-color)
        (.drawOval g -150 -20 300 40)
        (.drawLine g -1000 0 1000 0)
        (.drawLine g 0 -1000 0 1000)))))

(defn dymmy-source []
  {:left {:finger-count    3
          :pitch           1.0
          :yaw             0.0
          :roll            0.1}
   :right {:finger-count   1
           :pitch          0.5
           :yaw            0.5
           :roll          -1.1}}
  #_(leap/frame c))

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
