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
  (pull-namespaced {:a 1 :b 2} :an-ns "an-ns"))




(defn update-if [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

;; TODO
(comment)



;;;;;;;;;;;


(defn adapt-toplevel [core]
  (-> core
      (namespaced-keys "bible.core")))

(comment
  (-> bible-pt-br
      adapt-toplevel
      first))


(defn adapt-data [payload]
  (-> payload
      (namespaced-keys "bible")
      (pull-namespaced :bible.data "bible.data")))

(comment
  (-> bible-pt-br
      adapt-data
      #_:bible/data))

(defn adapt-language [a-bible]
  (-> a-bible
      adapt-data
      :bible/data
      (namespaced-keys "laanguage")
      (pull-namespaced :bible.data/language "bible.data/language")))

(comment
  (-> bible-pt-br
      adapt-data
      #_:bible/data
      (namespaced-keys "bible")
      #_:bible.data/language
      (pull-namespaced :bible.data/language "bible.data.language")))

