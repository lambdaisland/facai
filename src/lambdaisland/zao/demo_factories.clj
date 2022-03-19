(ns lambdaisland.zao.demo-factories
  (:require [lambdaisland.zao :as z]))

(z/defactory :zao/user
  {:user/name "John Doe"
   :user/handle (z/sequence #(str "john" %))
   :user/email (z/with
                [:user/handle]
                #(str % "@doe.com"))
   :user/roles #{}})

(z/defactory :zao/admin
  {:user/roles #{:admin}}
  {:inherit :zao/user})

(z/build :zao/admin)
(z/build :zao/user {:user/handle "timmy"})

(z/build {:john :zao/user
          :mick :zao/admin}
         #_         {[:john :user/handle] "johny"
                     [:mick :user/handle] "micky"})

(get @z/registry :zao/admin)
