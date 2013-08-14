(ns sormilla.core-test
  (:require [midje.sweet :refer :all]
            [sormilla.core :refer :all]))

(facts "normalizer"
  ((normalizer [0 10] [100 200])  0)  => 100
  ((normalizer [0 10] [100 200])  5)  => 150
  ((normalizer [0 10] [100 200]) 10)  => 200
  
  ((normalizer [-300.0 300.0] [-1000.0 +1000.0]) -300.0)  => -1000.0
  ((normalizer [-300.0 300.0] [-1000.0 +1000.0])    0.0)  => 0.0
  ((normalizer [-300.0 300.0] [-1000.0 +1000.0]) +300.0)  => +1000.0
  
  ((normalizer [-300.0 300.0] [50.0 5.0]) -300.0)  => 50.0
  ((normalizer [-300.0 300.0] [50.0 5.0])    0.0)  => 27.5
  ((normalizer [-300.0 300.0] [50.0 5.0]) +300.0)  =>  5.0)
