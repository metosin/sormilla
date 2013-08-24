(ns sormilla.bin-test
  (:require [sormilla.bin :refer :all]
            [midje.sweet :refer :all]))

(facts ubyte
  (type (ubyte 0)) => Byte
  (ubyte 0)        => 0
  (ubyte 1)        => 1
  (ubyte 0xFF)     => -1)

(facts uint
  (type (uint 0))   => Integer
  (uint 0)          => 0
  (uint 1)          => 1
  (uint 0xFFFFFFFF) => -1)

(facts "f->i and i->f"
  ; gold file http://abstract.cs.washington.edu/~shwetak/classes/ee472/assignments/lab4/drone_api.pdf
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

(facts get-int
  (get-int (i->ba 0x00000000) 0) => 0x00000000
  (get-int (i->ba 0x00010203) 0) => 0x03020100
  (get-int (i->ba 0x55443322) 0) => 0x22334455
  (get-int (i->ba 0xFFFFFFFF) 0) => 0xFFFFFFFF
  (get-int (i->ba 0x55555555) 0) => 0x55555555
  (get-int (i->ba 0x66666666) 0) => 0x66666666
  (get-int (i->ba 0x77777777) 0) => 0x77777777
  (get-int (i->ba 0x77777777) 0) => 0x77777777
  (get-int (i->ba 0x88888888) 0) => 0x88888888
  (get-int (i->ba 0x88776655) 0) => 0x55667788)
