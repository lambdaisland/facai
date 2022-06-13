(ns repl-sessions.poke-xtdb
  (:require [xtdb.api :as xt]))

(def node (xt/start-node {}))

(def tx
  (xt/submit-tx node [[::xt/put
                       {:xt/id :dbpedia.resource/Pablo-Picasso
                        :first-name :Pablo}
                       ]] ))

(xt/await-tx node tx)

(xt/entity
 (xt/db node)
 :dbpedia.resource/Pablo-Picasso)
