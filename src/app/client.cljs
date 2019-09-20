(ns app.client
  (:require
    ;; external libs
    ["react-number-format" :as NumberFormat]
    ;; project libs
    [app.utils :refer [clog get-components-that-query-for-a-prop]]
    ;; internal libs
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.fulcrologic.fulcro.rendering.keyframe-render :as keyframe]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))

;;;;;;;; COMPONENTS ;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;;===== Number Format =======================================

(def ui-number-format (interop/react-factory NumberFormat))


(comment

  (comp/get-query NumberFormat)

  (comp/get-initial-state NumberFormat)

  (comp/component-options NumberFormat)

  @(::app/state-atom APP)

  )


;;===== Car Component =======================================

(defsc Car [this {:keys [:car/id :car/model] :as props}]
  {:query             [:car/id :car/model]
   :ident             :car/id
   :initial-state     {:car/id    :param/id
                       :car/model :param/model}
   :initLocalState    (fn [this]
                        (clog {:message "[Car] InitLocalState"}))
   :random-data       "This is some random data"
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[Car] MOUNTED" :props p})))}
  (clog {:message "[Car] UPDATED" :color "blue" :props props})
  (js/console.log "[Car] id" id)
  (js/console.log "[Car] model" model)
  (dom/div))

(def ui-car (comp/factory Car {:keyfn :car/id}))


(comment

  (comp/get-query Car)

  (comp/get-initial-state Car)

  (comp/component-options Car)

  (comp/get-ident Car {:car/id    22
                       :car/model "Escort"})

  @(::app/state-atom APP)

  )


;;===== Person Component =======================================

(defmutation make-older [{:keys [:person/id]}]
  (action [{:keys [state]}]
          (clog {:message "[PERSON] MUTATION make-older" :color "magenta" :props state})
          (swap! state update-in [:person/id id :person/age] inc)))

(comment
  (comp/transact! APP [(make-older {:person/id 1})])
  )


(defsc Person [this {:keys [:person/id :person/name :person/age :person/cars #_:person/addresses] :as props}]
  {:query             [:person/id :person/name :person/age
                       {:person/cars (comp/get-query Car)}
                       #_{:person/addresses (comp/get-query Address)}]
   :ident             :person/id
   :initial-state     {:person/id   :param/id
                       :person/name :param/name
                       :person/age  20
                       :person/cars [{:id 1 :model "Leaf"}
                                     {:id 2 :model "Escort"}]}
   :initLocalState    (fn [this]
                        (clog {:message "[Person] InitLocalState"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[Person] MOUNTED" :props p})))}
  (clog {:message "[Person] UPDATED" :color "blue" :props props})
  (js/console.log "[Person] id" id)
  (js/console.log "[Person] name" name)
  (js/console.log "[Person] age" age)
  (dom/div
    ;; I'm sending to Car the value associated with the cars key
    (dom/div (map ui-car cars))
    #_(ui-number-format {:thousandSeparator true
                         :prefix            "$"})
    #_(dom/div (map ui-address addresses))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

(comment

  (comp/get-query Person)

  (comp/get-initial-state Person)

  (comp/props Person)

  @(::app/state-atom APP)

  (app/current-state APP)

  (swap! (::app/state-atom APP) assoc-in [:person/id 3 :person/age] 18)

  (app/schedule-render! APP)

  (comp/class->all APP Person)

  ;;any one of the class->all outputs
  (comp/class->any APP Person)

  (comp/prop->classes APP :person/id)

  (comp/prop->classes APP :person/age)

  (map comp/get-ident
       (get-components-that-query-for-a-prop APP :person/name))

  (map comp/get-ident
       (get-components-that-query-for-a-prop APP :person/name))

  )



;;===== PersonList Component =======================================


;; a client only component ID  - no server identity
(defsc PersonList [this {:keys [:person-list/people] :as props}]
  {:query             [{:person-list/people (comp/get-query Person)}]
   ;; singleton components only have a single rep in the app db
   ;; for those we created a new table called component/id
   :ident             (fn [_ _]
                        [:component/id ::person-list])
   :initial-state     {:person-list/people [{:id 1 :name "Joe"}
                                            {:id 2 :name "Sally"}]} ;; the {} join ends up fetching the initial state of a Person
   :initLocalState    (fn [this]
                        (clog {:message "[PersonList] InitLocalState"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[PersonList] MOUNTED" :props p})))}
  (clog {:message "[PersonList] UPDATED" :color "blue" :props props})
  (dom/div
    (map ui-person people)))

(def ui-person-list (comp/factory PersonList #_{:keyfn :person/id}))

(comment

  (comp/get-query PersonList)

  (comp/get-initial-state PersonList)

  (comp/props PersonList)

  @(::app/state-atom APP)

  (app/current-state APP)

  (swap! (::app/state-atom APP) assoc-in [:person/id 3 :person/age] 18)

  (app/schedule-render! APP)

  )



;;===== Root Component =======================================

(defsc Root [this {:keys [:root] :as props}]
  {:query             [{:root (comp/get-query PersonList)}]
   :initial-state     {:root {}}
   :initLocalState    (fn [this]
                        (clog {:message "[ROOT]: InitLocalState"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[APP] ROOT Mount TimeStamp:" :props (js/Date.) :color "purple"})))}
  (clog {:message "[Root] UPDATED" :color "blue" :props props})
  (dom/div
    (dom/h1 "Hello, Fulcro!")
    (dom/div
      ;; I'm sending to Person the value associated with the root key
      (ui-person-list root))))



(comment

  (comp/get-query Root)

  (comp/get-initial-state Root)

  @(::app/state-atom APP)

  (comp/props Root)

  (app/schedule-render! APP {:force-root? true})

  )



;;===== FULCRO APP INIT =======================================


(defonce APP (app/fulcro-app {:optimized-render! keyframe/render!}))

(defn ^:export init []
  (app/mount! APP Root "app"))


(comment

  (-> APP
      (::app/state-atom)
      deref)

  (keys APP)

  (::app/algorithms APP)
  (:com.fulcrologic.fulcro.application/runtime-atom APP)
  (:com.fulcrologic.fulcro.application/config APP)


  (reset! (::app/state-atom APP) {})

  (reset! (::app/state-atom APP) {:root {:person/id   1
                                         :person/name "Joe"
                                         :person/cars [{:car/id    22
                                                        :car/model "Escort"}]}})



  (app/schedule-render! APP {:force-root? true})

  )




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; COMMENTS
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(comment


  ;;====== REPL utils ======

  (shadow/repl :main)

  ;; js hard reset
  (.reload js/location true)


  (.clear js/console)





  (app/schedule-render! APP {:force-root? true})

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



  (js/console.log (deref (::app/state-atom APP)))

  (app/current-state APP)

  (reset! (::app/state-atom APP) {})





  (reset! (::app/state-atom APP) {
                                  :root/people  [:component/id :app.client/person-list],
                                  :car/id       {0 {:car/id 0, :car/model "Feet"}, 1 {:car/id 1, :car/model "Wheel"}, 22 {:car/id 22, :car/model "Ford"}},
                                  :person/id    {1 {:person/id 1, :person/name "Bob", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                 2 {:person/id 2, :person/name "Sally", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                 9 {:person/id 9, :person/name "Joe", :person/age 28, :person/cars [[:car/id 22]]}},
                                  :component/id {:app.client/person-list {:person-list/people [[:person/id 1] [:person/id 2] [:person/id 9]]}}})

  (app/schedule-render! APP {:force-root? true})


  (reset! (::app/state-atom APP) {:root/people  [:component/id :app.client/person-list],
                                  :car/id       {0 {:car/id 0, :car/model "Feet"}, 1 {:car/id 1, :car/model "Wheel"}, 22 {:car/id 22, :car/model "Ford"}},
                                  :person/id    {1 {:person/id 1, :person/name "Bob", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                 2 {:person/id 2, :person/name "Sally", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                 4 {:person/id 4, :person/name "Sally", :person/age 28, :person/cars [[:car/id 0] [:car/id 1]]},
                                                 #_#_9 {:person/id 9, :person/name "Joe", :person/age 28, :person/cars [[:car/id 22]]}},
                                  :component/id {:app.client/person-list {:person-list/people [[:person/id 1]
                                                                                               [:person/id 2]
                                                                                               #_[:person/id 9]
                                                                                               [:person/id 4]]}}})



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
  (app/current-state APP)
  (app/default-global-eql-transform)
  (app/default-remote-error?)

  (app/default-tx!)

  (app/force-root-render! APP)

  (app/fulcro-app)
  (app/fulcro-app? APP)
  (app/initialize-state!)
  (app/mount!)

  (app/mount! APP Root "app")

  (app/mounted? APP)

  ;; does not exist
  ;;(app/render!)

  (app/root-class APP)

  (app/root-props-changed? APP)


  (app/schedule-render! APP)
  (app/set-root!)
  (app/tick! APP)
  (app/update-shared! APP)



  ;;============= f.merge ========



  ;;============= f.components ========

  ;comp/*app*
  ;comp/*blindly-render*
  ;comp/*depth*
  ;comp/*parent*
  ;comp/*query-state*
  ;comp/*shared*

  (comp/any->app Root)

  ;; TODO
  (comp/children)

  (comp/class->all)
  (comp/class->any)
  (comp/class->registry-key)
  (comp/component->state-map)
  (comp/component-class? Car)
  (comp/component-instance? Root)
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
  (comp/get-computed PersonList)
  (comp/get-extra-props PersonList)
  (comp/get-ident Car {:car/id    22
                       :car/model "Ford"})

  (comp/get-indexes)
  (comp/get-initial-state Person)
  (comp/get-query Root)
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

  (comp/ident->any APP :person/id)

  (comp/ident->components)
  (comp/initial-state Root)
  (comp/is-factory? ui-car)

  (comp/isoget)
  (comp/isoget-in)
  (comp/link-element)
  (comp/link-query)
  (comp/make-state-map Root)
  (comp/mounted? Root)
  (comp/newer-props)
  (comp/normalize-query)
  (comp/normalize-query-elements)
  (comp/pre-merge)
  (comp/prop->classes APP :car/id)
  (comp/props Car)
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