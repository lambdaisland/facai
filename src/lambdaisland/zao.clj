(ns lambdaisland.zao
  "Factories for unit tests, devcards, etc."
  (:refer-clojure :exclude [def])
  (:require [lambdaisland.zao.kernel :as k]))

(defonce registry (atom {}))

(defmacro defactory [name factory & [opts]]
  (swap! registry assoc name (assoc opts :factory factory)))

(defactory :uuid random-uuid)
(defactory :sequence inc {:init 0})
