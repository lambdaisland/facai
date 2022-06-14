(ns lambdaisland.facai.xtdb-test
  (:require [clojure.test :refer :all]
            [lambdaisland.facai :as f]
            [lambdaisland.facai.kernel :as fk]
            [lambdaisland.facai.xtdb :as fxt]
            [xtdb.api :as xt]))

(f/defactory institution
  {:institution/name "Ghent University"})

(f/defactory course
  {:course/name "Formele Logica I"
   :course/institution institution})

(deftest create-test
  (let [node (xt/start-node {})
        result (fxt/create!
                node course
                {:xt-id-fn
                 (let [i (atom 0)]
                   (fn [_]
                     (keyword (str "entity" (swap! i inc)))))})]
    (is (= {:institution/name "Ghent University"
            :xt/id :entity1}
           (xt/pull (xt/db node) '[*] :entity1)))

    (is (= {:course/name "Formele Logica I"
            :course/institution :entity1
            :xt/id :entity2}
           (xt/pull (xt/db node) '[*] :entity2)))

    (is (= {:course/name "Formele Logica I"
            :course/institution :entity1
            :xt/id :entity2}
           (:facai.result/value result)))

    (is (= '{[lambdaisland.facai.xtdb-test/course
              :course/institution
              lambdaisland.facai.xtdb-test/institution]
             {:institution/name "Ghent University"
              :xt/id :entity1}

             [lambdaisland.facai.xtdb-test/course]
             {:course/name "Formele Logica I"
              :course/institution :entity1
              :xt/id :entity2}}
           (:facai.result/linked result)))))
