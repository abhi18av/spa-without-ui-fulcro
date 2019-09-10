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

   :some-random-data      "open-ended keys in options map"

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

(def ui-person-list
  (comp/factory PersonList {:keyfn :person-list/people}))


;;;;;;;;;;;

(defsc Root [this {:root/keys [people]}]
  {:query             [{:root/people (comp/get-query PersonList)}]
   :initial-state     {:root/people {}}
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (js/console.log "[Root] MOUNTED" p)))}
  (dom/div
    (dom/div "Hello, Fulcro!")
    (ui-person-list people)))

(defonce APP (app/fulcro-app))

(defn ^:export init []
  (app/mount! APP Root "app"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; COMMENTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn get-components-that-query-for-a-prop
  [prop]
  (reduce (fn [mounted-instances cls]
            (concat mounted-instances
                    (comp/class->all APP (comp/registry-key->class cls))))
          []
          (comp/prop->classes APP prop)))

(comment


  ;;====== REPL utils ======

  (shadow/repl :main)

  ;; js hard reset
  (.reload js/location true)

  (app/schedule-render! APP {:force-root? true})

  ;;====== APP state atom ======

  (-> APP
      (::app/state-atom)
      deref)

  (keys APP)

  (::app/state-atom APP)
  (::app/algorithms APP)
  (:com.fulcrologic.fulcro.application/runtime-atom APP)
  (:com.fulcrologic.fulcro.application/config APP)



  ;;====== Merge data with the app db ======


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


  ;; TODO figure this out
  (comp/children PersonList)

  (comp/get-ident Person {:person/id   3
                          :person/age  45
                          :person/name "Billy"
                          :person/cars [{:car/id    24
                                         :car/model "Ferrari"}]})




  (app/current-state APP)

  (reset! (::app/state-atom APP) {})


  (reset! (::app/state-atom APP) {:fulcro.inspect.core/app-uuid #uuid"da484b5a-4c0b-41a2-96e3-0c82f667505b",
                                  :root/people                  [:component/id :app.client/person-list],
                                  :car/id                       {0 {:car/id 0, :car/model "Feet"}, 1 {:car/id 1, :car/model "Wheel"}, 22 {:car/id 22, :car/model "Ford"}},
                                  :person/id                    {1 {:person/id 1, :person/name "Bob", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                                 2 {:person/id 2, :person/name "Sally", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                                 9 {:person/id 9, :person/name "Joe", :person/age 28, :person/cars [[:car/id 22]]}},
                                  :component/id                 {:app.client/person-list {:person-list/people [[:person/id 1] [:person/id 2] [:person/id 9]]}}})


  (swap! (::app/state-atom APP) assoc-in [:person/id 3 :person/age] 22)


  (get-in (deref (::app/state-atom APP)) [:person/id 9 :person/age] "Nope")

  (merge/merge-component! APP Person {:person/id   9
                                      :person/name "Joe"
                                      :person/cars [{:car/id    22
                                                     :car/model "Ford"}]})

  (merge/merge-component! APP Person {:person/id   11
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
  (comp/get-query APP)

  (comp/get-initial-state APP)

  (app/current-state APP)

  (meta (comp/get-query Person))

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


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;============ FULCRO API ============
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment

  ;;============= UTILS ========

  (clojure.repl/dir com.fulcrologic.fulcro.components)

  ;;============= f.application ========


  (app/abort!)

  (app/app-root APP)

  (app/basis-t)
  (app/current-state)
  (app/default-global-eql-transform)
  (app/default-remote-error?)

  (app/default-tx!)

  (app/force-root-render! APP)

  (app/fulcro-app)
  (app/fulcro-app?)
  (app/initialize-state!)
  (app/mount!)

  (app/mount! APP Root "app")

  (app/mounted? APP)

  ;; does not exist
  ;;(app/render!)

  (app/root-class)


  (app/root-props-changed?)


  (app/schedule-render! APP)
  (app/set-root!)
  (app/tick!)
  (app/update-shared!)



  ;;============= f.merge ========



  ;;============= f.components ========

  ;comp/*app*
  ;comp/*blindly-render*
  ;comp/*depth*
  ;comp/*parent*
  ;comp/*query-state*
  ;comp/*shared*

  (comp/any->app APP)
  ;; TODO
  (comp/children)

  (comp/class->all)
  (comp/class->any)
  (comp/class->registry-key)
  (comp/component->state-map)
  (comp/component-class? Car)
  (comp/component-instance?)
  (comp/component-name Car)
  (comp/component-options Car)
  (comp/compressible-transact!)
  (comp/computed)
  (comp/computed-factory Car)
  (comp/computed-initial-state? ui-car)
  (comp/configure-component!)
  (comp/defsc)
  (comp/denormalize-query)
  (comp/depth)
  (comp/factory)
  (comp/force-children)
  (comp/fragment)
  (comp/get-computed)
  (comp/get-extra-props)
  (comp/get-ident Car {:car/id    22
                       :car/model "Ford"})

  (comp/get-indexes)
  (comp/get-initial-state Person)
  (comp/get-query)
  (comp/get-query-by-id)
  (comp/get-raw-react-prop)
  (comp/get-state Person)
  (comp/has-feature? Car :car/id)
  (comp/has-ident? Root)
  (comp/has-initial-app-state? Root)
  (comp/has-pre-merge?)
  (comp/has-query? Person)
  (comp/ident Car {:car/id    22
                   :car/model "Ford"})

  (comp/ident->any)
  (comp/ident->components)
  (comp/initial-state)
  (comp/is-factory? ui-car)

  (comp/isoget)
  (comp/isoget-in)
  (comp/link-element)
  (comp/link-query)
  (comp/make-state-map Root)
  (comp/mounted?)
  (comp/newer-props)
  (comp/normalize-query)
  (comp/normalize-query-elements)
  (comp/pre-merge)
  (comp/prop->classes)
  (comp/props)
  (comp/query Root)
  (comp/query-id Root)
  (comp/raw->newest-props)
  (comp/react-type Person)
  (comp/register-component!)
  (comp/registry-key->class)
  (comp/set-query!)
  (comp/set-query*)
  (comp/set-state!)
  (comp/shared)
  (comp/transact!)
  (comp/update-state!)
  (comp/with-parent-context)
  (comp/wrap-update-extra-props)
  (comp/wrapped-render)

  )