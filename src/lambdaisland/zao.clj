(ns lambdaisland.zao
  "Factories for unit tests, devcards, etc."
  (:refer-clojure :exclude [def])
  (:require [lambdaisland.zao.kernel :as zk]
            [lambdaisland.zao.toposort :as zt]))

(def registry (atom {}))


(defn defactory [fname & args]
  (let [m (apply make-factory fname args)]
    (swap! registry assoc fname m)))

(def ref zk/ref)
(def ref? zk/ref?)
(def with zt/with)
(def with? zt/with?)

(defn refify [registry o]
  (if (keyword? o)
    (ref o)
    o))

(defn build
  ([factory]
   (build factory nil))
  ([factory {:keys [with traits]}]
   (let [{:keys [value] :as res}
         (zk/build {:registry @registry} (refify @registry factory))]
     value)))

(defn build-all
  ([factory]
   (build-all factory nil))
  ([factory rules]
   (build-all factory rules nil))
  ([factory rules opts]
   (let [{:keys [value linked] :as res}
         (zk/build {:registry @registry} (refify @registry factory))]
     (into [value] linked))))
