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

(defn refify [o]
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
   (let [{:keys [value] :as res} (zk/build {:registry @registry
                                            :rules rules}
                                           (refify factory))]
     (with-meta value res))))

(def ref zk/ref)
(def with zt/with)
