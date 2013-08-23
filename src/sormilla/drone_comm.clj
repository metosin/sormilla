(ns sormilla.drone-comm
  (:require [clojure.string :as s])
  (:import [java.net InetAddress DatagramPacket DatagramSocket]))

(set! *warn-on-reflection* true)

;;
;; packing and unpacking data from buffers:
;;

(defn ubyte ^Byte [v]
  (if (>= v 0x80)
    (byte (- v 0x100))
    (byte v)))

(defn uint ^Integer [v]
  (if (>= v 0x80000000)
    (int (- v 0x100000000))
    (int v)))

(defn f->i ^long [^double v]
  (let [b (java.nio.ByteBuffer/allocate 4)]
    (.put (.asFloatBuffer b) 0 v)
    (.get (.asIntBuffer b) 0)))

(defn i->f ^double [^long v]
  (let [b (java.nio.ByteBuffer/allocate 4)]
    (.put (.asIntBuffer b) 0 (uint v))
    (.get (.asFloatBuffer b) 0)))

(defn i->ba [v]
  (let [buffer (java.nio.ByteBuffer/allocate 4)]
    (doto buffer
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 24))))
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 16))))
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 8))))
      (.put (ubyte (bit-and 0xFF v))))
    (.array buffer)))

(defn get-int [buff offset]
  (bit-and 0xFFFFFFFF
    (bit-or
      (bit-and (nth buff offset) 0xFF)
      (bit-shift-left (bit-and (nth buff (+ offset 1)) 0xFF) 8)
      (bit-shift-left (bit-and (nth buff (+ offset 2)) 0xFF) 16)
      (bit-shift-left (bit-and (nth buff (+ offset 3)) 0xFF) 24))))

(defn get-short [buff offset]
  (bit-and 0xFFFF (get-int buff offset)))

(defn get-float [ba offset]
  (i->f (uint (get-int ba offset))))

(defn bit-set? [value bit]
  (not (zero? (bit-and value (bit-shift-left 1 bit)))))

;;
;; AT commands:
;;

(def ^InetAddress drone-ip (InetAddress/getByName "192.168.1.1"))

(defonce command-id (atom 0))
(defonce at-socket (agent (doto (DatagramSocket.) (.setSoTimeout 1000))))

(defmulti a->s type)
(defmethod a->s String [v] (str \" v \"))
(defmethod a->s Double [v] (str (f->i v)))
(defmethod a->s Long [v] (str v))
(defmethod a->s clojure.lang.PersistentVector [v] (reduce (fn [v b] (bit-or v (bit-shift-left 1 b))) 0 v))

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
      (catch Exception e
        (println "error in send:" e)
        (.printStackTrace e)))
    s))

(defn send-at [command & args]
  (send-off at-socket send-message command args)
  nil)

(defn leds-reset []
  (send-at "AT*LED" 10 (f->i 0.1) 0))

(defn leds-active []
  (send-at "AT*LED" 0 (f->i 5.0) 0))

(defn trim []
  (send-at "AT*FTRIM"))

(defn comm-reset []
  (reset! command-id 0)
  (send-at "AT*COMWDG"))

(defn takeoff []
  (send-at "AT*REF" [9 18 20 22 24 28]))

(defn land []
  (send-at "AT*REF" [18 20 22 24 28]))

(defn enable-navdata []
  (send-at "AT*CONFIG" "general:navdata_demo" "FALSE"))

(defn ctrl-ack []
  (send-at "AT*CTRL" 0))

(defn emergency []
  (send-at "AT*REF" [8 18 20 22 24 28]))

(defn send-pcmd [pitch roll yaw alt]
  (send-at "AT*PCMD" [1 pitch roll alt yaw]))

(defn send-hover []
  (send-at "AT*PCMD" [0 0.0 0.0 0.0 0.0]))

#_(do
  (trim)
  (println "prepare....")
  (Thread/sleep 5000)
  (reset! foo true)
  (println "takeoff")
  (takeoff)
  (Thread/sleep 8000)
  (println "land")
  (land)
  (Thread/sleep 8000)
  (println "emerg")
  (emergency)
  (Thread/sleep 1000)
  (reset! foo false)
  (println "bye"))

;;
;; Dump:
;;

(defn dump [b len]
  (loop [[d & r] b
         c 0]
    (when (and c (zero? (mod c 8))) (println))
    (print (format "%02X " d))
    (when (< c len) (recur r (inc c))))
  (println))

;;
;; Nav data:
;;

(defn get-int-by-n [ba offset n]
  (get-int ba (+ offset (* n 4 ))))

(defn get-float-by-n [ba offset n]
  (get-float ba (+ offset (* n 4 ))))

(def state-masks
  [{:state-name :flying             :mask 0   :values [:landed :flying]}
   {:state-name :video              :mask 1   :values [:off :on]}
   {:state-name :vision             :mask 2   :values [:off :on]}
   {:state-name :control            :mask 3   :values [:euler-angles :angular-speed]}
   {:state-name :altitude-control   :mask 4   :values [:off :on]}
   {:state-name :user-feedback      :mask 5   :values [:off :on]}
   {:state-name :command-ack        :mask 6   :values [:none :received]}
   {:state-name :camera             :mask 7   :values [:not-ready :ready]}
   {:state-name :travelling         :mask 8   :values [:off :on]}
   {:state-name :usb                :mask 9   :values [:not-ready :ready]}
   {:state-name :demo               :mask 10  :values [:off :on]}
   {:state-name :bootstrap          :mask 11  :values [:off :on]}
   {:state-name :motors             :mask 12  :values [:ok :motor-problem]}
   {:state-name :communication      :mask 13  :values [:ok :communication-lost]}
   {:state-name :software           :mask 14  :values [:ok :software-fault]}
   {:state-name :battery            :mask 15  :values [:ok :too-low]}
   {:state-name :emergency-landing  :mask 16  :values [:off :on]}
   {:state-name :timer              :mask 17  :values [:not-elapsed :elapsed]}
   {:state-name :magneto            :mask 18  :values [:ok :needs-calibration]}
   {:state-name :angles             :mask 19  :values [:ok :out-of-range]}
   {:state-name :wind               :mask 20  :values [:ok :too-much]}
   {:state-name :ultrasound         :mask 21  :values [:ok :deaf]}
   {:state-name :cutout             :mask 22  :values [:ok :detected]}
   {:state-name :pic-version        :mask 23  :values [:bad-version :ok]}
   {:state-name :atcodec-thread     :mask 24  :values [:off :on]}
   {:state-name :navdata-thread     :mask 25  :values [:off :on]}
   {:state-name :video-thread       :mask 26  :values [:off :on]}
   {:state-name :acquisition-thread :mask 27  :values [:off :on]}
   {:state-name :ctrl-watchdog      :mask 28  :values [:ok :delay]}
   {:state-name :adc-watchdog       :mask 29  :values [:ok :delay]}
   {:state-name :com-watchdog       :mask 30  :values [:ok :problem]}
   {:state-name :emergency          :mask 31  :values [:ok :detected]}]) 

(defn parse-nav-state [state]
  (reduce
    (fn [result {state-name :state-name mask :mask [off on] :values}]
      (assoc result state-name (if (bit-set? state mask) on off)))
    {}
    state-masks))

(def control-states
  [:default
   :init
   :landed
   :flying
   :hovering
   :test
   :trans-takeoff
   :trans-gotofix
   :trans-landing
   :trans-looping])

(defn parse-control-state [ba offset]
  (control-states (bit-shift-right (get-int ba offset) 16)))

(def detection-types
  [:horizontal-deprecated
   :vertical-deprecated
   :horizontal-drone-shell
   :none-disabled
   :roundel-under-drone
   :oriented-roundel-under-drone
   :oriented-roundel-front-drone
   :stripe-ground
   :roundel-front-drone
   :stripe
   :multiple
   :cap-orange-green-front-drone
   :black-white-roundel
   :2nd-verion-shell-tag-front-drone
   :tower-side-front-camera])

(def camera-sources
  [:horizontal
   :vertical
   :vertical-hsync])

(defn parse-tag-detect [n]
  (when n
    (camera-sources (bit-shift-right n 16))))

(def detect-tag-types
  {0 :none
   6 :shell_tag_v2
   8 :black_roundel})

(def option-tags [0 :NAVDATA-DEMO-TAG])


(defn tag-type-mask [type-num]
  (bit-shift-left 1 (- type-num 1)))

(defn parse-target-tag [ba offset n]
  {:target-type           (parse-tag-detect (get-int-by-n ba offset n))
   :target-xc             (get-int-by-n ba (+ offset 16) n)
   :target-yc             (get-int-by-n ba (+ offset (* 2 16)) n)
   :target-width          (get-int-by-n ba (+ offset (* 3 16)) n)
   :target-height         (get-int-by-n ba (+ offset (* 4 16)) n)
   :target-dist           (get-int-by-n ba (+ offset (* 5 16)) n)
   :target-orient-angle   (get-float-by-n ba (+ offset (* 6 16)) n)
   :target-camera-source  (camera-sources (get-int-by-n ba (+ offset (* 7 16) 144 48) n))})

(defn parse-target-option [ba offset]
  (let [targets-num (get-int ba (+ offset 4))]
    {:targets-num  targets-num
     :targets      (vec (map (partial parse-target-tag ba (+ offset 8)) (range targets-num)))}))

(defn parse-control-state [ba offset]
  (control-states (bit-shift-right (get-int ba offset) 16)))

(defn deg->rad [v]
  (-> v (/ 180.0) (* Math/PI)))

(defn parse-demo-option [ba offset]
  {:control-state    (parse-control-state ba (+ offset 4))
   :battery-percent  (get-int ba (+ offset 8))
   :pitch            (-> (get-float ba (+ offset 12)) (/ -1000.0) deg->rad)
   :roll             (-> (get-float ba (+ offset 16)) (/ 1000.0) deg->rad)
   :yaw              (-> (get-float ba (+ offset 20)) (/ 1000.0) deg->rad)
   :alt              (double (get-int ba (+ offset 24)))
   :vel-x            (get-float ba (+ offset 28))
   :vel-y            (get-float ba (+ offset 32))
   :vel-z            (get-float ba (+ offset 26))
   :detect-cam-type  (detection-types (get-int ba (+ offset 96)))})

(defn parse-option [ba offset option-header]
  (case (int option-header)
    0   (parse-demo-option ba offset)
    16  (parse-target-option ba offset)
    nil))

(defn parse-options [ba offset options]
  (let [option-header  (get-short ba offset)
        option-size    (get-short ba (+ offset 2))
        option         (when-not (zero? option-size) (parse-option ba offset option-header))
        next-offset    (+ offset option-size)
        new-options    (merge options option)]
    (if (or (zero? option-size) (>= next-offset (count ba)))
      new-options
      (parse-options ba next-offset new-options))))

(defn parse-navdata [navdata-bytes]
  (let [header       (get-int navdata-bytes 0)
        state        (get-int navdata-bytes 4)
        seqnum       (get-int navdata-bytes 8)
        vision-flag  (= (get-int navdata-bytes 12) 1)
        pstate       (parse-nav-state state)
        options      (parse-options navdata-bytes 16 {})]
    (merge
      {:header header
       :seq-num seqnum
       :vision-flag vision-flag}
      pstate
      options)))

(defonce ^DatagramSocket nav-socket (doto (DatagramSocket. 5554) (.setSoTimeout 1000)))
(def trigger (DatagramPacket. (byte-array (map ubyte [0x01 0x00 0x00 0x00])) 4 drone-ip 5554))

(defn get-nav-data []
  (try
    (let [packet (DatagramPacket. (byte-array 1024) 1024)]
      (doto nav-socket
        (.send trigger)
        (.receive packet))
      (parse-navdata (.getData packet)))
    (catch java.net.SocketTimeoutException e
      nil)))
