(ns lambdaisland.facai.next-jdbc
  (:require [clojure.string :as str]
            [inflections.core :as inflections]
            [camel-snake-kebab.core :as csk]
            [lambdaisland.facai.kernel :as fk]
            [camel-snake-kebab.core :as csk]
            [next.jdbc :as jdbc]
            [next.jdbc.sql :as sql]
            [next.jdbc.quoted :as quoted]
            [next.jdbc.result-set :as result-set]))

(defn build-factory [{::keys [ds primary-key table-fn insert-opts quote-fn]
                      :facai.build/keys [path]
                      :as ctx} fact opts]
  (let [table (quote-fn (or (::table fact)
                            (table-fn fact)))
        result (fk/build-factory* ctx fact opts)
        row (sql/insert! ds table
                         (:facai.result/value result)
                         insert-opts)
        pk (or (:facai.factory/primary-key fact) primary-key)
        value (merge (:facai.result/value result) row)]
    (if (< 1 (count path))
      (-> result
          (assoc :facai.result/value value)
          (fk/add-linked path value)
          (update :facai.result/value get pk))
      (assoc result :facai.result/value value))))

(defn build-association [{::keys [fk-col-fn] :as ctx} acc k fact opts]
  (let [ctx (fk/push-path ctx k)
        {:facai.result/keys [value linked] :as result}
        (fk/build-factory ctx fact opts)]
    {:facai.result/value (assoc acc (fk-col-fn k) value)
     :facai.result/linked linked}))

(defn as-kebab-maps [rs opts]
  (let [kebab #(str/replace % #"_" "-")]
    (result-set/as-modified-maps rs (assoc opts :qualifier-fn (constantly nil) :label-fn kebab))))

(defn table-fn [fact]
  (inflections/plural (name (fk/factory-id fact))))

(def default-ctx
  {::primary-key :id
   ::quote-fn quoted/ansi
   ::table-fn table-fn
   ::fk-col-fn identity
   ::insert-opts {:builder-fn as-kebab-maps
                  :column-fn (comp quoted/ansi csk/->snake_case_string)}})

(defn create-fn [ctx]
  (let [ctx (merge-with #(if (map? %1) (merge %1 %2) %2) default-ctx ctx)
        ctx (update-in ctx [::insert-opts :column-fn] #(comp (::quote-fn ctx) %))]
    (fn create!
      ([factory]
       (create! factory nil))
      ([factory opts]
       (let [ctx (merge-with #(if (map? %1) (merge %1 %2) %2)
                             ctx
                             (select-keys opts (keys ctx)))]
         (fk/build ctx factory opts))))))

(defn create!
  ([jdbc-opts factory]
   (create! jdbc-opts factory nil))
  ([jdbc-opts factory opts]
   (let [ctx (merge
              default-ctx
              (if (::ds jdbc-opts)
                jdbc-opts
                {::ds jdbc-opts}))]
     (fk/build ctx factory
               (-> opts
                   (update :association-key (fnil comp identity)
                           (fn [{::keys [fk-col-fn]} k]
                             (fk-col-fn k)))
                   (update :after-build-factory (fnil comp identity)
                           (fn [{::keys [ds primary-key table-fn insert-opts quote-fn]
                                 :facai.result/keys [value]
                                 :facai.build/keys [path]
                                 factory :facai.build/input
                                 :as ctx}]
                             (let [table (quote-fn (or (::table factory)
                                                       (table-fn factory)))
                                   row (sql/insert! ds table value insert-opts)
                                   pk (or (:facai.factoryory/primary-key factory) primary-key)
                                   value (merge (:facai.result/value ctx) row)]
                               (-> ctx
                                   (assoc :facai.result/value (if (< 1 (count path))
                                                                (get value pk)
                                                                value))
                                   (fk/add-linked path value))))))))))
