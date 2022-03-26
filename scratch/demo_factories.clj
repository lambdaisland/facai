(ns scratch.demo-factories)

(ns lambdaisland.facai.demo-factories
  (:require [lambdaisland.facai :as facai]
            [lambdaisland.facai.kernel :as zk]
            [lambdaisland.facai.helpers :as zh]))

(facai/defactory ::user
  {:user/name "John Doe"
   :user/handle (facai/sequence #(str "john" %))
   :user/email (facai/with [:user/handle]
                         (fn [handle]
                           (str handle "@doe.com")))
   :user/roles #{}}

  :traits
  {:admin
   {:user/roles #{:admin}}})

(facai/defactory ::member
  :inherit ::user
  {:membership_expires #(zh/days-from-now 100)})

(facai/defactory ::article
  {:author (facai/ref :facai/user)
   :title "7 Tip-top Things To Try"}
  :traits
  {:published {:status "published"}
   :unpublished {:status "unpublished"}
   :in-the-future {:published-at #(zh/days-from-now 2)}
   :in-the-past {:published-at #(zh/days-ago 2)}})




(comment
  (facai/build ::user)
  (facai/build ::user {:user/handle "timmy"})
  (facai/build ::user {} {:traits [:admin]})
  (facai/build ::article {} {::facai/traits [:published :in-the-future]})

  (facai/build ::article
             {[::article :> :title] ""
              ::facai/traits [:published :in-the-future]
              [:author :user/handle] "timmy"
              [:author ::facai/traits] [:admin]})

  (zk/build {:registry @facai/registry
             :hooks [{:ref (fn [result qry ctx]
                             (prn :ref qry '-> result)
                             result)
                      :map (fn [result qry ctx]
                             (prn :map qry '-> result)
                             result)}]}
            (facai/ref :facai/article))

  (facai/build {:john :facai/user
              :mick :facai/admin}
             {[:john :user/handle] "johny"
              [:mick :user/handle] "micky"})

  (facai/build (vec (repeat 5 :facai/user))
             {[:> 0 :user/handle] "foo"})

  (facai/build-all :facai/article
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
