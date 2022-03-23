# zao

<!-- badges -->
[![cljdoc badge](https://cljdoc.org/badge/com.lambdaisland/zao)](https://cljdoc.org/d/com.lambdaisland/zao) [![Clojars Project](https://img.shields.io/clojars/v/com.lambdaisland/zao.svg)](https://clojars.org/com.lambdaisland/zao)
<!-- /badges -->

The ultimate factory library

Or it will be... this is still heavily work in progress. This library is baby.
Stay tuned.

- test.check generators
- specmonstah -> not intuitive at all
- fakers

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
