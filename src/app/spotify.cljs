(ns app.spotify
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.connect.graphql2 :as pcg]
    [com.wsscode.pathom.graphql :as pg]
    [com.wsscode.pathom.trace :as pt]
    [com.wsscode.pathom.profile :as pp]
    [com.wsscode.common.async-cljs :refer [let-chan <!p go-catch <? <?maybe]]
    [goog.object :as gobj]
    [cljs.core.async :refer-macros [go go-loop alt!]]
    [cljs.core.async :as async :refer [chan put! take! >! <! buffer dropping-buffer sliding-buffer timeout close! alts!]]
    [com.wsscode.pathom.diplomat.http :as http]
    [com.wsscode.pathom.diplomat.http.fetch :as fetch]))


;;;;;;;;;;;

(def token )

(def requestbin-url "https://en3j0mtk1m6ft.x.pipedream.net")

;;;;;;;;;;;
;; HTTP fetch exploration


(go
  (prn
    (<? (fetch/request-async {::http/url    requestbin-url
                              ::http/as     ::http/json
                              ::http/method "post"}))))

(go
  (prn
    (<? (fetch/request-async {::http/url    requestbin-url
                              ::http/as     ::http/json
                              ::http/method "get"}))))



(go
  (<? (fetch/request-async {::http/url    requestbin-url
                            ::http/as     ::http/json
                            ::http/method "get"})))





(comment


  (-> (http/request {:method  :get
                     :as      :json
                     :url     requestbin-url})
      :body)



  (-> (http/request {:method  :get
                     :headers {"Authorization" (str "Bearer " token)}
                     :as      :json
                     :url     (str "https://api.spotify.com/v1/" endpoint)})
      :body))

(go
  (prn
    (<? (fetch/request-async {#_#_::http/url "https://api.spotify.com/v1/artists/3WrFJ7ztbogyGnTHbHJFl2"
                              ::http/url     "https://ens7oe1e7e88k.x.pipedream.net"
                              ::http/headers {:authorization (str "Bearer " token)}
                              #_#_::http/as ::http/json
                              ::http/method  "get"}))))



(go
  (<? (fetch/request-async {::http/url     "https://api.spotify.com/v1/artists/3WrFJ7ztbogyGnTHbHJFl2"
                            ::http/headers {:authorization (str "Bearer " token)}
                            ::http/as      ::http/json
                            ::http/method  "get"}))) )



(comment

  (def y
    (let [ch (chan)]
      (go (while true
            (let [v (<! ch)]
              (println "Read: " v))))
      (go (>! ch 9)
          #_(<! (timeout 5000))
          (>! ch 18))))

  )



;;;;;;;;;;;

;(def token (-> "./secrets.edn"
;               slurp
;               read-string
;               :token))


(def indexes* (atom {}))

(comment

  (def indexes
    (-> {}
        (pc/add `artist-by-id
                {::pc/input #{:spotify.artist/id}
                 ::pc/output})))

  )



(defn add-resolver! [sym options]
  (swap! indexes* pc/add sym options))

(defn namespace-keys [m ns]
  (into {} (map (fn [[k v]] [(keyword ns (name k)) v])) m))


;
;(defn api [{::keys [endpoint method token]
;            :or    {method :get}}]
;  (-> (http/request {:method  method
;                     :headers {"Authorization" (str "Bearer " token)}
;                     :as      :json
;                     :url     (str "https://api.spotify.com/v1/" endpoint)})
;      :body))
;





(defn api [{::keys [endpoint method token]
            :or    {method :get}}]
  (->
    (go
      (<? (fetch/request-async {#_#_::http/url "https://api.spotify.com/v1/artists/3WrFJ7ztbogyGnTHbHJFl2"
                                ::http/url     (str "https://api.spotify.com/v1/" endpoint)
                                ::http/headers {:authorization (str "Bearer " token)}
                                ::http/as      ::http/json
                                ::http/method  "get"}))
      :body)))




(comment

  )



(defn adapt-album [album]
  (namespace-keys album "spotify.album"))

(defn adapt-artist [artist]
  (namespace-keys artist "spotify.artist"))

(defn adapt-track [track]
  (-> track
      (update :album adapt-album)
      (update :artists #(mapv adapt-artist %))
      (namespace-keys "spotify.track")))

(defn artist-by-id [env {:spotify.artist/keys [id]}]
  (->> {::endpoint (str "artists/" id)}
       (merge env)
       (api)
       #_(json/read-json)
       (adapt-artist)))

(comment
  (artist-by-id {::token token}
                {:spotify.artist/id "3WrFJ7ztbogyGnTHbHJFl2"})


  (pc/data->shape (artist-by-id {::token token}
                                {:spotify.artist/id "3WrFJ7ztbogyGnTHbHJFl2"}))

  )


(add-resolver! `artist-by-id
               {::pc/input  #{:spotify.artist/id}
                ;; NOTE Generated using data->shape function
                ::pc/output [#:spotify.artist{:external_urls [:spotify]}
                             :spotify.artist/popularity
                             #:spotify.artist{:images [:height :url :width]}
                             :spotify.artist/genres
                             :spotify.artist/name
                             :spotify.artist/uri
                             :spotify.artist/type
                             :spotify.artist/id
                             :spotify.artist/href
                             #:spotify.artist{:followers [:href :total]}]})

(defn artist-top-tracks [env {:spotify.artist/keys [id]}]
  (->> {::endpoint (str "artists/" id "/top-tracks?country=BR")}
       (merge env)
       (api)
       :tracks
       (mapv adapt-track)
       (hash-map :spotify.artist/top-tracks)))

(comment
  (artist-top-tracks {::token token}
                     {:spotify.artist/id "3WrFJ7ztbogyGnTHbHJFl2"})
  )


(add-resolver! `artist-top-tracks
               {::pc/input  #{:spotify.artist/id}
                ::pc/output [#:spotify.artist{:top-tracks [:spotify.track/href
                                                           :spotify.track/available_markets
                                                           :spotify.track/popularity
                                                           :spotify.track/disc_number
                                                           #:spotify.track{:album [:spotify.album/album_type
                                                                                   #:spotify.album{:external_urls [:spotify]}
                                                                                   #:spotify.album{:images [:height :url :width]}
                                                                                   :spotify.album/available_markets
                                                                                   #:spotify.album{:artists [{:external_urls [:spotify]}
                                                                                                             :href
                                                                                                             :id
                                                                                                             :name
                                                                                                             :type
                                                                                                             :uri]}
                                                                                   :spotify.album/name
                                                                                   :spotify.album/uri
                                                                                   :spotify.album/type
                                                                                   :spotify.album/href
                                                                                   :spotify.album/id]}
                                                           :spotify.track/explicit
                                                           :spotify.track/name
                                                           :spotify.track/duration_ms
                                                           #:spotify.track{:artists [#:spotify.artist{:external_urls [:spotify]}
                                                                                     :spotify.artist/href
                                                                                     :spotify.artist/id
                                                                                     :spotify.artist/name
                                                                                     :spotify.artist/type
                                                                                     :spotify.artist/uri]}
                                                           :spotify.track/uri
                                                           :spotify.track/type
                                                           #:spotify.track{:external_ids [:isrc]}
                                                           #:spotify.track{:external_urls [:spotify]}
                                                           :spotify.track/preview_url
                                                           :spotify.track/id
                                                           :spotify.track/track_number]}]})

(defn track-audio-features [env {:spotify.track/keys [id]}]
  (-> {::endpoint (str "audio-features/" id)}
      (merge env)
      (api)
      (namespace-keys "spotify.track")))

(add-resolver! `track-audio-features
               {::pc/input  #{:spotify.track/id}
                ::pc/output [:spotify.track/instrumentalness
                             :spotify.track/track_href
                             :spotify.track/acousticness
                             :spotify.track/energy
                             :spotify.track/time_signature
                             :spotify.track/analysis_url
                             :spotify.track/valence
                             :spotify.track/duration_ms
                             :spotify.track/uri
                             :spotify.track/type
                             :spotify.track/key
                             :spotify.track/tempo
                             :spotify.track/mode
                             :spotify.track/liveness
                             :spotify.track/loudness
                             :spotify.track/speechiness
                             :spotify.track/danceability
                             :spotify.track/id]})

(defn track-by-id [env {:spotify.track/keys [id]}]
  (-> {::endpoint (str "tracks/" id)}
      (merge env)
      (api)
      (adapt-track)))

(add-resolver! `track-by-id
               {::pc/input  #{:spotify.track/id}
                ::pc/output [:spotify.track/href
                             :spotify.track/available_markets
                             :spotify.track/popularity
                             :spotify.track/disc_number
                             #:spotify.track{:album [:spotify.album/album_type
                                                     #:spotify.album{:external_urls [:spotify]}
                                                     #:spotify.album{:images [:height :url :width]}
                                                     :spotify.album/available_markets
                                                     #:spotify.album{:artists [{:external_urls [:spotify]} :href :id :name :type :uri]}
                                                     :spotify.album/name
                                                     :spotify.album/uri
                                                     :spotify.album/type
                                                     :spotify.album/href
                                                     :spotify.album/id]}
                             :spotify.track/explicit
                             :spotify.track/name
                             :spotify.track/duration_ms
                             #:spotify.track{:artists [#:spotify.artist{:external_urls [:spotify]}
                                                       :spotify.artist/href
                                                       :spotify.artist/id
                                                       :spotify.artist/name
                                                       :spotify.artist/type
                                                       :spotify.artist/uri]}
                             :spotify.track/uri
                             :spotify.track/type
                             #:spotify.track{:external_ids [:isrc]}
                             #:spotify.track{:external_urls [:spotify]}
                             :spotify.track/preview_url
                             :spotify.track/id
                             :spotify.track/track_number]})

(def parser
  (p/parser {::p/plugins [(p/env-plugin {::p/reader   [p/map-reader
                                                       pc/all-readers
                                                       (p/placeholder-reader ">")]
                                         ::pc/indexes @indexes*
                                         ::token      token})
                          pp/profile-plugin
                          p/request-cache-plugin
                          p/error-handler-plugin]}))

(comment
  (parser {::p/reader {:hello (constantly "world!")}} [:hello])

  (parser {::p/reader [p/map-reader
                       pc/all-readers]})
  )



;;;;;;;;;;;;


(comment
  (def CONTEXT {:spotify.artist/id "0oSGxfWSnnOXhD2fKuz2Gy"})

  (parser {}
          [{[:spotify.artist/id "0oSGxfWSnnOXhD2fKuz2Gy"]
            [:spotify.artist/top-tracks]}])

  (parser {}
          [{[:spotify.artist/id "3WrFJ7ztbogyGnTHbHJFl2"]
            [:spotify.artist/name :spotify.artist/genres]}])

  ; example artist id: "3WrFJ7ztbogyGnTHbHJFl2"

  (pc/data->shape (artist-by-id {::token token}
                                {:spotify.artist/id "3WrFJ7ztbogyGnTHbHJFl2"}))
  )
