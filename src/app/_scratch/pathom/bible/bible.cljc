(ns ^{:author "Abhinav Sharma (@abhi18av)"
      :doc    "Wraps the api for https://scripture.api.bible/livedocs "}

  app.-scratch.pathom.bible.bible

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




;;;;;;;;;;;


(def token secrets/token-bible)

(def memory (atom {}))

(defn api [{::keys [endpoint token]}]
  (take!
    (phttp.fetch/request-async {::http/url     (str "https://api.scripture.api.bible/v1/bibles" endpoint)
                                ::http/headers {:api-key token}
                                ::http/as      ::http/json
                                ::http/method  "get"})
    #(reset! memory (:com.wsscode.pathom.diplomat.http/body %))))


(comment

  @memory


  ;; Get all bibles

  (api {::token token ::endpoint ""})
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
  " Set the namespace for all keywords in a collection. The collection kind will be preserved. "
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


;; TODO adapt-bible
;; TODO adapt-language
;; TODO adapt-country

@memory

(api {::token token ::endpoint "/90799bb5b996fddc-01"})

(->
  @memory
  (namespaced-keys "bible")
  (pull-namespaced :bible/data "bible.data")
  (pull-namespaced :bible.data/language "bible.data.language")
  (pc/data->shape))



(def bible-br
  (->
    @memory
    (namespaced-keys "bible")
    (pull-namespaced :bible/data "bible.data")
    (pull-namespaced :bible.data/language "bible.data.language")))


(def bible {:bible.data/updatedAt                "2019-08-16T05:57:31.000Z",
            :bible.data/abbreviationLocal        "TfTP",
            :bible.data/countries                [{:id "BR", :name "Brazil", :nameLocal "Brazil"}],
            :bible.data/info                     "<p>This text is provided courtesy of Ellis W. Deibler, Jr. Proofreading and spelling correction by Ethnos 360 (New Tribes Mission) is gratefully acknowledged.</p>",
            :bible.data/name                     "Translation for Translators in Brasilian Portuguese",
            :bible.data/descriptionLocal         "comum",
            :bible.data.language/name            "Portuguese",
            :bible.data/copyright                "Copyright © 2018 Ellis W. Deibler, Jr. Released under a CC-BY-SA 4.0 license.",
            :bible.data/id                       "90799bb5b996fddc-01",
            :bible.data/type                     "text",
            :bible.data.language/id              "por",
            :bible.data/description              "common",
            :bible.data/relatedDbl               nil,
            :bible.data/audioBibles              [],
            :bible.data.language/script          "Latin",
            :bible.data/abbreviation             "TfTP",
            :bible.data/nameLocal                "Translation for Translators in Brasilian Portuguese",
            :bible.data.language/nameLocal       "Português",
            :bible.data/dblId                    "90799bb5b996fddc",
            :bible.data.language/scriptDirection "LTR"}
  )




(def indexes (atom {}))


(pc/defresolver bible-id->language
  [env {:keys [id]}]
  {#_#_::pc/input #{}
   ::pc/output [:language #_[:bible.data.language/id]]}
  {:language (get bible :bible.data.language/id)})

(comment

  (entity-parse {:id "90799bb5b996fddc-01"}
                [:language])

  (entity-parse {} [:bible.data/id "90799bb5b996fddc-01"])
  )


(pc/defresolver my-number [_ _]
  {::pc/output [:my-number]}
  {:my-number 18})


(comment
  (entity-parse {}
                [:my-number :answer-of-everything]))


(pc/defresolver the-answer [_ _]
  {::pc/output [:answer-of-everything]}
  {:answer-of-everything 42})


(comment
  (entity-parse {}
                [:answer-of-everything :my-number]))



(def app-registry
  [bible-id->language
   the-answer
   my-number])

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



#?(:cljs
   (defn entity-parse [entity query]
     (take! (parser {::p/entity (atom entity)} query) prn)))



(comment

  (entity-parse {} [:bible.data/id "90799bb5b996fddc-01"])


  [{[:bible.data/id " 90799bb5b996fddc-01 "]
    [:bible.data/updatedAt
     :bible.data.language/script]}]


  (entity-parse {:first-name "Wilker" :last-name "Silva"}
                [:full-name])

  (entity-parse {:email "elaina.lind@gmail.com"}
                [:full-name :host/domain])

  (pc/compute-paths (::pc/index-oir @indexes) #{:email} #{}
                    :full-name)

  (entity-parse {:email "elaina.lind@gmail.com"}
                [:host/domain])
  )


(parser {}
        [{[:bible.data/id " 90799bb5b996fddc-01 "]
          [:bible.data/updatedAt
           :bible.data.language/script]}])



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


