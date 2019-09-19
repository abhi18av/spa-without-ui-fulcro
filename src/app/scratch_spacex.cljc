(ns app.scratch-spacex
  (:require [clojure.core.async :refer [go timeout <! #?(:clj <!!)]]
            [clojure.string :as str]

            [#?(:clj  com.wsscode.common.async-clj
                :cljs com.wsscode.common.async-cljs)
             :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]

            [com.wsscode.pathom.diplomat.http :as p.http]

            #?(:clj  [com.wsscode.pathom.diplomat.http.clj-http :as p.http.clj]
               :cljs [com.wsscode.pathom.diplomat.http.fetch :as p.http.fetch])

            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]))


;region entity helpers
(defn adapt-key [k]
  (str/replace k #"_" "-"))

(defn set-ns
  "Set the namespace of a keyword"
  [ns kw]
  (keyword ns (adapt-key (name kw))))

(defn set-ns-seq
  "Set the namespace for all keywords in a collection. The collection kind will
  be preserved."
  [ns s]
  (into (empty s) (map #(set-ns ns %)) s))

(defn set-ns-x
  "Set the namespace of a value. If sequence will use set-ns-seq."
  [ns x]
  (if (coll? x)
    (set-ns-seq ns x)
    (set-ns ns x)))

(defn namespaced-keys
  "Set the namespace of all map keys (non recursive)."
  [e ns]
  (reduce-kv
    (fn [x k v]
      (assoc x (set-ns ns k) v))
    {}
    e))

(defn pull-key
  "Pull some key"
  [x key]
  (-> (dissoc x key)
      (merge (get x key))))

(defn pull-namespaced
  "Pull some key, updating the namespaces of it"
  [x key ns]
  (-> (dissoc x key)
      (merge (namespaced-keys (get x key) ns))))
;endregion

(defn update-if [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

(defn adapt-core [core]
  (-> core
      (namespaced-keys "spacex.core")))

(defn adapt-payload [payload]
  (-> payload
      (namespaced-keys "spacex.payload")
      (pull-namespaced :spacex.payload/orbit-params "spacex.payload.orbit-params")))

(defn adapt-rocket [rocket]
  (-> rocket
      (namespaced-keys "spacex.rocket")
      (pull-namespaced :spacex.rocket/first-stage "spacex.launch.first-stage")
      (update-if :spacex.launch.first-stage/cores #(mapv adapt-core %))
      (pull-namespaced :spacex.rocket/second-stage "spacex.launch.second-stage")
      (update-if :spacex.launch.second-stage/payloads #(mapv adapt-payload %))
      (pull-namespaced :spacex.rocket/fairings "spacex.launch.fairings")))

(defn adapt-launch-pad [launch-pad]
  (-> launch-pad
      (namespaced-keys "spacex.launch-pad")))

(defn adapt-launch [launch]
  (-> launch
      (update :launch_site adapt-launch-pad)
      (update :rocket adapt-rocket)
      (namespaced-keys "spacex.launch")
      (pull-key :spacex.launch/launch-site)
      (pull-key :spacex.launch/rocket)
      (pull-namespaced :spacex.launch/links "spacex.launch.links")
      (pull-namespaced :spacex.launch/telemetry "spacex.launch.telemetry")))

(def launch-out
  [:spacex.launch/details
   :spacex.launch/flight-number
   :spacex.launch/is-tentative
   :spacex.launch/launch-date-local
   :spacex.launch/launch-date-unix
   :spacex.launch/launch-date-utc
   :spacex.launch/launch-success
   :spacex.launch/launch-year
   :spacex.launch/mission-id
   :spacex.launch/mission-name
   :spacex.launch/ships
   :spacex.launch/static-fire-date-unix
   :spacex.launch/static-fire-date-utc
   :spacex.launch/tentative-max-precision
   :spacex.launch/upcoming
   :spacex.launch-pad/site-id
   :spacex.launch-pad/site-name
   :spacex.launch-pad/site-name-long
   :spacex.launch.fairings/recovered
   :spacex.launch.fairings/recovery-attempt
   :spacex.launch.fairings/reused
   :spacex.launch.fairings/ship
   {:spacex.launch.first-stage/cores [:spacex.core/block
                                      :spacex.core/core-serial
                                      :spacex.core/flight
                                      :spacex.core/gridfins
                                      :spacex.core/land-success
                                      :spacex.core/landing-intent
                                      :spacex.core/landing-type
                                      :spacex.core/landing-vehicle
                                      :spacex.core/legs
                                      :spacex.core/reused]}
   :spacex.launch.links/article-link
   :spacex.launch.links/flickr-images
   :spacex.launch.links/mission-patch
   :spacex.launch.links/mission-patch-small
   :spacex.launch.links/video-link
   :spacex.launch.links/wikipedia
   :spacex.launch.second-stage/block
   {:spacex.launch.second-stage/payloads [:spacex.payload/customers
                                          :spacex.payload/manufacturer
                                          :spacex.payload/nationality
                                          :spacex.payload/norad-id
                                          :spacex.payload/orbit
                                          :spacex.payload/payload-id
                                          :spacex.payload/payload-mass-kg
                                          :spacex.payload/payload-mass-lbs
                                          :spacex.payload/payload-type
                                          :spacex.payload/reused
                                          :spacex.payload.orbit-params/apoapsis-km
                                          :spacex.payload.orbit-params/arg-of-pericenter
                                          :spacex.payload.orbit-params/eccentricity
                                          :spacex.payload.orbit-params/epoch
                                          :spacex.payload.orbit-params/inclination-deg
                                          :spacex.payload.orbit-params/lifespan-years
                                          :spacex.payload.orbit-params/longitude
                                          :spacex.payload.orbit-params/mean-anomaly
                                          :spacex.payload.orbit-params/mean-motion
                                          :spacex.payload.orbit-params/periapsis-km
                                          :spacex.payload.orbit-params/period-min
                                          :spacex.payload.orbit-params/raan
                                          :spacex.payload.orbit-params/reference-system
                                          :spacex.payload.orbit-params/regime
                                          :spacex.payload.orbit-params/semi-major-axis-km
                                          :spacex.payload/cap-serial
                                          :spacex.payload/cargo-manifest
                                          :spacex.payload/flight-time-sec
                                          :spacex.payload/mass-returned-kg
                                          :spacex.payload/mass-returned-lbs]}
   :spacex.launch.telemetry/flight-club
   :spacex.rocket/rocket-id
   :spacex.rocket/rocket-name
   :spacex.rocket/rocket-type
   :spacex.launch.links/presskit
   :spacex.launch.links/reddit-campaign
   :spacex.launch.links/reddit-launch
   :spacex.launch.links/reddit-media
   :spacex.launch.links/reddit-recovery])

(pc/defresolver all-launches
  [env _]
  {::pc/output [{:spacex/all-launches launch-out}]}
  (go-catch
    (->> (p.http/request env "https://api.spacexdata.com/v3/launches"
                         {::p.http/accept ::p.http/json}) <?maybe
         ::p.http/body
         (mapv adapt-launch)
         (hash-map :spacex/all-launches))))

(pc/defresolver past-launches
  [env _]
  {::pc/output [{:spacex/past-launches launch-out}]}
  (go-catch
    (->> (p.http/request env "https://api.spacexdata.com/v3/launches/past?limit=10"
                         {::p.http/accept ::p.http/json}) <?maybe
         ::p.http/body
         (mapv adapt-launch)
         (hash-map :spacex/past-launches))))

(pc/defresolver upcoming-launches
  [env _]
  {::pc/output [{:spacex/upcoming-launches launch-out}]}
  (go-catch
    (->> (p.http/request env "https://api.spacexdata.com/v3/launches/upcoming"
                         {::p.http/accept ::p.http/json}) <?maybe
         ::p.http/body
         (mapv adapt-launch)
         (hash-map :spacex/upcoming-launches))))

(pc/defresolver one-launch
  [env {:spacex.launch/keys [flight-number]}]
  {::pc/input     #{:spacex.launch/flight-number}
   ::pc/output    launch-out
   ::pc/transform (pc/transform-auto-batch 10)}
  (go-catch
    (->> (p.http/request env (str "https://api.spacexdata.com/v3/launches/" flight-number)
                         {::p.http/accept ::p.http/json}) <?maybe
         ::p.http/body
         adapt-launch)))

(pc/defresolver latest-launch
  [env _]
  {::pc/output [{:spacex/latest-launch launch-out}]}
  (go-catch
    (->> (p.http/request env "https://api.spacexdata.com/v3/launches/latest"
                         {::p.http/accept ::p.http/json}) <?maybe
         ::p.http/body
         adapt-launch
         (hash-map :spacex/latest-launch))))

(def resolvers
  [all-launches past-launches upcoming-launches one-launch latest-launch])

(defn spacex-plugin []
  {::pc/register resolvers})

(comment

  (gstr/replaceAll "rocket_type" "_" "-")

  (->> launches
       (mapv adapt-launch)
       (hash-map :spacex/all-launches)
       (pc/data->shape))

  (->> launches
       (mapv :links))

  (-> launches first)

  (-> latest-launch)

  )

;;;;;;;;;;;;;;;;;;;
;; from conj2018 file
;;;;;;;;;;;;;;;;;;;


(defonce gql-indexes (atom {}))

(def http-driver
  #?(:clj  p.http.clj/request
     :cljs p.http.fetch/request-async))

;(def youtube-token
;  #?(:clj  ""
;     :cljs (ls/get :youtube/token)))

(def youtube-token )

;endregion

;region 1
(pc/defresolver full-name-resolver
  [env {:keys [first-name last-name]}]
  {::pc/input  #{:first-name :last-name}
   ::pc/output [:full-name]}
  {:full-name (str first-name " " last-name)})

(comment
  (entity-parse {:first-name "Wilker" :last-name "Silva"}
                [:full-name]))
;endregion

;region 2
(def email-db
  {"elaina.lind@gmail.com"
   {:first-name "Elaina" :last-name "Lind"}

   "shanna.harber@yahoo.com"
   {:first-name "Shanna" :last-name "Harber"}

   "sydni.considine@gmail.com"
   {:first-name "Sydni" :last-name "Considine"}

   "margaret.brakus@gmail.com"
   {:first-name "Margaret" :last-name "Brakus"}

   "delaney.wehner@hotmail.com"
   {:first-name "Delaney" :last-name "Wehner"}})

(pc/defresolver email->name
  [env {:keys [email]}]
  {::pc/input  #{:email}
   ::pc/output [:first-name :last-name]}
  (get email-db email {}))

(comment
  (entity-parse {:email "elaina.lind@gmail.com"}
                [:full-name]))

(comment
  (pc/compute-paths (::pc/index-oir @indexes) #{:email} #{}
                    :full-name))
;endregion

;region 3
(pc/defresolver all-emails
  [env _]
  {::pc/output [{:all-emails [:email]}]}
  {:all-emails (->> email-db keys (mapv #(hash-map :email %)))})

(comment
  (entity-parse {} [{:all-emails [:full-name]}]))

(comment
  ; ident joins
  (entity-parse {}
                [{[:email "elaina.lind@gmail.com"]
                  [:full-name]}]))
;endregion

;region 4
(pc/defresolver the-answer [_ _]
  {::pc/output [:answer-of-everything]}
  {:answer-of-everything 42})

(comment
  (entity-parse {}
                [{:all-emails [:email :answer-of-everything]}]))
;endregion

;region 5
(def host-by-domain
  {"gmail.com"   {:host/domain "gmail.com"
                  :host/name   "Google Email"}
   "hotmail.com" {:host/domain "hotmail.com"
                  :host/name   "Microsoft Hotmail"}
   "yahoo.com"   {:host/domain "yahoo.com"
                  :host/name   "Yahoo Mail"}})

(pc/defresolver email->domain [_ {:keys [email]}]
  {::pc/input  #{:email}
   ::pc/output [:host/domain]}
  (if-let [[_ domain] (re-find #"@(.+)" email)]
    {:host/domain domain}))

(comment
  (entity-parse {:email "elaina.lind@gmail.com"}
                [:host/domain]))

(pc/defresolver host [_ {:host/keys [domain]}]
  {::pc/input  #{:host/domain}
   ::pc/output [:host/domain
                :host/name]}
  (get host-by-domain domain))

(comment
  (entity-parse {:email "elaina.lind@gmail.com"}
                [:host/name]))

(comment
  (entity-parse {:email "elaina.lind@gmail.com"}
                [:email
                 {:>/cascas [:host/domain :host/name]
                  }]))
;endregion

;region parser
(def app-registry
  [full-name-resolver email->name all-emails the-answer
   email->domain host
   (pc/alias-resolver :spacex.launch.links/video-link :youtube.video/url)
   (pc/alias-resolver :conj-pathom.favorite-launch/flight-number :spacex.launch/flight-number)
   ])

(def indexes (atom @gql-indexes))

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}
                  ::youtube/access-token   youtube-token
                  ::p.http/driver          http-driver}
     ::p/plugins [(pc/connect-plugin {::pc/register app-registry
                                      ::pc/indexes  indexes})
                  (spacex-plugin)
                  p/error-handler-plugin
                  p/trace-plugin]}))
;endregion

;region
(def graphql-config
  {::pcg/url       "https://api.graph.cool/simple/v1/cjo58bqvp7k070194uq8ff9g9"
   ::pcg/prefix    "conj-pathom"
   ::pcg/ident-map {}
   ::p.http/driver http-driver})

(comment
  (go
    (reset! gql-indexes (<! (pcg/load-index graphql-config)))
    (swap! indexes pc/merge-indexes @gql-indexes)))
;endregion

;region entity-parse
#?(:clj
   (defn entity-parse [entity query]
     (<!! (parser {::p/entity (atom entity)} query))))
;endregion
