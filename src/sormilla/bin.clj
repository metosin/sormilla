(ns sormilla.bin)

(set! *warn-on-reflection* true)

(defn ubyte ^Byte [v]
  (if (>= v 0x80)
    (byte (- v 0x100))
    (byte v)))

(defn uint ^Integer [v]
  (if (>= v 0x80000000)
    (int (- v 0x100000000))
    (int v)))

(defn f->i ^long [^double v]
  (let [b (java.nio.ByteBuffer/allocate 4)]
    (.put (.asFloatBuffer b) 0 v)
    (.get (.asIntBuffer b) 0)))

(defn i->f ^double [^long v]
  (let [b (java.nio.ByteBuffer/allocate 4)]
    (.put (.asIntBuffer b) 0 (uint v))
    (.get (.asFloatBuffer b) 0)))

(defn i->ba [v]
  (let [buffer (java.nio.ByteBuffer/allocate 4)]
    (doto buffer
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 24))))
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 16))))
      (.put (ubyte (bit-and 0xFF (bit-shift-right v 8))))
      (.put (ubyte (bit-and 0xFF v))))
    (.array buffer)))

(defn ba->ia [^bytes source ^ints target ^Long size]
  (doseq [i (range size)]
    (aset-int target i (bit-and 0xFF (aget source i))))
  target)

(defn get-int [buff offset]
  (bit-or
    (bit-and (nth buff offset) 0xFF)
    (bit-shift-left (bit-and (nth buff (+ offset 1)) 0xFF) 8)
    (bit-shift-left (bit-and (nth buff (+ offset 2)) 0xFF) 16)
    (bit-shift-left (bit-and (nth buff (+ offset 3)) 0xFF) 24)))

(defn get-short [buff offset]
  (bit-or
    (bit-and (nth buff offset) 0xFF)
    (bit-shift-left (bit-and (nth buff (+ offset 1)) 0xFF) 8)))

(defn get-byte [buff offset]
  (bit-and (nth buff offset) 0xFF))

(defn get-float [ba offset]
  (i->f (uint (get-int ba offset))))

(defn bit-set? [value bit]
  (not (zero? (bit-and value (bit-shift-left 1 bit)))))

(defn bits->i [bits]
  (reduce
    (fn [value bit-index] 
      (bit-or value (bit-shift-left 1 bit-index)))
    0
    bits))
