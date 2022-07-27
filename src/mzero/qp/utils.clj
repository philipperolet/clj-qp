(ns mzero.qp.utils
  (:require [clojure.tools.logging :as log]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [clojure.data.generators :as g]
            [clojure.string :as str]
            [clojure.tools.logging.impl :as li]))

(defn abs [x]
  (if (pos? x) x (- x)))

(defn almost=
  "Compares 2 numbers with a given `precision`, returns true if the
  numbers' difference is lower or equal than the precision

  Precision defaults to a ten thousandth of the first number's value."
  ([a b precision]
   (<= (abs (- a b)) precision))
  ([a b]
   (almost= a b (* (Math/abs a) 0.0001))))

(defn coll-almost=
  "almost= for collections"
  ([c1 c2]
   (and (= (count c1) (count c2))
        (every? true? (map almost= c1 c2))))
  ([c1 c2 precision]
   (and (= (count c1) (count c2))
        (every? true? (map #(almost= %1 %2 precision) c1 c2)))))

(defn map-map
  "Return a map with `f` applied to each of `m`'s keys"
  [f m]
  (reduce-kv (fn [acc k v] (assoc acc k (f v))) {} m))

(defn fn-name
  [fn-var]
  {:pre [(var? fn-var)]}
  (str (:name (meta fn-var))))

(defn with-logs
  "Return a function identical to `fn_`, that logs a message every time
  it is called, whose first part is fn_'s name & call count and second
  part is a custom message computed by `str-fn`"
  ([fn-var str-fn]
   {:pre [(var? fn-var)]}
   (let [call-count (atom 0)]
     (fn [& args]
       (log/info (format "%s : call # %d %s"
                         (fn-name fn-var)
                         @call-count
                         (apply str-fn args)))
       (swap! call-count inc)
       (apply fn-var args))))
  ([fn_]
   (with-logs fn_ (constantly ""))))

(defn with-mapped-gen
  "Like with-gen, but takes a fn to be fed to fmap, and applies it to a
  regular generator for the provided spec"
  [spec fn]
  (s/with-gen spec #(gen/fmap fn (s/gen spec))))

(defn ipmap
  "Similar to pmap, but interruptible--all threads will receive an
  interrupt signal if the main thread is interrupted. However,
  contrarily to pmap, evaluation is eager."
  [f & colls]
  (let [rets (apply map (fn [& vals_] (future (apply f vals_))) colls)]
    (try
      (vec (map deref rets))
      (catch InterruptedException _
        (doall (map future-cancel rets))
        (throw (InterruptedException.))))))

(defmacro timed
  "Returns a vector with 2 values:
  -  time in miliseconds to run the expression, as a
  float--that is, taking into account micro/nanoseconds, subject to
  the underlying platform's precision;
  - expression return value."
  [expr]
  `(let [start-time# (System/nanoTime)
         result# ~expr]
     [(/ (- (System/nanoTime) start-time#) 1000000.0) result#]))
