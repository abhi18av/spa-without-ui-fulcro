(ns app.components.address
  (:require
    ;; external libs
    ;; internal libs
    [com.fulcrologic.fulcro.application :as app]
    [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
    [com.fulcrologic.fulcro.dom :as dom]
    [com.fulcrologic.fulcro.algorithms.merge :as merge]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))




;;===== Address Component =======================================
;; TODO How to add this component in the same manner as the car component?
(defsc Address [this {:keys [:address/id :address/state] :as props}]
  {:query             [:address/id :address/state]
   :ident             :address/id
   :initial-state     {}
   :componentDidMount (fn [this]
                        (let [p (comp/props this)]
                          (clog {:message "[Address] MOUNTED" :props p})))}
  (js/console.log "[Address] UPDATED" props)
  (js/console.log "[Address] id" id)
  (js/console.log "[Address] state" state)
  (dom/div))

(def ui-address (comp/factory Address {:keyfn :address/id}))


(comment

  (comp/get-query Address)

  (comp/get-initial-state Address)

  (comp/props Address)

  (comp/get-ident Address {:address/id    01
                           :address/state "Joe-1"})

  (-> APP
      (::app/state-atom)
      deref)


  (reset! (::app/state-atom APP) {})


  (reset! (::app/state-atom APP) {:root {:person/id        1
                                         :person/name      "Joe"
                                         :person/addresses {:address/id    01
                                                            :address/state "Joe-1"}
                                         :person/cars      [{:car/id    01
                                                             :car/model "Joe-1"}]}})


  (merge/merge-component! APP Address {:person/id        2
                                       :person/name      "Sally"
                                       :person/addresses {:address/id    02
                                                          :address/state "Sally-1"}
                                       :person/cars      [{:car/id    02
                                                           :car/model "Sally-1"}]})


  (app/schedule-render! APP)

  )



