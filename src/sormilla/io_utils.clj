(ns sormilla.io-utils
  (:require [sormilla.bin :as bin])
  (:import [java.io InputStream]
           [java.net Socket InetAddress InetSocketAddress]
           [org.apache.commons.io IOUtils]))

(defn read-fully [^InputStream in ^bytes buffer ^long offset ^long size]
  (IOUtils/readFully in buffer offset size)
  buffer)

(defn skip-fully [^InputStream in ^long size]
  (IOUtils/skipFully in size))

(defn open-socket ^Socket [^InetAddress addr port]
  (try
    (doto (Socket.)
      (.setSoTimeout 2000)
      (.connect (InetSocketAddress. addr port)))
    (catch java.net.ConnectException _
      nil)))

(defn write-to-socket ^Socket [^Socket socket & data]
  (doto (.getOutputStream socket)
    (.write (byte-array (map bin/ubyte data)))
    (.flush))
  socket)

(defn open-video-socket ^Socket [^InetAddress addr]
  (when-let [socket (open-socket addr 5555)]
    (write-to-socket socket 1 0 0 0)))

(defn close-socket! [^Socket socket]
  (when socket
    (try (.close socket) (catch Exception _))))