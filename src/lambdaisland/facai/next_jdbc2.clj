(ns lambdaisland.facai.next-jdbc2
  (:require [clojure.string :as str]
            [inflections.core :as inflections]
            [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as fk]
            [lambdaisland.facai.persistence :as fp]
            [camel-snake-kebab.core :as csk]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.quoted :as quoted]
            [next.jdbc.result-set :as result-set]))

(def next-jdbc-persistence
  (reify fp/Persistence
    (-persist! [this {:facai.next-jdbc/keys [ds primary-key table-fn insert-opts quote-fn] :as ctx} fact opts value]
      (let [table (quote-fn (or (:facai.factory/table-name fact)
                                (table-fn fact)))]
        (merge value (sql/insert! ds table value insert-opts))))
    (-link-value [this {:facai.next-jdbc/keys [primary-key fk-col-fn] :as ctx} fact opts acc k value]
      (let [pk (or (:facai.factory/primary-key fact) primary-key)]
        (assoc acc (fk-col-fn k) (get value pk))))))

(defn as-kebab-maps [rs opts]
  (let [kebab #(str/replace % #"_" "-")]
    (result-set/as-modified-maps rs (assoc opts :qualifier-fn (constantly nil) :label-fn kebab))))

(defn table-fn [fact]
  (inflections/plural (name (:facai.factory/id fact))))

(def default-ctx
  {:facai.hooks/build-association fp/build-association
   :facai.hooks/build-factory fp/build-factory
   :facai/persistence next-jdbc-persistence
   :facai.next-jdbc/primary-key :id
   :facai.next-jdbc/quote-fn quoted/ansi
   :facai.next-jdbc/table-fn table-fn
   :facai.next-jdbc/fk-col-fn identity
   :facai.next-jdbc/insert-opts {:builder-fn as-kebab-maps
                                 :column-fn csk/->snake_case_string}})

(defn create-fn [ctx]
  (let [ctx (merge-with #(if (map? %1) (merge %1 %2) %2) default-ctx ctx)
        ctx (update-in ctx [:facai.next-jdbc/insert-opts :column-fn] #(comp (:facai.next-jdbc/quote-fn ctx) %))]
    (fn create!
      ([factory]
       (create! factory nil))
      ([factory rules]
       (create! factory rules nil))
      ([factory rules opts]
       (let [ctx (merge-with #(if (map? %1) (merge %1 %2) %2)
                             ctx
                             (select-keys opts (keys ctx)))]
         (fk/build ctx factory opts))))))
