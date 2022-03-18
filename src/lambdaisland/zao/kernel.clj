(ns lambdaisland.zao.kernel
  "Heart of the Zao factory logic

  This is strictly a Mechanism namespace: generic, unopinionated, verbose.
  See [[lambdaisland.zao]] for an interface meant for human consumption."
  (:refer-clojure :exclude [ref]))

(defrecord Ref
    [ref opts])

(defn ref
  "Create a reference to a factory, to be resolved in the registry."
  ([ref] (->Ref ref nil))
  ([ref opts] (->Ref ref opts)))

(defn ref?
  "Is `o` a [[Ref]]"
  [o]
  (instance? Ref o))

(defn run-hooks [hooks hook val & args]
  (reduce (fn [val hookmap]
            (if-let [f (get hookmap hook)]
              (apply f val args)
              val))
          val
          hooks))

(defn build [{:keys [registry hooks path] :as ctx}
             query]
  (cond
    (ref? query)
    (let [{:keys [ref]} query
          {:keys [factory]} (doto (get registry ref) assert)]
      (prn [query ref factory])
      (assoc
        (build (assoc (update ctx :path conj query) :ref ref)
               factory)
        :ref ref
        :path (vec (reverse path))))

    (map? query)
    (run-hooks
     hooks :finalize-entity
     (reduce-kv (fn [acc k v]
                  (let [{:keys [value linked] :as result} (build (update ctx :path conj k) v)]
                    (if (ref? v)
                      (run-hooks
                       hooks :handle-association
                       (update acc :linked (fn [l] (into (conj l (dissoc result :linked)) linked)))
                       k v value)
                      (-> acc
                          (assoc-in [:value k] value)
                          (update :linked into linked)))))
                {:value {}
                 :linked []}
                query)
     ctx)

    (fn? query)
    {:value (query)}

    (coll? query)
    (let [results (map-indexed (fn [idx qry]
                                 (build (update ctx :path conj idx) qry))
                               query)]
      {:value (into (empty query) (map :value) results)
       :linked (into [] (mapcat :linked results))})

    :else
    {:value query}))
