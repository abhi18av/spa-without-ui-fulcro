(ns app.scratch-async
  (:require [cljs.core.async :refer [chan put! take! >! <! buffer dropping-buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))


;;;;;;;
;; NOTES

;; - Channels are imp
;; - channels are like vectors - are collection - take value at one end - discharge at another
;; - behaves like a queue ( FIFO )
;; - diff from vector - channel convey one value at a time


;; box => buffer
;; can pass or halt operations which put/take values in this channel (queue)

;; channels like functions are first class values

;; we can treat channel i/o as data and use map/filter ... transducers

;;;;;;;


(def bufferless-chan (chan))

(comment
  bufferless-chan
  )

;;;;;;;;;;;;;;;;;;;;;;;;;

;;
;;       ------------
;; order1
;;       ------------
;;


(put! bufferless-chan
      "Futo Maki"
      ;; this function executes when the order is delivered
      #(prn (str "order put? " %)))


(put! bufferless-chan
      "Vegan Spider"
      #(prn (str "order put? " %)))


;;
;;       ------------
;;                    order1
;;       ------------
;;

(take!
  bufferless-chan
  #(prn (str "order taken: " %)))


;;;;;;;;;;;;;;;;;;;;;;;;;
;; UTILS
;;;;;;;;;;;;;;;;;;;;;;;;;


(defn take-logger [order]
  (prn (str "order taken: " order)))

(defn put-logger [boolean]
  (prn (str "order put? " boolean)))

(defn put!-order [channel order]
  (put! channel order put-logger))

(defn take!-order [channel]
  (take! channel take-logger))


; Create a basic channel
(def bufferless-chan (chan))

(put!
  bufferless-chan ; channel
  "Futo Maki" ; order
  #(prn (str "order put? " %))) ; callback
;;=> true ; <- pending put

(put!
  bufferless-chan ; channel
  "Vegan Spider" ; order
  #(prn (str "order put? " %))) ; callback
;;=> true ; <- pending put

(take!
  bufferless-chan ; channel
  #(prn (str "order taken: " %))) ; callback
; take 1) =>
; "order taken: Futo Maki"
; "order put? true"
; nil

; take 2) =>
; "order taken: Vegan Spider"
; "order put? true"
; nil

; take 3) =>
; nil ; <- pending take

; eval put! function again =>
; "order put? true"
; "order taken: Vegan Spider"
; true

; ===============================
; Robo Orders Overflow
; ===============================

(def bufferless-chan (chan))

; What happens if we got one of those infamous robo-dialers, just spamming away at the phone line trying to sell us timeshares? Well, with our simple `chan` our line would overflow.

(defn bot-orders [channel order]
  (dotimes [x 1030]
    (put!-order channel order)))

(bot-orders bufferless-chan "Sushi!")
;;=> Error: Assert failed: No more than 1024 pending puts are allowed on a single channel. Consider using a windowed buffer. (< (.-length puts) impl/MAX-QUEUE-SIZE)



; There are two ways to create a channel with a fixed buffer. One is by explicitly using the `buffer` funcction. The other is just to pass an integer as an argument to a basic channel like so:

(def fixed-chan (chan 10)) ; buffer = 10 values

; eval at will
(bot-orders fixed-chan "Sushi!")
;; => "order put? true"
;; => "order put? true"
;; => "order put? true"
; ...7 more
; nil


; eval at will:
(take!-order fixed-chan)
; takes 1 - 1020) =>
; "order taken: Sushi!"
; "order put? true"
; nil

; take 1021) =>
; "order taken: Sushi!"
; nil


(defn put!-n-order [channel order n]
  (put! channel (str "#: " n " order: " order) put-logger))

(defn IHOS-orders [channel order]
  (dotimes [x 2100] ; lots-o'-orders
    (put!-n-order channel order x)))

; refresh (re-eval) our fixed-chan
(def fixed-chan (chan 10)) ; buffer = 10 put values

(IHOS-orders fixed-chan "Nigiri!")
;;=>
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; ...
; Error: Assert failed: No more than 1024 pending puts are allowed on a single channel. Consider using a windowed buffer. (< (.-length puts) impl/MAX-QUEUE-SIZE) ...


(take!-order fixed-chan)
;; =>
; "order taken: #: 0 order: Nigiri!"
; "order put? true"
; ...


; ===============================
; Handling Orders Deluge with a `sliding-buffer` (Drop Oldest Puts)
; ===============================


(def slide-chan (chan (sliding-buffer 10))) ; buffer = 10 put values

; Warning: Big log (and I don't mean the song by Robert Plant. That would be cool though....) ahead!
(IHOS-orders slide-chan "Sashimi")
; "order put? true"
; "order put? true"
; ... **2088 more**

(take!-order slide-chan)
; take 1) => "order taken: #: 2090 order: Nigiri!"
; nil
; take 2) => "order taken: #: 2091 order: Nigiri!"
; nil
; ...
; take 10) => "order taken: #: 2099 order: Nigiri!"
; nil
; take 11) =>  nil

; ===============================
; Handling Traffic with a `dropping-buffer` (Drop Latest Puts)
; ===============================

(def drop-chan (chan (dropping-buffer 10)))

(IHOS-orders drop-chan "Tofu Katsu")
; "order put? true"
; "order put? true"
; ... **2088 more**

(take!-order drop-chan)
; take 1) => "order taken: #: 0 order: Tofu Katsu"
; nil
; take 2) => "order taken: #: 1 order: Tofu Katsu"
; nil
; ...
; take 10) => "order taken: #: 9 order: Tofu Katsu"

; ===============================
; `go` caveat (stops translation at fn boundaries/closures)

(comment
  (defn >!-order [channel order count] ; = (def (fn ...))
    (put-logger (>! channel (str "#: " count " order: " order))))

  (defn backpressured-orders [channel order]
    (go
      (dotimes [x 2100] ; increase number of bot orders
        (>!-order channel order x)))))

; => Error: >! used not in (go ...) block
; ===============================

(defn backpressured-orders [channel order]
  (go
    (dotimes [x 2100] ; increase number of bot orders
      (put-logger (>! channel (str "#: " x " order: " order))))))

(def burst-chan (chan 50))


(defn burst-take! [channel]
  (dotimes [x 50] ; increase number of bot orders
    (take!-order channel)))

(backpressured-orders burst-chan "Umami Tamago")
; =>
; "order put? true"
; "order put? true"
; ... 48 more

(burst-take! burst-chan)
; FIRST EVAL =>
; "order taken: #: 0 order: Umami Tamago"
; ...
; "order taken: #: 49 order: Umami Tamago"
; "order put? true"
; "order put? true"
; ... 48 more

; SECOND EVAL =>
; "order taken: #: 50 order: Umami Tamago"
; ...
; "order taken: #: 99 order: Umami Tamago"
; "order put? true"
; "order put? true"
; ... 48 more

; ===============================
; Burst Orders with Backpressure Upstream (`>!`), "Parking" Downstream (`<!`)
; ===============================

; We get an immaterial difference when using "blocking" syntax downstream


(def burst-chan (chan 50))

(defn burst-<! [channel]
  (go
    (dotimes [x 50] ; increase number of bot orders
      (take-logger (<! channel)))))

(backpressured-orders burst-chan "Umami Tamago")
; same as before
(burst-<! burst-chan)
; same as before

; ===============================
; Burst Orders with Async Upstream (`put!`) Downstream "Parking" (`<!`)
; ===============================

(def burst-chan (chan 50))

(IHOS-orders burst-chan "Miso Soup!")
; Old faithful:
; Error: Assert failed: No more than 1024 pending puts are allowed on a single channel. Consider using a windowed buffer. (< (.-length puts) impl/MAX-QUEUE-SIZE)

(burst-<! burst-chan)


(defn max-order [channel order]
  (go
    (dotimes [x 12]
      (put-logger (>! channel (str "#: " x " order: " order))))
    (close! channel)))

(def capacity (chan 5))

(defn take!-til-closed [channel]
  (dotimes [x 5]
    (take!-order channel)))

(max-order capacity "Wildcard")
;;=>
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"
; "order put? true"

(take!-til-closed capacity)
;; take 1) =>
; "order taken: #: 0 order: Wildcard"
; ...
; "order put? true"
; ...
; nil

;; take 2) =>
; "order taken: #: 5 order: Wildcard"
; ...
; "order put? true"
; ...
; nil

;; take 3) =>
; "order taken: #: 10 order: Wildcard"
; "order taken: #: 11 order: Wildcard"
; "order taken: "
; "order taken: "
; "order taken: "
; nil

(put! capacity "Overflow" put-logger)
;; =>
; "order put? false"
; false




(defn timeout-chan [channel]
  (let [closer (timeout 3000)]
    (go (while true (<! (timeout 250)) (>! channel "Main Bar")))
    (go (while true (<! (timeout 500)) (>! channel "Online Order")))
    (go (while true (<! (timeout 750)) (>! channel "Roulette Room")))
    (go-loop [_ []]
             (let [[val ch] (alts! [channel closer])] ; <- `alts!`
               (cond
                 (= ch closer) (do
                                 (close! channel)
                                 (.log js/console (str "No more orders. Domo arigatogozaimashita.")))
                 :else
                 (recur (.log js/console (str "Order up: " (<! channel)))))))))

(def capacity (chan 5))

(timeout-chan capacity)
;;=>
; Order up: Online Order
; Order up: Roulette Room
; Order up: Online Order
; Order up: Main Bar
; Order up: Online Order
; Order up: Main Bar
; Order up: Main Bar
; Order up: Main Bar
; Order up: Main Bar
; Order up: Roulette Room
; No more orders. Domo Arigatogozaimashita.


(take! capacity take-logger)
;;=> "order taken: Online Order"
;;=> "order taken: "

; `go` blocks return channels

(.log js/console (go 5))

; `>!` & `<!` ("parking" put & take)

(go (.log js/console (<! (go 5))))

; `chan` (channels)

(let [c (chan)]
  (go
    (.log js/console "We got here")
    (.log js/console (<! c)) ; take a value off the channel
    (.log js/console "We'll never get here"))) ; => We got here

(let [c (chan)]
  (go
    (.log js/console "Got here")
    (.log js/console (<! c))
    (.log js/console "We made progress"))
  (go ; when this following go block runs, it allows the prior go to finish
    (>! c 5)))

(let [c (chan)]
  (go
    (.log js/console "Before")
    (>! c (js/Date.))
    (.log js/console "After"))
  (go
    (.log js/console "Order")
    (.log js/console (<! c))
    (.log js/console "doesn't matter")))

; `timeout`

; Gotchas of `chan`:

(def ch (chan))

(go (while true (<! (timeout 250)) (>! ch 1)))
(go (while true (<! (timeout 500)) (>! ch 2)))
(go (while true (<! (timeout 750)) (>! ch 3)))

(go-loop []
         (recur (.log js/console (str "process: " (<! ch)))))


; Control Flow with `alts!`
; USING `alts!`


(defn timeout-chan [port]
  (let [tmt (timeout 3000)]
    (go (while true (<! (timeout 250)) (>! port 1)))
    (go (while true (<! (timeout 500)) (>! port 2)))
    (go (while true (<! (timeout 750)) (>! port 3)))
    (go-loop [_ []]
             (let [[val ch] (alts! [port tmt])]
               (cond
                 (= ch tmt) (.log js/console (str "done"))
                 :else
                 (recur (.log js/console (str "process: " (<! port)))))))))


(def test-chan (chan))

(timeout-chan test-chan)

; `put!` and 'take!`

(defn toggle-chan [process stopper]
  (go (while true (<! (timeout 250)) (>! process 1)))
  (go (while true (<! (timeout 500)) (>! process 2)))
  (go (while true (<! (timeout 750)) (>! process 3)))
  (go-loop [_ []] ; accumulator = placeholder, replaced with each `(recur (.log...`
           (let [[val ch] (alts! [process stopper])]
             (cond
               (= ch stopper) (take! stopper #(.log js/console (str "take val: " %)))
               :else
               (recur (.log js/console (str "process: " (<! process))))))))

(defn stopping-put-async [port val]
  (put! port val #(.log js/console (str "put val: " %))))

(defn stopping-put-park [port val]
  (go (>! port val #(.log js/console (str "put val: " %)))))

(def test-chan2 (chan))
(def stopping-chan (chan 2))

(toggle-chan test-chan2 stopping-chan)

(stopping-put-async stopping-chan "HALT")
(stopping-put-park stopping-chan "STOP")

; Buffers

(def fixed-chan (chan 2))
(timeout-chan fixed-chan)
;(msg->chan fixed-chan "TEST-CHAN")

(def buff-chan (chan (buffer 2)))
(timeout-chan buff-chan)
;(msg->chan buff-chan "BUFF-CHAN")

(def slide-chan (chan (sliding-buffer 2)))
(timeout-chan slide-chan)
;(msg->chan slide-chan "SLIDE-CHAN")

(def drop-chan (chan (dropping-buffer 2)))
(timeout-chan drop-chan)

;(msg->chan drop-chan "DROP-CHAN")


