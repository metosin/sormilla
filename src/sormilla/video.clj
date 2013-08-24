(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [org.opencv.core CvType Mat MatOfByte]
           [org.opencv.highgui Highgui]
           [java.util Arrays]
           [java.awt RenderingHints]
           [java.awt.image BufferedImage]
           [java.net Socket InetSocketAddress]
           [javax.imageio ImageIO]
           [java.io FileOutputStream DataOutputStream File ByteArrayInputStream]
           [javax.swing JFrame JPanel]
           [java.awt FlowLayout])
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

(defn ba->ia [^bytes source ^ints target]
  (doseq [i (range (count source))]
    (aset-int target i (bit-and 0xFF (nth source i))))
  target)

(defn convert! [context picture avpkt]
  (let [got-picture? (int-array [0])]
    (.avcodec_decode_video2 context picture got-picture? avpkt)
    (not (zero? (first got-picture?)))))

(defn get-image-icon [picture buffer]
  (let [w (.imageWidth picture)
        h (.imageHeight picture)
        image (BufferedImage. w h BufferedImage/TYPE_INT_RGB)]
    (.setRGB image 0 0 w h buffer 0 w)
    image))

(defn convert-frame [b]
  (set! (.size avpkt) (count b))
  (set! (.data_base avpkt) (ba->ia b inbuf-int))
  (set! (.data_offset avpkt) 0)
  (if (convert! context picture avpkt)
    (let [p (.displayPicture (.priv_data context))
          buffer-size (* (.imageHeight p) (.imageWidth p))
          buffer (int-array buffer-size)]
      (FrameUtils/YUV2RGB p buffer)
      (get-image-icon p buffer))
    (println "Could not decode frame")))

(def socket (doto (Socket.)
              (.setSoTimeout 2000)
              (.connect (InetSocketAddress. 5555))))
(def input (io/input-stream socket))

(declare g view)

(defn update-image [bi]
  (do
    (.drawImage g bi 10 10 view)))

(defn init-video-stream []
  (doto (.getOutputStream socket)
    (.write (byte-array (map bin/ubyte [1 0 0 0])))
    (.flush)))

(defn read-from-input [in size]
  (let [ba (byte-array size)
        c (.read in ba)]
    (when-not (= c size) (throw (Exception. (str "could not read " size " bytes, got only " c " bytes"))))
    ba))

(defn read-header [in]
  (read-from-input in 68))

(defn parse-signature [data]
  (-> (StringBuilder.)
    (.append (char (nth data 0)))
    (.append (char (nth data 1)))
    (.append (char (nth data 2)))
    (.append (char (nth data 3)))
    (.toString)))

(defn parse-header [data]
  {:version              (bin/get-byte   data   4)
   :codec                (bin/get-byte   data   5)
   :header-size          (bin/get-short  data   6)
   :payload-size         (bin/get-int    data   8)
   :encoded-width        (bin/get-short  data  12)
   :encoded-height       (bin/get-short  data  14)
   :display-width        (bin/get-short  data  16)
   :display-height       (bin/get-short  data  18)
   :frame-number         (bin/get-int    data  20)
   :timestamp            (bin/get-int    data  24)
   :total-chunks         (bin/get-byte   data  28)
   :chunk-index          (bin/get-byte   data  29)
   :frame-type           (bin/get-byte   data  30)
   :control              (bin/get-byte   data  31)
   :stream-byte-pos-lw   (bin/get-int    data  32)
   :stream-bypte-pos-uw  (bin/get-int    data  36)
   :stream-id            (bin/get-short  data  40)
   :total-slices         (bin/get-byte   data  42)
   :slice-index          (bin/get-byte   data  43)
   :header1-size         (bin/get-byte   data  44)
   :header2-size         (bin/get-byte   data  45)
   :advertised-size      (bin/get-int    data  48)})

(defn read-payload [size]
  (read-from-input size))

(defn buf-to-mat [buf image-type]
  (let [itype (if (= image-type :gray) CvType/CV_8UC1 CvType/CV_8UC3)
        img-b  (-> buf .getRaster .getDataBuffer .getData)
        mat (Mat. (.getHeight buf) (.getWidth buf) itype)]
    (.put mat 0 0 img-b)
    mat))

(defn convert-buffer-image-to-mat [img image-type]
  (let [itype (if (= image-type :gray) BufferedImage/TYPE_BYTE_GRAY BufferedImage/TYPE_3BYTE_BGR)
        w (.getWidth img)
        h (.getHeight img)
        nw (/ w 2)
        nh (/ h 2)
        new-frame  (BufferedImage. nw nh itype)
        g (.getGraphics new-frame)]
    (doto g
      (.setRenderingHint RenderingHints/KEY_INTERPOLATION  RenderingHints/VALUE_INTERPOLATION_BILINEAR)
      (.drawImage img 0 0 nw nh 0 0 w h nil)
      (.dispose))
    (buf-to-mat new-frame image-type)))

(defn convert-mat-to-buffer-image [mat]
  (let [new-mat (MatOfByte.)]
    (Highgui/imencode ".png" mat new-mat)
    (ImageIO/read (ByteArrayInputStream. (.toArray new-mat)))))

(defn process-and-return-image [imgbuf]
  (convert-mat-to-buffer-image (convert-buffer-image-to-mat imgbuf :gray)))

(defn display-frame [video]
  (try
    (let [buff-img (convert-frame video)]
      (def my-img buff-img)
      (future (update-image (process-and-return-image buff-img))))
    (catch Exception e (println (str "Error displaying frame - skipping " e)))))


(defn read-frame [in]
  (try
    (let [header-data  (read-header in)
          signature    (parse-signature header-data)
          header       (parse-header header-data)]
      (if (> (count vheader) -1)
       (if (= "PaVE" )
         (do
           (let [vpayload (read-payload (:payload-size header))]
             (display-frame vpayload)))
         (do (println "not a pave")))
       (do (println "disconnected")
           (.close @vsocket)
           (init-video-stream host)
                                        ;need to wait a bit after reconnecting 
           (Thread/sleep 600))))
    (catch Exception e (println (str "Problem reading frame - skipping " e)))))

(def stream)

(defn stream-video [_ host out]
  (while @stream (do
                   (read-frame host out)
                   ;(Thread/sleep 30)
                   )))


(defn end-video []
  (reset! stream false))


(defn init-video [host]
  #_(setup-viewer)
  (init-video-stream host))

(defn start-video [host]
  (do
    (reset! stream true)
    (Thread/sleep 40)
    ;wait for the first frame
    (send video-agent stream-video host nil)))



