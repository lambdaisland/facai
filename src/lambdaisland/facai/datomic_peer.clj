(ns lambdaisland.facai.datomic-peer
  (:require [clojure.walk :as walk]
            [datomic.api :as d]
            [lambdaisland.facai :as f]
            [lambdaisland.facai.kernel :as fk]))

(defn update-tempids [coll tempids]
  (walk/postwalk (fn [o]
                   (if-let [tid (and (string? o) (get tempids o))]
                     tid
                     o))
                 coll))

(defn walk-entity->id [coll]
  (walk/postwalk (fn [e]
                   (if-let [id (:db/id e)]
                     id
                     e))
                 coll))

(defn transact-result! [conn result]
  (let [{:keys [tempids db-after]} @(d/transact conn [(:facai.result/value result)])]
    (-> result
        (update :facai.result/value update-vals walk-entity->id)
        (update :facai.result/linked update-vals (fn [v] (update-vals v walk-entity->id)))
        (update :facai.result/value update-tempids tempids)
        (update :facai.result/linked update-tempids tempids)
        (assoc ::db-after db-after))))

(defn create!
  "Create datomic entities based on the factory and any factories it links to."
  ([conn factory]
   (create! conn factory nil))
  ([conn factory opts]
   (as-> opts $
     (assoc $ :after-build-factory
            (fn [ctx]
              (update ctx :facai.result/value
                      assoc :db/id (str (gensym "facai.datomic/tempid")))))
     (fk/build nil factory $)
     (transact-result! conn $))))

(defn entity [result selector]
  (when-let [id (:db/id (f/sel1 result selector))]
    (d/entity (::db-after result) id)))
