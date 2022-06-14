(ns lambdaisland.facai.xtdb
  "XTDB support for factories"
  (:require [xtdb.api :as xt]
            [lambdaisland.facai.kernel :as fk]))

(defn result-tx
  "Turn a Facai result map into a XTDB transaction."
  [{:facai.result/keys [value linked]}]
  (into []
        (keep #(when (:xt/id %)
                 [::xt/put %]))
        (cons value (vals linked))))

(defn after-build-factory-impl
  "`:facai.hooks/after-build-factory` implementation which assigns a `:xt/id` and
  replaces association values with their id."
  [ctx]
  (update ctx :facai.result/value
          (fn [v]
            (update-vals
             (cond-> v
               (not (:xt/id v))
               (assoc :xt/id ((:xt-id-fn ctx) v)))
             (fn [v]
               (if-let [id (:xt/id v)]
                 id
                 v))))))

(defn build
  "Like `lambdaisland.facai/build`, but returns data that is suitable for
  transacting into XTDB."
  ([template]
   (build template nil))
  ([template {:keys [xt-id-fn]
              :or {xt-id-fn (fn [_] (random-uuid))}
              :as opts}]
   (let [opts (update
               opts :after-build-factory
               (fnil comp identity) after-build-factory-impl)]
     (fk/build {:xt-id-fn xt-id-fn} template opts))))

(defn create!
  "Based on a factory/template, persist all entities in the given XTDB node.
  Returns a result map."
  ([node template]
   (create! node template nil))
  ([node template opts]
   (let [result (build template opts)]
     (->> result
          result-tx
          (xt/submit-tx node)
          (xt/await-tx node))
     result)))
