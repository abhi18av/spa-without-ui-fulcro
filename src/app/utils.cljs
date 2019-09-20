(ns app.utils
  (:require [com.fulcrologic.fulcro.components :as comp :refer [defsc]]))


;;===== UTILS=======================================



#_(js/console.log "%cExtra Large Yellow Text with Red Background", "background: red; color: yellow; font-size: large")

(defn clog [{:keys [message props color] :or {message "Hello, World!" color "green" props {}}}]
  (js/console.log (str "%c" message), (str "color: " color "; font-weight: bold; font-size: small;"))
  (js/console.log props))

(comment
  (clog {:message "Hello, CLog" :color "blue"})
  )



(defn get-components-that-query-for-a-prop
  [App prop]
  (reduce (fn [mounted-instances cls]
            (concat mounted-instances
                    (comp/class->all App (comp/registry-key->class cls))))
          []
          (comp/prop->classes App prop)))


