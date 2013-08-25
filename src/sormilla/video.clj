(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [org.opencv.core CvType Mat MatOfByte]
           [org.opencv.highgui Highgui]
           [java.awt RenderingHints]
           [java.awt.image BufferedImage]
           [java.net Socket InetSocketAddress]
           [javax.imageio ImageIO]
           [java.io InputStream ByteArrayInputStream]
           [org.apache.commons.io IOUtils])
  (:require [sormilla.bin :as bin]
            [sormilla.drone-comm :as comm]
            [clojure.java.io :as io]))

; (set! *warn-on-reflection* true)

(def INBUF_SIZE 65535)
(def inbuf-int (int-array (+ INBUF_SIZE MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE)))
(def avpkt (doto (AVPacket.) (.av_init_packet)))
(def codec (H264Decoder.))
(def context ^MpegEncContext (MpegEncContext/avcodec_alloc_context))
(def picture (AVFrame/avcodec_alloc_frame))

(if-not (zero? (bit-and (.capabilities codec) H264Decoder/CODEC_CAP_TRUNCATED))
  (throw (Exception. "need to configure CODEC_FLAG_TRUNCATED")))
(if (< (.avcodec_open context codec) 0)
  (throw (Exception. "Could not open codec")))






(defn read-input [^InputStream in size]
  (let [b (byte-array size)]
    (IOUtils/readFully in b)
    b))

(defn skip-input [^InputStream in size]
  (.skip in size)
  in)

(defn init-video [s]
  (doto (.getOutputStream s)
    (.write (byte-array (map bin/ubyte [1 0 0 0])))
    (.flush)))

(defn capture []
  (let [socket (doto (Socket.)
                 (.setSoTimeout 2000)
                 (.connect (InetSocketAddress. comm/drone-ip 5555)))
        in (io/input-stream socket)
        out (io/output-stream (io/file "capture.h264"))]
    (try
      (init-video socket)
      (doseq [n (range 1024)]
        (.write out (read-input in 4096)))
      (.flush out)
      (.close out)
      (println "success!")
      (catch Exception e
        (println "failure" e)
        (.printStackTrace e)
        (try (.close socket) (catch Exception e))
        (try (.close out) (catch Exception e))))))

(comment
  (comm/send-commands! [(comm/video-codec :h264-360p) (comm/video-frame-rate 15) comm/leds-active])
  (capture)
  (comm/send-commands! [comm/leds-reset])
  )










(defn update-image [img]
  (ImageIO/write img "png" (io/file "image.png")))







(defn parse-header [data]
  {:sig                  (bin/get-int    data   0)
   ; :version            (bin/get-byte   data   4)
   ; :codec              (bin/get-byte   data   5)
   :header-size          (bin/get-short  data   6)
   :payload-size         (bin/get-int    data   8)})

(defn ba->ia [^bytes source ^ints target]
  (doseq [i (range (count source))]
    (aset-int target i (bit-and 0xFF (nth source i))))
  target)

(defn decode! [context picture avpkt b]
  (let [got-picture? (int-array [0])]
    (set! (.size avpkt) (count b))
    (set! (.data_base avpkt) (ba->ia b inbuf-int))
    (set! (.data_offset avpkt) 0)
    (.avcodec_decode_video2 context picture got-picture? avpkt)
    (when (zero? (first got-picture?)) (throw (Exception. "Could not decode frame")))))

(defn buffer->image [w h buffer]
  (let [image (BufferedImage. w h BufferedImage/TYPE_INT_RGB)]
    (.setRGB image 0 0 w h buffer 0 w)
    image))

(defn frame->image [frame]
  (decode! context picture avpkt frame)
  (let [p (.displayPicture (.priv_data context))
        buffer (int-array (* (.imageHeight p) (.imageWidth p)))]
    (FrameUtils/YUV2RGB p buffer)
    (buffer->image (.imageWidth p) (.imageHeight p) buffer)))

(defn parse-file []
  (let [in (io/input-stream (io/file "capture.h264"))]
    (try
      (doseq [i (range 1)]
        (let [header-data  (read-input in 12)
              header       (parse-header header-data)]
          (when-not (= (:sig header) 0x45566150) (throw (Exception. "out of sync")))
          (skip-input in (- (:header-size header) 12))
          (let [frame (read-input in (:payload-size header))
                img (frame->image frame)]
            (update-image img))))
      (println "success!")
      (catch Exception e
        (println "failure" e)
        (.printStackTrace e))
      (finally
        (try (.close in) (catch Exception e))))))

(parse-file)

#_(doseq [n (range 100)]
          (let [header-data  (read-input in 68)
                signature    (parse-signature header-data)
                header       (parse-header header-data)]
            (if (= signature "PaVE")
              (do
                (.write out header-data)
                (.write out (read-input in (:payload-size header))))
              (println "not a pave" n))))
