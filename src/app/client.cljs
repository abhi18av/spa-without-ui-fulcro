(ns app.client
  (:require
    ;; external libs
    ["react-number-format" :as NumberFormat]
    ;; project libs
    [app.utils :refer [clog get-components-that-query-for-a-prop]]
    [app.model.person :refer [make-older select-person picker-path]]
    ;; internal libs
    [com.fulcrologic.fulcro.networking.http-remote :as http]
    [com.fulcrologic.fulcro.algorithms.react-interop :as interop]
    [com.fulcrologic.fulcro.rendering.keyframe-render :as keyframe]
    [com.fulcrologic.fulcro.dom :as dom :refer [div ul li button h3 label a]]
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.algorithms.denormalize :as fdn]
    [com.fulcrologic.fulcro.data-fetch :as df]))


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
   #_#_:initial-state {:car/id    :param/id
                       :car/model :param/model}
   :initLocalState    (fn [this]
                        (clog {:message "[Car] InitLocalState" :color "teal"}))
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



;;===== PersonDetail Component =======================================

(defsc PersonDetail [this {:person/keys [id name age cars] :as props}]
  {:query             [:person/id :person/name :person/age {:person/cars (comp/get-query Car)}]
   :ident             :person/id
   :initLocalState    (fn [this]
                        (clog {:message "[PersonDetail]: InitLocalState" :color "teal"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[PersonDetail] MOUNTED" :color "green"})))}
  (let [onClick (comp/get-state this :onClick)]
    (clog {:message "[PersonDetail] UPDATED" :color "blue" :props props})
    (div :.ui.segment
         (h3 :.ui.header "Selected Person")
         (when id
           (div :.ui.form
                (div :.field
                     (label {:onClick onClick} "Name: ")
                     name)
                (div :.field
                     (label "Age: ")
                     age)
                (button :.ui.button {:onClick (fn []
                                                (comp/transact! this
                                                                [(make-older {:person/id id})]
                                                                {:refresh [:person-list/people]}))}
                        "Make Older")
                (h3 {} "Cars")
                (ul {}
                    (map ui-car cars)))))))

(def ui-person-detail (comp/factory PersonDetail {:keyfn :person/id}))



;;===== PersonListItem Component =======================================

(defsc PersonListItem [this {:person/keys [id name] :as props}]
  {:query             [:person/id :person/name]
   :ident             :person/id
   :initLocalState    (fn [this]
                        (clog {:message "[PersonListItem]: InitLocalState" :color "teal"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[PersonListItem] MOUNTED" :color "green"})))}
  (clog {:message "[PersonListItem] UPDATED" :color "blue" :props props})
  (li :.item
      (a {:href    "#"
          :onClick (fn []
                     (df/load! this [:person/id id] PersonDetail
                               {:target (picker-path :person-picker/selected-person)}))}
         name)))

(def ui-person-list-item (comp/factory PersonListItem {:keyfn :person/id}))


;;===== PersonList Component =======================================


(defsc PersonList [this {:person-list/keys [people] :as props}]
  {:query             [{:person-list/people (comp/get-query PersonListItem)}]
   :ident             (fn [] [:component/id :person-list])
   :initLocalState    (fn [this]
                        (clog {:message "[PersonList]: InitLocalState" :color "teal"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[PersonList] MOUNTED" :color "green"})))
   :initial-state     {:person-list/people []}}
  (clog {:message "[PersonList] UPDATED" :color "blue" :props props})
  (div :.ui.segment
       (h3 :.ui.header "People")
       (ul
         (map ui-person-list-item people))))

(def ui-person-list (comp/factory PersonList))



;;===== PersonPicker Component =======================================


(defsc PersonPicker [this {:person-picker/keys [list selected-person] :as props}]
  {:query             [{:person-picker/list (comp/get-query PersonList)}
                       {:person-picker/selected-person (comp/get-query PersonDetail)}]
   :initial-state     {:person-picker/list {}}
   :ident             (fn [] [:component/id :person-picker])
   :initLocalState    (fn [this]
                        (clog {:message "[PersonPicker]: InitLocalState" :color "teal"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[PersonPicker] MOUNTED" :color "green"})))}
  (clog {:message "[PersonPicker] UPDATED" :color "blue" :props props})
  (div :.ui.two.column.container.grid
       (div :.column
            (ui-person-list list))
       (div :.column
            (ui-person-detail selected-person))))

(def ui-person-picker (comp/factory PersonPicker {:keyfn :person-picker/people}))

;;===== Root Component =======================================


(defsc Root [this {:keys [:root/person-picker] :as props}]
  {:query             [{:root/person-picker (comp/get-query PersonPicker)}]
   :initial-state     {:root/person-picker {}}
   :initLocalState    (fn [this]
                        (clog {:message "[Root]: InitLocalState" :color "teal"}))
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[Root] MOUNTED (with TimeStamp)" :props (js/Date.) :color "green"})))}
  (clog {:message "[Root] UPDATED" :color "blue" :props props})
  (dom/div
    (dom/h1 "Hello, Fulcro!")
    (dom/div
      ;; I'm sending to Person the value associated with the root key
      #_(ui-person-list root)
      (ui-person-picker person-picker))))



(comment


  (comp/get-query Root)

  (comp/get-initial-state Root)

  @(::app/state-atom APP)

  (comp/props Root)

  (app/schedule-render! APP {:force-root? true})

  )





;;;;;;;;;;;;;;;;;;;;;;;;;;;;;



;;===== FULCRO APP INIT =======================================


#_(defonce APP (app/fulcro-app
                 {:remotes {:remote (http/fulcro-http-remote {})}}
                 #_{:optimized-render! keyframe/render!}))


(defonce APP (app/fulcro-app {:remotes          {:remote (http/fulcro-http-remote {})}
                              :client-did-mount (fn [app]
                                                  (df/load! app :all-people PersonListItem
                                                            {:target [:component/id :person-list :person-list/people]}))}))

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






;;;; PREVIOUS MAIN APP


(comment


  ;;===== PersonDetail Component =======================================


  (defsc PersonDetail [this {:person/keys [id name age cars] :as props}]
    {:query [:person/id :person/name :person/age {:person/cars (comp/get-query Car)}]
     :ident :person/id}
    (let [onClick (comp/get-state this :onClick)]
      #_(div :.ui.segment
             (h3 :.ui.header "Selected Person")
             (when id
               (div :.ui.form
                    (div :.field
                         (label {:onClick onClick} "Name: ")
                         name)
                    (div :.field
                         (label "Age: ")
                         age)
                    (button :.ui.button {:onClick (fn []
                                                    (comp/transact! this
                                                                    [(make-older {:person/id id})]
                                                                    {:refresh [:person-list/people]}))}
                            "Make Older")
                    (h3 {} "Cars")
                    (ul {}
                        (map ui-car cars)))))))

  (def ui-person-detail (comp/factory PersonDetail {:keyfn :person/id}))

  (comment
    (df/load! APP [:person/id 1] PersonDetail))


  ;;===== Person Component =======================================
  ;; NOTE moved this to the model.person ns
  #_(defmutation make-older [{:keys [:person/id]}]
      (action [{:keys [state]}]
              (clog {:message "[PERSON] MUTATION make-older" :color "magenta" :props state})
              (swap! state update-in [:person/id id :person/age] inc)))

  ;; this does not update :person-list - why?
  (comp/transact! APP [(make-older {:person/id 2})]
                  {:refresh [;; refresh anything looking at :person/id 2
                             [:person/id 2]]})

  ;; Via ident-render, this one explicitely tell the UI to also update the parent component
  (comp/transact! APP [(make-older {:person/id 2})]
                  {:refresh [:person-list/people]})
  )

(defsc Person [this {:keys [:person/id :person/name :person/age :person/cars #_:person/addresses] :as props}]
  {:query             [:person/id :person/name :person/age
                       {:person/cars (comp/get-query Car)}
                       #_{:person/addresses (comp/get-query Address)}]
   :ident             :person/id
   #_#_:initial-state {:person/id   :param/id
                       :person/name :param/name
                       :person/age  20
                       :person/cars [{:id 1 :model "Leaf"}
                                     #_{:id 2 :model "Escort"}]}
   :initLocalState    (fn [this]
                        (clog {:message "[Person] InitLocalState" :color "teal"}))
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

  ;; this is an overview of how the ident-optimized render is designed
  (let [state (app/current-state APP)
        component-query (comp/get-query Person)
        component-ident [:person/id 1]
        starting-entity (get-in state component-ident)]
    (fdn/db->tree component-query starting-entity state))

  )
