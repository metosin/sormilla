(ns sormilla.drone-test
  (:require [sormilla.drone :refer :all]
            [sormilla.drone-comm :as comm]
            [midje.sweet :refer :all]))

(facts toggle-fly
  (toggle-fly {})                                  => {:intent {:intent-state :landed}}
  (toggle-fly {:intent {:intent-state :init}}  )   => {:intent {:intent-state :landed}}
  (toggle-fly {:intent {:intent-state :landed}})   => {:intent {:intent-state :hovering}}
  (toggle-fly {:intent {:intent-state :hovering}}) => {:intent {:intent-state :landed}})

(facts control-state-command
  (control-state-command
    {:intent    {:intent-state  :emergency}
     :telemetry {:control-state :foo}})
    => comm/emergency
  (control-state-command
    {:intent    {:intent-state  :landed}
     :telemetry {:control-state :foo}})
    => comm/land
  (control-state-command
    {:intent    {:intent-state  :hovering}
     :telemetry {:control-state :landed}})
    => comm/takeoff
  (control-state-command
    {:intent    {:intent-state  :hovering}
     :telemetry {:control-state :foo}})
    => nil)

(facts roughly-zero?
  (roughly-zero?  0.00000) => true
  (roughly-zero?  0.00001) => true
  (roughly-zero?  0.00010) => true
  (roughly-zero?  0.00100) => false
  (roughly-zero? -0.00001) => true
  (roughly-zero? -0.00010) => true
  (roughly-zero? -0.00100) => false)

(facts yaw
  (yaw nil)  => 0.0
  (yaw {})   => 0.0
  (yaw {:left false :right false}) => 0.0
  (yaw {:left false :right  true})  => yaw-speed
  (yaw {            :right  true})  => yaw-speed
  (yaw {:left  true :right false})  => (- yaw-speed)
  (yaw {:left  true             })  => (- yaw-speed)
  (yaw {:left  true :right  true})  => 0.0)

(facts alt
  (alt nil) => 0.0
  (alt {})  => 0.0
  (alt {:up false  :down false})  => 0.0
  (alt {:up false  :down  true})  => (- alt-speed)
  (alt {           :down  true})  => (- alt-speed)
  (alt {:up  true  :down false})  => alt-speed
  (alt {:up  true             })  => alt-speed
  (alt {:up  true  :down  true})  => 0.0)

(facts pitch
  (pitch    0.0) => (roughly  0.0)
  (pitch -100.0) => (roughly -0.7)
  (pitch  100.0) => (roughly  0.7)
  (pitch  120.0) => (roughly  0.8))

(facts roll
  (roll   0.0) => (roughly  0.0)
  (roll  -0.6) => (roughly -0.8)
  (roll   0.6) => (roughly  0.8)
  (roll   0.8) => (roughly  0.8))

(facts move-command
  (move-command nil) => comm/hover
  (move-command {})  => comm/hover
  (move-command {:keys {:right true}})            => (comm/move 0.0 0.0 yaw-speed 0.0)
  (move-command {:keys {:right true :up true}})   => (comm/move 0.0 0.0 yaw-speed alt-speed)
  (move-command {:keys {:right true :up true}
                 :leap {:pitch 100.0 :roll 0.6}}) => (comm/move 0.7 0.8 yaw-speed alt-speed)
  
  )
