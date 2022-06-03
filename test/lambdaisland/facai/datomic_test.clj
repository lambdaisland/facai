(ns lambdaisland.facai.datomic-test
  (:require [datomic.api :as d]
            [lambdaisland.facai :as f]
            [lambdaisland.facai.datomic :as fd]
            [lambdaisland.facai.kernel :as fk]
            [clojure.test :refer :all]))

(d/create-database "datomic:mem://foo")
(def conn (d/connect "datomic:mem://foo"))

(defn s [sname type & {:as opts}]
  (merge
   {:db/ident sname
    :db/valueType (keyword "db.type" (name type))
    :db/cardinality :db.cardinality/one}
   opts))

(f/defactory line-item
  {:line-item/description "Widgets"
   :line-item/quantity 5
   :line-item/price 1.0})

(f/defactory cart
  {:cart/created-at #(java.util.Date.)
   :cart/line-items [line-item line-item]})

(def schema
  [{:db/ident       :line-item/description,
    :db/valueType   :db.type/string,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :line-item/quantity,
    :db/valueType   :db.type/long,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :line-item/price,
    :db/valueType   :db.type/double,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :cart/created-at,
    :db/valueType   :db.type/instant,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :cart/line-items,
    :db/valueType   :db.type/ref,
    :db/cardinality :db.cardinality/many}])

(deftest basic-datomic-test
  (let [url (doto (str "datomic:mem://db" (rand-int 1e8))
              d/create-database)
        conn (d/connect url)]
    @(d/transact conn schema)
    (let [{:facai.result/keys [linked value]
           :keys [db-after]} (fd/create! conn cart)]
      (is (= #{:cart/created-at :cart/line-items :db/id}
             (set (keys value))))
      (is (= "Widgets"
             (get-in linked [[`cart :cart/line-items 0 `line-item]
                             :line-item/description]))))))
