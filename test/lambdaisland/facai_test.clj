(ns lambdaisland.facai-test
  (:require [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as fk]
            [clojure.test :refer :all]))

(facai/defactory user
  {:name "Arne"})

(deftest basic-attributes
  (testing "factories are themselves callable"
    (is (= {:name "Arne"} (user))))
  (testing "factories can be built explicitly"
    (is (= {:name "Arne"} (facai/build user))))
  (testing "overriding attributes"
    (is (= {:name "John"} (user {:with {:name "John"}})))
    (is (= {:name "John"} (facai/build user {:with {:name "John"}}))))
  (testing "additional attributes"
    (is (= {:name "Arne" :age 39} (user {:with {:age 39}})))
    (is (= {:name "Arne" :age 39} (facai/build user {:with {:age 39}})))))

(facai/defactory post
  {:title "Things To Do"
   :author (user {:with {:name "Tobi"}})})

(facai/defactory admin
  :inherit user
  {:admin? true})

(deftest association-test
  (testing "expansion of nested factories is deferred"
    (is (= (fk/map->Factory
            {:facai.factory/id 'lambdaisland.facai-test/post,
             :facai.factory/template
             {:title "Things To Do",
              :author (fk/->DeferredBuild #'lambdaisland.facai-test/user {:with {:name "Tobi"}})}})
           post)))

  (is (= {:title "Things To Do", :author {:name "Tobi"}}
         (post)))

  (is (= {:title "Things To Do", :author {:name "Arne", :admin? true}}
         (post {:with {:author admin}}))))

(deftest inheritance
  (is (= {:name "Arne", :admin? true} (admin))))

(facai/defactory line-item
  {:description "widget"
   :quantity 1
   :price 9.99}

  :traits {:discounted
           {:price 0.99
            :discount "5%"}})

(deftest traits
  (is (= {:description "widget", :quantity 1, :price 0.99, :discount "5%"}
         (line-item {:traits [:discounted]}))))

(facai/defactory dice-roll
  {:dice-type (constantly 6)
   :number-of-dice (constantly 2)})

(deftest evaluate-functions
  (is (= {:dice-type 6 :number-of-dice 2} (dice-roll))))
