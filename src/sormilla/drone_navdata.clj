(ns sormilla.drone-navdata
  (:require [sormilla.bin :refer :all])
  (:import [java.net InetAddress DatagramPacket DatagramSocket]))

(set! *warn-on-reflection* true)

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

(defn get-int-by-n [ba offset n]
  (get-int ba (+ offset (* n 4 ))))

(defn get-float-by-n [ba offset n]
  (get-float ba (+ offset (* n 4 ))))

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

(defn parse-navdata [navdata]
  (merge
    {:header   (get-int navdata 0)
     :seq-num  (get-int navdata 8)
     :vision   (= (get-int navdata 12) 1)}
    (parse-nav-state (get-int navdata 4))
    (parse-options navdata 16 {})))

(def nav-socket (atom nil))

(def ^InetAddress drone-ip (InetAddress/getByName "192.168.1.1"))
(def trigger (DatagramPacket. (byte-array (map ubyte [1 0 0 0])) 4 drone-ip 5554))

(defn get-nav-data []
  (try
    (let [packet (DatagramPacket. (byte-array 1024) 1024)]
      (doto ^DatagramSocket @nav-socket
        (.send trigger)
        (.receive packet))
      (parse-navdata (.getData packet)))
    (catch java.net.SocketTimeoutException e
      nil)))

;;
;; ============================================================================
;; Lifecycle:
;; ============================================================================
;;

(defn stop-subsys! [config]
  (when-let [s ^DatagramSocket @nav-socket]
    (reset! nav-socket nil)
    (try (.close s) (catch java.io.IOException _)))
  config)

(defn start-subsys! [config]
  (stop-subsys! {})
  (reset! nav-socket (doto (DatagramSocket. 5554) (.setSoTimeout 1000)))
  config)
