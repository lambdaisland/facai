(ns repl-sessions.poke
  (:require [lambdaisland.facai :as f]
            [clojure.string :as str]))

(def short-words
  ["bat" "bar" "cat" "dud" "lip" "map" "pap" "sip" "fig" "wip"])

(defn rand-id []
  (str/join "-"
            (take 3
                  (shuffle (concat (map str/upper-case short-words)
                                   short-words)))))

(f/defactory cycle
  {:type :cycle
   :id rand-id})

(f/defactory user
  {:type :user
   :id rand-id
   :name "Finn"})

(f/defactory organization
  {:type :organization
   :id rand-id})

(f/defactory organization-user
  {:type :organization-user
   :id rand-id
   :organization-id organization
   :user-id user})

(f/defactory property
  {:type :property
   :id rand-id
   :org-id organization
   :created-by user})

(f/defactory property-cycle-user
  {:type :property-cycle-user
   :id rand-id
   :cycle-id cycle
   :property-id property
   :user-id user})

(defrecord LVar [identity])

(property-cycle-user
 {:rules {[:created-by] (->LVar :user)
          [:user-id] (->LVar :user)
          [:org-id] (->LVar :org)
          [:organization-id] (->LVar :org)}})

(f/sel
 (f/build property-cycle-user)
 [:created-by])

(f/sel
 (f/build property-cycle-user)
 [#{:user-id :created-by}])

(f/sel
 (f/build property-cycle-user)
 [user])
(keys
 (:facai.result/linked
  (f/build property-cycle-user)))

(f/build property-cycle-user
         {:rules {[user :name] "Jake"
                  [property] {:foo "bar"}}})
