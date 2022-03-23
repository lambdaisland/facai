(ns lambdaisland.zao.macro-util)

(defn qualify-sym
  [env s]
  (if (:ns env)
    ;; cljs
    (symbol (name (-> env :ns :name)) (name s))

    ;; clj
    (symbol (str *ns*) (str s))))

(defmacro example [name val]
  `(def ~name {:id '~(qualify-sym &env name) :value ~val}))

(example xxx 123)

xxx
;;=>
{:value 123, :id my-ns/xxx}
