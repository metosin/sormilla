(ns sormilla.gui
  (:import [java.awt Color Graphics2D]
           [java.awt.geom Ellipse2D$Double]
           [javax.swing JFrame JComponent]))

(set! *warn-on-reflection* true)

(defn ->safe [throttle f]
  (fn [& args]
    (try
      (apply f args)
      (catch Throwable e
        (println "error:" e)
        (Thread/sleep throttle)
        nil))))

(defn make-frame [source render & {:keys [interval safe safe-throttle max-size top] :or {interval 50 safe-throttle 500 max-size true}}]
  (let [run (atom true)
        frame (JFrame.)
        source (if safe (->safe safe-throttle source) source)
        render (if safe (->safe safe-throttle render) render)]
    (if max-size
      (.setExtendedState frame JFrame/MAXIMIZED_BOTH)
      (.setSize frame 500 500))
    (when top
      (.setAlwaysOnTop frame true))
    (.setLocationByPlatform frame true)
    (.setDefaultCloseOperation frame JFrame/EXIT_ON_CLOSE)
    (.setVisible frame true)
    (.createBufferStrategy frame 2)
    (let [strategy (.getBufferStrategy frame)]
      (future
        (try
          (while (deref run)
            (when-let [data (source)]
              (loop [contents-lost? true]
                (loop [restored? true]
                  (let [g (.getDrawGraphics strategy)]
                    (render g (.getWidth frame) (.getHeight frame) data)
                    (.dispose g))
                  (when (.contentsRestored strategy) (recur true)))
                (.show strategy)
                (when (.contentsLost strategy) (recur true))))
            (Thread/sleep interval))
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
    (let [s (min (/ w 1000.0) (/ h 1000.0))]
      (.translate g (/ w 2.0) (/ h 2.0))
      (.scale g s s))
    (doseq [[cx cy r ^Color c] data]
      (let [d (* 2 r)
          x (- cx r)
          y (- cy r)]
        (.setColor g c)
        (.fill g (Ellipse2D$Double. (double x) (double y) (double d) (double d))))))
  
  (defn s []
    [[0 0 10 Color/GREEN]
     [-400 -400 50 Color/RED]
     [400 400 50 Color/BLUE]])
  
  (def f (make-frame #'s #'r))
  (close-frame! f))
