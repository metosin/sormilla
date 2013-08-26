(ns sormilla.main
  (:require [sormilla.system :refer [task] :as system]
            [sormilla.leap :as leap]
            [sormilla.drone :as drone]
            [sormilla.drone-comm :as comm]
            [sormilla.video :as video]
            [sormilla.swing :as swing]
            [sormilla.gui :as gui]
            [sormilla.video-gui :as video-gui]))

(defn -main [& args]
  (println "starting...")
  (task 60 drone/upstream)
  (task 60 drone/telemetry)
  (task 50 leap/leap)
  (swing/make-frame gui/render :exit-on-close true)
  (println "running. press any key to exit...")
  (.read (System/in))
  (println "closing...")
  (system/shutdown!)
  (System/exit 0))

(comment
  (task 100 drone/upstream)
  (task 60 drone/telemetry)
  (task 50 leap/leap)
  (swing/make-frame #'gui/render :safe true :top true)

  (system/shutdown!)
  (reset! system/status {:run true})

  (video/init-video-saving!)
  (video/init-video-streaming!)
  (swing/make-frame video-gui/render :safe true :top true)

  (comm/send-commands! [comm/video-to-usb-on])
  (comm/send-commands! [comm/video-to-usb-off])
)

"application ready"
