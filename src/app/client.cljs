(ns app.client 
  (:require 
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; NOTE:  {:keys [:person/name]} AND  {:person/keys [name]} are equivalent

(defsc Person [this {:keys [:person/id :person/name] :as props}]
       {}
       (dom/div "Name: " name))

(def ui-person (comp/factory Person {:keyfn :person/id}))


(defsc Sample [this {:keys [sample]}]
  {}
  (dom/div
    (dom/div "Hello, World!")
    (dom/div (ui-person sample))))

(defonce APP (app/fulcro-app))

(defn ^:export init []
  (app/mount! APP Sample "app"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
(shadow/repl :main)

(::app/state-atom APP)
(keys APP)

(-> APP
    (::app/state-atom)
    deref)

(reset! (::app/state-atom APP) {:a 1})

(app/schedule-render! APP)

(reset! (::app/state-atom APP) {:sample {:person/id 1
                                         :person/name "Joe"}})


)