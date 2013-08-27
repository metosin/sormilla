(ns sormilla.navdata-logging
  (:require [clojure.java.io :as io]
            [clojure.string :as s]))

(set! *warn-on-reflection* true)

(defonce navdata-log (agent nil))

(defn navdata-logging! [enable?]
  (send navdata-log (if enable?
                      (constantly (io/writer (io/file "navdata.log") :append true))
                      (fn [^java.io.Writer w] (when w (.close w)) nil))))

(defn write-navdata [^java.io.Writer w navdata]
  (when w
    (.write w (format "%016X " (System/currentTimeMillis)))
    (doseq [b navdata]
      (.write w (format "%02X " (bit-and 0xFF b))))
    (.write w "\n")
    (.flush w)
    w))

(defn log [navdata]
  (send-off navdata-log write-navdata navdata)
  navdata)
