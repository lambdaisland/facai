(ns lambdaisland.zao.next-jdbc
  (:require [clojure.string :as str]
            [inflections.core :as inflections]
            [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk]
            [camel-snake-kebab.core :as csk]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.quoted :as quoted]
            [next.jdbc.result-set :as result-set]))

(defn build-factory [{:zao.next-jdbc/keys [ds primary-key table-fn insert-opts quote-fn] :as ctx} fact opts]
  (let [table (quote-fn (or (:zao.next-jdbc/table fact)
                            (table-fn fact)))
        result (zk/build-factory* ctx fact opts)
        row (sql/insert! ds table
                         (:zao.result/value result)
                         insert-opts)]
    (update result :zao.result/value merge row)))

(defn build-association [{:zao.next-jdbc/keys [primary-key] :as ctx} acc k fact opts]
  (let [pk (or (:zao.next-jdbc/primary-key fact) primary-key)
        ctx (zk/push-path ctx k)
        {:zao.result/keys [value linked] :as result}
        (zk/build-factory ctx fact opts)]
    (-> {:zao.result/value (assoc acc k (get value pk))
         :zao.result/linked linked}
        (zk/add-linked (:zao.build/path ctx) value))))

(defn as-kebab-maps [rs opts]
  (let [kebab #(str/replace % #"_" "-")]
    (result-set/as-modified-maps rs (assoc opts :qualifier-fn (constantly nil) :label-fn kebab))))

(defn create-fn [{:zao.next-jdbc/keys [ds primary-key table-fn quote-fn insert-opts]
                  :or {primary-key :id
                       quote-fn quoted/ansi
                       table-fn (fn [fact]
                                  (inflections/plural (name (:zao.factory/id fact))))
                       }}]
  (let [insert-opts (merge {:builder-fn as-kebab-maps
                            :column-fn (comp quote-fn csk/->snake_case_string)} insert-opts)]
    (fn create!
      ([factory]
       (create! factory nil))
      ([factory rules]
       (create! factory rules nil))
      ([factory rules opts]
       (let [ctx  {:zao.hooks/build-association build-association
                   :zao.hooks/build-factory build-factory
                   :zao.next-jdbc/ds (:zao.next-jdbc/ds opts ds)
                   :zao.next-jdbc/primary-key (:zao.next-jdbc/primary-key opts primary-key)
                   :zao.next-jdbc/quote-fn (:zao.next-jdbc/quote-fn opts quote-fn)
                   :zao.next-jdbc/table-fn (:zao.next-jdbc/table-fn opts table-fn)
                   :zao.next-jdbc/insert-opts (merge (:zao.next-jdbc/insert-opts opts) insert-opts)}]
         (zk/build ctx factory opts))))))
