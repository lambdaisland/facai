(ns lambdaisland.facai.macro-util)

(defn qualify-sym
  [env s]
  (if (:ns env)
    ;; cljs
    (symbol (name (-> env :ns :name)) (name s))

    ;; clj
    (symbol (str *ns*) (str s))))
