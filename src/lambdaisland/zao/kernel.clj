(ns lambdaisland.zao.kernel
  "Heart of the Zao factory logic

  This is strictly a Mechanism namespace: generic, unopinionated, verbose.
  See [[lambdaisland.zao]] for an interface meant for human consumption."
  (:refer-clojure :exclude [ref])
  (:require [lambdaisland.zao.macro-util :as macro-util]
            [lambdaisland.data-printers :as data-printers]))

(def ^:dynamic *defer-build?* false)

(defrecord DeferredBuild [var opts])

(defn deferred-build? [o]
  (instance? DeferredBuild o))

(declare build)

(defrecord Factory []
  clojure.lang.IFn
  (invoke [this]
    (if *defer-build?*
      (->DeferredBuild (resolve (:zao.factory/id this)) nil)
      (build nil this nil)))
  (invoke [this opts]
    (if *defer-build?*
      (->DeferredBuild (resolve (:zao.factory/id this)) nil)
      (build nil this nil))))

(defn factory
  "Create a factory instance, these are just maps with a `(comp :type meta)` of
  `:zao/factory`. Will take keyword arguments (`:id`, `:traits`), and one
  non-keyword argument which will become the factory template (can also be
  passed explicitly with a `:template` keyword)."
  [& args]
  (loop [m (with-meta (->Factory) {:type :zao/factory})
         [x & xs] args]
    (cond
      (nil? x)
      m
      (simple-keyword? x)
      (recur (assoc m (keyword "zao.factory" (name x)) (first xs))
             (next xs))
      (qualified-keyword? x)
      (recur (assoc m x (first xs))
             (next xs))
      :else
      (recur (assoc m :zao.factory/template x)
             xs))))

(defmacro defactory [fact-name & args]
  `(def ~fact-name
     (binding [*defer-build?* true]
       (factory :id '~(macro-util/qualify-sym &env fact-name) ~@args))))

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

(defn- push-path [ctx segment]
  (assert segment)
  (update ctx :zao.build/path (fnil conj []) segment))

(defn- into-linked [result results]
  (update result :zao.result/linked (fnil into []) results))


(declare build build-template)

(defn build-factory [{:zao.build/keys [path] :as ctx} factory opts]
  (let [{:zao.factory/keys [id]} factory
        ctx (cond-> ctx id (push-path id))
        result (-> ctx
                   (build-template (factory-template factory opts))
                   (assoc :zao.factory/id id))]
    (if path
      (-> result
          (into-linked [(:zao.result/value result)])
          (assoc :zao.build/path path))
      result)))

(defn build-template [{:zao.build/keys [path] :as ctx} tmpl]
  (cond
    (map-entry? tmpl)
    (let [{:zao.result/keys [value linked] :as result} (build (push-path ctx (key tmpl)) (val tmpl) nil)]
      {:zao.result/value [(key tmpl) value]
       :zao.result/linked (cond-> linked (ref? (val tmpl)) (conj (dissoc result :zao.result/linked)))})

    (map? tmpl)
    (reduce
     (fn [acc kv]
       (let [{:zao.result/keys [value linked]}
             (build-template (assoc ctx :zao.result/value (:zao.result/value acc)) kv)]
         (-> acc
             (update :zao.result/value conj value)
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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defactory user
  {:name "Arne"})

(defactory post
  {:title "Things To Do"
   :author (user {:with {:name "Sonja"}})})

(defactory admin
  :inherit user
  {:admin? true})

(defactory line-item
  {:description "widget"
   :quantity 1
   :price 9.99}
  :traits
  {:discounted
   {:price 0.99
    :discount "5%"}})

(defactory dice-roll
  {:dice-type #(rand-nth [4 6 8 10 12 20])
   :number-of-dice #(inc (rand-int 5))})

(build nil user nil)
(build nil user {:with {:name "John"}})
(build nil user {:with {:age 20}})

(build nil post nil)
(build nil admin nil)

(build nil post {:with {:author admin}})

(build nil line-item {:traits [:discounted]})

(build nil dice-roll nil)

;; Var based approach
;; - simpler and more intuitive, no need for a registry
;; - liking the defactory syntax and (build ... {:with ... :traits ...})
;; - downside, replication. associated factories are always repeated/inlined
;; - Should the vars be callable? what do they return?
;; - nice distinction between keywords/symbols in :path
;; - how to pass options to associations? from within definition or from without
