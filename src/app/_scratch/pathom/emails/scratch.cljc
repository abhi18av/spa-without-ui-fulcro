(ns app.-scratch.pathom.emails.scratch
  (:require [clojure.core.async :refer [go timeout <! take! #?(:clj <!!)]]
            [clojure.string :as str]
            [#?(:clj  com.wsscode.common.async-clj
                :cljs com.wsscode.common.async-cljs)
             :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]))


(def indexes (atom {}))

(pc/defresolver full-name-resolver
  [env {:keys [first-name last-name]}]
  {::pc/input  #{:first-name :last-name}
   ::pc/output [:full-name]}
  {:full-name (str first-name " " last-name)})

(entity-parse {:first-name "Abhinav" :last-name "Sharma"}
              [:full-name])


(pc/defresolver email->name
  [env {:keys [email]}]
  {::pc/input  #{:email}
   ::pc/output [:first-name :last-name]}
  (get email-db email {}))


(get email-db "elaina.lind@gmail.com" {})


(entity-parse {:email "elaina.lind@gmail.com"}
              [:full-name])




(def app-registry
  [full-name-resolver
   email->name])

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

#?(:cljs
   (defn entity-parse [entity query]
     (take! (parser {::p/entity (atom entity)} query) prn)))



(def email-db
  {#_#_:a-random-key {:first-name "Abhinav" :last-name "Sharma"}
   "elaina.lind@gmail.com"      {:first-name "Elaina" :last-name "Lind"}
   "shanna.harber@yahoo.com"    {:first-name "Shanna" :last-name "Harber"}
   "sydni.considine@gmail.com"  {:first-name "Sydni" :last-name "Considine"}
   "margaret.brakus@gmail.com"  {:first-name "Margaret" :last-name "Brakus"}
   "delaney.wehner@hotmail.com" {:first-name "Delaney" :last-name "Wehner"}})



(entity-parse {:email "elaina.lind@gmail.com"}
              [:first-name])


(entity-parse {} [{:all-emails [:full-name]}])


(entity-parse {}
              [{[:email "elaina.lind@gmail.com"]
                [:first-name]}])


