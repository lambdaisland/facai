(ns repl-sessions.new-api
  (:require [lambdaisland.facai :as facai]))

(set! *print-namespace-maps* false)

(facai/make-factory ::user
                  {:foo "bar"}
                  :traits
                  {:foo.bar/baz 123})

(facai/defactory ::user
  {:handle (facai/sequence #(str "user" %))
   :email (facai/with [:handle] #(str % "@email.com"))})

(facai/build ::user
           {:with   {:email "foo.bar@example.com"}
            :traits [:admin]})

;; extra properties
