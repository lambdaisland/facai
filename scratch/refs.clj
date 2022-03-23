(ns scratch.refs)

(defn- resolve-ref
  "Based on a registry and ref (factory name + options), return a fully expanded
  factory (typically a map)"
  [registry {:keys [ref opts]}]
  (let [ref-traits (:traits opts)
        ref-with (:with opts)
        {:zao.factory/keys [definition inherit traits]} (doto (get registry ref) assert)]
    (cond->
        (reduce
         (fn [fact trait]
           (merge fact (get traits trait)))
         (if inherit
           (merge (resolve-ref registry inherit) definition)
           definition)
         ref-traits)
      ref-with
      (merge ref-with))))


(comment
  (resolve-ref {:author {:factory {:name "Arne"}
                         :traits {:admin {:admin? true}}}}
               (ref :author {:traits [:admin]})))


(ref? query)
(let [ref (:ref query)]
  (-> ctx
      (push-path ref)
      (build (resolve-ref registry query))
      (assoc :ref ref)
      (cond-> path (assoc :path path))))
