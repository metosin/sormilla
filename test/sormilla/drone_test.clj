(ns sormilla.drone-test
  (:require [sormilla.drone :refer :all]
            [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]))

(testable-privates sormilla.drone make-at-command)

(facts make-at-command
  (make-at-command "foo" 2 [])          => "foo=2\r"
  (make-at-command "foo" 2 ["a"])       => "foo=2,a\r"
  (make-at-command "foo" 2 ["a" "b"])   => "foo=2,a,b\r")