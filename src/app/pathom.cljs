(ns app.pathom
  (:require
    [com.wsscode.pathom.core :as p]
    [com.wsscode.pathom.connect :as pc]
    [com.wsscode.pathom.connect.graphql2 :as pcg]
    [com.wsscode.pathom.graphql :as pg]
    [com.wsscode.pathom.trace :as pt]
    [com.wsscode.common.async-cljs :refer [let-chan <!p go-catch <? <?maybe]]
    [goog.object :as gobj]))


;;;;;;;;;;;


(pc/defresolver answer [_ _]
  {::pc/output [:answer-to-everything]}
  {:answer-to-everything 42})


(pc/defresolver answer-plus-one [_ {:keys [answer-to-everything]}]
  {::pc/input  #{:answer-to-everything}
   ::pc/output [:answer-plus-one]}
  {:answer-plus-one (inc answer-to-everything)})


(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register [answer answer-plus-one]})
                  p/error-handler-plugin
                  p/trace-plugin]}))



(comment

  (parser {} [:answer-to-everything :answer-plus-one])


  )


;;;;;;;;;;;

; creating our first resolver
(pc/defresolver latest-product [_ _]
  {::pc/output [{::latest-product [:product/id :product/title :product/price]}]}
  {::latest-product {:product/id    1
                     :product/title "Acoustic Guitar"
                     :product/price 199.99M}})


(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register latest-product})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(def brand->id {"Taylor" 44151})

(pc/defresolver brand-id-from-name [_ {:keys [product/brand]}]
  {::pc/input  #{:product/brand}
   ::pc/output [:product/brand-id]}
  {:product/brand-id (get brand->id brand)})


(comment

  (parser {} [::latest-product])

  (parser {} [{::latest-product [:product/title :product/brand-id]}])

  )


;;;;;;;;;;;

(def parser
  (p/parser {::p/plugins
             [(p/env-plugin
                {::p/reader [p/map-reader
                             pc/all-readers]})]}))

(comment
  (parser {::p/entity {:hello "World"}} [:hello])
  )

;;;;;;;;;;;

(def reader
  {:dog.ceo/random-dog-url
   (fn [_]
     (go-catch
       (-> (js/fetch "https://dog.ceo/api/breeds/image/random") <!p
           (.json) <!p
           (gobj/get "message"))))})

(def parser
  (p/async-parser {::p/plugins [(p/env-plugin {::p/reader reader})
                                p/error-handler-plugin
                                p/trace-plugin]}))

(comment
  (js/console.log
    (parser {} [:dog.ceo/random-dog-url]))
  )

;;;;;;;;;;;


(pc/defresolver env-modifier [env input]
  {::pc/output [{:change-env [:val]}]}
  {:change-env {:val    123
                ::p/env (assoc env ::demo-env-key "modified")}})

(pc/defresolver env-read-thing [{::keys [demo-env-key]} input]
  {::pc/cache? false
   ::pc/output [:env-data]}
  {:env-data demo-env-key})

(pc/defresolver global-thing [{::keys [demo-env-key]} input]
  {::pc/cache? false
   ::pc/output [:env-data]}
  {:env-data demo-env-key})

(def register
  [env-modifier env-read-thing])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::demo-env-key           "original"
                  ::p/placeholder-prefixes #{">"}}
     ::p/plugins [(pc/connect-plugin {::pc/register register})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(comment

  (parser {} [:env-data {:change-env [:env-data]}])

  )


;;;;;;;;;;;
;; How to go from :person/id to that person's details
(pc/defresolver person-resolver [env {:keys [person/id] :as params}]
  ;; The minimum data we must already know in order to resolve the outputs
  {::pc/input  #{:person/id}
   ;; A query template for what this resolver outputs
   ::pc/output [:person/name {:person/address [:address/id]}]}
  ;; normally you'd pull the person from the db, and satisfy the listed
  ;; outputs. For demo, we just always return the same person details.
  {:person/name    "Tom"
   :person/address {:address/id 1}})

;; how to go from :address/id to address details.
(pc/defresolver address-resolver [env {:keys [address/id] :as params}]
  {::pc/input  #{:address/id}
   ::pc/output [:address/city :address/state]}
  {:address/city  "Salem"
   :address/state "MA"})

;; define a list with our resolvers
(def my-resolvers [person-resolver address-resolver])

;; setup for a given connect system
(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register my-resolvers})
                  p/error-handler-plugin
                  p/trace-plugin]}))




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;




;;;;;;;;;;;

