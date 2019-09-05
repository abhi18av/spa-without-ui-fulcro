(ns app.client 
  (:require 
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.algorithms.merge :as merge]
            [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defsc Car [this {:car/keys [id model] :as props}]
       {:query [:car/id :car/model]
        :ident :car/id}
       #_(dom/div "Model: " model))

(def ui-car (comp/factory Car {:keyfn :car/id}))


;; NOTE:  {:keys [:person/name]} AND  {:person/keys [name]} are equivalent
(defsc Person [this {:keys [:person/id :person/name :person/cars] :as props}]
       {:query [:person/id :person/name { :person/cars (comp/get-query Car)}]
        :ident :person/id}
       #_(dom/div
       (dom/div "Name: " name)
       (dom/h3 "Cars: ")
       (dom/ul (map ui-car cars))))

(def ui-person (comp/factory Person {:keyfn :person/id}))


(defsc Sample [this {:root/keys [person]}]
  {:query [{:root/person (comp/get-query Person)}]}
  (dom/div
    (dom/div "Hello, World!")
    (dom/div (ui-person person))))

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
                                         :person/name "Joe"
                                         :person/cars [{:car/id 22
                                                        :car/model "Escort"}]}})
(app/current-state APP)

(reset! (::app/state-atom APP) {})



(merge/merge-component! APP Person {:person/id 1
                                         :person/name "Joe"
                                         :person/cars [{:car/id 22
                                                        :car/model "Ford"}]})

(merge/merge-component! APP Person {:person/id 2
                                             :person/name "Sally"
                                             :person/cars [{:car/id 23
                                                            :car/model "BMW"}]})

(merge/merge-component! APP Person {:person/id 3
                                             :person/name "Billy"
                                             :person/cars [{:car/id 24
                                                            :car/model "Ferrari"}]})

(meta (comp/get-query Person))


)