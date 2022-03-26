(ns repl-sessions.next-jdbc2
  (:require [lambdaisland.facai.jdbc :as jdbc]
            [next.jdbc :as nj]
            [lambdaisland.facai.next-jdbc :as next-jdbc]
            [lambdaisland.facai.next-jdbc2 :as next-jdbc2]
            [lambdaisland.facai :as facai]
            [clojure.string :as str]
            [clojure.test :refer :all])
  (:import (java.sql DriverManager)))

(defn make-h2-conn [name]
  (str "jdbc:h2:/tmp/" name))

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
                            "author" "INT"}}])

(let [ds (nj/get-datasource (make-h2-conn (str "h2-db-" (rand-int 1e8))))
      create! (next-jdbc/create-fn {:facai.next-jdbc/ds ds})]
  (run! #(nj/execute! ds [(create-table-sql %)]) table-defs)
  (create! post)
  #_
  (is (= {:facai.factory/id `post
          :facai.result/value {:id 1
                               :title "POST TITLE"
                               :author 1}
          :facai.result/linked {[`post
                                 :author]
                                {:id 1
                                 :name "Tobi"}}}
         (create! post))))
