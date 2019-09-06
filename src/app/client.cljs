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
;; GENERAL NOTES
;; NOTE all dom elements *always* take string - input and return output

;; optimizations in react and fulcro can be done in 3 ways
;; 1. reduce the number of queries we need to run
;; 2. output of factories (VDOM) (reduce the number that need to run)
;; 3. react dom diff - specify stable keys

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(def ui-number-format (interop/react-factory NumberFormat))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defsc Car [this {:car/keys [id model] :as props}]
  {:query                 [:car/id :car/model]
   #_#_:initial-state (fn [{:keys [id model]}]
                        {:car/id    id
                         :car/model model})
   :initial-state         {:car/id    :param/id
                           :car/model :param/model}
   :ident                 :car/id
   :shouldComponentUpdate (fn [] true)
   ;; NOTE optional elements within a component
   :some-random-data      "random data"}
  (js/console.log "Render Car" id)
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

  {:query                 [:person/id :person/name :person/age #_:person/cars
                           {:person/cars (comp/get-query Car)}]
   #_:initial-state #_(fn [{:keys [id name]}]
                        {:person/id   id
                         :person/name name
                         :person/age  33
                         :person/cars [(comp/get-initial-state Car {:id 0 :model "Feet"})
                                       (comp/get-initial-state Car {:id 1 :model "Wheel"})]})
   :initial-state         {:person/id   :param/id
                           :person/name :param/name
                           :person/age  33
                           :person/cars [{:id 0 :model "Feet"}
                                         {:id 1 :model "Wheel"}]}

   ;; as soon as we move to a 2-vector form in :ident it re-normalizes to a new table in fulcro app db into a to-many relationship with :people/id
   :ident                 :person/id #_[:people :person/id]

   :some-random-data      "This is random data which can be added to any component because it's options map is open-ended"
   ;; NOTE react lifecycle methods ( should component is provided by default in fulcro3
   #_:shouldComponentUpdate #_(fn [this props state])

   :shouldComponentUpdate (fn [] true)
   ;; NOTE for this lifecycle we need to pull the current component props
   :componentDidMount     (fn [this]
                            (let [p (comp/props this)]
                              #_(js/console.log "MOUNTED" p)))

   ;; NOTE a constructor placeholder for a component-ONLY props - doesn't reflect in the fulcro app DB
   ;; commonly used for callback functions
   ;; and for avoiding re-definitions for functions and trigger multiple renders - which can happen a lot inside the let bindings
   :initLocalState        (fn [this props]
                            {:onClick (fn [evt] (js/console.log "Clicked on Name in Person Component"))})}

  (let [onClick (comp/get-state this :onClick)]
    (js/console.log "Render Person" id)
    (dom/div :.ui.segment
             (dom/div :.ui.form                             ;; {:className "ui form"}
                      ;; dom/div and others are actually adaptive macros/functions and their nature depends on their usage
                      #_(dom/div :.ui.field)                ;; as a macro ;; evaluated at compile time
                      #_(map dom/div ["Div1" "Div2"])       ;; as a function
                      (dom/div :.ui.field {:style {:color "red"}} ;; optional option map {} - but it makes things a bit performant - 3x speed up with options map by reducing the react overhead
                               (dom/label {:onClick onClick} "Name: ")
                               name)
                      (dom/div :.ui.field {}
                               (dom/label {} "Amount: ")
                               ;; translated from the react-number-format examples
                               ;; https://github.com/s-yadav/react-number-format#prefix-and-thousand-separator--format-currency-in-input
                               (ui-number-format {:value             "111111"
                                                  :thousandSeparator true
                                                  :prefix            "$"}))
                      (dom/div :.ui.field {}
                               (dom/label {} "Age: ")
                               age)
                      (dom/button :.ui.positive.basic.button {:onClick #(comp/transact! this [(make-older {:person/id id})])} "make-older")
                      (dom/h3 {} "Cars: ")
                      (dom/ul {} (map ui-car cars))))))

(def ui-person (comp/factory Person {:keyfn :person/id}))

;;;;;;;;;;;

(defsc PersonList [this {:person-list/keys [people] :as props}]
  {:query                 [{:person-list/people (comp/get-query Person)}]

   :shouldComponentUpdate (fn [] true)
   ;; singleton ident for a component
   :ident                 (fn [_ _] [:component/id ::person-list])  #_:person-list/people
   :initial-state         {:person-list/people [{:id 1 :name "Bob"}
                                                {:id 2 :name "Sally"}]}} ;; will get the initial state from a join to Person
  (js/console.log "Render Person List")
  (dom/div
    (dom/h3 "People")
    (map ui-person people)))

(def ui-person-list (comp/factory PersonList {:keyfn :person-list/people}))

;;;;;;;;;;;

(defsc Root [this {:root/keys [people]}]
  {:query                 [{:root/people (comp/get-query PersonList)}]
   :shouldComponentUpdate (fn [] true)
   ;; NOTE root does NOT need an :ident
   ;; NOTE alternate notation for expressing initial-state
   #_:initial-state #_(fn [_] {:root/person (comp/get-initial-state Person {:id 1 :name "Adam"})})
   :initial-state         {:root/people {}}}
  (js/console.log "Render Root")
  (dom/div
    (when people
      (dom/div (ui-person-list people)))))

;; NOTE this is also called the ident-optimized render
;; NOTE can override the entire rendering system per-se like preact or other vdom diff libraries ( maybe incr_dom - jane street!)
;; keyframe/render! always runs the entire query
(defonce APP (app/fulcro-app #_{:optimized-render! keyframe/render!}))

(defn ^:export init []
  (app/mount! APP Root "app"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; NOTE convenient function for experimenting later inside the comment
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