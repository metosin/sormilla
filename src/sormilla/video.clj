(ns sormilla.video
  (:import [com.twilight.h264.decoder AVFrame AVPacket H264Decoder MpegEncContext]
           [com.twilight.h264.player FrameUtils]
           [java.util Arrays]
           [java.awt.image BufferedImage]))

(def INBUF_SIZE 65535)
(def inbuf-int (int-array (+ INBUF_SIZE MpegEncContext/FF_INPUT_BUFFER_PADDING_SIZE)))
(def avpkt (AVPacket.))
(.av_init_packet avpkt)

(def codec (H264Decoder.))
(if (nil? codec) (println "Codec not found"))
(def c (MpegEncContext/avcodec_alloc_context))
(def picture (AVFrame/avcodec_alloc_frame))

(if-not (zero? (bit-and (.capabilities codec) H264Decoder/CODEC_CAP_TRUNCATED))
  (println "need to configure CODEC_FLAG_TRUNCATED"))

(if (< (.avcodec_open c codec) 0)
  (println "Could not open codec"))

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

(defn update-image [bi]
  (do
    (.drawImage g bi 10 10 view)))

(defn init-video-stream [host]
  (do
    (let [vs (Socket. host 5555)]
      (.setSoTimeout vs 5000)
      ;wakes up the socket so that it will continue to stream data
      (.write (DataOutputStream. (.getOutputStream vs)) (byte-array (map byte [1 0 0 0])))
      (reset! vsocket vs))))

(defn read-from-input [size]
  (let [bv (byte-array size)]
    (.read (.getInputStream @vsocket) bv)
    bv))

(defn read-header []
  (read-from-input header-size))

(defn read-signature [in]
  (String. (byte-array (map #(nth in %1) [0 1 2 3]))))

(defn get-uint8 [ba offset]
  (bytes-to-int ba offset 1))

(defn get-header [in]
  (let [version (get-uint8 in 4)
        codec (get-uint8 in 5)
        header-size (get-short in 6)
        payload-size (get-int in 8)
        encoded-width (get-short in 12)
        encoded-height (get-short in 14)
        display-width (get-short in 16)
        display-height (get-short in 18)
        frame-number (get-int in 20)
        timestamp (get-int in 24)
        total-chunks (get-uint8 in 28)
        chunk-index (get-uint8 in 29)
        frame-type (get-uint8 in 30)
        control (get-uint8 in 31)
        stream-byte-pos-lw (get-int in 32)
        stream-byte-pos-uw (get-int in 36)
        stream-id (get-short in 40)
        total-slices (get-uint8 in 42)
        slice-index (get-uint8 in 43)
        header1-size (get-uint8 in 44)
        header2-size (get-uint8 in 45)
        advertised-size (get-int in 48)]
    {:version version
     :codec codec
     :header-size header-size
     :payload-size payload-size
     :encoded-width encoded-width
     :encoded-height encoded-height
     :display-width display-width
     :display-height display-height
     :frame-number frame-number
     :timestamp timestamp
     :total-chunks total-chunks
     :chunk-index chunk-index
     :frame-type frame-type
     :control control
     :stream-byte-pos-lw stream-byte-pos-lw
     :stream-bypte-pos-uw stream-byte-pos-uw
     :stream-id stream-id
     :total-slices total-slices
     :slice-index slice-index
     :header1-size header1-size
     :header2-size header2-size
     :advertised-size advertised-size
     }))


(defn payload-size [in]
  (:payload-size (get-header in)))

(defn read-payload [size]
  (read-from-input size))

(defn write-payload [video out]
  (.write out video))

(defn save-image [bi]
  (ImageIO/write bi "png" (File. "opencvin.png")))

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

(defn stream-video [_ host out]
  (while @stream (do
                   (read-frame host out)
                   ;(Thread/sleep 30)
                   )))


(defn end-video []
  (reset! stream false))


(defn init-video [host]
  (init-decoder)
  (setup-viewer)
  (init-video-stream host))

(defn start-video [host]
  (do
    (reset! stream true)
    (Thread/sleep 40)
    ;wait for the first frame
    (send video-agent stream-video host (when @save-video
                                          (FileOutputStream. "vid.h264")))))
