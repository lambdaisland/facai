(ns scratch.demo-factories)

(ns lambdaisland.zao.demo-factories
  (:require [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk]
            [lambdaisland.zao.helpers :as zh]))

(zao/defactory ::user
  {:user/name "John Doe"
   :user/handle (zao/sequence #(str "john" %))
   :user/email (zao/with [:user/handle]
                         (fn [handle]
                           (str handle "@doe.com")))
   :user/roles #{}}

  :traits
  {:admin
   {:user/roles #{:admin}}})

(zao/defactory ::member
  :inherit ::user
  {:membership_expires #(zh/days-from-now 100)})

(zao/defactory ::article
  {:author (zao/ref :zao/user)
   :title "7 Tip-top Things To Try"}
  :traits
  {:published {:status "published"}
   :unpublished {:status "unpublished"}
   :in-the-future {:published-at #(zh/days-from-now 2)}
   :in-the-past {:published-at #(zh/days-ago 2)}})




(comment
  (zao/build ::user)
  (zao/build ::user {:user/handle "timmy"})
  (zao/build ::user {} {:traits [:admin]})
  (zao/build ::article {} {::zao/traits [:published :in-the-future]})

  (zao/build ::article
             {[::article :> :title] ""
              ::zao/traits [:published :in-the-future]
              [:author :user/handle] "timmy"
              [:author ::zao/traits] [:admin]})

  (zk/build {:registry @zao/registry
             :hooks [{:ref (fn [result qry ctx]
                             (prn :ref qry '-> result)
                             result)
                      :map (fn [result qry ctx]
                             (prn :map qry '-> result)
                             result)}]}
            (zao/ref :zao/article))

  (zao/build {:john :zao/user
              :mick :zao/admin}
             {[:john :user/handle] "johny"
              [:mick :user/handle] "micky"})

  (zao/build (vec (repeat 5 :zao/user))
             {[:> 0 :user/handle] "foo"})

  (zao/build-all :zao/article
                 {}
                 {:hooks [{:map-entry (fn [result query _]
                                        (if (zk/ref? (val query))
                                          (update-in result [:value (key query)]  :id)
                                          result)
                                        )
                           :ref (fn [result _ _]
                                  (prn [:ref (:value result)])
                                  (if (map? (:value result))
                                    (update result :value assoc :id (rand-int 100))
                                    result))}]})


  )
