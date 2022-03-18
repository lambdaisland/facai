(ns lambdaisland.zao.kernel
  "Heart of the Zao factory logic"
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
    (let [{:keys [ref]} query]
      (assoc
        (build (update ctx :path conj query)
               (get-in registry [ref :factory]))
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

    (list? query)
    {:value (eval query)}

    (coll? query)
    (let [results (map-indexed (fn [idx qry]
                                 (build (update ctx :path conj idx) qry))
                               query)]
      {:value (into (empty query) (map :value) results)
       :linked (into [] (mapcat :linked results))})

    :else
    {:value query}))

(build {:registry {:uuid {:factory '(random-uuid)}}}
       (ref :uuid))

(build {:registry {:uuid {:factory '(random-uuid)}
                   :user {:factory {:id (ref :uuid)}}}}
       [(ref :user) (ref :uuid) 123 '(+ 1 2)])

(build {:registry {:user {:factory {:id (ref :uuid)}}}}
       :user)

(build {} '(+ 1 1))

(def profile-factory
  {:handle '(rand-nth ["chromatic69" "headflights" "RoombaBrain"])
   :name "Jack Appleflap"
   :website "http://random.site"})

(def article-factory
  {:title "the article title"
   :profile (ref ::profile)})

(def uuid-hooks
  {:finalize-entity (fn [m {:keys [path]}]
                      (assoc-in m [:value :id] (random-uuid))
                      #_(assoc-in m [:value :path] path))
   :handle-association (fn [acc k v value]
                         (assoc-in acc [:value k] (:id value)))})

(def registry
  {:uuid {:factory '(random-uuid)}
   ::profile {:factory profile-factory}
   ::article {:factory article-factory}})

(build {:registry registry
        :hooks [uuid-hooks]}
       (ref ::article))


(build {:registry registry
        :hooks [uuid-hooks]}
       {:article {:n (ref ::article)}})

(build {:registry registry
        :hooks [uuid-hooks]}
       [(ref ::article)
        (ref ::article)])
