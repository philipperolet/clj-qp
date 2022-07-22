(ns mzero.qp.xprs-speed-test
  (:require [mzero.qp.xprs :as sut]
            [clojure.test :refer [is]]
            [mzero.utils.testing :refer [deftest]]))

(deftest solve-qp-time-test
  (testing "For PCNS to work, we need the QP solver to be fast enough"
    (let [msize 1000
          random-matrix (nn/dge (mzi/random-weights msize msize 0))
          Q (seq (nc/mmt random-matrix))
          c (first (mzi/random-weights msize 1 1))
          A (repeat msize [])
          b []
          bounds {:lower -100.0 :upper 100.0}
          [time res] (u/timed (mzqp/solve-qp Q c A b bounds))]
      (println (:x res))
      (println time)
      (is (< time 2000)))))
