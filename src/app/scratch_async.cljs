(ns app.scratch-async
  (:require [cljs.core.async :refer [chan put! take! >! <! buffer dropping-buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

(def bufferless-chan (chan))

(put!
  bufferless-chan
  "Futo Maki"
  #(prn (str "order put? " %)))


(put!
  bufferless-chan
  "Maggi Noodles"
  #(prn (str "order put? " %)))


(put!
  bufferless-chan
  "Vegan Spider"
  #(prn (str "order put? " %)))


(take!
  bufferless-chan
  #(prn (str "order taken: " %)))


