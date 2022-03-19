(ns lambdaisland.zao.kernel-test
  (:require [lambdaisland.zao.kernel :as zk]
            [clojure.test :refer :all]))

(def profile-factory
  {:handle #(rand-nth ["chromatic69" "headflights" "RoombaBrain"])
   :name "Jack Appleflap"
   :website "http://random.site"})

(def article-factory
  {:title "the article title"
   :profile (zk/ref ::profile)})

(def registry
  {:uuid {:factory random-uuid}
   ::profile {:factory profile-factory}
   ::article {:factory article-factory}})

(def uuid-hooks
  {:finalize-entity (fn [m {}]
                      (assoc-in m [:value :id] (random-uuid)))
   :handle-association (fn [acc k v value]
                         (assoc-in acc [:value k] (:id value)))})

(def nest-hooks
  {:handle-association (fn [acc k v value]
                         (assoc-in acc [:value k] value))})

(deftest atomic-values-test
  (is (= {:value :foo} (zk/build {} :foo)))
  (is (= {:value 123} (zk/build {} 123)))
  (is (= {:value "foo"} (zk/build {} "foo")))
  (is (= {:value #inst "2022-03-18"} (zk/build {} #inst "2022-03-18"))))

(deftest basic-registry-test
  (is (= "Arne Brasseur"
         (:value
          (zk/build {:registry {:name {:factory #(str "Arne" " " "Brasseur")}}}
                    (zk/ref :name))))))

(deftest collections-test
  (is (= {:name "Arne Brasseur" :age 39}
         (:value (zk/build {:registry {::name {:factory #(str "Arne" " " "Brasseur")}}
                            :hooks [nest-hooks]}
                           {:name (zk/ref ::name)
                            :age 39}))))


  )

;; (require 'kaocha.repl)
;; (kaocha.repl/run 'lambdaisland.zao.kernel-test)


;; (zk/build {:registry {:uuid {:factory '(random-uuid)}}}
;;           (ref :uuid))

;; (build {:registry {:uuid {:factory '(random-uuid)}
;;                    :user {:factory {:id (ref :uuid)}}}}
;;        [(ref :user) (ref :uuid) 123 '(+ 1 2)])

;; (build {:registry {:user {:factory {:id (ref :uuid)}}}}
;;        :user)

;; (build {} '(+ 1 1))



;; (build {:registry registry
;;         :hooks [uuid-hooks]}
;;        (ref ::article))


;; (build {:registry registry
;;         :hooks [uuid-hooks]}
;;        {:article {:n (ref ::article)}})

;; (build {:registry registry
;;         :hooks [nest-hooks]}
;;        [(ref ::article)
;;         (ref ::article)])
