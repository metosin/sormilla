(ns sormilla.gui
  (:require [clojure.java.io :as io])
  (:import [java.awt Canvas Color Graphics2D Font]
           [java.awt.geom Ellipse2D$Double]
           [javax.swing JFrame JComponent]))

(set! *warn-on-reflection* true)

(def cutive (with-open [in (io/input-stream (io/resource "CutiveMono-Regular.ttf"))]
              (Font/createFont Font/TRUETYPE_FONT in)))

(defn ->safe [throttle f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable e
        (println "error:" e)
        (Thread/sleep throttle)
        nil))))

(defn make-frame [source render & {:keys [interval safe safe-throttle max-size top exit-on-close] :or {interval 50 safe-throttle 500 max-size true}}]
  (let [run (atom true)
        frame (JFrame.)
        canvas (Canvas.)
        source (if safe (->safe safe-throttle source) source)
        render (if safe (->safe safe-throttle render) render)]
    (if max-size
      (.setExtendedState frame JFrame/MAXIMIZED_BOTH)
      (.setSize frame 800 500))
    (when top
      (.setAlwaysOnTop frame true))
    (when exit-on-close
      (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE))
    (.setIgnoreRepaint frame true)
    (.setIgnoreRepaint canvas true)
    (.add frame canvas)
    (.pack frame)
    (.setVisible frame true)
    (.createBufferStrategy canvas 2)
    (let [strategy (.getBufferStrategy canvas)]
      (future
        (try
          (while (deref run)
            (let [start (System/currentTimeMillis)
                  data (source)]
              (when data
                (loop [contents-lost? true]
                  (loop [restored? true]
                    (let [g (.getDrawGraphics strategy)]
                      (render g (.getWidth canvas) (.getHeight canvas) data)
                      (.dispose g))
                    (when (.contentsRestored strategy) (recur true)))
                  (.show strategy)
                  (when (.contentsLost strategy) (recur true))))
              (let [time-left (- (System/currentTimeMillis) start interval)]
                #_(println "time-left:" time-left interval)
                (when (pos? time-left)
                  (Thread/sleep time-left)))))
          (catch Throwable e
            (println "Oh shit!" e)
            (throw e))))
      {:run run
       :frame frame})))

(defn close-frame! [f]
  (when-let [{run :run ^JFrame frame :frame} f]
    (reset! run false)
    (doto frame
      (.setVisible false)
      (.dispose))))

(comment
  
  (defn r [^Graphics2D g ^long w ^long h data]
    (.setColor g Color/DARK_GRAY)
    (.fillRect g 0 0 w h)
    (.setColor g Color/RED)
    (.drawLine g 0 0 w h)
    (.drawLine g 0 h w 0))
  
  (defn s []
    "foo")
  
  (def f (make-frame #'s #'r))
  (close-frame! f))
