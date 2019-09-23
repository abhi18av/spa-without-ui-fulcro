(ns ^{:author "Abhinav Sharma (@abhi18av)"
      :doc    "Wraps the api for https://scripture.api.bible/livedocs "}

 app.-scratch.pathom.bible.scratch

  (:require [app.secrets :as secrets]
            [clojure.core.async :refer [go timeout <! <!!]]
            [clj-http.client :as http]
            [clojure.string :as str]
            [com.wsscode.common.async-clj :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]
            [com.wsscode.pathom.diplomat.http :as p.http.clj]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql :as pcg]
            #_[com.wsscode.pathom.diplomat.http :as p.http]))



;;;;;;;;;;;


(def token secrets/token-bible)

(defn api [{::keys [endpoint method token]
            :or    {method :get}}]
  (-> (http/request {:method  method
                     :headers {:api-key token}
                     :as      :json
                     :url     (str "https://api.scripture.api.bible/v1/bibles/" endpoint)})
      :body))

(comment
  (api {::token token ::endpoint "90799bb5b996fddc-01"}))

(def bible-pt-br
  (api {::token token ::endpoint "90799bb5b996fddc-01"}))


;;;;;;;;;;;


(defn adapt-key [k]
  (str/replace k #"_" "-"))

(defn set-ns
  "Set the namespace of a keyword"
  [ns kw]
  (keyword ns (adapt-key (name kw))))

(comment
  (set-ns "an-ns" :a_key))

(defn set-ns-seq
  "Set the namespace for all keywords in a collection. The collection kind will
  be preserved."
  [ns s]
  (into (empty s) (map #(set-ns ns %)) s))

(comment
  (set-ns-seq "an-ns" [:a_key1 :a_key2]))

(defn set-ns-x
  "Set the namespace of a value. If sequence will use set-ns-seq."
  [ns x]
  (if (coll? x)
    (set-ns-seq ns x)
    (set-ns ns x)))

(comment
  (set-ns-x "an-ns" :a_key)
  (set-ns-x "an-ns" [:a_key1 :a_key2]))

(defn namespaced-keys
  "Set the namespace of all map keys (non recursive)."
  [e ns]
  (reduce-kv
   (fn [x k v]
     (assoc x (set-ns-x ns k) v))
   {}
   e))

(comment
  (namespaced-keys bible-pt-br "bible")
  (pc/data->shape bible-pt-br))

(defn pull-namespaced
  "Pull some key, updating the namespaces of it"
  [m key ns]
  (-> (dissoc m key)
      (merge (namespaced-keys (get m key) ns))))

(comment
  (pull-namespaced (first [{:a 1 :b 2 :c 3}]) :an-ns "an-ns"))

(defn update-if [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

;; TODO
(comment)



;;;;;;;;;;;


(comment
  (-> bible-pt-br
      (namespaced-keys "bible")
      (pull-namespaced :bible/data "bible.data")
      (pull-namespaced :bible.data/language "bible.data.language")
      ;; TODO
      #_(pull-namespaced :bible.data/countries "bible.data.countries")))

(pc/data->shape bible-pt-br)






;;;;;;;;;;;;


(defn adapt-bible [a-bible]
  (-> a-bible
      (namespaced-keys "bible")
      (pull-namespaced :bible/data "bible.data")))

(defn adapt-language [a-bible]
  (-> a-bible
      (pull-namespaced :bible.data/language "bible.data.language")))


;; NOTE this function is actually a resolver


(defn bible-by-id [env {:bible.data/keys [id]}]
  (->> {::endpoint id}
       (merge env)
       (api)
       (adapt-bible)
       (adapt-language)))

(pc/data->shape
 (bible-by-id {::token token} {:bible.data/id  "90799bb5b996fddc-01"}))

(def indexes
  (-> {}
      (pc/add `bible-by-id
              {::pc/input #{:bible.data/id}

               ::pc/output [:bible.data.language/id
                            :bible.data.language/name
                            :bible.data.language/nameLocal
                            :bible.data.language/script
                            :bible.data.language/scriptDirection
                            :bible.data/abbreviation
                            :bible.data/abbreviationLocal
                            :bible.data/audioBibles
                            :bible.data/copyright
                            #:bible.data{:countries [:id :name :nameLocal]}
                            :bible.data/dblId
                            :bible.data/description
                            :bible.data/descriptionLocal
                            :bible.data/id
                            :bible.data/info
                            :bible.data/name
                            :bible.data/nameLocal
                            :bible.data/relatedDbl
                            :bible.data/type
                            :bible.data/updatedAt]})))

(def parser (p/parser {}))

(parser {::p/reader [p/map-reader
                     pc/all-readers]
         ::pc/indexes indexes
         ::token token}

        [{[:bible.data/id  "90799bb5b996fddc-01"]

          [#_:bible.data/updatedAt
           #_:bible.data/language
           :bible.data.language/script

           :bible.data/countries]}])







;;;;;;;;;;;;


(def indexes (atom {}))

(pc/defresolver language-resolver [_ {:host/keys [domain]}]
  {::pc/input  #{:bible/data}
   ::pc/output [:bible.data/languages]}
  (get host-by-domain domain))

(def app-registry
  [language-resolver])

(def parser
  (p/parallel-parser
   {::p/env     {::p/reader               [p/map-reader
                                           pc/parallel-reader
                                           pc/open-ident-reader
                                           p/env-placeholder-reader]
                 ::p/placeholder-prefixes #{">"}}
    ::p/plugins [(pc/connect-plugin {::pc/register app-registry
                                     ::pc/indexes  indexes})
                 p/error-handler-plugin
                 p/trace-plugin]}))

#?(:clj
   (defn entity-parse [entity query]
     (<!! (parser {::p/entity (atom entity)} query))))











