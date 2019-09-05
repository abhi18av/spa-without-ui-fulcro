(ns app.client
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defsc Car [this {:car/keys [id model] :as props}]
       {:query         [:car/id :car/model]
        :initial-state (fn [{:keys [id model]}]
                           {:car/id    id
                            :car/model model})
        :ident         :car/id}
       (dom/div "Model: " model))

(def ui-car (comp/factory Car {:keyfn :car/id}))


;; NOTE:  {:keys [:person/name]} AND  {:person/keys [name]} are equivalent
(defsc Person [this #_{:keys [:person/id :person/name :person/age :person/cars] :as props}
               {:person/keys [id name age cars] :as props}]
       {:query         [:person/id :person/name :person/age #_:person/cars
                        {:person/cars (comp/get-query Car)}]
        #_:initial-state #_(fn [{:keys [id name]}]
                           {:person/id   id
                            :person/name name
                            :person/age  33
                            :person/cars [(comp/get-initial-state Car {:id 0 :model "Feet"})
                                          (comp/get-initial-state Car {:id 1 :model "Wheel"})]})
        :initial-state {:person/id :param/id
                        :person/name :param/name
                        :person/age 33
                        :person/cars [{:id 0 :model "Feet"}
                                       {:id 1 :model "Wheel"}]}
        :ident         :person/id}
       (dom/div
         (dom/div "Name: " name)
         (dom/div "Age: " age)
         (dom/h3 "Cars: ")
         (dom/ul (map ui-car cars))))

(def ui-person (comp/factory Person {:keyfn :person/id}))


(defsc Sample [this {:root/keys [person]}]
       {:query         [{:root/person (comp/get-query Person)}]
        ;; NOTE alternate notation for expressing initial-state
        #_:initial-state #_(fn [_] {:root/person (comp/get-initial-state Person {:id 1 :name "Adam"})})
        :initial-state {:root/person {:id 1 :name "Adam"}}}
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

  (reset! (::app/state-atom APP) {:sample {:person/id   1
                                           :person/name "Joe"
                                           :person/cars [{:car/id    22
                                                          :car/model "Escort"}]}})
  (app/current-state APP)

  (reset! (::app/state-atom APP) {})


  (swap! (::app/state-atom APP) assoc-in [:person/id 3 :person/age] 22)

  (merge/merge-component! APP Person {:person/id   1
                                      :person/name "Joe"
                                      :person/cars [{:car/id    22
                                                     :car/model "Ford"}]})

  (merge/merge-component! APP Person {:person/id   2
                                      :person/name "Sally"
                                      :person/cars [{:car/id    23
                                                     :car/model "BMW"}]})

  (merge/merge-component! APP Person {:person/id   3
                                      :person/name "Billy"
                                      :person/cars [{:car/id    24
                                                     :car/model "Ferrari"}]}
                          :replace [:root/person])



  (merge/merge-component! APP Car {:car/id    20
                                   :car/model "Mercedes"})

  (merge/merge-component! APP Car
                          {:car/id    18
                           :car/model "Tesla"}

                          :append [:person/id 3 :person/cars])


  (comp/get-query Sample)

  (comp/get-initial-state Sample)

  (app/current-state APP)

  (meta (comp/get-query Person))
  (comp/get-query Person)

  (comp/get-query Car)


  (comp/get-ident Car {:car/id    22
                       :car/model "Ford"})

  )