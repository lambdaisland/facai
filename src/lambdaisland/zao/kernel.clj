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

(defn- resolve-ref [registry {:keys [ref opts]}]
  (let [ref-traits (:traits opts)
        {:keys [factory inherit traits]} (doto (get registry ref) assert)]
    (reduce
     (fn [fact trait]
       (merge fact (get traits trait)))
     (if inherit
       (merge (resolve-ref registry inherit) factory)
       factory)
     ref-traits)))

(comment
  (resolve-ref {:author {:factory {:name "Arne"}
                         :traits {:admin {:admin? true}}}}
               (ref :author {:traits [:admin]})))

(defn- path-match? [path selector]
  (when (seq path)
    (loop [[p & ps] path
           [s & ss] (if (sequential? selector) selector [:> selector])
           i 0]
      (prn [p ps s ss])
      (cond
        (= i 10)
        (throw (ex-info "overflow" {:pps [p ps] :sss [s ss]}))

        (and (nil? p) (nil? s))
        true

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

(defn match-rule [rules path]
  (some #(when (path-match? path (key %))
           (val %))
        rules))

(defn build [{:keys [registry hooks path rules] :as ctx} query]
  (let [rule (match-rule rules path)
        process (fn [type f]
                  (let [result (f ctx)]
                    (run-hooks hooks type result query ctx)))]
    (cond
      (and rule (not= rule query))
      (process :rule #(build % rule))

      (ref? query)
      (process
       :ref
       #(let [rule-traits (match-rule rules (conj path :lambdaisland.zao/traits))
              query (cond-> query rule-traits (update-in [:opts :traits] into rule-traits))
              ref (:ref query)]
          (prn path)
          (-> %
              (push-path ref)
              (assoc :ref ref)
              (build (doto (resolve-ref registry query) prn))
              (assoc :ref ref :path path))))

      (topo/with? query)
      (process
       :with
       (fn [ctx]
         {:value (apply (:f query) ((apply juxt (:args query)) (:value ctx)))
          :ctx ctx}))

      (map-entry? query)
      (process
       :map-entry
       (fn [ctx]
         (let [{:keys [value linked ctx] :as result} (build (push-path ctx (key query)) (val query))]
           {:value (assoc (:value ctx) (key query) value)
            :linked (cond-> linked (ref? (val query)) (conj (dissoc result :linked :ctx)))
            :ctx ctx})))

      (map? query)
      (process
       :map
       (fn [ctx]
         (reduce
          (fn [{:keys [ctx] :as acc} kv]
            (let [{:keys [value linked #_ctx]}
                  (build (assoc ctx :value (:value acc)) kv)]
              (-> acc
                  (assoc :value value)
                  (into-linked linked))))
          {:value {}
           :ctx ctx}
          (topo/sort-by-with query))))

      (coll? query)
      (process
       :coll
       (fn [ctx]
         (let [results (map-indexed (fn [idx qry]
                                      (build (push-path ctx idx) qry))
                                    query)]
           {:value (into (empty query) (map :value) results)
            :linked (into [] (mapcat :linked results))
            :ctx ctx})))

      (fn? query)
      (process :fn (fn [ctx] {:value (query) :ctx ctx}))

      :else
      (process :literal (fn [ctx] {:value query :ctx ctx})))))
