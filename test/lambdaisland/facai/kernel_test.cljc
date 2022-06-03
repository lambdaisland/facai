(ns lambdaisland.facai.kernel-test
  (:require [clojure.test :refer [deftest testing is are]]
            [lambdaisland.facai.kernel :as fk]))

(def my-factory ^{:type :facai/factory} {:facai.factory/id `my-factory})

(deftest path-match?-test
  (testing "matches if the last element of the path matches"
    (is (fk/path-match? [:a :b :c] [:c]))
    (is (not (fk/path-match? [:a :b :c] [:b]))))
  (testing "intermediate path elements don't need to match"
    (is (fk/path-match? [:a :b :c] [:a :c])))
  (testing "supports direct descendant with :>"
    (is (fk/path-match? [:a :b :c] [:b :> :c]))
    (is (not (fk/path-match? [:a :b :c] [:a :> :c]))))
  (testing ":> can anchor to the start"
    (is (fk/path-match? [:a :b :c] [:> :a :c])))
  (testing "matches factory instance to factory id"
    (is (fk/path-match? [:a :b `my-factory] [my-factory])))
  (testing "supports wildcard matching with :*"
    (is (fk/path-match? [:a :b :c] [:a :*]))
    (is (fk/path-match? [:a :b :c] [:* :b :> :c]))
    (is (fk/path-match? [:a :a :b :c] [:* :> :b :c]))
    (is (not (fk/path-match? [:a :b :c] [:c :*])))
    (is (not (fk/path-match? [:a :a :b :c] [:a :> :* :> :a :c])))
    (is (fk/path-match? [:a :a :b :c] [:> :a :> :a :c])))
  (testing "can match either factory (symbol) or map entry (keyword)"
    (is (fk/path-match? [`f1 :mk1 `f2 :mk2 `f3] [`f3]))
    (is (fk/path-match? [`f1 :mk1 `f2 :mk2 `f3] [:mk2]))
    (is (not (fk/path-match? [`f1 :mk1 `f2 :mk2 `f3] [`f2])))
    (is (not (fk/path-match? [`f1 :mk1 `f2 :mk2 `f3] [:mk1]))))
  (testing "will wrap non-sequence paths into a sequence"
    (is (fk/path-match? [:a :b] :b))
    (is (fk/path-match? [:a :b `c] `c))
    (is (fk/path-match? [:a :b `c] :*))
    (is (fk/path-match? [`my-factory] my-factory)))
  (testing "can match alternatives via sets"
    (is (fk/path-match? [:b] #{:a :b}))
    (is (fk/path-match? [:b] [#{:a :b}]))
    (is (fk/path-match? [:a :b :c] [#{:b} :> :*]))
    (is (fk/path-match? [:a :b :c] [#{:a :b} :> :*]))
    (is (not (fk/path-match? [:c :b] [#{:a :b} :> :*])))))

(deftest match1?-test
  (is (fk/match1? :x :x))
  (is (fk/match1? :x :*))
  (is (fk/match1? :x #{:x}))
  (is (fk/match1? `x ^{:type :facai/factory} {:facai.factory/id `x}))
  (is (fk/match1? `x #{^{:type :facai/factory} {:facai.factory/id `x}}))

  (is (not (fk/match1? :x :y))))
