(ns repl-sessoins.var-factories
  (:require [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as fk]))

(def user
  (fk/factory
   :id `user
   {:name "Arne"}))
