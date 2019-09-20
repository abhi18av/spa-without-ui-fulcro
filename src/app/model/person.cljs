(ns app.model.person
  (:require
    [app.utils :refer [clog get-components-that-query-for-a-prop]]
    [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]
    [com.fulcrologic.fulcro.data-fetch :as df]
    [com.fulcrologic.fulcro.components :as comp]))

(defn person-path [& ks] (into [:person/id] ks))
(defn picker-path [k] [:component/id :person-picker k])
(defn person-list-path [k] [:component/id :person-list k])

(defmutation make-older [{:person/keys [id] :as params}]
  (action [{:keys [state]}]
          (clog {:message "[PERSON] MUTATION make-older" :color "magenta" :props state})
          (swap! state update-in (person-path id :person/age) inc))

  (remote [env] true))

(defmutation select-person [{:person/keys [id] :as params}]
  (action [{:keys [app state]}]
          (clog {:message "[PERSON] MUTATION select-person" :color "magenta" :props state})
          (swap! state assoc-in (picker-path :person-picker/selected-person) [:person/id id]))
  (remote [env] true))
