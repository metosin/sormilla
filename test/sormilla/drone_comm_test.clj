(ns sormilla.drone-comm-test
  (:require [sormilla.drone-comm :refer :all]
            [midje.sweet :refer :all]
            [midje.util :refer [testable-privates]]))

(testable-privates sormilla.drone-comm make-at-command)

(facts make-at-command
  (make-at-command "foo" 2 [])          => "foo=2\r"
  (make-at-command "foo" 2 ["a"])       => "foo=2,a\r"
  (make-at-command "foo" 2 ["a" "b"])   => "foo=2,a,b\r"
  (make-at-command "foo" 2 [0.5 123])   => "foo=2,1056964608,123\r")

(facts f->i
  (let [i-f [[  0.00            0 ]
             [  0.05   1028443341 ]
             [  0.10   1036831949 ]
             [  0.20   1045220557 ]
             [  0.50   1056964608 ]
             [ -0.05  -1119040307 ]
             [ -0.10  -1110651699 ]
             [ -0.20  -1102263091 ]
             [ -0.50  -1090519040 ]]]
    (doseq [[f i] i-f]
      (f->i f) => i
      (i->f i) => (roughly f 0.000001))))


(facts i->ba
  (java.util.Arrays/equals (i->ba 0x00000000) (byte-array (map byte (repeat 4 0)))) => true
  (java.util.Arrays/equals (i->ba 0x00010203) (byte-array (map byte (range 4)))) => true)

(defn dump [a]
  (doseq [v a] (print (format "%02X " (bit-and v 0xFF))))
  (println))

(dump (i->ba 0x04030201))
(dump (byte-array (map byte (range 4))))

(facts get-int
  (get-int (i->ba 0x00000000) 0) => 0x00000000
  (get-int (i->ba 0x00010203) 0) => 0x03020100
  (get-int (i->ba 0x55443322) 0) => 0x22334455
  (get-int (i->ba 0xFFFFFFFF) 0) => 0xFFFFFFFF
  
  ;(get-int (byte-array (map byte (range 5))) 1) => 0x04030201
  ;(get-int (byte-array (map byte (range 5))) 0) => 0x03020100
  
  ;(get-int (byte-array (map byte (repeat 4 0xFF))) 0) => 0xFFFFFFFF
  )



