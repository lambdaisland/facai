# facai

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/facai)](https://cljdoc.org/d/com.lambdaisland/facai) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/facai.svg)](https://clojars.org/com.lambdaisland/facai)
<!-- /badges -->

Factories for fun and profit! Gongxi facai!

## Getting Started

An example speaks a thousand words. How often have you looked at a function
definition and said to yourself, "if only I could see a real-world example of
the arguments that this function is called with".

It's a recurring complaint with Clojure, it's often not obvious from the code
what the shape of the data is. And yet we constantly are in need of snippets of
data, to drive our REPL sessions, to use in our tests, to populate databases so
we can test the UI, or to display in Devcards or notebooks.

There are a few solutions to this.

- simply write out example data as EDN literals, whether it be in tests, or in
  comments interspersed or at the bottom of the file
- provide test.check style generators so you can generate values on the fly
- provide schema/spec-like type information, as a form of documentation, runtime
  validation, and possibly to drive test.check-style generators

These approaches all have value, but we think there's room for something that
sits in the middle of literal data and generators, and that can complement these
approaches.

Test.check is great at finding obscure bugs and testing the narrowest edge
cases, it does this by deliberately generating "weird" data. Unreadable
characters, huge data structures, nils. It's great for validating code
properties, but it's not meant for human consumption. The data is hard to scan.
As an example of what kind of data flows around in your system it's worthless.
It's the epitome of untypical data.

Even for testing test.check is not always the best option. Writing good
generators (even with the help of Spec or Malli) and finding suitable properties
to test is not trivial and takes practice. Many code bases would be well served
with more and better example based tests before going that route.

Writing data simply as literals, for instance in unit tests, can be totally
adequate for that, but it gets repetitive. And much of that data may be defaults
that have little bearing on what your test is asserting, thus adding noise. It
also becomes a maintenance issue if you ever decide to change your data
representation, because of the amount of duplication.

Finally we think that coming up with good example data is valuable work that
deserves a larger payoff. The same shape of data you need for your tests can
serve you well when testing UI, or when documenting internal APIs in repl
sessions or notebooks.

So this is what Facai factories are about. A standardized way to define
"factories" for the various types of information that flow through your app. It
has a bit of smarts, it knows about associations between different types of
entities, and can integrate with the database (be it relational or datalog), to
quickly create and insert the data you need.

That concludes the TED talk. Let's have a quick runthrough of what all of this
looks like.

```clojure
(ns my-app.factories
  "When starting out simply create a single `factories` namespace, you can split
  it up later if it gets too big.

  I'll alias `lambdaisland.facai` to `f` for brevity, you can alias to `facai`
  if you prefer to be a bit more explicit.
  "
  (:require [lambdaisland.facai :as f]))

;; Let's define our first factory. You pass it a template of what your data
;; looks like. In this case we simply put a literal map.
(f/defactory user
  {:user/name   "Lilliam Predovic"
   :user/handle "lilli42"
   :user/email  "lilli42@example.com"})

;; We can generate data from this template by calling `f/build-val`

(f/build-val user)
;; => #:user{:name "Lilliam Predovic",
;;           :handle "lilli42",
;;           :email "lilli42@example.com"}

;; To override additional values you can pass a map to the factory:

(f/build-val (user {:user/name "Mellissa Schimmel"}))
;; => #:user{:name "Mellissa Schimmel",
;;           :handle "lilli42",
;;           :email "lilli42@example.com"}

;; Functions in the template will be called, and the result in turn is treated
;; as a factory template.

(f/defactory user
  {:user/name   "Lilliam Predovic"
   :user/handle "lilli42"
   :user/email  #(str "lilli" (rand-int 100) "@example.com")})

(f/build-val user)
;; => #:user{:name "Lilliam Predovic",
;;           :handle "lilli42",
;;           :email "lilli92@example.com"}

;; This means you can decide if you want your factories to be fairly static, or
;; more dynamic and random. Maybe you want all usernames to be unique, the
;; numbered helper can help with that.

(require '[lambdaisland.facai.helpers :as fh])

(f/defactory user
  {:user/name   "Lilliam Predovic"
   :user/handle (fh/numbered #(str "lilli" %))
   :user/email  #(str "lilli" (rand-int 100) "@example.com")})

;; The Faker library can also be useful, especially if you want to for instance
;; populate UIs to take screenshots

(require '[lambdaisland.faker :refer [fake]])

(f/defactory user
  {:user/name   #(fake [:name :name])
   :user/handle #(fake [:internet :username])
   :user/email  #(fake [:internet :email])})

;; Factories can have traits, which are merged into the main template on demand.

(f/defactory article
  {:article/title "7 Tip-top Things To Try"
   :article/status :draft}

  :traits
  {:published {:with {:article/status "published"}}
   :unpublished {:with {:article/status "unpublished"}}
   :in-the-future {:with {:article/published-at #(fh/days-from-now 2)}}
   :in-the-past {:with {:article/published-at #(fh/days-ago 2)}}})

(f/build-val article {:traits [:published :in-the-future]})
;; => #:article{:title "7 Tip-top Things To Try",
;;              :status "published",
;;              :published-at
;;              #object[java.time.ZonedDateTime 0x345be9a1 "2022-03-30T10:09:17.584903790Z[UTC]"]}

;; If you have associations between data, then you simply use the factory as the value.
;; Or if you want to set some specifics with `:with` or `:traits` you can call the factories.

(f/defactory article
  {:article/title "7 Tip-top Things To Try"
   :article/submitter user
   :article/author (user {:with {:user/roles #{"author"}}})})

(f/build-val article)
;; => #:article{:title "7 Tip-top Things To Try",
;;              :submitter
;;              #:user{:name "Mr. Reinaldo Hartmann",
;;                     :handle "garth",
;;                     :email "ligia@grantgroup.co"},
;;              :author
;;              #:user{:name "Doria Wisoky Sr.",
;;                     :handle "romaine",
;;                     :email "darrin@schummandsons.co",
;;                     :roles #{"author"}}}


;; Note that the actual expansion gets deferred in this case, the `(user)` call
;; will only get expanded when building the `article`, so you get a different
;; username each time.

(f/build-val article)
;; => #:article{:title "7 Tip-top Things To Try",
;;              :submitter
;;              #:user{:name "Hobert Fadel",
;;                     :handle "kerry91",
;;                     :email "delana57@bednarbednarand.com"},
;;              :author
;;              #:user{:name "Rosemary Reynolds",
;;                     :handle "spencer.bruen",
;;                     :email "laurine@lockmanlockmana.info",
;;                     :roles #{"author"}}}


;; Factories can inherit from other factories. Often it's preferrable to use
;; traits, but this can be a useful feature.

(f/defactory blog-post
  :inherit article
  {:post/uri-slug "/post"})

(f/build-val blog-post)
;; => {:article/title "7 Tip-top Things To Try",
;;     :article/submitter
;;     #:user{:name "Rima Wintheiser",
;;            :handle "numbers",
;;            :email "leandrowelch@welchwelchandwe.co"},
;;     :article/author
;;     #:user{:name "Jeffrey Bruen DO",
;;            :handle "margy81",
;;            :email "williams.rippin@rippingroup.biz",
;;            :roles #{"author"}},
;;     :post/uri-slug "/post"}
```

## Deferred factory generation

When calling a factory as a function, rather than actually generating data, we
simply return a value that keeps track of the factory you called, and the
options you passed. Actual instantiation only happens when you call
`f/build-val`.

This provides a concise way to specify overrides, either when creating
factories, or when building values.

```clj
(f/defactory article
 {:article/submitter (user {:user/name "Mr. submit"})})
 
;; or

(f/build-val article {:with {:article/submitter {:user/name "Mr. submit"}}})

;; or

(f/build-val (article {:article/submitter {:user/name "Mr. submit"}}))
```

The syntax works as follows. `f/build-val` takes two arguments, a factory or
template, and a map of options: `:with`, `:rules`, `:traits`.

Calling a factory with *keyword arguments* (key value pairs) is equivalent to
passing these options in to `f/build-val`.

When passing a single map instead of keyword args this is equivalent to using
`:with`.

```clj
(f/build-val user {:with {:user/name "jill"}})
;; equals
(user :with {:user/name "jill"}
;; equals
(user {:user/name "jill"}
```

This provides a concise syntax for the most common case: overriding values,
while still allowing us to distinguish explicit options like `:rules` and
`:traits`.

## Paths, Selectors, Rules, and Unification

During the build process Facai keeps track of the "path" it is currently at.
When it handles a map entry the map key is added to the path. When it handles a
nested factory then the factory id (a fully qualified symbol) is added onto the
path. When handling other collections the sequential index is added to the path.

So for the `(article)` example above you get these successive paths:

```clj
[my-app.factories/article]
[my-app.factories/article :article/title]
[my-app.factories/article :article/submitter my-app.factories/user]
[my-app.factories/article :article/submitter my-app.factories/user :user/name]
[my-app.factories/article :article/submitter my-app.factories/user :user/name]
[my-app.factories/article :article/submitter my-app.factories/user :user/handle]
[my-app.factories/article :article/submitter my-app.factories/user :user/handle]
[my-app.factories/article :article/submitter my-app.factories/user :user/email]
[my-app.factories/article :article/submitter my-app.factories/user :user/email]
[my-app.factories/article :article/author my-app.factories/user]
[my-app.factories/article :article/author my-app.factories/user :user/name]
[my-app.factories/article :article/author my-app.factories/user :user/name]
[my-app.factories/article :article/author my-app.factories/user :user/handle]
[my-app.factories/article :article/author my-app.factories/user :user/handle]
[my-app.factories/article :article/author my-app.factories/user :user/email]
[my-app.factories/article :article/author my-app.factories/user :user/email]
[my-app.factories/article :article/author my-app.factories/user :user/roles]
[my-app.factories/article :article/author my-app.factories/user :user/roles 0]
```

These paths can be matched with selectors, these function conceptually a lot like CSS selectors:

- Selectors are vectors of keywords, symbols/factories, or numbers (indices)
- A non-vector path `:xxx` is the same as `[:xxx]`
- `[:foo]` matches any path that ends with `:foo`
- `[:foo :bar]` matches any path that ends with `:bar`, and that has `:foo` somewhere before `:bar` in the path
- `[:foo :> :bar]` matches any path that ends `:foo :bar`
- `[#{:foo :bar}]` matches any path that ends with `:foo` or `:bar`

### Selecting Associations

When creating an `(article)` Facai also created two users, one of them the
author, and one of them the submitter. You can use selectors to conveniently
pull out these associated entities. For this you use `facai/build`, which
returns a map with `:facai.result/value` (in this case the author), as well as
`:facai.result/linked`, a map with any "linked" entities. With this result map
in hand you can select linked values.

```clj
(let [result    (f/build article)
      submitter (f/sel1 result :article/submitter)
      author    (f/sel1 result :article/author)]
  {:article   (f/value result)
   :submitter submitter
   :author    author})
```

This is quite convenient in tests, you can let a single factory generate a bunch
of linked entities, and then you just reference the ones you need.

We could also get all users:

```clj
(let [result (f/build article)]
  (f/sel result user))
```

### Rules

Selectors can also be used to pass in rules when calling a factory. This
provides a convenient way to set deeply nested values. Rules are provided as a map from selector to value.

```clj
(article {:rules {[:article/submitter :user/handle] "editosaurus"}})
;;=>
{:article/title "7 Tip-top Things To Try",
 :article/submitter
 {:user/name "Elisa Ferry",
  :user/handle "editosaurus",
  :user/email "ty41@ankundingankund.net"},
 :article/author
 {:user/name "Amalia Sporer",
  :user/handle "bobby",
  :user/email "aimee24@brakusbrakusand.org",
  :user/roles #{"author"}}}
```

### Unification

As a rule value you can pass the special value `(f/unify)`. The first time such
a rule matches, it will generate factory data as usual, any subsequent matches
will then reuse that data. This has some really useful applications. Say we have
a data model where many types of entities have a link back to an `organization`.

```clj
(f/defactory user
  {:username "jonny"
   :org organization})

(f/defactory department
  {:name "sales"
   :org organization
   :head user})

(f/defactory meeting-room
  {:org organization
   :room-number "123"
   :department department})

(f/defactory booking
  {:start-time #(java.util.Date.)
   :booked-by user
   :room meeting-room})
```

When asking for a `(booking)` we'll get no fewer than three different
organizations. That doesn't really make sense, bookings are only made within a
single organization. We can unify these as follows:

```clj
(booking {:rules {organization (f/unify)}})
```

You can pass a value to `(f/unify)` to unify across multiple rules:

```clj
(booking {:rules {:org-id (f/unify :org) :organization-id (f/unify :org)}})
```

All rules that have the same unify value will end up linking to the same value.

### Hooks

Factories can contain an `after-build` hook, this is function which gets called
after we've constructed a value for that factory. It gets passed a "context"
map, which contains among other things the `:facai.result/value`. A common use
case is to update this value. You can use the `f/update-result` helper for that,
which is a shorthand for `(update ctx :facai.result/value ...)`

```clj
(f/defactory product
  {:sku "123"
   :price 12.99})

(f/defactory product-line-item
  {:product product
   :quantity 1}

  :after-build
  (fn [ctx]
    (f/update-result
     ctx
     (fn [{:as res :keys [product quantity]}]
       (assoc res :total (* (:price product) quantity))))))

(f/build-val product-line-item);; => {:product {:sku "123", :price 12.99}, :quantity 1, :total 12.99}
(f/build-val product-line-item {:with {:quantity 2}});; => {:product {:sku "123", :price 12.99}, :quantity 2, :total 25.98}
(f/build-val product-line-item {:rules {:price 69 :quantity 2}});; => {:product {:sku "123", :price 69}, :quantity 2, :total 138}
```

Notice how the result always has the right total price.

## Database Integration

One of the big selling points of Facai is that it makes it trivial to create
test data and insert it into the database in one go. This can dramatically clean
up test setup. Currently we ship initial support for next-jdbc, and for Datomic
peer. These integrations will indubitably have to be improved, but they provide
a good starting off point. In practice people will likely want to tailor these
to their specific persistence conventions, so they can serve as inspiration.

### Next.jdbc

In this case you first use `create-fn`, passing in a datasource, and any
specific configuration regarding the conventions of how you map Clojure data to
tables.

Factories themselves can take some options as well, like setting an explici
`facai.next-jdbc/table-name`, if the name can't be inferred from the factory
name.

```clojure
(in-ns 'my-app.factories)
(require '[clojure.string :as str]
         '[lambdaisland.facai.next-jdbc :as fnj]
         '[next.jdbc :as nj]
         '[next.jdbc.quoted :as quoted])

;; Some setup to quickly get an in-memory database. Supposedly you'll have this
;; kind of stuff in your app somewhere already.

(defn create-table-sql [{:keys [table columns]}]
  (str "CREATE TABLE " (quoted/ansi table)
       " (" (str/join
             ","
             (for [[k v] columns] (str (quoted/ansi k) " " v)))
       ")"))

(def table-defs
  [{:table "users"
    :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
              "name" "VARCHAR(255)"}}
   {:table "posts"
    :columns {"id" "INT AUTO_INCREMENT PRIMARY KEY"
              "title" "VARCHAR(255)"
              "author_id" "INT"
              "created_at" "TIMESTAMP DEFAULT CURRENT_TIMESTAMP"}}])

(f/defactory user
  {:name #(fake [:name :name])})

(f/defactory post
  {:title #(fake [:dc-comics :title])
   :author user})

(f/defactory article
  {:title "Article"
   :author user}
  :facai.jdbc/table "posts")


(def ds (nj/get-datasource (str "jdbc:h2:/tmp/h2-db-" (rand-int 1e8))))
(run! #(nj/execute! ds [(create-table-sql %)]) table-defs)
(def create!
  (fnj/create-fn
   {:facai.next-jdbc/ds ds
    :facai.next-jdbc/fk-col-fn #(keyword (str (name %) "-id"))}))

(:facai.result/value (create! post))
;; => {:title "Gotham Central",
;;     :author-id 2,
;;     :id 2,
;;     :created-at #inst "2022-03-28T10:44:05.447624000-00:00"}

;; So what happened here? Faca generated a user, persisted it to the database,
;; the database assigned a unique id, and we then used that for the association
;; when generating the article. The datbase also generated the value for
;; created-at.

;; If you look at the full result of `create!` you see that it also includes these
;; linked entities.

(create! post)
;; => {:facai.result/value
;;     {:title "Crisis On Infinite Earths",
;;      :author-id 3,
;;      :id 3,
;;      :created-at #inst "2022-03-28T10:45:03.367515000-00:00"},
;;     :facai.result/linked
;;     {[my-app.factories/post :author] {:name "Richard Quigley", :id 3}},
;;     :facai.factory/id my-app.factories/post}

;; These are in a map with the keys being the "path". This includes map keys
;; whenever we recurse into a map, and it includes the names of factories. When
;; recursing into a sequential collection the indexes are added to the path.

(create! (repeat 3 post))
;; => #:facai.result{:value (6 5 4),
;;                   :linked
;;                   {[0 my-app.factories/post :author]
;;                    {:name "Ettie Weissnat DDS", :id 4},
;;                    [0]
;;                    {:title "Swamp Thing: The Anatomy Lesson",
;;                     :author-id 4,
;;                     :id 4,
;;                     :created-at #inst "2022-03-28T10:46:39.842557000-00:00"},
;;                    [1 my-app.factories/post :author]
;;                    {:name "Kermit Hartmann", :id 5},
;;                    [1]
;;                    {:title "Detective Comics",
;;                     :author-id 5,
;;                     :id 5,
;;                     :created-at #inst "2022-03-28T10:46:39.898035000-00:00"},
;;                    [2 my-app.factories/post :author]
;;                    {:name "Rolanda Torphy", :id 6},
;;                    [2]
;;                    {:title "All Star Superman",
;;                     :author-id 6,
;;                     :id 6,
;;                     :created-at #inst "2022-03-28T10:46:39.947156000-00:00"}}}

;; You can use the `sel` and `sel1` helpers to fetch these objects. The matching
;; works similar to CSS selectors.

;; Say we want to create three posts in the database, and then care about the
;; authors of the posts.

(f/sel (create! (repeat 3 post)) [:author])
;; => ({:name "The Hon. Bradley Leuschke", :id 7}
;;     {:name "Violet Koelpin", :id 8}
;;     {:name "Annita Hauck", :id 9})

(let [res (create! (repeat 3 post))
      first-author (f/sel1 res [0 :author])]
  first-author)
;; => {:name "Fr. Denisha Wyman", :id 28}
```

### Datomic

The datomic integration has a `create!` function which takes a datomic
connection and a factory/template. It will handle tempids.

```clojure
(require '[datomic.api :as d]
         '[lambdaisland.facai :as f]
         '[lambdaisland.facai.datomic :as fd])

(d/create-database "datomic:mem://foo")
(def conn (d/connect "datomic:mem://foo"))

(f/defactory line-item
  {:line-item/description "Widgets"
   :line-item/quantity 5
   :line-item/price 1.0})

(f/defactory cart
  {:cart/created-at #(java.util.Date.)
   :cart/line-items [line-item line-item]})

(def schema
  [{:db/ident       :line-item/description,
    :db/valueType   :db.type/string,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :line-item/quantity,
    :db/valueType   :db.type/long,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :line-item/price,
    :db/valueType   :db.type/double,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :cart/created-at,
    :db/valueType   :db.type/instant,
    :db/cardinality :db.cardinality/one}
   {:db/ident       :cart/line-items,
    :db/valueType   :db.type/ref,
    :db/cardinality :db.cardinality/many}])

(let [url (doto (str "datomic:mem://db" (rand-int 1e8))
            d/create-database)
      conn (d/connect url)]
  @(d/transact conn schema)
  (fd/create! conn cart))

;; => {:facai.result/value
;;     {:cart/created-at #inst "2022-03-28T11:02:19.667-00:00",
;;      :cart/line-items [17592186045419 17592186045420],
;;      :db/id 17592186045418},
;;     :facai.result/linked
;;     {[user/cart :cart/line-items 0]
;;      {:line-item/description "Widgets",
;;       :line-item/quantity 5,
;;       :line-item/price 1.0,
;;       :db/id 17592186045419},
;;      [user/cart :cart/line-items 1]
;;      {:line-item/description "Widgets",
;;       :line-item/quantity 5,
;;       :line-item/price 1.0,
;;       :db/id 17592186045420}},
;;     :facai.factory/id user/cart,
;;     :db-after datomic.db.Db@9e510866}

```


## Introduction

Most application code deals with some form of "entities", often stored in a
database, be it relational or something else. In Clojure code these are
typically represented and passed around as maps.

When calling functions from the REPL, or writing tests, or rendering demo UI
(e.g. with devcards), you will need data to feed into these things.

```clj
(deftest user-login-test
  (let [user {:user/handle "plexus", :user/full-name "Arne Brasseur", :user/pwd-hsh "ff2f92842fa0428"}
        user (db/create! user)]
    (is (user/auth-ok? user "sekrit"))))
```

This is fine, but it has some drawbacks. This test does not care about
`:user/handle` or `:user/full-name`, but there's a good chance we have to
include them to have a valid user representation, so it adds noise and reduces
clarity. And if we add an extra mandatory attribute to a user then we might have
to update a lot of tests.

This is also a trivial example, maybe to have a valid `user` you first have to
create a `profile`. Now every test that touches users needs to also create a
profile and link them, even if the test does not actually care about profiles.

## Same data, different viewpoint

To continue our example of users and profiles. When inserting these into a
datalog database they might look like this:

```clj
[{:db/id "profile"
  :profile/avatar "avatar.jpg"}
 {:db/id "user"
  :user/handle "plexus"
  :user/profile "profile"}]
```

When getting them back from the database they may look like this:

```clj
[{:db/id 16904842
  :profile/avatar "avatar.jpg"}
 {:db/id 16904913
  :user/handle "plexus"
  :user/profile 16904842}]
```

But when requesting them through the `entity` API (as provided e.g. by Datomic)
they'll look like this:

```clj
{:db/id 16904913
 :user/handle "plexus"
 :user/profile {:db/id 16904842
                :profile/avatar "avatar.jpg"}}
```

When using relational databases you may add an `_id` to a foreign key column,
and so forth. It's all the same data, but depending on the part of the system
there may be different convenients around nesting vs flat, normalized or
denormalized, representation of associations/links, etc.

The idea with factories is that you define the factories for your data once, and
then set up helpers to get data in the shape you need, and to deal with any
persistence concerns. This way you get maximal reuse between backend, frontend,
Clerk notebooks, demo UI, and so forth.

## Getting started



## Design goals

- simple, convenient, example-based factories a la factory-bot
- for use in tests, devcards, clerk...
- handle groups of related objects, associations, etc.
- easy to pick up, more intuitive than Specmonstah
- can generate data suitable for relational (flat, explicit foreign keys), or datalog (tx-style or entity style), or whatever you need. FK handling is pluggable.
- optionally integrate with spec and/or malli, for validation and/or generation
- provide a smooth on-ramp towards generative testing
- comes with ready-made database integration for JDBC, Datomic, Crux, etc.
- Handle inserting and getting default values from the db (e.g. `auto_increment` keys, `created_by`, etc)
- for datalog dbs: deal with tempids
- separate factory definitions from db/app particulars -> encourage reusability in multiple contexts in the same app
- manipulate factories through code
- inheritance, composition, specialization, nestable
- arbitrarily shaped data. Maps, collections, whatever.
- Clojure + ClojureScript

## Potential design goals

- Allow for factory serializability, store in db, travel over the wire
- Faker integration

Note that this is not a replacement but rather a complement to test.check-style generators.

<!-- installation -->
<!-- /installation -->

<!-- opencollective -->
## Lambda Island Open Source

<img align="left" src="https://github.com/lambdaisland/open-source/raw/master/artwork/lighthouse_readme.png">

&nbsp;

facai is part of a growing collection of quality Clojure libraries created and maintained
by the fine folks at [Gaiwan](https://gaiwan.co).

Pay it forward by [becoming a backer on our Open Collective](http://opencollective.com/lambda-island),
so that we may continue to enjoy a thriving Clojure ecosystem.

You can find an overview of our projects at [lambdaisland/open-source](https://github.com/lambdaisland/open-source).

&nbsp;

&nbsp;
<!-- /opencollective -->

<!-- contributing -->
## Contributing

Everyone has a right to submit patches to facai, and thus become a contributor.

Contributors MUST

- adhere to the [LambdaIsland Clojure Style Guide](https://nextjournal.com/lambdaisland/clojure-style-guide)
- write patches that solve a problem. Start by stating the problem, then supply a minimal solution. `*`
- agree to license their contributions as MPL 2.0.
- not break the contract with downstream consumers. `**`
- not break the tests.

Contributors SHOULD

- update the CHANGELOG and README.
- add tests for new functionality.

If you submit a pull request that adheres to these rules, then it will almost
certainly be merged immediately. However some things may require more
consideration. If you add new dependencies, or significantly increase the API
surface, then we need to decide if these changes are in line with the project's
goals. In this case you can start by [writing a pitch](https://nextjournal.com/lambdaisland/pitch-template),
and collecting feedback on it.

`*` This goes for features too, a feature needs to solve a problem. State the problem it solves, then supply a minimal solution.

`**` As long as this project has not seen a public release (i.e. is not on Clojars)
we may still consider making breaking changes, if there is consensus that the
changes are justified.
<!-- /contributing -->

<!-- license -->
## License

Copyright &copy; 2022 Arne Brasseur and Contributors

Licensed under the term of the Mozilla Public License 2.0, see LICENSE.
<!-- /license -->
