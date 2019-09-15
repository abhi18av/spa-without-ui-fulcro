(ns app.scratch-async
  (:require [cljs.core.async :refer [chan put! take! >! <! buffer dropping-buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(def ch (chan))

(take! ch #(println "Got a value:" %))
;; => nil

;; there is a now a pending take operation, let's put something on the channel

(put! ch 42)
;; Got a value: 42
;; => 42



(put! ch 42 #(println "Just put 42"))
;; => true

(put! ch 43 #(println "Just put 43"))
;; => true

(take! ch #(println "Got" %))
;; Got 42
;; Just put 42
;; => nil

(take! ch #(println "Got" %))
;; Got 43
;; Just put 43
;; => nil


(close! ch)
;; => nil

(put! ch 42)

(dotimes [n 1025]
  (put! ch n))
;; Error: Assert failed: No more than 1024 pending puts are allowed on a single channel.

(def ch (chan))

(dotimes [n 1025]
  (take! ch #(println "Got" %)))
;; Error: Assert failed: No more than 1024 pending takes are allowed on a single channel.

(put! ch 42 #(println "Put succeeded!"))
;; Put succeeded!
;; => true

(dotimes [n 1024]
  (put! ch n))
;; => nil

(put! ch 42)
