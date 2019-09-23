(ns com.wsscode.pathom.connect.scratch-yt
  (:require [app.secrets :as secrets]
            [clojure.core.async :refer [go timeout <!]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql :as pcg]
            [com.wsscode.pathom.connect.youtube :as youtube]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.diplomat.http :as p.http]
            [com.wsscode.pathom.diplomat.http.fetch :as p.http.fetch]
            [nubank.workspaces.lib.local-storage :as ls]))



(defonce gql-indexes (atom {}))

(def http-driver
  p.http.fetch/request-async)

(def youtube-token secrets/token-yt)

;endregion

;region 1
(pc/defresolver full-name-resolver
  [env {:keys [first-name last-name]}]
  {::pc/input  #{:first-name :last-name}
   ::pc/output [:full-name]}
  {:full-name (str first-name " " last-name)})

(comment
  (entity-parse {:first-name "Wilker" :last-name "Silva"}
                [:full-name])

  )
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
                [:full-name])

  )

(comment
  (pc/compute-paths (::pc/index-oir @indexes) #{:email} #{}
                    :full-name)

  )
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
   (pc/alias-resolver #_:spacex.launch.links/video-link :youtube.video/url)
   (pc/alias-resolver :conj-pathom.favorite-launch/flight-number #_:spacex.launch/flight-number)
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
                  (youtube/youtube-plugin)
                  #_(spacex/spacex-plugin)
                  p/error-handler-plugin
                  p/trace-plugin]}))
;endregion

;region 6
(def graphql-config
  {::pcg/url       "https://api.graph.cool/simple/v1/cjo58bqvp7k070194uq8ff9g9"
   ::pcg/prefix    "conj-pathom"
   ::pcg/ident-map {}
   ::p.http/driver http-driver})

(comment
  (go
    (reset! gql-indexes (<! (pcg/load-index graphql-config)))
    (swap! indexes pc/merge-indexes @gql-indexes))
  )
;endregion

;region entity-parse
#?(:clj
   (defn entity-parse [entity query]
     (<!! (parser {::p/entity (atom entity)} query))))
;endregion

(defn entity-parse [entity query]

  j)