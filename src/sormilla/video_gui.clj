(ns sormilla.video-gui
  (:require [sormilla.swing :as swing]
            [sormilla.video :as video])
  (:import [java.awt Image Graphics2D Color]))

(set! *warn-on-reflection* true)

(defn render [^Graphics2D g ^long w ^long h]
  (if-let [^Image image @video/image]
    (.drawImage g image 0 0 nil)
    (doto g
      (.setColor (Color/BLACK))
      (.fillRect 0 0 w h)
      (.setColor (Color/WHITE))
      (.drawString "No image" 20 20))))
