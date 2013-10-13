(ns sormilla.drone-comm
  (:require [sormilla.bin :refer :all]
            [sormilla.at-command :as at]
            [clojure.string :as s])
  (:import [java.net InetAddress DatagramPacket DatagramSocket]))


;;
;; Drone commands:
;;

(def leds-reset        [:led 10 0.1 0])
(def leds-active       [:led 0 5.0 0])
(def trim              [:trim])
(def takeoff           [:ref [9 18 20 22 24 28]])
(def land              [:ref [  18 20 22 24 28]])
(def emergency         [:ref [8 18 20 22 24 28]])
(def enable-navdata    [:config "general:navdata_demo" false])
(def ctrl-ack          [:ctrl 0])
(def hover             [:pcmd 0 0.0 0.0 0.0 0.0])
(def comm-reset        [:comwdg])
(def video-to-usb-on   [:config "video:video_on_usb" true])
(def video-to-usb-off  [:config "video:video_on_usb" false])

(defn move [pitch roll yaw alt] [:pcmd 1 roll pitch alt yaw])

(defonce command-id (atom 0))

(defn add-next-command-id [command]
  (cons (swap! command-id inc) command))

(def ^InetAddress drone-ip (InetAddress/getByName "192.168.1.1"))
(defonce at-socket (agent (doto (DatagramSocket.) (.setSoTimeout 1000)) :error-mode :continue))

(defn send-packet [^DatagramSocket s ^DatagramPacket packet]
  (.send s packet)
  s)

(defn commands->packet [commands]
  (let [buffer (->> commands (map add-next-command-id) (at/make-at-commands) (.getBytes))]
    (DatagramPacket. buffer (count buffer) drone-ip 5556)))

(defn send-commands! [commands]
  (send-off at-socket send-packet (commands->packet commands))
  nil)
