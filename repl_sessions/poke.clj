(ns repl-sessions.poke
  (:require [lambdaisland.facai :as f]
            [lambdaisland.facai.kernel :as fk]
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
         {:rules {[user :name] cycle
                  [property] user}})

(some #(when (fk/path-match? '[repl-sessions.poke/property-cycle-user :user-id repl-sessions.poke/user :name] (key %)) (val %)) {[user :name] "Jake"
                                                                                                                                 [property] {:foo "bar"}})


(def kk2
  (keys
   (:facai.result/linked
    (f/build property-cycle-user
             #_{:rules {[user] (fk/->LVar :x)}}))))

(count kk2)

(remove (set kk) kk2)

(f/build-val [property-cycle-user
              organization-user]
             {:rules {[#{:org-id :organization-id}] (fk/->LVar :x)}})


(f/build-val [property-cycle-user
              organization-user]
             {:rules {[organization] (fk/->LVar :x)}})


(fk/path-match? `[0 repl-sessions.poke/property-cycle-user :property-id repl-sessions.poke/property :org-id repl-sessions.poke/organization]
                [#{:org-id :organization-id}])

(f/defactory a
  {:a #(rand-int 100)})

(f/defactory b
  {:a1 a
   :a2 a
   :b "b"})

(f/defactory c
  {:b1 b
   :b2 b
   :c "c"})

(keys (:facai.result/linked (f/build c {:rules {a (f/unify)}})))
([repl-sessions.poke/c :b1 repl-sessions.poke/b :a1 repl-sessions.poke/a]
 [repl-sessions.poke/c :b1 repl-sessions.poke/b :a2 repl-sessions.poke/a]
 [repl-sessions.poke/c :b1 repl-sessions.poke/b]
 [repl-sessions.poke/c :b2 repl-sessions.poke/b :a1 repl-sessions.poke/a]
 [repl-sessions.poke/c :b2 repl-sessions.poke/b :a2 repl-sessions.poke/a]
 [repl-sessions.poke/c :b2 repl-sessions.poke/b]
 [repl-sessions.poke/c])
