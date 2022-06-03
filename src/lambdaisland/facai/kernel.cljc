(ns lambdaisland.facai.kernel
  "Heart of the Facai factory logic

  This is strictly a Mechanism namespace: generic, unopinionated, verbose.
  See [[lambdaisland.facai]] for an interface meant for human consumption."
  (:refer-clojure :exclude [ref])
  (:require [lambdaisland.data-printers :as data-printers]))

(def ^:dynamic *defer-build?* false)

(defrecord DeferredBuild [thunk opts])

(defn defer [thunk opts]
  (->DeferredBuild thunk opts))

(defn deferred-build? [o]
  (instance? DeferredBuild o))

(declare build)

(defn factory? [o]
  (= :facai/factory (:type (meta o))))

(defn path-match? [path selector]
  (let [selector (if (sequential? selector) selector [selector])
        path (if (and (keyword? (last selector))
                      (not (keyword? (last path))))
               (butlast path)
               path)]
    (when (seq path)
      (loop [[p & ps] path
             [s & ss] selector
             i        0]
        (when (< 10 i) (throw (Exception. "too much recursion")))
        (let [s (cond
                  (factory? s)
                  (:facai.factory/id s)
                  (set? s)
                  (into #{}
                        (map #(if (factory? %) (:facai.factory/id %) %))
                        s)
                  :else
                  s)]
          (cond
            (and (nil? p) (nil? s))
            true

            (or (nil? p) (nil? s))
            false

            (or (= s p)
                (and (set? s) (contains? s p)))
            (if (and (seq ps) (seq ss))
              (recur ps ss (inc i))
              (and (empty? ps) (empty? ss)))

            (= s :*)
            (cond
              ;; consume :* and continue
              (and (seq ss) (seq ps))
              (or (path-match? ps ss)
                  (path-match? ps (cons :* ss)))
              ;; matched last element, return true
              (empty? ss)  true

              :else                         (path-match? ps (cons :* ss)))

            (= s :>)
            (if (= (first ss) p)
              (recur ps (next ss) (inc i))
              false)

            :else
            (recur ps (cons s ss) (inc i))))))))

(defn factory-template
  [{:facai.factory/keys [template inherit traits]}
   {with :with selected-traits :traits}]
  (cond->
      (reduce
       (fn [fact trait]
         (merge fact (get-in traits [trait :with])))
       (if inherit
         (merge (factory-template inherit nil) template)
         template)
       selected-traits)
    with
    (merge with)))

(defn push-path [ctx segment]
  (assert segment)
  (update ctx :facai.build/path (fnil conj []) segment))

(defn add-linked [result path entity]
  (update result :facai.result/linked (fnil assoc {}) path entity))

(defn merge-linked [result linked]
  (update result :facai.result/linked merge linked))

(declare build build-template)

(defn build-factory* [{:facai.build/keys [path] :as ctx} factory opts]
  (let [{:facai.factory/keys [id]} factory
        ctx (cond-> ctx id (push-path id))
        result (-> ctx
                   (build-template (factory-template factory opts))
                   (assoc :facai.factory/id id))]
    (if path
      (assoc result :facai.build/path (:facai.build/path ctx))
      result)))

(defn build-factory [{:facai.hooks/keys [build-factory] :as ctx} factory opts]
  (if build-factory
    (build-factory ctx factory opts)
    (let [{:as   result
           path  :facai.build/path
           value :facai.result/value} (build-factory* ctx factory opts)]
      (prn path)
      (cond-> result path (add-linked path value)))))

(defn build-map-entry [{:facai.hooks/keys [build-association] :as ctx} val-acc k v]
  (if (and build-association (or (factory? v) (deferred-build? v)))
    (let [[fact opts] (if (deferred-build? v) [((:thunk v)) (:opts v)] [v nil])]
      (build-association ctx val-acc k fact opts))
    (let [{:facai.result/keys [value linked] :as result} (build (push-path ctx k) v nil)]
      {:facai.result/value (assoc val-acc k value)
       :facai.result/linked linked})))

(defn build-template [{:facai.build/keys [path] :as ctx} tmpl]
  (cond
    (map? tmpl)
    (reduce-kv
     (fn [acc k v]
       (let [{:facai.result/keys [value linked]}
             (build-map-entry ctx (:facai.result/value acc) k v)]
         (-> acc
             (assoc :facai.result/value value)
             (merge-linked linked))))
     {:facai.result/value {}}
     tmpl)

    (coll? tmpl)
    (let [results (map-indexed (fn [idx qry]
                                 (build (push-path ctx idx) qry nil))
                               tmpl)]
      {:facai.result/value (into (empty tmpl) (map :facai.result/value) results)
       :facai.result/linked (transduce (map :facai.result/linked) merge results)})

    (fn? tmpl)
    (build ctx (tmpl) nil)

    :else
    {:facai.result/value tmpl}))

(defn build [ctx query {selected-traits :traits :as opts}]
  (let [ctx (cond
              (factory? query)
              (build-factory ctx query opts)

              (deferred-build? query)
              (build-factory ctx ((:thunk query)) (:opts query))

              :else
              (build-template ctx query))
        traits (and (factory? query) (:facai.factory/traits query))
        ctx (if (and traits selected-traits)
              (reduce
               (fn [ctx trait-key]
                 (if-let [hook (:after-build (get traits trait-key))]
                   (hook ctx)
                   ctx))
               ctx
               selected-traits)
              ctx)]
    ctx (if-let [hook (:facai.factory/after-build query)]
          (hook ctx)
          ctx)))
