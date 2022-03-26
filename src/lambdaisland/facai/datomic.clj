(ns lambdaisland.facai.datomic
  (:require [clojure.walk :as walk]
            [datomic.api :as d]
            [lambdaisland.facai.kernel :as fk]))

(defn build-factory
  "Facai hook that overrides the logic for building a factory."
  [{:facai.datomic/keys [conn]
    :facai.build/keys [path] :as ctx} fact opts]
  (let [result (fk/build-factory* ctx fact opts)
        result (update result :facai.result/value
                       #(cond-> %
                          (not (:db/id %))
                          (assoc :db/id (str (gensym "facai.datomic/tempid")))))]
    (if (seq path)
      (-> result
          (fk/add-linked path (:facai.result/value result))
          (update :facai.result/value :db/id))
      (let [{:keys [tempids db-after]}
            @(d/transact conn
                         (conj (vals (:facai.result/linked result))
                               (:facai.result/value result)))]
        (assoc
          (walk/postwalk (fn [o]
                           (if-let [tid (and (string? o) (get tempids o))]
                             tid
                             o))
                         result)
          :db-after db-after)))))

(defn create!
  "Create datomic entities based on the factory and any factories it links to."
  ([conn factory]
   (create! conn factory nil))
  ([conn factory rules]
   (create! conn factory rules nil))
  ([conn factory rules opts]
   (fk/build {:facai.hooks/build-factory build-factory
              :facai.datomic/conn conn} factory opts)))
