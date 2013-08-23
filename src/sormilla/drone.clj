(ns sormilla.drone
  (:require [sormilla.drone-comm :as comm]
            [sormilla.system :refer [task] :as system]
            [sormilla.swing :as swing]))

(defn toggle-fly [status]
  (let [current-status (get-in status [:intent :intent-state] :land)
        next-status    ({:init :land :land :fly :fly :land} current-status current-status)]
    (assoc-in status [:intent :intent-state] next-status)))

(swing/add-key-listener! {:type :pressed :code swing/key-space}
  (fn [_] (swap! system/status toggle-fly)))

(swing/add-key-listener! {:type :pressed :code swing/key-esc :ctrl false}
  (fn [_] (swap! system/status assoc-in [:intent :intent-state] :emergency)))

(swing/add-key-listener! {:type :pressed :code swing/key-esc :ctrl true}
  (fn [_] (swap! system/status assoc-in [:intent :intent-state] :init)))

(swing/add-key-listener! {:type :pressed :code (int \T)}
  (fn [_] (comm/trim)))

(defn init []
  (comm/enable-navdata)
  (comm/ctrl-ack))

(defn upstream [{:keys [leap telemetry keys intent]}]
  (-> )
  #_(let [current-state   (:control-state telemetry)
        intent-state    (:intent-state intent)]
    (if-not (= current-state intent-state)
      ))
  ;(send-pcmd pitch roll yaw alt)
  )

(defn telemetry [_]
  (comm/get-nav-data)
  {:pitch 0.0
   :yaw 0.0
   :roll 0.0
   :alt 758.0
   :vel-x 0.0
   :vel-y 0.0
   :vel-z 0.0
   :control-state :init
   :battery-percent 120.0})

(comment
  
)