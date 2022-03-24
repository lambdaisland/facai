(ns lambdaisland.zao
  "Factories for unit tests, devcards, etc."
  (:refer-clojure :exclude [def])
  (:require [lambdaisland.zao.kernel :as zk]
            [lambdaisland.zao.macro-util :as macro-util]
            [lambdaisland.zao.toposort :as zt]))

(defn factory
  "Create a factory instance, these are just maps with a `(comp :type meta)` of
  `:zao/factory`. Will take keyword arguments (`:id`, `:traits`), and one
  non-keyword argument which will become the factory template (can also be
  passed explicitly with a `:template` keyword)."
  [& args]
  (loop [m (with-meta (zk/->Factory) {:type :zao/factory})
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
     (binding [zk/*defer-build?* true]
       (factory :id '~(macro-util/qualify-sym &env fact-name) ~@args))))

(defn build*
  ([factory]
   (build* factory nil))
  ([factory opts]
   (zk/build nil factory opts)))

(defn build
  ([factory]
   (build factory nil))
  ([factory opts]
   (:zao.result/value (zk/build nil factory opts))))

(defn build-all
  ([factory]
   (build-all factory nil))
  ([factory rules]
   (build-all factory rules nil))
  ([factory rules opts]
   (let [{:zao.result/keys [value linked] :as res}
         (zk/build nil factory opts)]
     (into [value] (map :value linked)))))
