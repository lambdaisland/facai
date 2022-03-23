(ns lambdaisland.zao.toposort
  "When factory properties depend on other factory properties using `with`, make
  sure they are evaluated in dependency order."
  (:require [lambdaisland.data-printers :as data-printers]))

(defrecord With [args f])
(defn with [args f] (->With args f))
(defn with? [o] (instance? With o))
(data-printers/register-pprint With 'zao/with :args)

;; Kahn's algorithm for topological sort

;; L ← Empty list that will contain the sorted elements
;; S ← Set of all nodes with no incoming edge

;; while S is not empty do
;;     remove a node n from S
;;     add n to L
;;     for each node m with an edge e from n to m do
;;         remove edge e from the graph
;;         if m has no other incoming edges then
;;             insert m into S

;; if graph has edges then
;;     return error   (graph has at least one cycle)
;; else
;;     return L   (a topologically sorted order)

(defn sort-by-with [m]
  (let [nodes (seq m)
        edges (for [[k v] nodes
                    :when (with? v)
                    a (:args v)]
                [a k])
        s (remove (comp with? val) nodes)]
    (loop [edges edges
           l []
           s s]
      (let [[n & s] s]
        (cond
          (some? n)
          (let [l (conj l n)
                [edges s]
                (reduce
                 (fn [[edges s] [from to]]
                   (if (= from (key n))
                     (let [edges (remove #{[from to]} edges)]
                       [edges
                        (if (some (comp #{to} second) edges)
                          s
                          (conj s (some (fn [[k v :as kv]]
                                          (when (= k to)
                                            kv))
                                        nodes)))])
                     [edges s]))
                 [edges s]
                 edges)]
            (recur edges l s))

          (seq edges)
          (throw (ex-info "Cycle in `with` declarations"
                          {:edges edges}))

          :else
          l)))))

(comment
  (def m
    {:foo 123
     :bar (with [:foo] nil)
     :baq (with [:bar] nil)
     :qux (with [:bar :baq] nil)
     :zoo (with [:qux :foo] nil)

     })

  (sort-by-with m))


#_#_
(topo/with? query)
{:value (apply (:f query) ((apply juxt (:args query)) (:value ctx)))}
