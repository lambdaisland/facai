# zao

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/zao)](https://cljdoc.org/d/com.lambdaisland/zao) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/zao.svg)](https://clojars.org/com.lambdaisland/zao)
<!-- /badges -->

The ultimate factory library

Or it will be... this is still heavily work in progress. This library is baby.
Stay tuned.

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

zao is part of a growing collection of quality Clojure libraries created and maintained
by the fine folks at [Gaiwan](https://gaiwan.co).

Pay it forward by [becoming a backer on our Open Collective](http://opencollective.com/lambda-island),
so that we may continue to enjoy a thriving Clojure ecosystem.

You can find an overview of our projects at [lambdaisland/open-source](https://github.com/lambdaisland/open-source).

&nbsp;

&nbsp;
<!-- /opencollective -->

<!-- contributing -->
## Contributing

Everyone has a right to submit patches to zao, and thus become a contributor.

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
