(ns notebooks.walkthrough
  (:require [lambdaisland.facai :as f]
            [lambdaisland.faker :as faker :refer [fake]]))


;; # 🧧 Factories for Fun and Profit. 恭喜發財

;; Facai (rhymes with "high") is a factory library for Clojure and ClojureScript,
;; inspired by the likes of Factory Bot (Ruby) and Ex Machina (Elixir), but with
;; some unique features that set it apart. In this walkthrough we'll build up your
;; understanding from bottom to top.

;; The main entry point when using Facai is the [[lambdaisland.facai/build]]
;; function. Build takes a _template_ or _factory_, and optionally a map of
;; options.

(f/build ["hello"])

;; `build` returns a "result map", contained a _value_, and a map of _linked
;; entities_, which in this case is empty. We'll ignore linked entities for now,
;; and instead use the `build-val` function, which returns the value directly.

(f/build-val ["hello"])

;; The argument passed to `build`/`build-val` is a _template_. Any value is a
;; valid template, when a template gets built, the following rules are applied.

;; - functions are invoked (they should take no arguments)
;; - factories build the factory template
;; - Clojure collections are traversed and built recursively
;; - other values are preserved as-is

(f/build-val {:number (partial rand-int 100)})

;; ## 🏭 Factories

;; A _factory_ combines a template, an id (qualified symbol), and optionally
;; other things like traits and hooks.

;; Let's look at a simple factory for a user, with an `:id` and `:email`. We'll
;; also use `lambdaisland.faker` to make our factory more random, while still
;; generating realistic-looking data. Consult
;; the [supported_fakers.clj](https://github.com/lambdaisland/faker/blob/main/supported_fakers.clj)
;; to find the appropriate fakers for your use case.

(def user
  ^{:type :facai/factory}
  {:facai.factory/id `user
   :facai.factory/template
   {:user/id random-uuid
    :user/email #(fake [:internet :email])}})

(f/build-val user)

;; Factories must have a `:type :facai/factory` metadata, and a fully qualified
;; `:facai.factory/id`. We recommend using the `defactory` convenience macro to set
;; up your factories.

(f/defactory user2
  {:user/id random-uuid
   :user/email #(fake [:internet :email])})

(f/build-val user2)

;; When you use `defactory` it also becomes possible to treat your factories as functions, calling them does the same thing as passing them to `build-val`.

(user2)

;; ## 🔗 Associations

;; Let's look at a more complex example where we combine multiple factories. Say we
;; are building a Educational Management System.

(defn date-of-birth []
  (let [over18-epoch-days (.toEpochDay
                           (.minusYears
                            (java.time.LocalDate/now)
                            18))]
    (java.time.LocalDate/ofEpochDay (- over18-epoch-days (rand-int 5000)))))

;; We'll have institutions (colleges or universities):

(f/defactory institution
  {:institution/name #(fake [:educator :university])})

;; And students:

(f/defactory student
  {:student/name #(fake [:name :name])
   :student/date-of-birth date-of-birth})

;; Courses have a course name, a start and end time, and are taught at a
;; specific institution. Note how we simply reference the institution factory
;; here.

(f/defactory course
  {:course/name #(fake [:educator :course-name])
   :course/start-time #(fake [:time :date-time])
   :course/end-time #(fake [:time :date-time])
   :course/institution institution})

;; Finally we allow students to enroll in courses.

(f/defactory enrollment
  {:enrollment/student student
   :enrollment/course course})

;; Now when you build an "enrollment", it follows the various associations, so
;; it generates a student, course, and institution as well.

(f/build-val
 enrollment
 {:after-build-factory
  (fn [ctx]
    (f/update-result ctx assoc :xt/db (random-uuid)))})
