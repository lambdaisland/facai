(ns lambdaisland.facai
  "Factories for unit tests, devcards, etc."
  (:refer-clojure :exclude [def])
  (:require [lambdaisland.facai.kernel :as fk]
            [lambdaisland.facai.macro-util :as macro-util]
            [lambdaisland.facai.toposort :as zt]))

(defn factory
  "Create a factory instance, these are just maps with a `(comp :type meta)` of
  `:facai/factory`. Will take keyword arguments (`:id`, `:traits`), and one
  non-keyword argument which will become the factory template (can also be
  passed explicitly with a `:template` keyword)."
  [& args]
  (loop [m (with-meta (fk/->Factory) {:type :facai/factory})
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

(defmacro defactory [fact-name & args]
  `(def ~fact-name
     (binding [fk/*defer-build?* true]
       (factory :id '~(macro-util/qualify-sym &env fact-name) ~@args))))

(defn build*
  ([factory]
   (build* factory nil))
  ([factory opts]
   (fk/build nil factory opts)))

(defn build
  ([factory]
   (build factory nil))
  ([factory opts]
   (:facai.result/value (fk/build nil factory opts))))

(defn build-all
  ([factory]
   (build-all factory nil))
  ([factory rules]
   (build-all factory rules nil))
  ([factory rules opts]
   (let [{:facai.result/keys [value linked] :as res}
         (fk/build nil factory opts)]
     (into [value] (map :value linked)))))
