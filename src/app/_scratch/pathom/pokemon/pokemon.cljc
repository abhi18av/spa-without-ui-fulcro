(ns ^{:author "Abhinav Sharma (@abhi18av)"
      :doc    "Wraps the api for https://pokeapi.co/docs/v2.html/#pokemon "}

  app.-scratch.pathom.pokemon.pokemon

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


;; CLJS
;(defn api [{::keys [endpoint method token]
;            :or    {method :get}}]
;  (go
;    (->
;      (<? (fetch/request-async {#_#_::http/url "https://api.spotify.com/v1/artists/3WrFJ7ztbogyGnTHbHJFl2"
;                                ::http/url     (str "https://api.spotify.com/v1/" endpoint)
;                                ::http/headers {:authorization (str "Bearer " token)}
;                                ::http/as      ::http/json
;                                ::http/method  "get"})))
;    :body))
;
;
;
;
;(fetch/request-async {::http/url    "https://pokeapi.co/api/v2/pokemon/1"
;                      ::http/as     ::http/json
;                      ::http/method "get"})

;(comment CLJS
;  (go
;    (->
;      (<! (fetch/request-async {::http/url    "https://pokeapi.co/api/v2/pokemon/1"
;                                ::http/as     ::http/json
;                                ::http/method "get"}))
;      :com.wsscode.pathom.diplomat.http/body
;      ;;:name
;      prn))
;
;
;
;
;
;
;
;  (go
;    (take! (fetch/request-async {::http/url    "https://pokeapi.co/api/v2/pokemon/1"
;                                 ::http/as     ::http/json
;                                 ::http/method "get"})
;           prn))
;
;
;  (go
;    (-> (fetch/request-async {::http/url    "https://pokeapi.co/api/v2/pokemon/1"
;                              ::http/as     ::http/json
;                              ::http/method "get"})
;        (take! println)))
;
;
;
;  (def memory (atom {}))
;
;  (take!
;    (fetch/request-async {::http/url    "https://pokeapi.co/api/v2/pokemon/1"
;                          ::http/as     ::http/json
;                          ::http/method "get"})
;    #(reset! memory %))
;
;
;  @memory
;  )












(defn api [{::keys [endpoint method]
            :or    {method :get}}]
  (-> (http/request {:method method
                     :as     :json
                     :url    (str "https://pokeapi.co/api/v2/" endpoint)})
      :body))



(comment
  (api {::endpoint "pokemon/pikachu"}))



(->
  {::endpoint "pokemon/pikachu"}
  api
  (utils/namespaced-keys "pokemon"))




