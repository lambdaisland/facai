(ns lambdaisland.facai.jdbc-test
  (:require [clojure.string :as str]
            [clojure.test :refer :all]
            [lambdaisland.facai :as f]
            [lambdaisland.facai.next-jdbc :as fnj]
            [next.jdbc :as nj]
            [next.jdbc.quoted :as quoted]))

(defn make-h2-url [name]
  (str "jdbc:h2:/tmp/" name))

(defn create-table-sql [{:keys [table columns]}]
  (str "CREATE TABLE "
       (quoted/ansi table)
       " ("
       (str/join "," (for [[k v] columns]
                       (str (quoted/ansi k) " " v)))
       ")"))


(f/defactory user
  {:name "Arne"})

(f/defactory post
  {:title "POST TITLE"
   :author (user {:with {:name "Tobi"}})})

(f/defactory article
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
  (let [ds (nj/get-datasource (make-h2-url (str "h2-db-" (rand-int 1e8))))
        jdbc-opts {::fnj/ds ds
                   ::fnj/fk-col-fn #(keyword (str (name %) "-id"))}]
    (run! #(nj/execute! ds [(create-table-sql %)]) table-defs)

    (let [result (fnj/create! jdbc-opts post)]
      (is (= {:id 1
              :title "POST TITLE"
              :author-id 1}
             (:facai.result/value result)))

      (is (= '{[lambdaisland.facai.jdbc-test/post :author lambdaisland.facai.jdbc-test/user] {:name "Tobi", :id 1}
               [lambdaisland.facai.jdbc-test/post] {:title "POST TITLE", :id 1, :author-id 1}}
             (:facai.result/linked result))))))
