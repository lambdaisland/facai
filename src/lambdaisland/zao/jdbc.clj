(ns lambdaisland.zao.jdbc
  "Basic implementation of JDBC-based factory creation. We only use what Java
  provides out of the box, and mash our own SQL strings together. This may serve
  or your need, or it may be inspiration for writing your own persistence hooks."
  (:require [clojure.string :as str]
            [inflections.core :as inflections]
            [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk])
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
  (prn [`insert kvs])
  (let [columns (map first kvs)
        values (mapv second kvs)
        stmt (insert-stmt conn {:table table :columns columns})]
    (dotimes [idx (count kvs)]
      (.setObject stmt (inc idx) (get values idx)))
    (assert (= 1 (.executeUpdate stmt)))
    (doto (first (resultset-seq (.getGeneratedKeys stmt))) prn)))

(defn exec! [conn sql]
  (prn sql)
  (.executeUpdate (.createStatement conn) sql))

(defn build-factory [{:zao.jdbc/keys [conn primary-key table-fn prop->col assoc->col col->prop] :as ctx} fact opts]
  (let [table (or (:zao.jdbc/table fact)
                  (table-fn fact))
        result (zk/build-factory* ctx fact opts)
        row (insert! conn table (map (fn [[k v]]
                                       (if ((some-fn zk/factory? zk/deferred-build?) (get-in fact [:zao.factory/template k]))
                                         [(assoc->col k) v]
                                         [(prop->col k) v]))
                                     (:zao.result/value result)))]
    (update result :zao.result/value
            #(reduce-kv (fn [acc k v]
                          (assoc acc (col->prop table k) v))
                        %
                        row))))

(defn build-association [{:zao.jdbc/keys [assoc->prop primary-key] :as ctx} acc k fact opts]
  (let [pk (or (:zao.jdbc/primary-key fact) primary-key)
        {:zao.result/keys [value linked] :as result}
        (zk/build-factory (zk/push-path ctx k) fact opts)]
    {:zao.result/value (assoc acc k (get value pk))
     :zao.result/linked ((fnil conj []) linked value)}))

(defn create-fn [{:zao.jdbc/keys [conn primary-key table-fn prop->col col->prop assoc->col qualify?]
                  :or {primary-key :id
                       table-fn (fn [fact]
                                  (inflections/plural (name (:zao.factory/id fact))))
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
       (let [ctx  {:zao.hooks/build-association build-association
                   :zao.hooks/build-factory build-factory
                   :zao.jdbc/conn (:zao.jdbc/conn opts conn)
                   :zao.jdbc/primary-key (:zao.jdbc/primary-key opts primary-key)
                   :zao.jdbc/table-fn (:zao.jdbc/table-fn opts table-fn)
                   :zao.jdbc/col->prop (:zao.jdbc/col->prop opts col->prop)
                   :zao.jdbc/prop->col (:zao.jdbc/prop->col opts prop->col)
                   :zao.jdbc/assoc->col (:zao.jdbc/assoc->col opts assoc->col)}]
         (zk/build ctx factory opts))))))
