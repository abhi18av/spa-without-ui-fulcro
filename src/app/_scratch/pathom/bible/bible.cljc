(ns ^{:author "Abhinav Sharma (@abhi18av)"
      :doc    "Wraps the api for https://scripture.api.bible/livedocs "}

 app.-scratch.pathom.bible.bible

  (:require [app.secrets :as secrets]
            [clojure.string :as str]

            #?(:clj  [clj-http.client :as client])

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




;;;;;;;;;;;


(def token secrets/token-bible)


(defn api [{::keys [endpoint token]
            :or    {method :get}}]
  (->
   (com.wsscode.pathom.diplomat.http.clj-http/request
    {::http/url     (str "https://api.scripture.api.bible/v1/bibles" endpoint)
     ::http/headers {:api-key token}
     ::http/as      ::http/json
     ::http/method  "get"})
   :com.wsscode.pathom.diplomat.http/body))

(api {::token token ::endpoint ""})



(comment

  ;; Get all bibles

  (api {::token token ::endpoint " "})
  ;; Get a bible
  (api {::token token ::endpoint "/90799bb5b996fddc-01"})
  ;; Get all books in bible
  (api {::token token ::endpoint "/90799bb5b996fddc-01/books"})
  ;; Get all chapters of book in bible
  (api {::token token ::endpoint "/90799bb5b996fddc-01/books/LUK/chapters"})
  ;; Get a chapter
  (api {::token token ::endpoint "/90799bb5b996fddc-01/chapters/LUK.22"})
  ;; Get all passages in a chapter
  (api {::token token ::endpoint "/90799bb5b996fddc-01/passages/LUK.22"})
  ;; Get all verses in a chapter
  (api {::token token ::endpoint "/90799bb5b996fddc-01/chapters/LUK.22/verses"})
  ;; Get a verse
  (api {::token token ::endpoint "/90799bb5b996fddc-01/verses/LUK.22.11"})
  ;; Get all books
  (api {::token token ::endpoint "/90799bb5b996fddc-01/books/LUK/sections"})
  ;; Get all sections of a chapter
  (api {::token token ::endpoint "/90799bb5b996fddc-01/chapters/LUK.22/sections"})
  ;; Get a section
  (api {::token token ::endpoint "/90799bb5b996fddc-01/sections/LUK.S131"})
  ;; Search a bible
  (api {::token token ::endpoint "/90799bb5b996fddc-01/search?query=hoje"})

  '())


;;;;;;;;;;;


(defn set-ns
  " Set the namespace of a keyword "
  [ns kw]
  (keyword ns (name kw)))

(defn set-ns-seq
  " Set the namespace for all keywords in a collection. The collection kind will
                                                              be preserved. "
  [ns s]
  (into (empty s) (map #(set-ns ns %)) s))

(defn set-ns-x
  " Set the namespace of a value. If sequence will use set-ns-seq. "
  [ns x]
  (if (coll? x)
    (set-ns-seq ns x)
    (set-ns ns x)))

(defn namespaced-keys
  " Set the namespace of all map keys (non recursive) . "
  [e ns]
  (reduce-kv
   (fn [x k v]
     (assoc x (set-ns-x ns k) v))
   {}
   e))

(defn pull-namespaced
  " Pull some key, updating the namespaces of it "
  [m key ns]
  (-> (dissoc m key)
      (merge (namespaced-keys (get m key) ns))))

(defn update-if [m k f & args]
  (if (contains? m k)
    (apply update m k f args)
    m))



;;;;;;;;;;;;;;;;;;;;;
;; BIBLES
;;;;;;;;;;;;;;;;;;;;;


(defn adapt-bible [a-bible]
  (-> a-bible
      (namespaced-keys " bible ")
      (pull-namespaced :bible/data " bible.data ")))

(->
 (api {::token token ::endpoint " /90799bb5b996fddc-01 "})
 (namespaced-keys " bible ")
 (pull-namespaced :bible/data " bible.data "))

(defn adapt-language [a-bible]
  (-> a-bible
      (pull-namespaced :bible.data/language " bible.data.language ")))

;; TODO
(defn adapt-country [a-country]
  (-> a-country
      (namespaced-keys " bible.data.countries ")))

(defn bible-by-id [env {:bible.data/keys [id]}]
  (->> {::endpoint (str " / " id)}
       (merge env)
       (api)
       (adapt-bible)
       (adapt-language)
       #_(adapt-country)))

(def indexes
  (-> {}
      (pc/add `bible-by-id
              {::pc/input  #{:bible.data/id}

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

(def parser (p/parser {::p/plugins [(p/env-plugin {::p/reader   [p/map-reader
                                                                 pc/all-readers]
                                                   ::pc/indexes indexes
                                                   ::token      token})]}))

(comment
  (parser {}
          [{[:bible.data/id " 90799bb5b996fddc-01 "]
            [:bible.data/updatedAt
             :bible.data.language/script]}]))



;;;;;;;;;;;;;;;;;;;;;
;; BOOKS
;;;;;;;;;;;;;;;;;;;;;


(comment
  (api {::token token ::endpoint " /90799bb5b996fddc-01/books "})

  '())

;; Get all books in bible
(->
 (api {::token token ::endpoint " /90799bb5b996fddc-01/books "})
 :data
 (pull-namespaced :books/data " data "))








;;;;;;;;;;;;;;;;;;;;;
;; CHAPTERS
;;;;;;;;;;;;;;;;;;;;;


;; Get all chapters of book in bible


(->
 (api {::token token ::endpoint " /90799bb5b996fddc-01/books/LUK/chapters "})
 (namespaced-keys " chapters "))



;; Get a chapter


(->
 (api {::token token ::endpoint " /90799bb5b996fddc-01/chapters/LUK.22 "})
 (namespaced-keys " chapter "))



;;;;;;;;;;;;;;;;;
