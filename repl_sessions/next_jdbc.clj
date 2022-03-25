(ns repl-sessions.next-jdbc
  (:require [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.quoted :as quoted]
            [clojure.string :as str]
            [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk]
            [lambdaisland.zao.next-jdbc :as znj])
  (:import (java.sql DriverManager)))

(defn make-h2-url [name]
  (str "jdbc:h2:/tmp/" name))

(defn create-table-sql [{:keys [table columns]}]
  (str "CREATE TABLE "
       (quoted/ansi table)
       " ("
       (str/join "," (for [[k v] columns]
                       (str (quoted/ansi k) " " v)))
       ")"))

(defn insert-sql [{:keys [table columns]}]
  (str "INSERT INTO "
       (quoted/ansi table)
       " (" (str/join "," (map quoted/ansi columns)) ") "
       "VALUES "
       " (" (str/join "," (repeat (count columns) "?")) ") "))

;; (def conn (make-h2-url (str "test" (rand-int 1e6))))

;; (def ds (jdbc/get-datasource conn))

;; (jdbc/execute! ds [(create-table-sql
;;                     {:table "posts"
;;                      :columns
;;                      {"id" "INT AUTO_INCREMENT PRIMARY KEY"
;;                       "title" "VARCHAR(255)"
;;                       "body" "CHARACTER LARGE OBJECT"}})])


;; (jdbc/execute! ds
;;                [(insert-sql {:table "posts"
;;                              :columns ["title" "body"]})
;;                 "Hello world"
;;                 "What a beautiful day"]
;;                {:return-keys true})

;; (sql/insert! ds "\"posts\"" {"\"title\"" "foo"})
;; (sql/insert! ds "\"posts\"" {"\"title\"" "foo"})

;; (jdbc/insert)

;; (jdbc/execute! ds ["SELECT * FROM \"posts\""])


;;;;;;


(zao/defactory user
  {:name "Arne"})

(zao/defactory post
  {:title "Things To Do"
   :author-id (user {:with {:name "Tobi"}})})

(zao/defactory article
  {:title "Things To Do"
   :author-id user}
  :zao.next-jdbc/table "posts")

(let [ds (jdbc/get-datasource (make-h2-url (str "test" (rand-int 1e6))))]
  (jdbc/execute! ds [(create-table-sql {:table "users"
                                        :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                                                  "name" "VARCHAR(255)"
                                                  }})])
  (jdbc/execute! ds [(create-table-sql {:table "posts"
                                        :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                                                  "title" "VARCHAR(255)"
                                                  "author_id" "INT"
                                                  }})])

  (def create! (znj/create-fn {:zao.next-jdbc/ds ds}))

  (get-in (create! article)
          [:zao.result/linked [`article :author-id]]))

(zao/build* (repeatedly 3 #(binding [zk/*defer-build?* true]
                             (post {:with {:author-id (user {:with {:age (rand-int 10)}})}}))))

(zao/build*
 (binding [zk/*defer-build?* true]
   (post {:with {:author-id (user {:with {:age (rand-int 10)}})}})))

(post {:with {:author-id (user {:with {:age (rand-int 10)}})}})
