(ns lambdaisland.facai.kernel-test
  (:require [lambdaisland.facai.kernel :as fk]
            [clojure.test :refer :all]))

(def profile-factory
  {:handle #(rand-nth ["chromatic69" "headflights" "RoombaBrain"])
   :name "Jack Appleflap"
   :website "http://random.site"})

(def article-factory
  {:title "the article title"
   :profile (fk/ref ::profile)})

(def registry
  {:uuid {:facai.factory/name :uuid
          :facai.factory/definition random-uuid}
   ::profile {:facai.factory/name ::profile
              :facai.factory/definition profile-factory}
   ::article {:facai.factory/name ::article
              :facai.factory/definition article-factory}})

(def uuid-hooks
  {:map (fn [res qry ctx]
          (assoc-in res [:value :id] (random-uuid)))
   :ref (fn [res qry ctx]
          (if-let [id (and (map? (:value res))
                           (:id (:value res)))]
            (assoc-in res [:value (:ref qry)] id)))})

(deftest atomic-values-test
  (is (= {:value :foo :ctx {}} (fk/build {} :foo)))
  (is (= {:value 123 :ctx {}} (fk/build {} 123)))
  (is (= {:value "foo" :ctx {}} (fk/build {} "foo")))
  (is (= {:value #inst "2022-03-18" :ctx {}} (fk/build {} #inst "2022-03-18"))))

(deftest basic-registry-test
  (is (= "Arne Brasseur"
         (:value
          (fk/build {:registry {:name {:facai.factory/definition #(str "Arne" " " "Brasseur")}}}
                    (fk/ref :name))))))

(deftest map-test
  (is (= {:name "Arne Brasseur" :age 39}
         (:value (fk/build {:registry {::name {:facai.factory/definition #(str "Arne" " " "Brasseur")}}}
                           {:name (fk/ref ::name)
                            :age 39})))))

(fk/build {:registry registry}
          (fk/ref ::article))
