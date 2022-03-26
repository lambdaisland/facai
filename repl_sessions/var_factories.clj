(ns repl-sessoins.var-factories
  (:require [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as zk]))

(def user
  (zk/factory
   :id `user
   {:name "Arne"}))
