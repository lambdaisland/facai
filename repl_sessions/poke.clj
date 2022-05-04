(ns repl-sessions.poke
  (:require [lambdaisland.facai :as f]))


(f/defactory order
  {:description "an order"}

  :after-build
  (fn [ctx]
    (f/update-result ctx assoc :xxx "xxx"))

  :after-create
  (fn [ctx]
    )

  :traits
  {:completed
   {:with {:completed-at #(java.util.Date.)}
    :after-build (fn [ctx])}

   :refunded
   {:traits [:completed]
    :with {:refunded-at #(java.util.Date.)}
    :after-build (fn [ctx])}})

(f/build order)


(f/defactory foo
  {:bar 1}

  :traits
  {:some-trait
   {:with {:bar 2}
    :after-build (fn [ctx]
                   (f/update-result ctx update :bar inc))}})

(f/build foo
         {:traits [:some-trait]})
