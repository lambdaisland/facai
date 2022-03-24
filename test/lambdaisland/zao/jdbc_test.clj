(ns lambdaisland.zao.jdbc-test
  (:require [lambdaisland.zao.jdbc :as jdbc]
            [lambdaisland.zao :as zao]
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

(zao/defactory user
  {:name "Arne"})

(zao/defactory post
  {:title "Things To Do"
   :author (user {:with {:name "Tobi"}})})

(zao/defactory article
  {:title "Things To Do"
   :author user}
  :zao.jdbc/table "posts")

#_
(let [conn (make-h2-conn "foo")]
  (jdbc/exec! conn (create-table-sql {:table "users"
                                      :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                                                "name" "VARCHAR(255)"
                                                }}))
  (jdbc/exec! conn (create-table-sql {:table "posts"
                                      :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                                                "title" "VARCHAR(255)"
                                                "author_id" "INT"
                                                }})))

(def create! (jdbc/create-fn {:zao.jdbc/conn (make-h2-conn "foo")
                              :zao.jdbc/qualify? false}))

(create! article)

(post)

(zao/build* post)
