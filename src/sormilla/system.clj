(ns sormilla.system
  (:require [metosin.system :refer [defsystem]]
            [sormilla.world]
            [sormilla.task]
            [sormilla.gui]
            [sormilla.drone-navdata]
            [sormilla.drone]
            [sormilla.leap]
            [sormilla.video]))

(defsystem [sormilla.world
            sormilla.task
            sormilla.leap
            sormilla.gui
            sormilla.drone-navdata
            sormilla.drone
            sormilla.video])
