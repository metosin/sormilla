(ns sormilla.math-test
  (:require [midje.sweet :refer :all]
            [sormilla.math :refer :all]))

(facts "avg"
  (avg [1.0 1.0 1.0]) => (roughly 1.0)
  (avg [1.0 2.0 3.0]) => (roughly 2.0))

(facts "lin-scale"
  (let [s (lin-scale [0.0 1.0] [10.0 20.0])]
    (s 0.0) => (roughly 10.0)
    (s 0.5) => (roughly 15.0)
    (s 1.0) => (roughly 20.0)))

(facts "bound"
  ((bound 0.0 1.0) 0.5) => (roughly 0.5)
  ((bound 0.0 1.0) -0.5) => (roughly 0.0)
  ((bound 0.0 1.0) 1.5) => (roughly 1.0))

(facts "abs"
  (abs 1.0) => (roughly 1.0)
  (abs -1.0) => (roughly 1.0))

(facts "clip-to-zero"
  (let [c (clip-to-zero 0.2)]
    (c +0.3) => (roughly +0.1)
    (c +0.2) => (roughly +0.0)
    (c +0.1) => (roughly  0.0)
    (c +0.0) => (roughly  0.0)
    (c -0.1) => (roughly  0.0)
    (c -0.2) => (roughly -0.0)
    (c -0.3) => (roughly -0.1)))

(fact "averager"
  (let [a (averager 3)]
    (a 1.0) => (roughly (avg [1.0 0.0 0.0]))
    (a 2.0) => (roughly (avg [2.0 1.0 0.0]))
    (a 3.0) => (roughly (avg [3.0 2.0 1.0]))
    (a 4.0) => (roughly (avg [4.0 3.0 2.0]))
    (a 5.0) => (roughly (avg [5.0 4.0 3.0]))))