(ns lambdaisland.facai.helpers)

(defn days-ago
  "Get a timestamp that is `n` days in the past. Returns a ZonedDateTime on Clojure
  and a js/Date on ClojureScript."
  [n]
  #?(:clj
     (.minusDays
      (java.time.ZonedDateTime/now (java.time.ZoneId/of "UTC")) n)
     :cljs
     (doto (js/Date. (.getTime (js/Date.)))
       (.setDate (- (.getDate (js/Date.)) n)))))

(defn days-from-now
  "Get a timestamp that is `n` days in the future. Returns a ZonedDateTime on
  Clojure and a js/Date on ClojureScript."
  [n]
  #?(:clj
     (.plusDays
      (java.time.ZonedDateTime/now (java.time.ZoneId/of "UTC")) n)
     :cljs
     (doto (js/Date. (.getTime (js/Date.)))
       (.setDate (+ (.getDate (js/Date.)) n)))))

(defn numbered
  "Return a function which generates successive values of a sequence whenever it
  is called.
  - no args: returns incrementing numbers
  - function arg: returns the result as passing incrementing numbers to the function
  - string arg: generates `(str arg num)` values"
  ([]
   (let [cnt (volatile! 0)]
     #(vswap! cnt inc)))
  ([f-or-s]
   (if (string? f-or-s)
     (recur #(str f-or-s %))
     (let [nf (numbered)]
       #(f-or-s (nf))))))
