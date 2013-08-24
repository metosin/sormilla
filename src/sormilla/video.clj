(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [org.opencv.core CvType Mat MatOfByte]
           [org.opencv.highgui Highgui]
           [java.util Arrays]
           [java.awt RenderingHints]
           [java.awt.image BufferedImage]
           [java.net Socket]
           [javax.imageio ImageIO]
           [java.io FileOutputStream DataOutputStream File ByteArrayInputStream]
           [javax.swing JFrame JPanel]
           [java.awt FlowLayout])
  (:require [sormilla.bin :as bin]))

(def INBUF_SIZE 65535)
(def inbuf-int (int-array (+ INBUF_SIZE MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE)))
(def avpkt (doto (AVPacket.) (.av_init_packet)))
(def codec (H264Decoder.))
(def c (MpegEncContext/avcodec_alloc_context))
(def picture (AVFrame/avcodec_alloc_frame))

(if-not (zero? (bit-and (.capabilities codec) H264Decoder/CODEC_CAP_TRUNCATED))
  (throw (Exception. "need to configure CODEC_FLAG_TRUNCATED")))
(if (< (.avcodec_open c codec) 0)
  (throw (Exception. "Could not open codec")))

(defn to-ba-int [b]
  (doall (for [i (range 0 (count b))]
           (aset-int inbuf-int i (bit-and 0xFF (nth b i))))))

(defn convert! [got-picture]
  (.avcodec_decode_video2 c picture got-picture avpkt))

(defn get-image-icon [picture buffer]
  (let [w (.imageWidth picture)
        h (.imageHeight picture)
        image (BufferedImage. w h BufferedImage/TYPE_INT_RGB)]
    (.setRGB image 0 0 w h buffer 0 w)
    image))

(defn convert-frame [b]
  (let [got-picture (int-array [0])]
    (to-ba-int b)
    (set! (.size avpkt) (count b))
    (set! (.data_base avpkt) inbuf-int)
    (set! (.data_offset avpkt) 0)
    (if (> (convert! got-picture) 0)
      (if (first got-picture)
        (let [picture (.displayPicture (.priv_data c))
              buffer-size (* (.imageHeight picture) (.imageWidth picture))
              buffer (int-array buffer-size)]
          (FrameUtils/YUV2RGB picture buffer)
          (get-image-icon picture buffer)))
      (println "Could not decode frame"))))

(def header-size 68)
(def video-agent (agent 0))
(def vsocket (atom nil))
(def frame-number (atom 0))
(def opencv-skip-frames 20)

(declare g view)

(defn update-image [bi]
  (do
    (.drawImage g bi 10 10 view)))

(defn init-video-stream [host]
  (do
    (let [vs (Socket. host 5555)]
      (.setSoTimeout vs 5000)
      ;wakes up the socket so that it will continue to stream data
      (doto (.getOutputStream vs)
        (.write (byte-array (map bin/ubyte [1 0 0 0])))
        (.flush))
      (reset! vsocket vs))))

(doto System/err
  (.write (byte-array (map bin/ubyte [65 66 67 10])))
  (.flush))
(.write (DataOutputStream. System/err) (byte-array (map byte [1 0 0 0])))
(.flush System/err)
(defn read-from-input [size]
  (let [bv (byte-array size)]
    (.read (.getInputStream @vsocket) bv)
    bv))

(defn read-header []
  (read-from-input header-size))

(defn read-signature [in]
  (String. (byte-array (map #(nth in %1) [0 1 2 3]))))

(defn get-header [in]
  {:version              (bin/get-byte in 4)
   :codec                (bin/get-byte in 5)
   :header-size          (bin/get-short in 6)
   :payload-size         (bin/get-int in 8)
   :encoded-width        (bin/get-short in 12)
   :encoded-height       (bin/get-short in 14)
   :display-width        (bin/get-short in 16)
   :display-height       (bin/get-short in 18)
   :frame-number         (bin/get-int in 20)
   :timestamp            (bin/get-int in 24)
   :total-chunks         (bin/get-byte in 28)
   :chunk-index          (bin/get-byte in 29)
   :frame-type           (bin/get-byte in 30)
   :control              (bin/get-byte in 31)
   :stream-byte-pos-lw   (bin/get-int in 32)
   :stream-bypte-pos-uw  (bin/get-int in 36)
   :stream-id            (bin/get-short in 40)
   :total-slices         (bin/get-byte in 42)
   :slice-index          (bin/get-byte in 43)
   :header1-size         (bin/get-byte in 44)
   :header2-size         (bin/get-byte in 45)
   :advertised-size      (bin/get-int in 48)})

(defn payload-size [in]
  (:payload-size (get-header in)))

(defn read-payload [size]
  (read-from-input size))

(defn write-payload [video out]
  (.write out video))

(defn save-image [bi]
  (ImageIO/write bi "png" (File. "opencvin.png")))

(defn buf-to-mat [buf type]
  (let [itype (if (= type :gray)  CvType/CV_8UC1  CvType/CV_8UC3)
        img-b  (-> buf (.getRaster) (.getDataBuffer) (.getData))
        mat (Mat. (.getHeight buf) (.getWidth buf) itype)]
    (.put mat 0 0 img-b)
    mat))

(defn convert-buffer-image-to-mat [img type]
  (let [itype (if (= type :gray) BufferedImage/TYPE_BYTE_GRAY BufferedImage/TYPE_3BYTE_BGR)
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
    (buf-to-mat new-frame type)))

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
      (swap! frame-number inc)
      (future (update-image (process-and-return-image buff-img))))
    (catch Exception e (println (str "Error displaying frame - skipping " e)))))


(defn read-frame [host out]
  (try
    (let [vheader (read-header)]
     (if (> (count vheader) -1)
       (if (= "PaVE" (read-signature vheader))
         (do
           (let [vpayload (read-payload (payload-size vheader))]
             (when out
               (write-payload vpayload out))
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



