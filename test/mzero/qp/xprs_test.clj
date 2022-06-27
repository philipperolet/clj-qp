(ns mzero.qp.xprs-test
  "Qtest, Atest and Btest are inspired from the example in
  https://www.fico.com/fico-xpress-optimization/docs/latest/getting_started/dhtml/chap17_sec_c17s4.html"
  (:import (com.dashoptimization XPRSprob XPRS))
  (:require [mzero.qp.xprs :as sut]
            [clojure.test :refer [is deftest testing]]
            [mzero.utils.utils :as u]))

(deftest ^:skip test-runLoadLP
  (testing "Run pure java example running a linear program solving in xpress
  optimizer

  Used to check that the FICO xpress optimizer is installed
  correctly and works in the java (prerequisite for it working in
  clojure)")
  (xpress.LoadLP/main nil))

(deftest ^:skip test-basic-qp
  (testing "Run a basic QP without using the clj-qp lib; checks there
  are no issue in the clojure / java interface (e.g. libs not found in
  clojure although found in java...)"
    (let [nrows 1 ncols 2 nquad 3
          start (int-array [0 1 2])
          rowind (int-array [0 0])
          rowcoef (double-array [1 1])
          rhs (double-array [1.9])
          row-type (.getBytes "L")
          lbound (double-array [XPRS/MINUSINFINITY XPRS/MINUSINFINITY])
          ubound (double-array [XPRS/PLUSINFINITY XPRS/PLUSINFINITY])
          objcoef (double-array [-6 0])
          objqcol1 (int-array [0 0 1])
          objqcol2 (int-array [0 1 1])
          dquad (double-array [4 -2 4])
          pb (XPRSprob. nil)]
      (.loadQP pb "basic"
               ncols nrows row-type rhs nil objcoef start nil rowind
               rowcoef lbound ubound nquad objqcol1 objqcol2 dquad)
      (.lpOptimize pb)
      (println (seq (.-x (.getSol pb)))))))

(defn- mult-coeffs [matr mult]
  (map #(map (partial * mult) %) matr))

(def Qtest
  [[0.1	0	0	0	0	0	0	0	0	0]
   [19	-2	4	1	1	1	0.5	10	5]
   [28	1	2	1	1	0	-2	-1]
   [22	0	1	2	0	3	4]
   [4	-1.5	-2	-1	1	1]
   [3.5	2	0.5	1	1.5]
   [5	0.5	1	2.5]
   [1	0.5	0.5]
   [25	8]
   [16]])

(def Atest
  [[1 1 5] [1 1 17] [1 1 26] [1 1 12] [0 1 8]
   [0 1 9] [0 1 7] [0 1 6] [0 1 31] [0 1 21]])

(def btest [0.5 1 9])

(deftest translate-constraints-test
  (let [A [[1] [1]] b [1.9]
        output {:start [0 1 2]
                :rowind [0 0]
                :rowcoef [1.0 1.0]
                :rhs [1.9]
                :row-type [(byte \L)]}]
    (is (= (u/map-map seq (#'sut/translate-constraints A b)) output)))
  (let [A [[1 3] [-1 2]] b [0.5 -0.5]
        output {:start [0 2 4]
                :rowind [0 1 0 1]
                :rowcoef [1.0 3.0 -1.0 2.0]
                :rhs [0.5 -0.5]
                :row-type [(byte \L) (byte \L)]}]
    (is (= (u/map-map seq (#'sut/translate-constraints A b)) output)))
  (let [A Atest b btest
        output {:start [0 3 6 9 12 14 16 18 20 22 24]
                :rowind [0,1,2,0,1, 2,0,1, 2,0,1, 2,1,2,1,2,1,2,1,2,1, 2,1,2]
                :rowcoef
                (map double [1,1,5,1,1,17,1,1,26,1,1,12,1,8,1,9,1,7,1,6,1,31,1,21])
                :rhs [0.5 1.0 9.0]
                :row-type [(byte \L) (byte \L) (byte \L)]}]
    (is (= (u/map-map seq (#'sut/translate-constraints A b)) output))))

(deftest translate-ojbective-test
  (let [Q [[2 -1] [2]] c [-6.0 0.0]
        output
        {:objcoef c
         :nquad 3
         :objqcol1 [0 0 1]
         :objqcol2 [0 1 1]
         :dquad [2.0 -1.0 2.0]}
        res (#'sut/translate-objective Q c)]
    (is (= (u/map-map seq (dissoc res :nquad)) (dissoc output :nquad)))
    (is (= (:nquad res) (:nquad output))))
  (let [Q Qtest
        c (mapv double [0 0 0 0 0 0 0 0 0 0])
        output
        {:objcoef c
         :nquad 43
         :objqcol1 [0,
                    1,1,1,1,1,1,1,1,1,
                    2,2,2,2,2,  2,2,
                    3,  3,3,  3,3,
                    4,4,4,4,4,4,
                    5,5,5,5,5,
                    6,6,6,6,
                    7,7,7,
                    8,8,
                    9] 
         :objqcol2 [0,
                    1,2,3,4,5,6,7,8,9,
                    2,3,4,5,6,  8,9,
                    3,  5,6,  8,9,
                    4,5,6,7,8,9,
                    5,6,7,8,9,
                    6,7,8,9,
                    7,8,9,
                    8,9,
                    9]
         :dquad (mapv double
                        [0.1,
                         19,-2, 4,1,   1, 1,0.5, 10,  5,
                         28, 1,2,   1, 1,     -2, -1,
                         22,     1, 2,      3,  4,
                         4,-1.5,-2, -1,  1,  1,
                         3.5, 2,0.5,  1,1.5,
                         5,0.5,  1,2.5,
                         1,0.5,0.5,
                         25,  8,
                         16])}
        res (#'sut/translate-objective Q c)]
    (is (= (u/map-map seq (dissoc res :nquad)) (dissoc output :nquad)))
    (is (= (:nquad res) (:nquad output)))))

(deftest solve-qp-works-on-toy-examples
  ;; using eq 1/2*xT*Q*x => coeffs need to be doubled
  (let [Q1 (mult-coeffs [[2 -1] [2]] 2)
        c1 [-6 0]
        A1 [[1] [1]]
        b1 [1.9]]
    (is (u/coll-almost= [1.45 0.45] (:x (sut/solve-qp Q1 c1 A1 b1)))))
  (let [Q1 (mult-coeffs [[1 0] [1]] 2)
        c1 [0 0]
        A1 [[1] [1]]
        b1 [1.9]]
    (is (u/coll-almost= [0 0] (:x (sut/solve-qp Q1 c1 A1 b1)) 1e-7)))

  ;; negative multiplication of coeffs of A & b since the example uses G ineqs while solve-qp uses L ineqs
  ;; the middle ineq of A is removed since the added equality constraint replaces it
  
  (let [Id (map #(assoc (vec (repeat 10 0.0)) % 1.0) (range 10))
        Q (mult-coeffs Qtest 2) c (repeat (count Q) 0.0)
        A (->> (mult-coeffs Atest -1)
               (map (partial keep-indexed #(when (not= 1 %1) %2))))
        b (->> (map (partial * -1) btest)
               (keep-indexed #(when (not= 1 %1) %2)))
        {:keys [x prob]} (sut/solve-qp Q c A b {:lower 0.0 :upper 0.3})]
    ;; equality constraint for all coeffs to sum to 1
    (.addRow prob
             (int-array (range 10)) ;; colind
             (double-array 10 1) ;; colval
             \E
             1.0)
    (.lpOptimize prob)
    ;; last value is close to 0 (~ 6e-8), which is at the precision
    ;; limit of the optimizer so we don't check this one
    ;; precision is ok up to 1e-4 wrt the source values
    (is (u/coll-almost=
         (butlast [0.3 0.0715401 0.0738237 0.0546362 0.126561 0.0591283
                   0.00333491 0.299979 0.010997 6.97039E-8])
         (butlast (seq (.x (.getSol prob))))
         0.0001))))

