(ns app.-scratch.pathom.spotify.scratch)

(defn adapt-album [album]
  (namespace-keys album "spotify.album"))

(defn adapt-artist [artist]
  (namespace-keys artist "spotify.artist"))

(defn artist-by-id [env {:spotify.artist/keys [id]}]
  (->> {::endpoint (str "artists/" id)}
       (merge env)
       (api)
       (adapt-artist)))

(add-resolver! `artist-by-id
               {::p.connect/input  #{:spotify.artist/id}
                ::p.connect/output [#:spotify.artist{:external_urls [:spotify]}
                                    :spotify.artist/popularity
                                    #:spotify.artist{:images [:height :url :width]}
                                    :spotify.artist/genres
                                    :spotify.artist/name
                                    :spotify.artist/uri
                                    :spotify.artist/type
                                    :spotify.artist/id
                                    :spotify.artist/href
                                    #:spotify.artist{:followers [:href :total]}]})



(defn adapt-track [track]
  (-> track
      (update :album adapt-album)
      (update :artists #(mapv adapt-artist %))
      (namespace-keys "spotify.track")))


(defn artist-top-tracks [env {:spotify.artist/keys [id]}]
  (->> {::endpoint (str "artists/" id "/top-tracks?country=BR")}
       (merge env)
       (api)
       :tracks
       (mapv adapt-track)
       (hash-map :spotify.artist/top-tracks)))




(add-resolver! `artist-top-tracks
               {::p.connect/input  #{:spotify.artist/id}
                ::p.connect/output [#:spotify.artist{:top-tracks [:spotify.track/href
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


(defn track-by-id [env {:spotify.track/keys [id]}]
  (-> {::endpoint (str "tracks/" id)}
      (merge env)
      (api)
      (adapt-track)))



(add-resolver! `track-by-id
               {::p.connect/input  #{:spotify.track/id}
                ::p.connect/output [:spotify.track/href
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
                                                              p.connect/all-readers
                                                              (p/placeholder-reader ">")]
                                         ::p.connect/indexes @indexes*
                                         ::token             token})
                          p.profile/profile-plugin
                          p/request-cache-plugin
                          p/error-handler-plugin]}))

