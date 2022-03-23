(ns repl-sessions.demonstration
  (:require [lambdaisland.zao :as zao]
            [lambdaisland.zao.kernel :as zk]
            [lambdaisland.zao.helpers :as zh]
            [lambdaisland.zao.jdbc :as zjdbc]))

;; You start by defining factories for each type of entity you are dealing with
;; or storing in the db (i.e. for each table). You just include default values
;; for any mandatory attributes.
;;
;; Functions are evaluated

(zao/defactory ::user
  {:traits
   {:admin {:roles #{:admin}}}}
  {:name "Enid Ramsey"
   :email "eramsey@richmond.ca"
   ;;   :date-of-birth #(zh/days-ago (* 19 365))
   ;;   :roles #{}
   })

;; You can now build data based on this factory

(zao/build ::user
           {::zao/traits [:admin]})

;; And provide any additional values

(zao/build ::user {:email "jeanine@openbg.com", :last-login (zh/days-ago 2)})

;; You can also immediately create this data in the database. There's some proof
;; of concept code in lambdaisland.zao.jdbc, but more likely we'd provide
;; building blocks for you to define your own `create!` function that handles
;; the particulars of your DB approach (e.g. table naming conventions,
;; namespacing of results, foreign key naming conventions)

(def conn (zjdbc/get-connection "jdbc:h2:./example"))

(zjdbc/exec!
 conn
 (zjdbc/create-table-sql {:table "user"
                          :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                                    "name" "VARCHAR(255)"
                                    "email" "VARCHAR(255)"
                                    "created_at" "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"}}))

(zjdbc/exec!
 conn
 (zjdbc/create-table-sql {:table "property"
                          :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
                                    "address" "VARCHAR(255)"
                                    "created_by" "INT"
                                    "created_at" "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"}}))

(zao/build ::user)
(zjdbc/create! conn ::user)
(zjdbc/create! conn ::property)


(zao/build-all ::property)

[{:address "7900 Nelson Road",
  :created_by 5}
 {:id 5
  :name "Enid Ramsey", :email "eramsey@richmond.ca"}]



(zao/defactory ::property {:table "properties"}
  {:address "7900 Nelson Road"
   ;;   :org-id (zao/ref ::organization)
   :created_by (zao/ref ::user)})

(zao/build ::property)


(zao/defactory ::organization
  {:name "City of Richmond"
   :payload {"logo_url" "http://opentech.eco/wp-content/uploads/logo_grid.png"}})

(zao/defactory ::cycle
  {:org-id (zao/ref ::organizaion)
   :name "2019 Compliance"
   :from-date #inst "2019-01-01"
   :to-date #inst "2020-01-01"
   :created-by (zao/ref ::user)})

(zao/defactory ::property-cycle
  {:property-id (zao/ref ::property)
   :cycle-id (zao/ref ::cycle)
   :state :state/added-to-cycle
   :payload {"year_built" "1996"}})
