(ns app.-scratch.pathom.spacex.scratch
  (:require [clojure.core.async :refer [go timeout <! <!!]]
            [clojure.data.json :as json]
            [clj-http.client :as http]
            [clojure.string :as str]
            [com.wsscode.common.async-clj :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]
            [com.wsscode.pathom.diplomat.http :as p.http.clj]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.connect.graphql :as pcg]
            #_[com.wsscode.pathom.diplomat.http :as p.http]))




(def latest-launch (-> (http/request {:method  :get
                                      :url     "https://api.spacexdata.com/v3/launches/latest"})
                    :body
                    json/read-str))


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


(comment


  (-> (http/request {:method  :get
                     :url     "https://api.spacexdata.com/v3/cores/B1042"})
      :body
      json/read-str
      adapt-core))




(defn adapt-payload [payload]
  (-> payload
      (namespaced-keys "spacex.payload")
      (pull-namespaced :spacex.payload/orbit-params "spacex.payload.orbit-params")))


(comment
  (-> (http/request {:method  :get
                     :url     "https://api.spacexdata.com/v3/payloads/Telkom-4"})
      :body
      json/read-str
      adapt-payload))




(defn adapt-rocket [rocket]
  (-> rocket
      (namespaced-keys "spacex.rocket")
      (pull-namespaced :spacex.rocket/first-stage "spacex.launch.first-stage")
      (update-if :spacex.launch.first-stage/cores #(mapv adapt-core %))
      (pull-namespaced :spacex.rocket/second-stage "spacex.launch.second-stage")
      (update-if :spacex.launch.second-stage/payloads #(mapv adapt-payload %))
      (pull-namespaced :spacex.rocket/fairings "spacex.launch.fairings")))



(comment
  (-> (http/request {:method  :get
                     :url     "https://api.spacexdata.com/v3/rockets/falcon9"})
      :body
      println
      json/read-str
      adapt-rocket))
