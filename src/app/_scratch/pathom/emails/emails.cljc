(ns app.-scratch.pathom.emails.emails
  (:require [clojure.core.async :refer [go timeout <! take! #?(:clj <!!)]]
            [clojure.string :as str]
            [#?(:clj  com.wsscode.common.async-clj
                :cljs com.wsscode.common.async-cljs)
             :refer [go-catch <? let-chan chan? <?maybe <!maybe go-promise]]
            [com.wsscode.pathom.core :as p]
            [com.wsscode.pathom.connect :as pc]))


(def indexes (atom {}))

(def email-db
  {"elaina.lind@gmail.com"      {:first-name "Elaina" :last-name "Lind"}
   "shanna.harber@yahoo.com"    {:first-name "Shanna" :last-name "Harber"}
   "sydni.considine@gmail.com"  {:first-name "Sydni" :last-name "Considine"}
   "margaret.brakus@gmail.com"  {:first-name "Margaret" :last-name "Brakus"}
   "delaney.wehner@hotmail.com" {:first-name "Delaney" :last-name "Wehner"}})



(pc/defresolver full-name-resolver
  [env {:keys [first-name last-name]}]
  {::pc/input  #{:first-name :last-name}
   ::pc/output [:full-name]}
  {:full-name (str first-name " " last-name)})

(comment

  (entity-parse {:first-name "Wilker" :last-name "Silva"}
                [:full-name]))




(pc/defresolver email->name
  [env {:keys [email]}]
  {::pc/input  #{:email}
   ::pc/output [:first-name :last-name]}
  (get email-db email {}))


(comment


  (get email-db "elaina.lind@gmail.com" {})


  (entity-parse {:email "elaina.lind@gmail.com"}
                [:full-name]))



(pc/defresolver all-emails
  [env _]
  {::pc/output [{:all-emails [:email]}]}
  {:all-emails (->> email-db keys (mapv #(hash-map :email %)))})



(comment
  (entity-parse {:email "elaina.lind@gmail.com"}
                [{:all-emails [:email :full-name]}]))




(pc/defresolver the-answer [_ _]
  {::pc/output [:answer-of-everything]}
  {:answer-of-everything 42})


(comment
  (entity-parse {}
                [:answer-of-everything]))


(pc/defresolver email->domain [_ {:keys [email]}]
  {::pc/input  #{:email}
   ::pc/output [:host/domain]}
  (if-let [[_ domain] (re-find #"@(.+)" email)]
    {:host/domain domain}))

(comment
  (entity-parse {:email "shanna.harber@yahoo.com"}
                [:host/domain]))


(def host-by-domain
  {"gmail.com"   {:host/domain "gmail.com"
                  :host/name   "Google Email"}
   "hotmail.com" {:host/domain "hotmail.com"
                  :host/name   "Microsoft Hotmail"}
   "yahoo.com"   {:host/domain "yahoo.com"
                  :host/name   "Yahoo Mail"}})


(pc/defresolver host [_ {:host/keys [domain]}]
  {::pc/input  #{:host/domain}
   ::pc/output [:host/domain
                :host/name]}
  (get host-by-domain domain))


(comment
  (entity-parse {:host/domain "yahoo.com"}
                [:host/name]))

;;;;;;;;;;;
;;;;;;;;;;;


(def app-registry
  [full-name-resolver
   email->name
   all-emails
   the-answer
   email->domain
   host])

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



(comment
  (entity-parse {:first-name "Wilker" :last-name "Silva"}
                [:full-name])

  (entity-parse {:email "elaina.lind@gmail.com"}
                [:full-name :host/domain])

  (pc/compute-paths (::pc/index-oir @indexes) #{:email} #{}
                    :full-name)

  (entity-parse {:email "elaina.lind@gmail.com"}
                [:host/domain])

  (entity-parse {}
                [{:all-emails [:email :answer-of-everything]}])

  (entity-parse {:email "elaina.lind@gmail.com"}
                [:host/name])

  (entity-parse {:email "elaina.lind@gmail.com"}
                [:email
                 {:>/cascas [:host/domain :host/name]}])


  (entity-parse {} [{:all-emails [:full-name]}])


  (entity-parse {}
                [{[:email "elaina.lind@gmail.com"]
                  [:full-name]}]))





