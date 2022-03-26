(ns lambdaisland.facai.jdbc
  "Basic implementation of JDBC-based factory creation. We only use what Java
  provides out of the box, and mash our own SQL strings together. This may serve
  or your need, or it may be inspiration for writing your own persistence hooks."
  (:require [clojure.string :as str]
            [inflections.core :as inflections]
            [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as fk])
  (:import (java.sql Statement)))

(defn sql-ident [s]
  (str "\"" (str/replace s #"\"" "\"\"") "\""))

(defn insert-sql [{:keys [table columns]}]
  (str "INSERT INTO "
       (sql-ident table)
       " (" (str/join "," (map sql-ident columns)) ") "
       "VALUES "
       " (" (str/join "," (repeat (count columns) "?")) ") "))

(defn insert-stmt [conn opts]
  (.prepareStatement
   conn (insert-sql opts)
   Statement/RETURN_GENERATED_KEYS))

(defn insert! [conn table kvs]
  (let [columns (map first kvs)
        values (mapv second kvs)
        stmt (insert-stmt conn {:table table :columns columns})]
    (dotimes [idx (count kvs)]
      (.setObject stmt (inc idx) (get values idx)))
    (assert (= 1 (.executeUpdate stmt)))
    (first (resultset-seq (.getGeneratedKeys stmt)))))

(defn exec! [conn sql]
  (.executeUpdate (.createStatement conn) sql))

(defn build-factory [{:facai.jdbc/keys [conn primary-key table-fn prop->col assoc->col col->prop] :as ctx} fact opts]
  (let [table (or (:facai.jdbc/table fact)
                  (table-fn fact))
        result (fk/build-factory* ctx fact opts)
        row (insert! conn table (map (fn [[k v]]
                                       (if ((some-fn fk/factory? fk/deferred-build?) (get-in fact [:facai.factory/template k]))
                                         [(assoc->col k) v]
                                         [(prop->col k) v]))
                                     (:facai.result/value result)))]
    (update result :facai.result/value
            #(reduce-kv (fn [acc k v]
                          (assoc acc (col->prop table k) v))
                        %
                        row))))


(defn build-association [{:facai.jdbc/keys [primary-key] :as ctx} acc k fact opts]
  (let [pk (or (:facai.jdbc/primary-key fact) primary-key)
        ctx (fk/push-path ctx k)
        {:facai.result/keys [value linked] :as result}
        (fk/build-factory ctx fact opts)]
    (-> {:facai.result/value (assoc acc k (get value pk))
         :facai.result/linked linked}
        (fk/add-linked (:facai.build/path ctx) value))))

(defn create-fn [{:facai.jdbc/keys [conn primary-key table-fn prop->col col->prop assoc->col qualify?]
                  :or {primary-key :id
                       table-fn (fn [fact]
                                  (inflections/plural (name (:facai.factory/id fact))))
                       prop->col #(str/replace (name %) #"-" "_")
                       assoc->col #(str (str/replace (name %) #"-" "_") "_id")}}]
  (let [col->prop (or col->prop
                      (fn [table col]
                        (if (false? qualify?)
                          (keyword (str/replace (name col) #"_" "-"))
                          (keyword (inflections/singular table) (str/replace (name col) #"_" "-")))))]
    (fn create!
      ([factory]
       (create! factory nil))
      ([factory rules]
       (create! factory rules nil))
      ([factory rules opts]
       (let [ctx  {:facai.hooks/build-association build-association
                   :facai.hooks/build-factory build-factory
                   :facai.jdbc/conn (:facai.jdbc/conn opts conn)
                   :facai.jdbc/primary-key (:facai.jdbc/primary-key opts primary-key)
                   :facai.jdbc/table-fn (:facai.jdbc/table-fn opts table-fn)
                   :facai.jdbc/col->prop (:facai.jdbc/col->prop opts col->prop)
                   :facai.jdbc/prop->col (:facai.jdbc/prop->col opts prop->col)
                   :facai.jdbc/assoc->col (:facai.jdbc/assoc->col opts assoc->col)}]
         (fk/build ctx factory opts))))))
