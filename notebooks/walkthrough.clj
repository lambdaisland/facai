(ns notebooks.walkthrough
  (:require [lambdaisland.facai :as f]
            [lambdaisland.faker :as faker :refer [fake]]
            [lambdaisland.facai.xtdb :as facai-xt]
            [xtdb.api :as xt]))

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

(enrollment)

;; So far hopefully this all hasn't been too surprising, what we've shown so far
;; can be done with fairly little straightforward Clojure code. Now let's look
;; at what actually sets these factories apart.

;; There are two sets of features that make Facai a valuable tool, one is
;; database support, the other is allowing fine-grained declarative mechanisms
;; for generating specific shapes of data.

;; Factories are typically used in combination with a database, so you can easily
;; insert some fixture data, which you can then leverage in your tests. Facai
;; currently has support for XTDB, Datomic Peer (Free or Pro), and relational
;; databases through next.jdbc.

;; When setting up fixture data for tests you'll find that much of it is
;; boilerplate. You might need a user, profile, or tenant to satisfy some
;; constraints, but for the test at hand it really doesn't matter which specific
;; user, profile, or tenant it is. On the other hand some things very much do
;; matter. If you are testing that discounts on a shopping cart are calculated
;; correctly, then what's in the cart and the applied discount are essential
;; inputs to the test.

;; The idea with factories is that in your tests you specify exactly and only
;; the pieces of information that are relevant to the test, and let everything
;; else be provided by the factories. To help with this there are traits, hooks,
;; and selectors.

;; Let's look at these in turn:

;; ## Database Support

;; I'll use XTDB here. Since it's schema-on-read it requires the least setup for
;; demonstration.

;; This starts an in-memory xtdb database:

^{:nextjournal.clerk/viewer nextjournal.clerk.viewer/hide-result}
(def node (xt/start-node {}))

;; Now we can use Facai to populate it with data:

(def result (facai-xt/create! node enrollment))

;; This returns a Facai result map, just like `f/build`, but notice how the
;; values now have a `:xt/id`. For each database we support there is a separate
;; `create!` function which takes an implementatation-specific reference to the
;; database and a factory/template. Any entities will be inserted, and if the
;; database assigns any default values, then you'll get those back as well. For
;; instance if you have an auto-increment key in a relational database, then
;; you'll see the assigned numbers in your result.

;; Facai has some helpers to find specific entities in the result. This uses
;; selectors which we'll discuss later. For now what you need to know is that
;; the factory itself functions as a selector that finds all entities built by
;; that factory.

(f/sel result course)
(f/sel result student)

;; Notice how this enrollment is no longer a nested map, but a flat structure
;; with UUID references, since that's how this structure gets represented in the
;; database.

(f/sel result enrollment)

;; ## Traits
