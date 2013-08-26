(ns sormilla.video-sim
  (:import [java.net Socket ServerSocket InetSocketAddress]
           [java.io InputStream OutputStream ByteArrayInputStream]
           [org.apache.commons.io IOUtils])
  (:require [sormilla.bin :as bin]
            [clojure.java.io :as io]))

(set! *warn-on-reflection* true)

(def delay-between-frames 50) ; 20 fps
(def delay-between-frames  5) ; 200 fps

(defn read-buffer ^bytes [^InputStream in ^Long size]
  (let [buffer (byte-array size)]
    (IOUtils/readFully in buffer 0 size)
    buffer))

(defn write-buffer [^OutputStream out ^bytes buffer]
  (.write out buffer))

(defn concat-arrays ^bytes [& as]
  (let [target (byte-array (reduce + (map (fn [^bytes a] (count a)) as)))]
    (loop [offset    0
           [a & as]  as]
      (when a
        (System/arraycopy a 0 target offset (count a))
        (recur (+ offset (count a)) as)))
    target))

(defn read-frame [^InputStream in]
  (when (pos? (.available in))
    (let [header      (read-buffer in 12)
          header-size (bin/get-short header 6)
          data-size   (bin/get-int header 8)]
      (concat-arrays header (read-buffer in (- header-size 12)) (read-buffer in data-size)))))

(defn load-frames [c]
  (with-open [i (io/input-stream (io/file "sormilla-20130825-163715.h264"))]
    (doall (take c (repeatedly (partial read-frame i))))))

(defn serve-client! [^Socket s frames]
  (println "client connected from" (str (.getRemoteSocketAddress s)))
  (try
    (let [in  (io/input-stream s)
          out (io/output-stream s)]
      (read-buffer in 4)
      (doseq [frame (cycle frames)]
        (write-buffer out frame)
        (Thread/sleep delay-between-frames)))
    (catch java.net.SocketException e
      (println "client gonez"))
    (catch Throwable e
      (println "client ups" e)
      (.printStackTrace e))))

(defn start-server! [frames]
  (future
    (try
      (let [server-socket (ServerSocket. 5555)]
        (while true
          (let [s (.accept server-socket)]
            (future (serve-client! s frames)))))
      (catch Throwable e
        (println "ups" e)
        (.printStackTrace e)))))

(defn -main [& args]
  (println "video server initializing....")
  (let [frames (load-frames 1024)]
    (println "starting...")
    (start-server! frames)
    (println "ready, press any key to exit")
    (.read (System/in))
    (println "closing...")
    (System/exit 0)))
