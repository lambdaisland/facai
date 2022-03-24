(ns lambdaisland.zao.kernel
  "Heart of the Zao factory logic

  This is strictly a Mechanism namespace: generic, unopinionated, verbose.
  See [[lambdaisland.zao]] for an interface meant for human consumption."
  (:refer-clojure :exclude [ref])
  (:require [lambdaisland.data-printers :as data-printers]))

(def ^:dynamic *defer-build?* false)

(defrecord DeferredBuild [var opts])

(defn deferred-build? [o]
  (instance? DeferredBuild o))

(declare build)

;; Factories don't have to be records, a simple map with the right keys will do,
;; but we like it to have invoke so they are callable as a shorthand for calling
;; build. They should have {:type :zao/factory} as metadata.
;; - :zao.factory/id - fully qualified symbol of the factory var
;; - :zao.factory/template - the template we will build, often a map but can be anything
;; - :zao.factory/traits - map of traits (name -> map)
(defrecord Factory []
  clojure.lang.IFn
  (invoke [this]
    ;; When called inside a factory definition we don't actually build the
    ;; value, we defer that to when the outer factory gets built
    (if *defer-build?*
      (->DeferredBuild (resolve (:zao.factory/id this)) nil)
      (:zao.result/value (build nil this nil))))
  (invoke [this opts]
    (if *defer-build?*
      (->DeferredBuild (resolve (:zao.factory/id this)) nil)
      (:zao.result/value (build nil this opts)))))

(defn factory? [o]
  (= :zao/factory (:type (meta o))))

(defn factory-template
  [{:zao.factory/keys [template inherit traits]}
   {with :with selected-traits :traits}]
  (cond->
      (reduce
       (fn [fact trait]
         (merge fact (get traits trait)))
       (if inherit
         (merge (factory-template inherit nil) template)
         template)
       selected-traits)
    with
    (merge with)))

(defn push-path [ctx segment]
  (assert segment)
  (update ctx :zao.build/path (fnil conj []) segment))

(defn- into-linked [result results]
  (prn [:il result results])
  (update result :zao.result/linked (fnil into []) results))

(declare build build-template)

(defn run-hook [hook ctx result]
  (if-let [hook-fn (get ctx hook)]
    (hook-fn ctx result)))

(defn build-factory* [{:zao.hooks/keys [build-factory]
                       :zao.build/keys [path] :as ctx} factory opts]
  (let [{:zao.factory/keys [id]} factory
        ctx (cond-> ctx id (push-path id))
        result (-> ctx
                   (build-template (factory-template factory opts))
                   (assoc :zao.factory/id id))]
    (if path
      (assoc result :zao.build/path path)
      result)))

(defn build-factory [{:zao.hooks/keys [build-factory]
                      :zao.build/keys [path] :as ctx} factory opts]
  (if build-factory
    (build-factory ctx factory opts)
    (let [result (build-factory* ctx factory opts)]
      (cond-> result
        path
        (into-linked [(:zao.result/value result)])))))

(defn build-map-entry [{:zao.hooks/keys [build-association] :as ctx} val-acc k v]
  (if (and build-association (or (factory? v) (deferred-build? v)))
    (let [[fact opts] (if (deferred-build? v) [@(:var v) (:opts v)] [v nil])]
      (build-association ctx val-acc k fact opts))
    (let [{:zao.result/keys [value linked] :as result} (build (push-path ctx k) v nil)]
      {:zao.result/value (assoc val-acc k value)
       :zao.result/linked linked})))

(defn build-template [{:zao.build/keys [path] :as ctx} tmpl]
  (cond
    (map? tmpl)
    (reduce-kv
     (fn [acc k v]
       (let [{:zao.result/keys [value linked]}
             (build-map-entry ctx (:zao.result/value acc) k v)]
         (-> acc
             (assoc :zao.result/value value)
             (into-linked linked))))
     {:zao.result/value {}}
     tmpl)

    (coll? tmpl)
    (let [results (map-indexed (fn [idx qry]
                                 (build (push-path ctx idx) qry nil))
                               tmpl)]
      {:zao.result/value (into (empty tmpl) (map :zao.result/value) results)
       :zao.result/linked (into [] (mapcat :zao.result/linked results))})

    (fn? tmpl)
    {:zao.result/value (tmpl)}

    :else
    {:zao.result/value tmpl}))

(defn build [ctx query opts]
  (cond
    (factory? query)
    (build-factory ctx query opts)

    (deferred-build? query)
    (do
      (prn (:factory query) (:opts query))
      (build-factory ctx @(:var query) (:opts query)))

    :else
    (build-template ctx query)))
