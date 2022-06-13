(ns lambdaisland.facai-test
  (:require [clojure.test :refer [deftest testing is are]]
            [lambdaisland.facai :as f]
            [lambdaisland.facai.kernel :as fk]))

(f/defactory user
  {:name "Arne"})

(deftest basic-attributes
  (testing "factories are themselves callable"
    (is (= {:name "Arne"} (user))))
  (testing "factories can be built explicitly"
    (is (= {:name "Arne"} (f/build-val user))))
  (testing "overriding attributes"
    (is (= {:name "John"} (user {:with {:name "John"}})))
    (is (= {:name "John"} (f/build-val user {:with {:name "John"}}))))
  (testing "additional attributes"
    (is (= {:name "Arne" :age 39} (user {:with {:age 39}})))
    (is (= {:name "Arne" :age 39} (f/build-val user {:with {:age 39}})))))

(f/defactory post
  {:title "Things To Do"
   :author (user {:with {:name "Tobi"}})})

(f/defactory post2
  {:author (f/build-val user {:with {:name "Tobi"}})})

(f/defactory admin
  :inherit user
  {:admin? true})

(deftest association-test
  (testing "expansion of nested factories is deferred"
    (is (fk/deferred-build? (get-in post [:facai.factory/template :author])))
    (is (fk/deferred-build? (get-in post2 [:facai.factory/template :author]))))

  (is (= {:title "Things To Do", :author {:name "Tobi"}}
         (post)))

  (is (= {:title "Things To Do", :author {:name "Arne", :admin? true}}
         (post {:with {:author admin}}))))

(deftest inheritance
  (is (= {:name "Arne", :admin? true} (admin))))

(f/defactory line-item
  {:description "widget"
   :quantity 1
   :price 9.99}

  :traits {:discounted
           {:with
            {:price 0.99
             :discount "5%"}}})

(deftest traits
  (is (= {:description "widget", :quantity 1, :price 0.99, :discount "5%"}
         (line-item {:traits [:discounted]}))))

(f/defactory dice-roll
  {:dice-type (constantly 6)
   :number-of-dice (constantly 2)})

(deftest evaluate-functions
  (is (= {:dice-type 6 :number-of-dice 2} (dice-roll))))

(deftest selector-test
  (let [res (f/build post)]
    (is (= {:name "Tobi"} (f/sel1 res [:author])))))

(f/defactory multiple-hooks
  {:bar 1}

  :traits
  {:some-trait
   {:with {:bar 2}
    :after-build
    (fn [ctx]
      (f/update-result ctx update :bar inc))}}

  :after-build
  (fn [ctx]
    (f/update-result ctx update :bar #(- %))))

(deftest multiple-hooks-test
  (is (= {:bar -1} (multiple-hooks)))

  (testing "hook is applied after :with overrides"
    (is (= {:bar -5} (multiple-hooks {:with {:bar 5}}))))

  (testing "trait provides both override and hook, order is override > trait hook > top-level hook"
    (is (= {:bar -3} (multiple-hooks {:traits [:some-trait]}))))

  (testing "trait and option override, trait override is ignored but hooks fire in right order and see override value"
    (is (= {:bar -10} (multiple-hooks {:with {:bar 9} :traits [:some-trait]})))))

(f/defactory product
  {:sku "123"
   :price 12.99})

(f/defactory product-line-item
  {:product product
   :quantity 1}
  :traits
  {:balloon
   {:with
    {:product
     (product
      {:with {:sku "BAL" :price 0.99}})}}}
  :after-build
  (fn [ctx]
    (f/update-result
     ctx
     (fn [{:as res :keys [product quantity]}]
       (assoc res :total (* (:price product) quantity))))))

(deftest rules-test
  (is (= {:product {:sku "123" :price 7.5}
          :quantity 3
          :total 22.5}
         (product-line-item {:rules {:quantity 3
                                     :price 7.5}})))

  (is (= {:product {:sku "XYZ", :price 0.99}, :quantity 1, :total 0.99}
         (product-line-item {:traits [:balloon]
                             :rules {:sku "XYZ"}})))

  (is (= {:product {:price 1}, :quantity 1, :total 1}
         (product-line-item {:rules {:product {:price 1}}}))))

(f/defactory f-a
  {:a #(rand-int 100)})

(f/defactory f-b
  {:a1 f-a
   :a2 f-a
   :b "b"})

(f/defactory f-c
  {:b1 f-b
   :b2 f-b
   :c "c"})

(deftest unification-test
  (is
   (let [v (f-c {:rules {[f-a] (f/unify)}})]
     (apply =
            (map #(get-in v %)
                 [[:b1 :a1 :a]
                  [:b1 :a2 :a]
                  [:b2 :a1 :a]
                  [:b2 :a2 :a]])))))
