(ns sormilla.drone-comm
  (:require [clojure.string :as s])
  (:import [java.net InetAddress DatagramPacket DatagramSocket]))

(set! *warn-on-reflection* true)

;;
;; packing and unpacking data from buffers:
;;

(defn f->i ^long [^double v]
  (let [b (java.nio.ByteBuffer/allocate 4)]
    (.put (.asFloatBuffer b) 0 v)
    (.get (.asIntBuffer b) 0)))

(defn i->f ^double [^long v]
  (let [b (java.nio.ByteBuffer/allocate 4)]
    (.put (.asIntBuffer b) 0 v)
    (.get (.asFloatBuffer b) 0)))

(defn ubyte ^Byte [v]
  (if (>= v 128)
    (byte (- v 256))
    (byte v)))

; int to big-endian byte-array
(defn i->ba [v]
  (let [buffer (java.nio.ByteBuffer/allocate 4)]
    (doto buffer
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 24))))
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 16))))
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 8))))
      (.put (ubyte (bit-and 0xFF v))))
    (.array buffer)))

; big-endian int from buffer:
(defn get-int [buff offset]
  (bit-and 0xFFFFFFFF
    (bit-or
      (nth buff offset)
      (bit-shift-left (nth buff (+ offset 1)) 8)
      (bit-shift-left (nth buff (+ offset 2)) 16)
      (bit-shift-left (nth buff (+ offset 3)) 24))))

;;
;; AT commands:
;;

(def ^InetAddress drone-ip (InetAddress/getByName "192.168.1.1"))

(defonce command-id (atom 0))
(defonce at-socket (agent (doto (DatagramSocket.) (.setSoTimeout 1000))))

(defmulti a->s type)
(defmethod a->s String [v] v)
(defmethod a->s Double [v] (str (f->i v)))
(defmethod a->s Long [v] (str v))

(defn- make-at-command ^String [command id args]
  (str command \= id
    (when (seq args)
      (str \, (s/join \, (map a->s args))))
    \return))

(defn- send-message [^DatagramSocket s command args]
  (let [id (swap! command-id inc)
        buffer (.getBytes (make-at-command command id args))
        packet (DatagramPacket. buffer (count buffer) drone-ip 5556)]
    (try
      (.send s packet)
      (catch Exception e (println "error in send:" e)))))

(defn send-at [command & args]
  (send-off at-socket send-message command args))

(defn leds-reset []
  (send-at "AT*LED" 10 (f->i 1.0) 0))

(defn leds-active []
  (send-at "AT*LED" 0 (f->i 5.0) 0))

(defn trim []
  (send-at "AT*FTRIM"))

(defn comm-reset []
  (reset! command-id 0)
  (send-at "AT*COMWDG"))

(defn takeoff []
  (send-at "AT*REF=" 0x11540200))

(defn land []
  (send-at "AT*REF=" 0x11540000))

;;
;; Nav data:
;;

(defonce ^DatagramSocket nav-socket (doto (DatagramSocket. 5554) (.setSoTimeout 1000)))

(defn get-nav-data []
  (let [trigger (byte-array (map byte [0x01 0x00 0x00 0x00]))
        response (byte-array 1024)
        packet (DatagramPacket. response (count response))]
    (.send nav-socket (DatagramPacket. trigger (count trigger) drone-ip 5554))
    (.receive nav-socket packet)
    (println "P:" (.getLength packet) (.getOffset packet))
    (println (format "R: 0x%08X" (get-int response 0)))
    (if (= (get-int response 0) 0x55667788)
      {:battery (get-int response 24)
       :state (let [state (bit-shift-right (get-int response 20) 16)]
                (condp = state
                  0 :default
                  1 :init
                  2 :landed
                  3 :flying
                  4 :hovering
                  5 :test
                  6 :trans-takeoff
                  7 :trans-gotofix
                  8 :trans-landing
                  :invalid))
       :roll (/ (Float/intBitsToFloat (get-int response 32)) 1000)
       :yaw (/ (Float/intBitsToFloat (get-int response 36)) 1000)
       :alt (double (/ (get-int response 40) 1000))
       :pitch (/ (Float/intBitsToFloat (get-int response 40)) 1000)}
      (do
        (println "Navdata Parse Error")
        #_(doseq [v response]
          (print (format "%02X " v)))))))

;(get-nav-data)

#_(let [trigger-buffer (byte-array (map byte [0x01 0x00 0x00 0x00]))
      trigger-buffer-size (count trigger-buffer)
      nav-data-buffer (byte-array 1024)
      nav-data-buffer-size (count nav-data-buffer)]
  (defn- parse-nav-data []
    (let [packet (java.net.DatagramPacket. trigger-buffer
                                           trigger-buffer-size
                                           ^java.net.Inet4Address drone-ip
                                           5554)]
      (try
        (.send ^java.net.DatagramSocket nav-data-socket packet)
        (let [packet-rcv (java.net.DatagramPacket. nav-data-buffer nav-data-buffer-size)]
          (.receive ^java.net.DatagramSocket nav-data-socket packet-rcv)

          (if (= (get-int nav-data-buffer 0) 0x55667788)
            (swap! nav-data-state assoc
                   :battery (get-int nav-data-buffer 24)
                   :state (let [state (bit-shift-right (get-int nav-data-buffer 20) 16)]
                            (cond (= state 0) :default
                                  (= state 1) :init
                                  (= state 2) :landed
                                  (= state 3) :flying
                                  (= state 4) :hovering
                                  (= state 5) :test
                                  (= state 6) :trans-takeoff
                                  (= state 7) :trans-gotofix
                                  (= state 8) :trans-landing
                                  :default :invalid))
                   :roll (/ (Float/intBitsToFloat (get-int nav-data-buffer 32)) 1000)
                   :yaw (/ (Float/intBitsToFloat (get-int nav-data-buffer 36)) 1000)
                   :alt (double (/ (get-int nav-data-buffer 40) 1000))
                   :pitch (/ (Float/intBitsToFloat (get-int nav-data-buffer 40)) 1000))
            (println "Navdata Parse Error")))
        (catch Exception e (println e))))))

#_(defn nav-data-start []
  (if (= (:listening @nav-data-state) false)
    (future
      (swap! nav-data-state assoc :listening true)
      (send-at-command (str "AT*CONFIG=" (cmd-counter) ",\"general:navdata_demo\",\"TRUE\""))
      (while (:listening @nav-data-state)
        (parse-nav-data)
        (Thread/sleep 5))
      (println "Nav Data Listener Exit"))
    (println "Nav Data Listener Running!!")))
