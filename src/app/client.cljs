(ns app.client
  (:require
    ;; external libs
    ["react-number-format" :as NumberFormat]
    ;; internal libs
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.fulcrologic.fulcro.rendering.keyframe-render :as keyframe]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.data-targeting :as targeting]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ui-number-format (interop/react-factory NumberFormat))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc Car [this {:car/keys [id model] :as props}]
  {:query            [:car/id :car/model]

   :initial-state    {:car/id    :param/id
                      :car/model :param/model}

   :ident            :car/id

   :some-random-data "random data"}
  (js/console.log "Render Car" id)
  (dom/div))

(def ui-car (comp/factory Car {:keyfn :car/id}))

;;;;;;;;;;;

(defmutation make-older [{:person/keys [id]}]
  (action [{:keys [state]}]
          (js/console.log state)
          (swap! state update-in [:person/id id
                                  :person/age] inc)))

(defsc Person [this {:person/keys [id name age cars] :as props}]

  {:query                 [:person/id :person/name :person/age
                           {:person/cars (comp/get-query Car)}]

   :initial-state         {:person/id   :param/id
                           :person/name :param/name
                           :person/age  28
                           :person/cars [{:id 0 :model "Feet"}
                                         {:id 1 :model "Wheel"}]}

   :ident                 :person/id

   :some-random-data      "This is random data which can be added to any component because it's options map is open-ended"

   :shouldComponentUpdate (fn [] true)

   :componentDidMount     (fn [this]
                            (let [p (comp/props this)]
                              (js/console.log "[Person] MOUNTED" p)))}

  (let [onClick (comp/get-state this :onClick)]
    (dom/div
      (map ui-car cars))
    (comment
      (comp/transact! this [(make-older {:person/id id})]
                      {:refresh [:person-list/people]}))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

;;;;;;;;;;;

(defsc PersonList [this {:person-list/keys [people] :as props}]
  {:query             [{:person-list/people (comp/get-query Person)}]

   :ident             (fn [_ _] [:component/id ::person-list])
   :initial-state     {:person-list/people [{:id 1 :name "Bob"}
                                            {:id 2 :name "Sally"}]}

   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (js/console.log "[PersonList] MOUNTED" p)))}

  (let [cnt (reduce
              (fn [c {:person/keys [age]}]
                (if (> age 30)
                  (inc c)
                  c))
              0
              people)]
    (dom/div
      (map ui-person people))))

(def ui-person-list (comp/factory PersonList {:keyfn :person-list/people}))


;;;;;;;;;;;

(defsc Root [this {:root/keys [people]}]
  {:query             [{:root/people (comp/get-query PersonList)}]
   :initial-state     {:root/people {}}
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (js/console.log "[Root] MOUNTED" p)))}
  (dom/div
    (ui-person-list people)))

(defonce APP (app/fulcro-app))

(defn ^:export init []
  (app/mount! APP Root "app"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-components-that-query-for-a-prop
  [prop]
  (reduce (fn [mounted-instances cls]
            (concat mounted-instances
                    (comp/class->all APP (comp/registry-key->class cls))))
          []
          (comp/prop->classes APP prop)))

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

  (comp/component-options Person)

  ;; TODO BUG - always re-rendered even before adding :shouldComponentUpdate to all components
  (comp/transact! APP [(make-older {:person/id 2})])

  (comp/class->all APP Person)

  (comp/class->any APP Person)

  ;;NOTE we can get the components which query for any particular piece of data
  (comp/prop->classes APP :person/age)

  (map
    comp/get-ident
    (get-components-that-query-for-a-prop :person/id))



  (let [state (app/current-state APP)
        component-query (comp/get-query Person)
        component-ident [:person/id 1]
        starting-entity (get-in state component-ident)]
    (fdn/db->tree component-query starting-entity state))



  (def before-mutation (app/current-state APP))

  (comp/transact! APP [(make-older {:person/id 2})])

  (def after-mutation (app/current-state APP))

  before-mutation
  after-mutation

  )