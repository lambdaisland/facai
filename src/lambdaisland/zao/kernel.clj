(ns lambdaisland.zao.kernel
  "Heart of the Zao factory logic

  This is strictly a Mechanism namespace: generic, unopinionated, verbose.
  See [[lambdaisland.zao]] for an interface meant for human consumption."
  (:refer-clojure :exclude [ref])
  (:require [lambdaisland.zao.toposort :as topo]
            [lambdaisland.data-printers :as data-printers]))

(defrecord Ref [ref opts])

(data-printers/register-print Ref 'zao/ref (comp vector :ref))

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

(defn- push-path [ctx segment]
  (assert segment)
  (update ctx :path (fnil conj []) segment))

(defn- into-linked [result results]
  (update result :linked (fnil into []) results))

(defn- ref->factory [registry ref]
  (let [{:keys [factory inherit]} (doto (get registry ref) assert)]
    (if inherit
      (merge (ref->factory registry inherit) factory)
      factory)))

(defn- path-match? [path selector]
  (when (seq path)
    (loop [[p & ps] path
           [s & ss] (if (sequential? selector) selector [selector])
           i 0]
      (prn [p ps s ss])
      (cond
        (= i 10)
        (throw (ex-info "overflow" {:pps [p ps] :sss [s ss]}))

        (or (nil? p) (nil? s))
        false

        (= s p)
        (if (and (seq ss) (seq ps))
          (recur ps ss (inc i))
          (and (empty? ss) (empty? ps)))


        (= s :>)
        (if (= (first ss) p)
          (recur ps (next ss) (inc i))
          false)

        :else
        (recur ps (cons s ss) (inc i))))))

(path-match?  [:mick :zao/admin :user/handle] [:mick :user/handle])

(defn build [{:keys [registry hooks path rules] :as ctx} query]
  (prn {:path path})
  (let [rule (some #(when (path-match? path (key %))
                      (prn [:match %])
                      (val %))
                   rules)]
    (when rule
      (prn [:rule rule path query]))
    (cond
      (and rule (not= rule query))
      (build ctx rule)

      (ref? query)
      (let [{:keys [ref]} query]
        (-> ctx
            (push-path ref)
            (assoc :ref ref)
            (build (ref->factory registry ref))
            (assoc :ref ref :path path)))

      (topo/with? query)
      {:value (apply (:f query) ((apply juxt (:args query)) (:value ctx)))
       :ctx ctx}

      (map-entry? query)
      (let [{:keys [value linked ctx] :as result} (build (push-path ctx (key query)) (val query))]
        (run-hooks hooks (if (ref? (val query))
                           :handle-association
                           :handle-map-entry)
                   {:value {(key query) value}
                    :linked (cond-> linked (ref? (val query)) (conj (dissoc result :linked :ctx)))
                    :ctx ctx}))

      (map? query)
      (reduce
       (fn [{:keys [ctx] :as acc} kv]
         (let [{:keys [value linked ctx]}
               (build (assoc ctx :value (:value acc)) kv)]
           (-> acc
               (update :value (fn [v]
                                (cond
                                  (map? value)
                                  (merge v value)
                                  (map-entry? value)
                                  (conj v value)
                                  :else
                                  (assoc v (key kv) value))))
               (into-linked linked))))
       {:value {}
        :ctx ctx}
       (topo/sort-by-with query))

      (coll? query)
      (let [results (map-indexed (fn [idx qry]
                                   (build (push-path ctx idx) qry))
                                 query)]
        {:value (into (empty query) (map :value) results)
         :linked (into [] (mapcat :linked results))
         :ctx ctx})

      (fn? query)
      {:value (query)
       :ctx ctx}

      :else
      {:value query
       :ctx ctx})))

(build {:registry {:bar {:factory "xyz"}
                   :baz {:factory {:name "hello"
                                   :address (ref :bar)}}}}
       {:foo 123
        :bbb (ref :baz)})


(build {:registry {:sequence {:factory (let [cnt (volatile! 0)]
                                         #(vswap! cnt inc))}
                   :address {:factory {:id (ref :sequence)
                                       :type :address}}
                   :profile {:factory {:id (ref :sequence)
                                       :type :profile}}}}
       [(ref :address)
        (ref :address)
        (ref :profile)
        (ref :profile)
        (ref :address)])

#_(build {:registry {
                     :address {:factory {:id (sequence)
                                         :type :address}}
                     :profile {:factory {:id (sequence #(str "foo" %))
                                         :type :profile}}}}
         [(ref :address)
          (ref :address)
          (ref :profile)
          (ref :profile)
          (ref :address)])
