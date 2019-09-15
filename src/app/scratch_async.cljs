(ns app.scratch-async
  (:require [cljs.core.async :refer [chan put! take! >! <! buffer dropping-buffer sliding-buffer timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]))

