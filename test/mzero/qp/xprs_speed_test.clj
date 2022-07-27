(ns mzero.qp.xprs-speed-test
  (:require [mzero.qp.xprs :as qp]
            [clojure.test :refer [is testing deftest]]
            [mzero.qp.utils :as u]))

(defn scale-float [flt low hi] (+ low (* flt (- hi low))))

(defn scaled-weights
  "Random weights uniformly sampled according to a given `variance` objective"
  [variance m n seed]
  (let [bound (Math/sqrt (* 3 variance))]
    (->> (repeatedly rand)
         (map #(scale-float % (- bound) bound))
         (take (* m n))
         (partition m)
         (mapv vec))))

(defn default-weights
  "Random initialization such that the sum of weights of a neuron
  approximates a normal distribution N(0,1), see Irwin-Hall. The
  corresponding per-neuron variance of 1/m can be scaled by optional
  parameter `alpha`"
  ([alpha m n seed]
   (scaled-weights (/ alpha m) m n seed))
  ([m n seed]
   (default-weights 1.0 m n seed)))

(deftest solve-qp-time-test
  (testing "QP solver is fast enough"
    (let [msize 100
          random-matrix (default-weights msize msize 0)
          Q (#'qp/tmm random-matrix)
          c (first (default-weights msize 1 1))
          A (repeat msize [])
          b []
          bounds {:lower -100.0 :upper 100.0}
          [time res] (u/timed (qp/solve-qp Q c A b bounds))]
      (println (:x res))
      (println time)
      (is (< time 200)))))
