(ns app.client
  (:require
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc Car [this {:car/keys [id model] :as props}]
  {:query            [:car/id :car/model]
   #_#_:initial-state (fn [{:keys [id model]}]
                        {:car/id    id
                         :car/model model})
   :initial-state    {:car/id    :param/id
                      :car/model :param/model}
   :ident            :car/id
   ;; NOTE optional elements within a component
   :some-random-data "random data"}
  (dom/div "Model: " model))

(def ui-car (comp/factory Car {:keyfn :car/id}))

;;;;;;;;;;;

(defmutation make-older [{:person/keys [id]}]
  ;; NOTE env is simply a map
  #_(remote [env] true)
  #_(rest [env] true)
  (action [{:keys [state]}]
          #_(js/console.log state)
          (swap! state update-in [:person/id id
                                  :person/age] inc)))

;; NOTE:  {:keys [:person/name]} AND  {:person/keys [name]} are equivalent
(defsc Person [this #_{:keys [:person/id :person/name :person/age :person/cars] :as props}
               {:person/keys [id name age cars] :as props}]

  {:query             [:person/id :person/name :person/age #_:person/cars
                       {:person/cars (comp/get-query Car)}]
   #_:initial-state #_(fn [{:keys [id name]}]
                        {:person/id   id
                         :person/name name
                         :person/age  33
                         :person/cars [(comp/get-initial-state Car {:id 0 :model "Feet"})
                                       (comp/get-initial-state Car {:id 1 :model "Wheel"})]})
   :initial-state     {:person/id   :param/id
                       :person/name :param/name
                       :person/age  33
                       :person/cars [{:id 0 :model "Feet"}
                                     {:id 1 :model "Wheel"}]}
   :ident             :person/id
   ;; NOTE react lifecycle methods ( should component is provided by default in fulcro3
   #_:shouldComponentUpdate #_(fn [this props state])
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (js/console.log "MOUNTED" p)))
   :initLocalState    (fn [this props]
                        {:onClick (fn [evt] (js/console.log "Clicked on Name in Person Component"))})}

  (let [onClick (comp/get-state this :onClick)]
    (dom/div :.ui.form                                      ;; {:className "ui form"}
             (dom/div :.ui.field {:style {:color "red"}}
                      (dom/label {:onClick onClick} "Name: ")
                      name)
             (dom/div :.ui.field
                      (dom/label "Age: ")
                      age)
             (dom/button :.ui.positive.basic.button {:onClick #(comp/transact! this [(make-older {:person/id id})])} "make-older")
             (dom/h3 "Cars: ")
             (dom/ul (map ui-car cars)))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

;;;;;;;;;;;

(defsc Sample [this {:root/keys [person]}]
  {:query         [{:root/person (comp/get-query Person)}]
   ;; NOTE root does NOT need an :ident
   ;; NOTE alternate notation for expressing initial-state
   #_:initial-state #_(fn [_] {:root/person (comp/get-initial-state Person {:id 1 :name "Adam"})})
   :initial-state {:root/person {:id 1 :name "Adam"}}}
  (dom/div
      (dom/div (ui-person person))))

(defonce APP (app/fulcro-app))

(defn ^:export init []
  (app/mount! APP Sample "app"))



;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment
  (shadow/repl :main)

  ;; js hard reset
  (.reload js/location true)

  (::app/state-atom APP)

  (keys APP)

  (-> APP
      (::app/state-atom)
      deref)

  (app/schedule-render! APP)

  (reset! (::app/state-atom APP) {:sample {:person/id   1
                                           :person/name "Joe"
                                           :person/cars [{:car/id    22
                                                          :car/model "Escort"}]}})

  ;; TODO understand how to merge directly to the app root
  #_(merge/merge! APP Sample {:root/person {:person/id   18
                                            :person/name "Abhinav"
                                            :person/cars [{:car/id    0
                                                           :car/model "Walking"}
                                                          {:car/id    1
                                                           :car/model "Running"}]}})

  ;; Doesn't work on both as  the :cars table needs to be at the root level
  (comp/get-ident Car {:car/id      24
                       :car/model   "Ferrari"
                       :person/cars [{:car/id    24
                                      :car/model "Ferrari"}]
                       :person/id   3
                       :person/age  45
                       :person/name "Billy"})


  (comp/get-ident Person {:person/id   3
                          :person/age  45
                          :person/name "Billy"
                          :person/cars [{:car/id    24
                                         :car/model "Ferrari"}]})



  (app/schedule-render! APP {:force-root? true})


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
                                      :person/age  45
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

  ;; use this on the root element to see the entire tree of query
  (comp/get-query Sample)

  (comp/get-initial-state Sample)

  (app/current-state APP)

  (meta (comp/get-query Person))

  (comp/get-query Person)

  (comp/get-query Car)

  (comp/get-ident Car {:car/id    22
                       :car/model "Ford"})

  ;; by default mutations return themselves as data
  ;;  `(make-older ~{:a 1}) ;; fulcro2 alternate notation
  (make-older {:a 1})

  (reset! (::app/state-atom APP) {})

  (app/current-state APP)

  (merge/merge-component! APP Person {:person/id 1 :person/age 20})

  (swap! (::app/state-atom APP) assoc-in [:person/id 1 :person/age] 22)

  (swap! (::app/state-atom APP) update-in [:person/id 3 :person/age] inc)

  ;; operate simply on Adam
  (comp/transact! APP [(make-older {:person/id 1})
                       ;; other mutations
                       ;; ()
                       ;; ()
                       ])

  ;; prints only the non-native keys in human readable form
  (comp/component-options Car)

  )