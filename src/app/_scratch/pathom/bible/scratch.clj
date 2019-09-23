(ns ^{:author "Abhinav Sharma (@abhi18av)"
      :doc    "Wraps the api for https://scripture.api.bible/livedocs "}

  app.-scratch.pathom.bible.scratch

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



;;;;;;;;;;;


(def token secrets/token-bible)

(def bible-pt-br
  (-> (http/request {:method  :get
                     :headers {:api-key token}
                     :url     "https://api.scripture.api.bible/v1/bibles/90799bb5b996fddc-01"})
      :body
      json/read-str))


(comment

  (-> (http/request {:method  :get
                     :headers {:api-key token}
                     :url     "https://api.scripture.api.bible/v1/bibles/90799bb5b996fddc-01"})
      :body
      json/read-str))



;;;;;;;;;;;


(defn adapt-key [k]
  (str/replace k #"_" "-"))

(comment
  (adapt-key "abcd_efgh"))




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
      (assoc x (set-ns ns k) v))
    {}
    e))

(comment
  (namespaced-keys {:a 1 :b 2} "an-ns"))




(defn pull-key
  "Pull some key"
  [m key]
  (-> (dissoc m key)
      (merge (get m key))))

(comment
  (pull-key {:a 1 :b 2} :an))



(defn pull-namespaced
  "Pull some key, updating the namespaces of it"
  [m key ns]
  (-> (dissoc m key)
      (merge (namespaced-keys (get m key) ns))))



(comment
  (pull-namespaced {:a 1 :b 2} :an-ns "an-ns")
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
      #_(pull-namespaced :bible.data/countries "bible.data.countries")
      (pull-namespaced :bible.data/language "bible.data.language")))


(defn adapt-data [payload]
  (-> payload
      (namespaced-keys "bible")
      (pull-namespaced :bible/data "bible.data")))

(comment
  (-> bible-pt-br
      adapt-data))

;; TODO
;; (defn adapt-countries [payload]
;;   (-> payload
;;       (pull-namespaced :bible.data/countries "bible.data.countries")))

;; (comment
;;   (-> bible-pt-br
;;       adapt-data
;;       :bible.data/countries
;;       #_(pull-namespaced :bible.data/countries "bible.data.countries")))

(defn adapt-language [a-bible]
  (-> a-bible
      (pull-namespaced :bible.data/language "bible.data.language")))

(comment
  (-> bible-pt-br
      adapt-data
      adapt-language))


;;;;;;;;;;;



(defonce indexes (atom {}))





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




(def parser
  (p/parallel-parser
   {::p/env     {::p/reader               [p/map-reader
                                           pc/parallel-reader
                                           pc/open-ident-reader
                                           p/env-placeholder-reader]
                 ::p/placeholder-prefixes #{">"}
                 ::p.http/driver          http-driver}
    ::p/plugins [(pc/connect-plugin {::pc/register app-registry
                                     ::pc/indexes  indexes})
                 (spacex-plugin)
                 p/error-handler-plugin
                 p/trace-plugin]}))



#?(:clj
   (defn entity-parse [entity query]
     (<!! (parser {::p/entity (atom entity)} query))))









