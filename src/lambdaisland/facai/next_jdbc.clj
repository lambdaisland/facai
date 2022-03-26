(ns lambdaisland.facai.next-jdbc
  (:require [clojure.string :as str]
            [inflections.core :as inflections]
            [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as fk]
            [camel-snake-kebab.core :as csk]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.quoted :as quoted]
            [next.jdbc.result-set :as result-set]))

(defn build-factory [{:facai.next-jdbc/keys [ds primary-key table-fn insert-opts quote-fn] :as ctx} fact opts]
  (let [table (quote-fn (or (:facai.next-jdbc/table fact)
                            (table-fn fact)))
        result (fk/build-factory* ctx fact opts)
        row (sql/insert! ds table
                         (:facai.result/value result)
                         insert-opts)]
    (update result :facai.result/value merge row)))

(defn build-association [{:facai.next-jdbc/keys [primary-key] :as ctx} acc k fact opts]
  (let [pk (or (:facai.next-jdbc/primary-key fact) primary-key)
        ctx (fk/push-path ctx k)
        {:facai.result/keys [value linked] :as result}
        (fk/build-factory ctx fact opts)]
    (-> {:facai.result/value (assoc acc k (get value pk))
         :facai.result/linked linked}
        (fk/add-linked (:facai.build/path ctx) value))))

(defn as-kebab-maps [rs opts]
  (let [kebab #(str/replace % #"_" "-")]
    (result-set/as-modified-maps rs (assoc opts :qualifier-fn (constantly nil) :label-fn kebab))))

(defn create-fn [{:facai.next-jdbc/keys [ds primary-key table-fn quote-fn insert-opts]
                  :or {primary-key :id
                       quote-fn quoted/ansi
                       table-fn (fn [fact]
                                  (inflections/plural (name (:facai.factory/id fact))))
                       }}]
  (let [insert-opts (merge {:builder-fn as-kebab-maps
                            :column-fn (comp quote-fn csk/->snake_case_string)} insert-opts)]
    (fn create!
      ([factory]
       (create! factory nil))
      ([factory rules]
       (create! factory rules nil))
      ([factory rules opts]
       (let [ctx  {:facai.hooks/build-association build-association
                   :facai.hooks/build-factory build-factory
                   :facai.next-jdbc/ds (:facai.next-jdbc/ds opts ds)
                   :facai.next-jdbc/primary-key (:facai.next-jdbc/primary-key opts primary-key)
                   :facai.next-jdbc/quote-fn (:facai.next-jdbc/quote-fn opts quote-fn)
                   :facai.next-jdbc/table-fn (:facai.next-jdbc/table-fn opts table-fn)
                   :facai.next-jdbc/insert-opts (merge (:facai.next-jdbc/insert-opts opts) insert-opts)}]
         (fk/build ctx factory opts))))))
