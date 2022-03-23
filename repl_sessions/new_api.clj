(ns repl-sessions.new-api
  (:require [lambdaisland.zao :as zao]))

(set! *print-namespace-maps* false)

(zao/make-factory ::user
                  {:foo "bar"}
                  :traits
                  {:foo.bar/baz 123})

(zao/defactory ::user
  {:handle (zao/sequence #(str "user" %))
   :email (zao/with [:handle] #(str % "@email.com"))})

(zao/build ::user
           {:with   {:email "foo.bar@example.com"}
            :traits [:admin]})

;; extra properties
