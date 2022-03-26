(ns lambdaisland.facai.persistence
  (:require [lambdaisland.facai.kernel :as fk]))

(defprotocol Persistence
  (-persist! [this ctx fact opts value]
    "Persist the given `value`, and return an updated `value`, typically assigning a primary key field.")
  (-link-value [this ctx fact opts acc k value]
    "After building a linked (associated, nested) entity, add it to the outer entity"))

(defn build-factory [{:facai/keys [persistence] :as ctx} fact opts]
  (let [result (fk/build-factory* ctx fact opts)]
    (assoc result :facai.result/value
           (-persist! persistence ctx fact opts (:facai.result/value result)))))

(defn build-association [{:facai/keys [persistence] :as ctx} acc k fact opts]
  (let [ctx (fk/push-path ctx k)
        {:facai.result/keys [value linked] :as result}
        (fk/build-factory ctx fact opts)]
    (-> {:facai.result/value (-link-value persistence ctx fact opts acc k value)
         :facai.result/linked linked}
        (fk/add-linked (:facai.build/path ctx) value))))
