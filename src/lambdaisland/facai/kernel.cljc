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

(defn match1? [p s]
  (or (= s p)
      (= :* s)
      (and (set? s) (some (partial match1? p) s))
      (and (factory? s) (match1? p (:facai.factory/id s)))))

(defn path-match? [path selector]
  (let [selector (if (sequential? selector) selector [selector])
        path (if (and (keyword? (last selector))
                      (symbol? (last path)))
               (butlast path)
               path)]
    (when (seq path)
      (loop [[p & ps] path
             [s & ss] selector
             i        0]
        (when (< 10 i) (throw (Exception. "too much recursion")))
        (cond
          (and (nil? p) (nil? s))
          true

          (or (nil? p) (nil? s))
          false

          (match1? p s)
          (do
            (if (seq ps)
              (or (when (seq ss)
                    (path-match? ps ss))
                  (path-match? ps (cons s ss)))
              (and (empty? ps) (empty? ss))))

          (= s :>)
          (if (match1? p (first ss))
            (recur ps (next ss) (inc i))
            false)

          :else
          (recur ps (cons s ss) (inc i)))))))

(defn match-rules [ctx]
  (some #(when (path-match? (:facai.build/path ctx) (key %)) (val %)) (:facai.build/rules ctx)))

(defn factory-template
  [{:facai.factory/keys [template inherit traits]}
   {:as opts with :with selected-traits :traits}]
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

(defn build-factory*
  "Handle building a factory, which means building its template, possibly adjusted
  for traits, and doing some contextual bookkeeping so we can keep track of the
  path within the build process."
  [{:facai.build/keys [path] :as ctx} factory opts]
  (let [{:facai.factory/keys [id]} factory
        ctx (cond-> ctx id (push-path id))
        result (-> ctx
                   (build-template (factory-template factory opts) opts)
                   (assoc :facai.factory/id id))]
    (if path
      (assoc result :facai.build/path (:facai.build/path ctx))
      result)))

(defn build-factory
  [{:facai.hooks/keys [build-factory] :as ctx} factory opts]
  (if build-factory
    (build-factory ctx factory opts)
    (let [{:as   result
           path  :facai.build/path
           value :facai.result/value} (build-factory* ctx factory opts)]
      (cond-> result path (add-linked path value)))))

(defn build-map-entry
  "Handle a single entry of a map-shaped template (see [[build-template]]),
  handles recursing into building the value, and associng the built value into
  the result."
  [{:facai.hooks/keys [build-association] :as ctx} val-acc k v opts]
  (if (and build-association (or (factory? v) (deferred-build? v)))
    (let [[fact opts] (if (deferred-build? v) [((:thunk v)) (:opts v)] [v nil])]
      (build-association ctx val-acc k fact opts))
    (let [{:as ctx path :facai.build/path} (push-path ctx k)
          v (or (match-rules ctx) v)]
      (let [{:facai.result/keys [value linked] :as result} (build ctx v nil)]
        {:facai.result/value (assoc val-acc k value)
         :facai.result/linked linked}))))

(defn build-template
  "Build a value out of a 'template'. This template is basically a data shape that
  describes the shape of the output value. It can be a map (each value gets
  built), a collection like a set or vector (each entry gets built), a
  thunk (the return value gets built), or a plain value (used as is).

  Factories contain templates (as well as an id, hooks, traits, etc). What you
  pass to `facai/build` is also a template."
  [{:facai.build/keys [path] :as ctx} tmpl opts]
  (cond
    (map? tmpl)
    (reduce-kv
     (fn [acc k v]
       (let [{:facai.result/keys [value linked]}
             (build-map-entry ctx (:facai.result/value acc) k v opts)]
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

(defn build [ctx query {:as opts
                        rules :rules
                        selected-traits :traits}]
  (let [opts (dissoc opts :rules)
        ctx (cond-> ctx rules (assoc :facai.build/rules rules))
        ctx (cond
              (factory? query)
              (build-factory ctx query opts)

              (deferred-build? query)
              (build-factory ctx ((:thunk query)) (:opts query))

              :else
              (build-template ctx query opts))
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
