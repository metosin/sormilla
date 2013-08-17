(ns sormilla.math-test
  (:require [midje.sweet :refer :all]
            [sormilla.math :refer :all]))

(facts "avg"
  (avg [1.0 1.0 1.0]) => (roughly 1.0)
  (avg [1.0 2.0 3.0]) => (roughly 2.0))

(facts "lin-scale"
  (lin-scale 0.5 0.0 1.0 10.0 20.0) => (roughly 15.0))

(facts "bound"
  (bound 0.0 1.0 0.5) => (roughly 0.5)
  (bound 0.0 1.0 -0.5) => (roughly 0.0)
  (bound 0.0 1.0 1.5) => (roughly 1.0))

(facts "abs"
  (abs 1.0) => (roughly 1.0)
  (abs -1.0) => (roughly 1.0))

(facts "clip-to-zero"
  (clip-to-zero +0.3) => (roughly +0.2)
  (clip-to-zero +0.2) => (roughly +0.1)
  (clip-to-zero +0.1) => (roughly  0.0)
  (clip-to-zero +0.0) => (roughly  0.0)
  (clip-to-zero -0.1) => (roughly  0.0)
  (clip-to-zero -0.2) => (roughly -0.1)
  (clip-to-zero -0.3) => (roughly -0.2))
