(ns lambdaisland.zao-test
  (:require [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk]
            [clojure.test :refer :all]))

(zao/defactory user
  {:name "Arne"})

(deftest basic-attributes
  (testing "factories are themselves callable"
    (is (= {:name "Arne"} (user))))
  (testing "factories can be built explicitly"
    (is (= {:name "Arne"} (zao/build user))))
  (testing "overriding attributes"
    (is (= {:name "John"} (user {:with {:name "John"}})))
    (is (= {:name "John"} (zao/build user {:with {:name "John"}}))))
  (testing "additional attributes"
    (is (= {:name "Arne" :age 39} (user {:with {:age 39}})))
    (is (= {:name "Arne" :age 39} (zao/build user {:with {:age 39}})))))

(zao/defactory post
  {:title "Things To Do"
   :author (user {:with {:name "Tobi"}})})

(deftest association-test
  (testing "expansion of nested factories is deferred"
    (is (= (zk/map->Factory
            {:zao.factory/id 'lambdaisland.zao-test/post,
             :zao.factory/template
             {:title "Things To Do",
              :author (zk/->DeferredBuild #'lambdaisland.zao-test/user nil)}})
           post)))

  (is (= {:title "Things To Do", :author {:name "Arne"}}
         (post)))

  (is (= {:title "Things To Do", :author {:name "Arne", :admin? true}}
         (post {:with {:author admin}}))))

(zao/defactory admin
  :inherit user
  {:admin? true})

(deftest inheritance
  (is (= {:name "Arne", :admin? true} (admin))))

(zao/defactory line-item
  {:description "widget"
   :quantity 1
   :price 9.99}

  :traits {:discounted
           {:price 0.99
            :discount "5%"}})

(deftest traits
  (is (= {:description "widget", :quantity 1, :price 0.99, :discount "5%"}
         (line-item {:traits [:discounted]}))))

(zao/defactory dice-roll
  {:dice-type (constantly 6)
   :number-of-dice (constantly 2)})

(deftest evaluate-functions
  (is (= {:dice-type 6 :number-of-dice 2} (dice-roll))))
