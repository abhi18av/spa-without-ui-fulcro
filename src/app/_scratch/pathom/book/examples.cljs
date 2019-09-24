(ns app.-scratch.pathom.book.examples
  (:require [com.wsscode.common.async-cljs :refer [go-catch <!p <?]]
            [cljs.core.async :as async :refer [chan put! take! >! <! timeout close! alts!]]
            [cljs.core.async :refer-macros [go go-loop alt!]]
            [com.wsscode.pathom.connect :as pc]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.trace :as pt]
            [goog.object :as gobj]
            [com.wsscode.pathom.diplomat.http :as http]
            [com.wsscode.pathom.diplomat.http.fetch :as fetch]))


;;;;;;;;;;;;;;;;;;;;;


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

(go
  (prn
    (<? (parser {} [:answer-to-everything :answer-plus-one]))))

;;;;;;;;;;;;;;;;;;;;;


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

(go
  (prn
    (<? (parser {} [::latest-product]))))


(go
  (prn
    (<? (parser {} [{::latest-product [:product/title]}]))))




(go
  (prn
    (<? (parser {} [{::latest-product
                     [* ::latest-product]}]))))

;;;;;;;;;;;;;;;;;;;;;


(def product->brand
  {1 "Taylor"})

(pc/defresolver latest-product [_ _]
  {::pc/output [{::latest-product [:product/id :product/title :product/price]}]}
  {::latest-product {:product/id    1
                     :product/title "Acoustic Guitar"
                     :product/price 199.99M}})

(pc/defresolver product-brand [_ {:keys [product/id]}]
  {::pc/input  #{:product/id}
   ::pc/output [:product/brand]}
  {:product/brand (get product->brand id)})

(def app-registry [latest-product product-brand pc/index-explorer-resolver])

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader [p/map-reader
                              pc/parallel-reader
                              pc/open-ident-reader]}
     ::p/mutate  pc/mutate-async
     ::p/plugins [(pc/connect-plugin {::pc/register app-registry})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(go
  (prn
    (<? (parser {} [{::latest-product [:product/title :product/brand]}]))))


;;;;;;;;;;;;;;;;;;;;





(pc/defresolver random-dog [env {:keys []}]
  {::pc/output [:dog.ceo/random-dog-url]}
  (go-catch
    {:dog.ceo/random-dog-url
     (-> (js/fetch "https://dog.ceo/api/breeds/image/random") <!p
         (.json) <!p
         (gobj/get "message"))}))

(def parser
  (p/parallel-parser
    {::p/env     {::p/reader               [p/map-reader
                                            pc/parallel-reader
                                            pc/open-ident-reader
                                            p/env-placeholder-reader]
                  ::p/placeholder-prefixes #{">"}}
     ::p/plugins [(pc/connect-plugin {::pc/register [random-dog]})
                  p/error-handler-plugin
                  p/trace-plugin]}))


(go
  (prn
    (<? (parser {} [:dog.ceo/random-dog-url]))))
;;;;;;;;;;;;




