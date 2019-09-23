(ns ^{:author "Abhinav Sharma (@abhi18av)"
      :doc    "Wraps the api for https://pokeapi.co/docs/v2.html/#pokemon "}

  app.-scratch.pathom.pokemon.pikachu

  (:require [app.-scratch.pathom.pokemon.utils :as utils]
            [app.-scratch.pathom.pokemon.pokemon]
            [clojure.core.async :refer [go timeout <! <!!]]
            [clj-http.client :as http]
            [clojure.string :as str]
            [com.wsscode.common.async-clj :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]
            [com.wsscode.pathom.diplomat.http :as pdh]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql :as pcg]))


;;;;;;;;;;;;;;;;;;;;



utils/pikachu

()
