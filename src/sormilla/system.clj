(ns sormilla.system
  (:require [metosin.system :refer [defsystem]]
            [sormilla.world]
            [sormilla.task]
            [sormilla.gui]
            [sormilla.drone-navdata]
            [sormilla.drone]
            [sormilla.leap]))

(defsystem [sormilla.world/service
            sormilla.task/service
            sormilla.gui/service
            sormilla.drone-navdata/service
            sormilla.drone/service
            sormilla.leap/service])
