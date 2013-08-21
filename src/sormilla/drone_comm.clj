(ns sormilla.drone
  (:require [sormilla.system :refer [task]]
            [clojure.string :as s])
  (:import [java.net InetAddress DatagramPacket DatagramSocket]))

(set! *warn-on-reflection* true)

(def commands {:take-off        {:command-class "AT*REF" :command-bit-vec [9 18 20 22 24 28]}
               :land            {:command-class "AT*REF" :command-bit-vec [18 20 22 24 28]}
               :emergency       {:command-class "AT*REF" :command-bit-vec [8 18 20 22 24 28]}
               :spin-right      {:command-class "AT*PCMD" :command-vec [1 0 0 0 :v] :dir 1}
               :spin-left       {:command-class "AT*PCMD" :command-vec [1 0 0 0 :v] :dir -1}
               :up              {:command-class "AT*PCMD" :command-vec [1 0 0 :v 0] :dir 1}
               :down            {:command-class "AT*PCMD" :command-vec [1 0 0 :v 0] :dir -1}
               :tilt-back       {:command-class "AT*PCMD" :command-vec [1 0 :v 0 0] :dir 1}
               :tilt-front      {:command-class "AT*PCMD" :command-vec [1 0 :v 0 0] :dir -1}
               :tilt-right      {:command-class "AT*PCMD" :command-vec [1 :v 0 0 0] :dir 1}
               :tilt-left       {:command-class "AT*PCMD" :command-vec [1 :v 0 0 0] :dir -1}
               :hover           {:command-class "AT*PCMD" :command-vec [0 0 0 0 0] :dir 1}
               :fly             {:command-class "AT*PCMD" :command-vec [1 :v :w :x :y] :dir 1}
               :flat-trim       {:command-class "AT*FTRIM"}
               :reset-watchdog  {:command-class "AT*COMWDG"}
               :init-navdata    {:command-class "AT*CONFIG" :option "\"general:navdata_demo\"" :value "\"FALSE\""}
               :init-targeting  {:command-class "AT*CONFIG" :option "\"detect:detect_type\"" :value "\"10\""}
               :control-ack     {:command-class "AT*CTRL" :value 0}})

(def ^InetAddress drone-ip (InetAddress/getByName "192.168.1.1"))

(defonce command-id (atom 0))
(defonce at-socket (agent (doto (DatagramSocket.) (.setSoTimeout 3000))))

(defn- make-at-command [at-command id args]
  (str at-command \= id (when (seq args) (str \, (s/join \, args))) \return))

(defn send-at [at-command & args]
  (send at-socket (fn [^DatagramSocket s]
                    (let [id (swap! command-id inc)
                          command (make-at-command at-command id args)
                          buffer (.getBytes command)
                          packet (DatagramPacket. buffer (count buffer) drone-ip 5556)]
                      (try
                        (println "COMMAND:" command)
                        (.send s packet)
                        (catch Exception e (println "error:" e))))
                    s)))

(send-at "AT*LED" "0" "1056964608" "4")

(defn telemetry []
  {:pitch 0.1
   :yaw   0.2
   :roll  0.3
   :alt   120})

(defn uplink [])
(task 50 telemetry)
(task 30 uplink)
