(ns lambdaisland.facai.jdbc-test
  (:require [lambdaisland.facai.jdbc :as jdbc]
            [lambdaisland.facai :as facai]
            [clojure.string :as str]
            [clojure.test :refer :all])
  (:import (java.sql DriverManager)))

(defn make-h2-conn [name]
  (DriverManager/getConnection (str "jdbc:h2:/tmp/" name)))

(defn create-table-sql [{:keys [table columns]}]
  (str "CREATE TABLE "
       (jdbc/sql-ident table)
       " ("
       (str/join "," (for [[k v] columns]
                       (str (jdbc/sql-ident k) " " v)))
       ")"))

(defn drop! [conn table]
  (jdbc/exec! conn (str "DROP TABLE " (jdbc/sql-ident table))))

(facai/defactory user
  {:name "Arne"})

(facai/defactory post
  {:title "POST TITLE"
   :author (user {:with {:name "Tobi"}})})

(facai/defactory article
  {:title "ARTICLE TITLE"
   :author user}
  :facai.jdbc/table "posts")

(def table-defs [{:table "users"
                  :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                            "name" "VARCHAR(255)"}}
                 {:table "posts"
                  :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                            "title" "VARCHAR(255)"
                            "author_id" "INT"}}])

(deftest basic-jdbc-persistence-test
  (let [conn (make-h2-conn (str "h2-db-" (rand-int 1e8)))
        create! (jdbc/create-fn {:facai.jdbc/conn conn
                                 :facai.jdbc/qualify? false})]
    (run! #(jdbc/exec! conn (create-table-sql %)) table-defs)

    (is (= {:facai.factory/id `post
            :facai.result/value {:id 1
                                 :title "POST TITLE"
                                 :author 1}
            :facai.result/linked {[`post
                                   :author]
                                  {:id 1
                                   :name "Tobi"}}}
           (create! post)))))
