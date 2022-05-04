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