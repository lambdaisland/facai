(ns lambdaisland.zao
  "Factories for unit tests, devcards, etc."
  (:refer-clojure :exclude [def])
  (:require [lambdaisland.zao.kernel :as zk]
            [lambdaisland.zao.toposort :as zt]))

(defonce registry (atom {}))

(defmacro defactory [name factory & [opts]]
  `(swap! registry assoc ~name (assoc ~opts :factory ~factory)))

(defn sequence
  ([]
   (let [cnt (volatile! 0)]
     #(vswap! cnt inc)))
  ([f]
   (let [s (sequence)] #(f (s)))))

(def ref zk/ref)
(def ref? zk/ref?)
(def with zt/with)
(def with? zt/with?)

(defn- refify [o]
  (cond
    (and (keyword? o) (contains? @registry o))
    (zk/ref o)

    (map? o)
    (update-vals o refify)

    (coll? o)
    (into (empty o) (map refify) o)

    :else o))

(defn build
  ([factory]
   (build factory nil))
  ([factory rules]
   (build factory rules nil))
  ([factory rules opts]
   (let [{:keys [value] :as res} (zk/build (merge {:registry @registry
                                                   :rules rules}
                                                  opts)
                                           (refify factory))]
     (with-meta value res))))

(defn build-all
  ([factory]
   (build-all factory nil))
  ([factory rules]
   (build-all factory rules nil))
  ([factory rules opts]
   (let [{:keys [value linked] :as res} (zk/build (merge {:registry @registry
                                                          :rules rules}
                                                         opts)
                                                  (refify factory))]
     (into [value] (map :value linked)))))
