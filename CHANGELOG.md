# Unreleased

## Fixed

- Fix two-arity version of build-all
- When unifying, don't recurse down the stored lvar value when reusing it

## Changed

- Remove three-arity version of build-all

# 0.7.59-alpha (2022-09-02 / 161a3b7)

## Added

- Introduced `with` and `with-opts` as a public API for creating deferreds. This
  may not stick, we may decide to always return a deferred when calling a
  factory directly instead.
- Add a new key `:facai.build/factory` to the context when calling
  {before,after}-create-factory hooks, so that hooks don't have to deal with
  unppwrapping deferreds

## Fixed

- Fix selector matching on a single segment wildcard selector: `[:*]`

## Changed

# 0.6.52-alpha (2022-07-12 / 509ba20)

## Changed

- Rework persistence and hooks

## Added

- Add XTDB support

# 0.5.41-alpha (2022-06-04 / c95169c)

## Added

- Support for rules
- Support for unification

# 0.4.32-alpha (2022-05-04 / 33d976f)

## Added

- Added `after-build` hooks on the factory and trait level
- Added `lambdaisland.factory/update-result` for use in hooks

## Changed

- Breaking: traits now need an extra wrapping map, similar to the options map, with `:with` and optionally `:after-build`

# 0.3.27-alpha (2022-05-04 / 8ab563a)

## Changed

- Make `lambdaisland.facai.helpers` ClojureScript compatible (convert to cljc) 
- Allow `helpers/numbered` to accept strings

# 0.2.23-alpha (2022-04-12 / 97f1b62)

## Changed

- Convert to cljc (clojure+clojurescript support)
- Drop "rules" support, will likely come back in some other form in the future
- Version bumps

# 0.1.18 (2022-03-28 / d5b5461)

## Added

- Initial implementation
