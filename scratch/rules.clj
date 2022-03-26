(ns scratch.rules)

(ns lambdaisland.facai.rules)

;; Parking this here for now, we'll ship rules in a v2, this needs more hammock
;; time.

(defn run-hooks [hooks hook val & args]
  (reduce (fn [val hookmap]
            (if-let [f (get hookmap hook)]
              (apply f val args)
              val))
          val
          hooks))

(defn- path-match? [path selector]
  (when (seq path)
    (loop [[p & ps] path
           [s & ss] (if (sequential? selector) selector [:> selector])
           i 0]
      (cond
        (and (nil? p) (nil? s))
        true

        (or (nil? p) (nil? s))
        false

        (= s p)
        (if (and (seq ss) (seq ps))
          (recur ps ss (inc i))
          (and (empty? ss) (empty? ps)))


        (= s :>)
        (if (= (first ss) p)
          (recur ps (next ss) (inc i))
          false)

        :else
        (recur ps (cons s ss) (inc i))))))

(defn match-rule [rules path]
  (some #(when (path-match? path (key %))
           (val %))
        rules))
