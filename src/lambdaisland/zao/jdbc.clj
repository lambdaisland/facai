(ns lambdaisland.zao.jdbc
  (:require [clojure.string :as str]
            [lambdaisland.zao.kernel :as zk])
  (:import (java.sql DriverManager Statement)))

(def conn (DriverManager/getConnection "jdbc:h2:./example"))

(defn sql-ident [s]
  (str "\"" (str/replace s #"\"" "\"\"") "\""))

(defn insert-sql [{:keys [table columns]}]
  (str "INSERT INTO "
       (sql-ident table)
       " (" (str/join "," (map sql-ident columns)) ") "
       "VALUES "
       " (" (str/join "," (repeat (count columns) "?")) ") "))

(defn create-table-sql [{:keys [table columns]}]
  (str "CREATE TABLE "
       (sql-ident table)
       " ("
       (str/join "," (for [[k v] columns]
                       (str (sql-ident k) " " v)))
       ")"))

(defn insert-stmt [conn opts]
  (.prepareStatement conn (insert-sql opts)
                     Statement/RETURN_GENERATED_KEYS))

(defn insert! [conn table m]
  (let [columns (keys m)
        stmt (insert-stmt conn {:table table
                                :columns (map name columns)})]
    (doseq [[col idx] (map list columns (range))]
      (.setObject stmt (inc idx) (get m col)))
    (assert (= 1 (.executeUpdate stmt)))
    (merge m (first (resultset-seq (.getGeneratedKeys stmt))))))

(defn exec! [conn sql]
  (.executeUpdate (.createStatement conn) sql))

(defn drop! [conn table]
  (exec! conn (str "DROP TABLE " (sql-ident table))))

(comment
  (run!
   #(exec! conn (create-table-sql %))
   #_ #(drop! conn (:table %))
   [{:table "profile"
     :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
               "handle" "VARCHAR(255)"
               "name" "VARCHAR(255)"
               "website" "VARCHAR(255)"}}
    {:table "article"
     :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
               "title" "VARCHAR(255)"
               "profile_id" "INT"}}
    ]))

(zk/build {:registry zk/registry
           :hooks [{:finalize-entity (fn [m {:keys [path] ::keys [conn]}]
                                       (if (seq path)
                                         (update m :value #(insert! conn (name (first path)) %))
                                         m))
                    :handle-association (fn [acc k v value]
                                          (assoc-in acc [:value (str (name k) "_id")] (:id value)))}]
           ::conn conn}
          (zk/ref ::zk/article))
