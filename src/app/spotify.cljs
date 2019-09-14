(ns app.spotify
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.connect.graphql2 :as pcg]
    [com.wsscode.pathom.graphql :as pg]
    [com.wsscode.pathom.trace :as pt]
    [com.wsscode.pathom.profile :as pp]
    [com.wsscode.common.async-cljs :refer [let-chan <!p go-catch <? <?maybe]]
    [goog.object :as gobj]))


;;;;;;;;;;;


(pc/defresolver answer [_ _]
  {::pc/output [:answer-to-everything]}
  {:answer-to-everything 42})


(pc/defresolver answer-plus-one [_ {:keys [answer-to-everything]}]
  {::pc/input  #{:answer-to-everything}
   ::pc/output [:answer-plus-one]}
  {:answer-plus-one (inc answer-to-everything)})


(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register [answer answer-plus-one]})
                  p/error-handler-plugin
                  p/trace-plugin]}))



(comment

  (parser {} [:answer-to-everything :answer-plus-one])


  )


;;;;;;;;;;;

; creating our first resolver
(pc/defresolver latest-product [_ _]
  {::pc/output [{::latest-product [:product/id :product/title :product/price]}]}
  {::latest-product {:product/id    1
                     :product/title "Acoustic Guitar"
                     :product/price 199.99M}})


(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register latest-product})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(def brand->id {"Taylor" 44151})

(pc/defresolver brand-id-from-name [_ {:keys [product/brand]}]
  {::pc/input  #{:product/brand}
   ::pc/output [:product/brand-id]}
  {:product/brand-id (get brand->id brand)})


(comment

  (parser {} [::latest-product])

  (parser {} [{::latest-product [:product/title :product/brand-id]}])

  )


;;;;;;;;;;;

(def parser
  (p/parser {::p/plugins
             [(p/env-plugin
                {::p/reader [p/map-reader
                             pc/all-readers]})]}))

(comment
  (parser {::p/entity {:hello "World"}} [:hello])
  )

;;;;;;;;;;;

(def reader
  {:dog.ceo/random-dog-url
   (fn [_]
     (go-catch
       (-> (js/fetch "https://dog.ceo/api/breeds/image/random") <!p
           (.json) <!p
           (gobj/get "message"))))})

(def parser
  (p/async-parser {::p/plugins [(p/env-plugin {::p/reader reader})
                                p/error-handler-plugin
                                p/trace-plugin]}))

(comment
  (js/console.log
    (parser {} [:dog.ceo/random-dog-url]))
  )

;;;;;;;;;;;


(pc/defresolver env-modifier [env input]
  {::pc/output [{:change-env [:val]}]}
  {:change-env {:val    123
                ::p/env (assoc env ::demo-env-key "modified")}})

(pc/defresolver env-read-thing [{::keys [demo-env-key]} input]
  {::pc/cache? false
   ::pc/output [:env-data]}
  {:env-data demo-env-key})

(pc/defresolver global-thing [{::keys [demo-env-key]} input]
  {::pc/cache? false
   ::pc/output [:env-data]}
  {:env-data demo-env-key})

(def register
  [env-modifier env-read-thing])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::demo-env-key           "original"
                  ::p/placeholder-prefixes #{">"}}
     ::p/plugins [(pc/connect-plugin {::pc/register register})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(comment

  (parser {} [:env-data {:change-env [:env-data]}])

  )


;;;;;;;;;;;
;; How to go from :person/id to that person's details
(pc/defresolver person-resolver [env {:keys [person/id] :as params}]
  ;; The minimum data we must already know in order to resolve the outputs
  {::pc/input  #{:person/id}
   ;; A query template for what this resolver outputs
   ::pc/output [:person/name {:person/address [:address/id]}]}
  ;; normally you'd pull the person from the db, and satisfy the listed
  ;; outputs. For demo, we just always return the same person details.
  {:person/name    "Tom"
   :person/address {:address/id 1}})

;; how to go from :address/id to address details.
(pc/defresolver address-resolver [env {:keys [address/id] :as params}]
  {::pc/input  #{:address/id}
   ::pc/output [:address/city :address/state]}
  {:address/city  "Salem"
   :address/state "MA"})

;; define a list with our resolvers
(def my-resolvers [person-resolver address-resolver])

;; setup for a given connect system
(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                  p/error-handler-plugin
                  p/trace-plugin]}))




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;

(def token (-> "./secrets.edn"
               slurp
               read-string
               :token))

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

(defn api [{::keys [endpoint method token]
            :or    {method :get}}]
  (-> (http/request {:method  method
                     :headers {"Authorization" (str "Bearer " token)}
                     :as      :json
                     :url     (str "https://api.spotify.com/v1/" endpoint)})
      :body))

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
       (json/read-json)
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
  (p/parser {::p/plugins [(p/env-plugin {::p/reader          [p/map-reader
                                                              pc/all-readers
                                                              (p/placeholder-reader ">")]
                                         ::pc/indexes @indexes*
                                         ::token             token})
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
