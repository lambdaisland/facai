(ns lambdaisland.facai
  "Factories for unit tests, devcards, etc."
  (:refer-clojure :exclude [def])
  (:require [lambdaisland.facai.kernel :as fk]
            #?(:clj [lambdaisland.facai.macro-util :as macro-util]))
  #?(:cljs (:require-macros [lambdaisland.facai])))

(declare build-val)

;; Factories don't have to be records, a simple map with the right keys will do,
;; but we like it to have invoke so they are callable as a shorthand for calling
;; build. They should have {:type :facai/factory} as metadata.
;; - :facai.factory/id - fully qualified symbol of the factory var
;; - :facai.factory/template - the template we will build, often a map but can be anything
;; - :facai.factory/traits - map of traits (name -> map)
(defrecord Factory []
  #?@(:clj
      (clojure.lang.IFn
       (invoke [this] (build-val this nil))
       (invoke [this opts] (build-val this opts)))
      :cljs
      (cljs.core/IFn
       (-invoke [this] (build-val this nil))
       (-invoke [this opts] (build-val this opts)))))

(defn factory
  "Create a factory instance, these are just maps with a `(comp :type meta)` of
  `:facai/factory`. Will take keyword arguments (`:id`, `:traits`), and one
  non-keyword argument which will become the factory template (can also be
  passed explicitly with a `:template` keyword)."
  [& args]
  (loop [m (with-meta (->Factory) {:type :facai/factory})
         [x & xs] args]
    (cond
      (nil? x)
      m
      (simple-keyword? x)
      (recur (assoc m (keyword "facai.factory" (name x)) (first xs))
             (next xs))
      (qualified-keyword? x)
      (recur (assoc m x (first xs))
             (next xs))
      :else
      (recur (assoc m :facai.factory/template x)
             xs))))

#?(:clj
   (defmacro defactory
     "Factory convenience macro. Takes a name and keyword-value pairs, if a keyword
  is omitted then the value is treated as the factory template. Simple keywords
  are namespaced to `facai.factory`."
     [fact-name & args]
     `(def ~fact-name
        (binding [fk/*defer-build?* true]
          (factory :id '~(macro-util/qualify-sym &env fact-name)
                   :resolve #(do ~fact-name)
                   ~@args)))))

(defn unify
  "Use in rules to signify that certain parts of the build tree should be unified,
  i.e. that they should share the same value. It plays a similar role as an lvar
  in logic programming. Invocations without arguments return unique lvars,
  invocations with an id argument are idempotent, allowing unification across
  multiple rules with the same lvar."
  ([]
   (unify (gensym "unify")))
  ([id]
   (fk/->LVar id)))

(defn build
  "Build the given factory or template, returns a result map with
  `:facai.result/value` and `:facai.result/linked`."
  ([factory]
   (build factory nil))
  ([factory opts]
   (fk/build nil factory opts)))

(defn build-val
  "Build the given factory or template, returns the result value, discarding the
  linked entities."
  ([factory]
   (build-val factory nil))
  ([factory opts]
   (if-let [thunk (and fk/*defer-build?* (:facai.factory/resolve factory))]
     (fk/defer thunk opts)
     (:facai.result/value (fk/build nil factory opts)))))

(defn build-all
  "Build the given factory or template. Returns a sequence of all entities that were built."
  ([factory]
   (build-all factory nil))
  ([factory rules]
   (build-all factory rules nil))
  ([factory rules opts]
   (let [{:facai.result/keys [value linked] :as res}
         (fk/build nil factory opts)]
     (into [value] (map :value linked)))))

(defn value
  "Given the result map returned by [[build]], retrieve the built value."
  [result]
  (:facai.result/value result))

(defn sel
  "Given the result map returned by [[build]], return any entities that match the
  given selector."
  [result selector]
  (let [selector (if (vector? selector) selector [selector])]
    (keep #(when (fk/path-match? (key %) selector)
             (val %))
          (:facai.result/linked result))))

(defn sel1
  "Given the result map returned by [[build]], return the first entity that
  matches the given selector."
  [result selector]
  (first (sel result selector)))

(defn update-result
  "Update the result value in a context map, useful in hooks."
  [ctx f & args]
  (apply update ctx :facai.result/value f args))
