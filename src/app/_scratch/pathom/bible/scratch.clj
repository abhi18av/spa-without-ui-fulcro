(ns app.-scratch.pathom.bible.scratch

  "Wraps the api for https://scripture.api.bible/livedocs "

  (:require [app.secrets :as secrets]
            [clojure.core.async :refer [go timeout <! <!!]]
            [clojure.data.json :as json]
            [clj-http.client :as http]
            [clojure.string :as str]
            [com.wsscode.common.async-clj :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]
            [com.wsscode.pathom.diplomat.http :as p.http.clj]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql :as pcg]
            #_[com.wsscode.pathom.diplomat.http :as p.http]))




(def token secrets/token-bible)


(-> (http/request {:method  :get
                   :headers {:api-key token}
                   :url     "https://api.scripture.api.bible/v1/bibles/90799bb5b996fddc-01"})
   :body
   json/read-str)


