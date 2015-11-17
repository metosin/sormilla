(ns sormilla.drone
  (:require [sormilla.task :as task]
            [sormilla.drone-comm :as comm]
            [sormilla.drone-navdata :as navdata]
            [sormilla.math :as math]
            [sormilla.swing :as swing]
            [com.stuartsierra.component :as component]))

(defn toggle-fly [status]
  (let [current-status (get-in status [:intent :intent-state] :init)
        next-status    ({:init :landed :landed :hovering :hovering :landed} current-status current-status)]
    (assoc-in status [:intent :intent-state] next-status)))

(defn init-drone []
  (comm/send-commands! [comm/comm-reset
                        comm/trim
                        comm/enable-navdata
                        comm/ctrl-ack
                        comm/land
                        comm/leds-active]))

(defn init-keybindings [swing world]
  (swing/add-key-listener! swing {:type :pressed :code swing/key-space}
                           (fn [_] (swap! world toggle-fly)))

  (swing/add-key-listener! swing {:type :pressed :code swing/key-esc :ctrl false}
                           (fn [_] (swap! world assoc-in [:intent :intent-state] :emergency)))

  (swing/add-key-listener! swing {:type :pressed :code swing/key-esc :ctrl true}
                           (fn [_] (swap! world assoc-in [:intent :intent-state] :init)))

  (swing/add-key-listener! swing {:type :pressed :code (int \T)}
                           (fn [_] (comm/send-commands! [comm/trim])))

  (swing/add-key-listener! swing {:type :pressed :code (int \L)}
                           (fn [_] (comm/send-commands! [comm/leds-active])))

  (swing/add-key-listener! swing {:type :released :code (int \L)}
                           (fn [_] (comm/send-commands! [comm/leds-reset])))

  (swing/add-key-listener! swing {:type :pressed :code (int \I)}
                           (fn [_] (init-drone))))

;                    user        drone    command:
(def state-commands [:emergency  :any     comm/emergency
                     :landed     :any     comm/land
                     :hovering   :landed  comm/takeoff])

(defn control-state-command [{{control-state :control-state} :telemetry {intent-state :intent-state} :intent}]
  (some
    (fn [[u d c]] (if (and (= u intent-state) (or (= d :any) (= d control-state))) c))
    (partition 3 state-commands)))

(def key-dir {[false false]    0
              [false true]     1
              [true  false]   -1
              [true  true]     0})

(defn speed [s key1 key2 keys]
  (* s (key-dir [(key1 keys false) (key2 keys false)])))

(def yaw-speed 0.55)
(def alt-speed 0.55)

(def yaw (partial speed yaw-speed :left :right))
(def alt (partial speed alt-speed :down :up))

(def pitch (comp
             (math/bound -0.8 +0.8)
             (math/lin-scale [-100.0 +100.0] [-0.7 +0.7])))

(def roll (comp
            (math/bound -0.8 +0.8)
            (math/lin-scale [-0.6 +0.6] [-0.8 +0.8])))

(defn roughly-zero? [v]
  (< -1e-3 v 1e-3))

(defn move-command [{:keys [keys leap]}]
  (let [p (pitch (:pitch leap 0.0))
        r (roll (:roll leap 0.0))
        y (yaw keys)
        a (alt keys)]
    (if (every? roughly-zero? [p r y a])
      comm/hover
      (comm/move p r y a))))

(defn upstream [world]
  (let [w @world]
    (when-let [command (or (control-state-command w) (move-command w))]
      (comm/send-commands! [command]))))

(defn telemetry [world nav-socket]
  (println nav-socket)
  (swap! world assoc :telemetry (navdata/get-nav-data nav-socket))
  #_(swap! world assoc :telemetry {:pitch            0.1
                                  :yaw              0.0
                                  :roll            -0.4
                                  :alt           1358.0
                                  :vel-x            0.0
                                  :vel-y            0.0
                                  :vel-z            0.0
                                  :control-state    :landed
                                  :battery-percent  25.1}))

(defrecord Drone [task world swing navdata]
  component/Lifecycle
  (start [this]
    (println "x" navdata)
    (init-keybindings swing (:state world))
    (task/schedule task :upstream 30 #'upstream (:state world))
    (task/schedule task :telemetry 60 #'telemetry (:state world) (:nav-socket navdata))
    this)
  (stop [this]
    (task/cancel task :upstream)
    (task/cancel task :telemetry)
    this))

(defn create []
  (map->Drone {}))
