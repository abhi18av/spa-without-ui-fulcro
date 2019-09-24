(ns app.-scratch.pathom.bible.archive


  (:require [app.secrets :as secrets]
            [clojure.string :as str]

            #?(:clj [clj-http.client :as client])

            [clojure.core.async :refer [go timeout <! take! #?(:clj <!!)]]

            #?(:clj  [com.wsscode.pathom.diplomat.http.clj-http :as ptthp.clj]
               :cljs [com.wsscode.pathom.diplomat.http.fetch :as phttp.fetch])

            [#?(:clj  com.wsscode.common.async-clj
                :cljs com.wsscode.common.async-cljs)
             :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]

            [com.wsscode.pathom.diplomat.http :as http]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql :as pcg]))




(def token secrets/token-bible)



;
;;; PATHOM CLJ - Works
;(defn api [{::keys [endpoint token]
;            :or    {method :get}}]
;  (->
;    (com.wsscode.pathom.diplomat.http.clj-http/request
;      {::http/url     (str "https://api.scripture.api.bible/v1/bibles" endpoint)
;       ::http/headers {:api-key token}
;       ::http/as      ::http/json
;       ::http/method  "get"})
;    :com.wsscode.pathom.diplomat.http/body))
;





;
;;; CLJ-http - Works
;(defn api [{::keys [endpoint token]}]
;  (-> (client/request {:method  :get
;                       :headers {:api-key token}
;                       :as      :json
;                       :url     (str "https://api.scripture.api.bible/v1/bibles" endpoint)})
;      :body))







; CLJS


(defn api [{::keys [endpoint token]}]
  (go
    (->
      (<? (phttp.fetch/request-async {::http/url     (str "https://api.scripture.api.bible/v1/bibles/" endpoint)
                                      ::http/headers {:api-key token}
                                      ::http/as      ::http/json
                                      ::http/method  "get"}))
      :com.wsscode.pathom.diplomat.http/body)))

(take! (api {::token token ::endpoint ""}) prn)


(comment
  ;; kept running  into
  ;; Access to fetch at 'https://api.scripture.api.bible/v1/bibles/' from origin 'http://localhost:8000' has been blocked by CORS policy: Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource. If an opaque response serves your needs, set the request's mode to 'no-cors' to fetch the resource with CORS disabled.
  )



(take!
  (phttp.fetch/request-async {::http/url     "https://api.scripture.api.bible/v1/bibles"
                              #_#_::http/url "https://en3k20g3pbhfn.x.pipedream.net"
                              ::http/headers {:api-key token
                                              #_#_:mode :no-cors
                                              #_#_:Access-Control-Allow-Origin "http://localhost:8000"}
                              ::http/as      ::http/json
                              ::http/method  "get"})
  prn)


(take! (phttp.fetch/request-async {::http/url    " https:// pokeapi.co / api/v2/pokemon/1 "
                                   ::http/as     ::http/json
                                   ::http/method " get "})
       prn)

(go
  (->
    (<! (phttp.fetch/request-async {::http/url    " https:// pokeapi.co / api/v2/pokemon/1 "
                                    ::http/as     ::http/json
                                    ::http/method " get "}))
    :com.wsscode.pathom.diplomat.http/body
    prn))

(comment

  (go
    (->
      (<! (phttp.fetch/request-async {::http/url    " https:// pokeapi.co / api/v2/pokemon/1 "
                                      ::http/as     ::http/json
                                      ::http/method " get "}))
      :com.wsscode.pathom.diplomat.http/body
      ;;:name
      prn))

  (go
    (take! (fetch/request-async {::http/url    " https:// pokeapi.co / api/v2/pokemon/1 "
                                 ::http/as     ::http/json
                                 ::http/method " get "})
           prn))

  (go
    (-> (fetch/request-async {::http/url    " https:// pokeapi.co / api/v2/pokemon/1 "
                              ::http/as     ::http/json
                              ::http/method " get "})
        (take! println)))

  (def memory (atom {}))

  (take!
    (fetch/request-async {::http/url    " https:// pokeapi.co / api/v2/pokemon/1 "
                          ::http/as     ::http/json
                          ::http/method " get "})
    #(reset! memory %))

  @memory)

