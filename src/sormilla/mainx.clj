(ns sormilla.mainx
  )

(comment

  (:require [sormilla.system :refer [task] :as system]
            [sormilla.leap :as leap]
            [sormilla.drone :as drone]
            [sormilla.drone-comm :as comm]
            [sormilla.video :as video]
            [sormilla.swing :as swing]
            [sormilla.gui :as gui])
  
(defn -main [& args]
  (println "starting...")
  (task 60 drone/upstream)
  (task 100 drone/telemetry)
  (task 50 leap/leap)
  (video/init-video-streaming!)
  (swing/make-frame gui/render :top true)
  (println "running. press any key to exit...")
  (.read (System/in))
  (println "closing...")
  (system/shutdown!)
  (System/exit 0))

(task 60 drone/upstream)
(task 100 drone/telemetry)
(task 50 leap/leap)
(swing/make-frame #'gui/render :safe true :top true)
(video/init-video-streaming!)

(system/shutdown!)
(reset! system/status {:run true})

(comm/send-commands! [comm/leds-active])
(comm/send-commands! [comm/video-to-usb-off])

)
