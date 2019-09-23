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

;;;;;;;;;;;


;region entity helpers
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
  [x key]
  (-> (dissoc x key)
      (merge (get x key))))

(comment
  ;; TODO
  (let [x {:a 1 :b 2}
        key "a"]
    (-> (dissoc x key)
        (merge (get x key))))

  (dissoc {:a 1 :b 2} :a)
  (get {:a 1} :a)
  (pull-key {:a 1 :b 2} :a))

(pull-key {:a 1 :b 2} :a)



(defn pull-namespaced
  "Pull some key, updating the namespaces of it"
  [x key ns]
  (-> (dissoc x key)
      (merge (namespaced-keys (get x key) ns))))



;; TODO
(comment)

(pull-namespaced {:a 1 :b 2} :a "an-ns")




(defn update-if [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))

;; TODO
(comment)




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


(defn adapt-toplevel [core]
  (-> core
      (namespaced-keys "bible.core")))

(comment
  (-> bible-pt-br
      adapt-toplevel))


(defn adapt-data [payload]
  (-> payload
      (namespaced-keys "bible.data")
      (pull-namespaced :bible.data "data")))

(comment
  (-> bible-pt-br
      adapt-data))

(defn adapt-rocket [rocket]
  (-> rocket
      (namespaced-keys "spacex.rocket")
      (pull-namespaced :spacex.rocket/first-stage "spacex.launch.first-stage")
      (update-if :spacex.launch.first-stage/cores #(mapv adapt-core %))
      (pull-namespaced :spacex.rocket/second-stage "spacex.launch.second-stage")
      (update-if :spacex.launch.second-stage/payloads #(mapv adapt-payload %))
      (pull-namespaced :spacex.rocket/fairings "spacex.launch.fairings")))

(comment
  (-> (http/request {:method :get
                     :url    "https://api.spacexdata.com/v3/rockets/falcon9"})
      :body
      println
      json/read-str
      adapt-rocket))
