(ns repl-sessoins.var-factories
  (:require [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk]))

(def user
  (zk/factory
   :id `user
   {:name "Arne"}))
