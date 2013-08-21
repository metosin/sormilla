(ns sormilla.system)

(set! *warn-on-reflection* true)

(def status (atom {:run true}))

(defn run? []
  (:run @status))

(defn shutdown! []
  (swap! status assoc :run false))

(defmacro task [interval f]
  `(future
     (while (run?)
       (try
         (swap! status assoc ~(keyword (name f)) (~f @status))
         (Thread/sleep ~interval)
         (catch Throwable e#
           (println "task failure:" ~(name f))
           (.printStackTrace e#)
           (Thread/sleep 1000))))))
