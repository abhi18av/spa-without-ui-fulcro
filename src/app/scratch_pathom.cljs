(ns app.scratch-pathom
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.connect.graphql2 :as pcg]
    [com.wsscode.pathom.graphql :as pg]
    [com.wsscode.pathom.trace :as pt]
    [com.wsscode.pathom.profile :as pp]
    [com.wsscode.common.async-cljs :refer [let-chan <!p go-catch <? <?maybe]]
    [goog.object :as gobj]
    [cljs.core.async :refer-macros [go go-loop alt!]]
    [cljs.core.async :as async :refer [chan put! take! >! <! buffer dropping-buffer sliding-buffer timeout close! alts!]]))


;;;;;;;;;;;


(pc/defresolver answer [_ _]
  {::pc/output [:answer-to-everything]}
  {:answer-to-everything 42})


(pc/defresolver answer-plus-one [_ {:keys [answer-to-everything]}]
  {::pc/input  #{:answer-to-everything}
   ::pc/output [:answer-plus-one]}
  {:answer-plus-one (inc answer-to-everything)})


(def parser
  (p/async-parser
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
  (go (prn (<! (parser {} [:answer-to-everything :answer-plus-one]))))


  )


;;;;;;;;;;


(pc/defresolver async-info [_ _]
  {::pc/output [:async-info]}
  (go
    (<! (async/timeout (+ 100 (rand-int 1000))))
    {:async-info "From async"}))

(pc/defresolver foo [_ _]
  {::pc/output [:foo]}
  {:foo "Regular"})

(def register [async-info foo])

(def parser
  (p/async-parser {::p/env     {::p/reader [p/map-reader
                                            pc/async-reader2]}
                   ::p/plugins [(pc/connect-plugin {::pc/register register})
                                pt/trace-plugin]}))


(comment
  (go (prn (<! (parser {} [:foo :async-info]))))
  )
;;;;;;;;;;;


(pc/defresolver async-info [_ _]
  {::pc/output [:async-info]}
  (go-catch
    (<? (async/timeout (+ 100 (rand-int 1000))))
    {:async-info "From async"}))

(pc/defresolver async-error [_ _]
  {::pc/output [:async-error]}
  ; go catch will catch any exception and return then as the channel value
  (go-catch
    ; <? macro will re-throw any exception that get read from the channel
    (<? (async/timeout (+ 100 (rand-int 1000))))
    (throw (ex-info "Error!!" {}))))

(pc/defresolver foo [_ _]
  {::pc/output [:foo]}
  {:foo "Regular"})

(def register [async-info async-error foo])

(def parser
  (p/async-parser {::p/env     {::p/reader [p/map-reader
                                            pc/async-reader2]}
                   ::p/plugins [(pc/connect-plugin {::pc/register register})
                                p/error-handler-plugin
                                pt/trace-plugin]}))



(comment
  (go (prn (<! (parser {} [:foo :async-info :async-error]))))
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


(comment

  (go (prn (<! (parser {} [::latest-product]))))

  )

;;;;;;;;;;


(def product->brand
  {1 "Taylor"})

(pc/defresolver latest-product [_ _]
  {::pc/output [{::latest-product [:product/id :product/title :product/price]}]}
  {::latest-product {:product/id    1
                     :product/title "Acoustic Guitar"
                     :product/price 199.99M}})

(pc/defresolver product-brand [_ {:keys [product/id]}]
  {::pc/input  #{:product/id}
   ::pc/output [:product/brand]}
  {:product/brand (get product->brand id)})

(def app-registry [latest-product product-brand pc/index-explorer-resolver])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader [p/map-reader
                              pc/parallel-reader
                              pc/open-ident-reader]}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register app-registry})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(def brand->id {"Taylor" 44151})

(pc/defresolver brand-id-from-name [_ {:keys [product/brand]}]
  {::pc/input #{:product/brand}
   ::pc/output [:product/brand-id]}
  {:product/brand-id (get brand->id brand)})


(comment

  (go (prn (<! (parser {} [{::latest-product [:product/title :product/brand]}]))))

  (go (prn (<! (parser {} [{::latest-product [:product/title :product/brand-id]}]))))

  (go (prn (<! (parser {} [{[:product/id 1] [:product/brand]}]))))

  (go (prn (<! (parser {} [{[:product/brand "Taylor"] [:product/brand-id]}]))))

  )


;;;;;;;;;;;



(pc/defresolver list-things [_ _]
  {::pc/output [{:items [:number]}]}
  {:items [{:number 3}
           {:number 10}
           {:number 18}]})

(pc/defresolver slow-resolver [_ {:keys [number]}]
  {::pc/input  #{:number}
   ::pc/output [:number-added]}
  (go
    (async/<! (async/timeout 1000))
    {:number-added (inc number)}))

(def app-registry [list-things slow-resolver])

(def parser
  (p/async-parser
    {::p/env     {::p/reader [p/map-reader
                              pc/async-reader2
                              pc/open-ident-reader]}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register app-registry})
                  p/error-handler-plugin
                  p/trace-plugin]}))

(comment

  (go (prn (<! (parser {} [{:items [:number-added]}]))))

  )

;;;;;;;;;;;


(pc/defresolver list-things [_ _]
  {::pc/output [{:items [:number]}]}
  {:items [{:number 3}
           {:number 10}
           {:number 18}]})

(pc/defresolver slow-resolver [_ input]
  {::pc/input  #{:number}
   ::pc/output [:number-added]
   ::pc/batch? true}
  (go
    (async/<! (async/timeout 1000))
    ; the input will be sequential if a batch opportunity happens
    (if (sequential? input)
      ; this will return a list of results, this order should match the input order, like this:
      ; [{:number-added 4}
      ;  {:number-added 11}
      ;  {:number-added 19}]
      (mapv (fn [v] {:number-added (inc (:number v))}) input)
      ; the else case still handles the single input case
      {:number-added (inc (:number input))})))

(def app-registry [list-things slow-resolver])

(def parser
  (p/async-parser
    {::p/env     {::p/reader        [p/map-reader
                                     pc/async-reader2
                                     pc/open-ident-reader]
                  ::p/process-error (fn [env error]
                                      (js/console.error "ERROR" error)
                                      (p/error-str error))}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register app-registry})
                  p/error-handler-plugin
                  p/trace-plugin]}))

;; for batching
(pc/defresolver slow-resolver [_ input]
  {::pc/input     #{:number}
   ::pc/output    [:number-added]
   ; use the transform, note we removed ::pc/batch? true, that's because the transform
   ; will add this for us
   ::pc/transform pc/transform-batch-resolver}
  (go
    (async/<! (async/timeout 1000))
    ; no need to detect sequence, it is always a sequence now
    (mapv (fn [v] {:number-added (inc (:number v))}) input)))

(pc/defresolver slow-resolver [_ {:keys [number]}]
  {::pc/input     #{:number}
   ::pc/output    [:number-added]
   ; set auto-batch with concurrency of 10
   ::pc/transform (pc/transform-auto-batch 10)}
  (go
    (async/<! (async/timeout 1000))
    ; dealing with the single case, as in the first example we did on batch
    {:number-added (inc number)}))

(comment
  (go (prn (<! (parser {} [{:items [:number-added]}]))))

  )
;;;;;;;;;;;
(pc/defmutation create-user [{::keys [db]} user]
  {::pc/sym    'user/create
   ::pc/params [:user/name :user/email]
   ::pc/output [:user/id]}
  (let [{:keys [user/id] :as new-user}
        (-> user
            (select-keys [:user/name :user/email])
            (merge {:user/id         (random-uuid)
                    :user/created-at (js/Date.)}))]
    (swap! db assoc-in [:users id] new-user)
    {:user/id id}))

(pc/defresolver user-data [{::keys [db]} {:keys [user/id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/id :user/name :user/email :user/created-at]}
  (get-in @db [:users id]))

(pc/defresolver all-users [{::keys [db]} _]
  {::pc/output [{:user/all [:user/id :user/name :user/email :user/created-at]}]}
  (vals (get @db :users)))

(def api-registry [create-user user-data all-users])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader [p/map-reader pc/parallel-reader pc/open-ident-reader]
                  ::db       (atom {})}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register api-registry})
                  p/error-handler-plugin
                  p/trace-plugin]}))

(comment

  (go (prn (<! (parser {} [{:items [:number-added]}]))))

  )


;;;;;;;;;;

(pc/defmutation create-user [{::keys [db]} user]
  {::pc/sym    'user/create
   ::pc/params [:user/name :user/email]
   ::pc/output [:user/id]}
  (let [{:keys [user/id] :as new-user}
        (-> user
            (select-keys [:user/name :user/email])
            (merge {:user/id         (random-uuid)
                    :user/created-at (js/Date.)}))]
    (swap! db assoc-in [:users id] new-user)
    {:user/id id}))

(pc/defresolver user-data [{::keys [db]} {:keys [user/id]}]
  {::pc/input  #{:user/id}
   ::pc/output [:user/id :user/name :user/email :user/created-at]}
  (get-in @db [:users id]))

(pc/defresolver all-users [{::keys [db]} _]
  {::pc/output [{:user/all [:user/id :user/name :user/email :user/created-at]}]}
  (vals (get @db :users)))

(def api-registry [create-user user-data all-users])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader [p/map-reader pc/parallel-reader pc/open-ident-reader]
                  ::db       (atom {})}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register api-registry})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(comment
  (go (prn (<! (parser {} [{(user/create {:user/name "Rick Sanches" :user/email "rick@morty.com"}) [:user/id :user/name :user/created-at]}]))))
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


(pc/defresolver random-dog [env {:keys []}]
  {::pc/output [:dog.ceo/random-dog-url]}
  (go-catch
    {:dog.ceo/random-dog-url
     (-> (js/fetch "https://dog.ceo/api/breeds/image/random") <!p
         (.json) <!p
         (gobj/get "message"))}))

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/plugins [(pc/connect-plugin {::pc/register [random-dog]})
                  p/error-handler-plugin
                  p/trace-plugin]}))

(comment

  (go (prn (<! (parser {} [:dog.ceo/random-dog-url]))))

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
