(ns mzero.qp.xprs
  "Module to solve convex quadratic programs:
  - `solve-qp` solves 0.5 xT.Q.x + cT.x;
  - `solve-lcls` solves the linearly constrained least squares problem
  (which is a subset of the above)

  The solving is done by FICO Xpress software:
  https://www.fico.com/fico-xpress-optimization/docs/latest/solver/optimizer/HTML/GUID-3BEAAE64-B07F-302C-B880-A11C2C4AF4F6.html"
  (:import (com.dashoptimization XPRSprob XPRS))
  (:require [clojure.spec.alpha :as s]))

(defn- transpose [m]
  (let [get-nth-col (fn [col-idx] (vec (map #(nth % col-idx) m)))]
    (vec (map get-nth-col (range (count (first m)))))))

(defn- lower-upper-bounds
  "Lower and upper bounds for the solution search of the qp, non-limited"
  [ncols {:as bounds :keys [lower upper]}]
  [(double-array ncols (or lower XPRS/MINUSINFINITY))
   (double-array ncols (or upper XPRS/PLUSINFINITY))])

(defn- translate-objective
  "Translate quadratic objective function to xpress optimizer
  format.

  Matrix `Q` is translated into arrays objqcol1, objqcol2 and dquad by
  storing respectively the row index, column index and value of each
  nonzero element of the matrix (only for the upper right half, since
  it's symmetric).

  nquad is the 'number of coefficients', aka the length of these arrays

  `c` is directly translated as `objcoef`

  See https://www.fico.com/fico-xpress-optimization/docs/latest/solver/optimizer/HTML/javadoc/com/dashoptimization/XPRSprob.html?scroll=loadQP-java.lang.String-int-int-byte:A-double:A-double:A-double:A-int:A-int:A-int:A-double:A-double:A-double:A-int-int:A-int:A-double:A-"
  [Q c]
  (let [nonzero-triplets-for-col
        (fn [idx col] ;; in symmetric matrix with smaller & smaller cols,
          ;; get triplets of colind, rowind, value
          (keep-indexed #(when (not (zero? %2)) [idx (+ idx %1) %2]) col))
        [objqcol1 objqcol2 dquad]
        (->> (map-indexed nonzero-triplets-for-col Q)
             (map vec)
             (reduce into)
             transpose)]
    {:objqcol1 (int-array objqcol1)
     :objqcol2 (int-array objqcol2)
     :dquad (double-array dquad)
     :objcoef (double-array c)
     :nquad (count dquad)}))

(defn- translate-constraints
  "Translate constraints to xpress optimizer format. Matrix `A` is
  translated as follows:
  
  - rowind contains the row indices of nonzero values of the matrix,
  in ascending order for row and columns. I.e. first row indices for
  column 0 in ascending order, then row indices for column 1, etc.

  - rowcoef contains the matrix values corresponding to indices in rowind;

  - start contains all the 'start of column' indices, i.e. indices of
  rowind/rowcoef that are the first of a new column. The last element
  of the array contains the length of rowind/rowcoef by convention.

  `b` is directly translated as rhs. Since all constraints are
  inequalities, row-type is always full of 'L' bytes (L for lower
  than).

  See https://www.fico.com/fico-xpress-optimization/docs/latest/solver/optimizer/HTML/javadoc/com/dashoptimization/XPRSprob.html?scroll=loadQP-java.lang.String-int-int-byte:A-double:A-double:A-double:A-int:A-int:A-int:A-double:A-double:A-double:A-int-int:A-int:A-double:A-"
  [A b]
  (let [nonzero-indices-by-col
        (map (fn [col] (keep-indexed #(when (not (zero? %2)) %1) col)) A)
        next-col-start-index
        (fn [partial-start next-col]
          (conj partial-start (+ (peek partial-start) (count next-col))))]
    {:start (int-array (reduce next-col-start-index [0] nonzero-indices-by-col))
     :rowind (int-array (flatten nonzero-indices-by-col))
     :rowcoef (double-array (flatten (map (partial remove zero?) A)))
     :rhs (double-array b)
     :row-type (byte-array (count b) (byte \L))}))

(s/def ::vector (s/every number?))

(s/def ::matrix
  (-> (s/every ::vector)
      (s/and (fn [m]
               (comment "All columns have same size")
               (apply = (map count m))))))

(s/def ::symmetric-matrix
  (-> (s/every ::vector)
      (s/and (fn [m]
               (comment "Columns decrease in size from N to 1")
               (apply = (map-indexed #(+ (count %2) %1) m))))))

(defn- solve-qp-validated-args [Q c A b bounds]
  (let [pb (XPRSprob. nil)
        nrows (count b) ncols (count A)
        [lbound ubound] (lower-upper-bounds ncols bounds)
        {:keys [start rowind rowcoef rhs row-type]} (translate-constraints A b)
        {:keys [objcoef nquad objqcol1 objqcol2 dquad]} (translate-objective Q c)]
    (.loadQP pb "basic"
             ncols nrows row-type rhs nil objcoef start nil rowind
             rowcoef lbound ubound nquad objqcol1 objqcol2 dquad)
    (.lpOptimize pb)
    {:prob pb
     :x (seq (.-x (.getSol pb)))}))

(s/fdef solve-qp
  :args (-> (s/cat :Q ::symmetric-matrix
                   :c ::vector
                   :A ::matrix
                   :b ::vector
                   :bounds (s/? (s/map-of #{:lower :upper} double?)))
            (s/and (fn [{:keys [Q c A b]}]
                     (comment "Dimensions fit")
                     (and (= (count A) (count c) (count Q))
                          (= (count b) (count (first A))))))))

(defn solve-qp
  "Solve a quadratic program, minimizing 0.5*xT.`Q`.x +`c`T.x constrained
  by linear inequalities `A`x - `b` <= 0 (xT means x transposed),
  where `Q` is a positive semi-definite matrix, `c` is a vector whose
  dimension matches Q's number of rows (or cols since Q is square),
  `A` is the constraint matrix and `b` is a vector whose dimension
  matches A's number of cols.

  Optional `bounds` can be specified, e.g. {:lower 0.0 :upper 0.3}, or
  {:upper 1e10}. If a bound is not given it is set to its minimal
  value (for lower bound) or maximal value (for upper bound).

  NOTE: relative to matrix Q, pay attention to the 0.5 coefficient of
  the quadratic term above; if the quadratic term of the problem at
  hand is in the form xT.K.x, then `Q`=2*K and doubling the matrix
  coeff is needed such as done in https://www.fico.com/fico-xpress-optimization/docs/latest/solver/optimizer/HTML/XPRSloadqp.html

  Matrices are input as seq of columns (columns are seqs), vectors are
  input as seqs.

  Since Q should be symmetric, its columns sizes should be decreasing
  until reaching 1.

  Javadoc about xpress optimizer parameters : https://www.fico.com/fico-xpress-optimization/docs/latest/solver/optimizer/HTML/javadoc/com/dashoptimization/XPRSprob.html?scroll=loadQP-java.lang.String-int-int-byte:A-double:A-double:A-double:A-int:A-int:A-int:A-double:A-double:A-double:A-int-int:A-int:A-double:A-
 
  Example of loadQP call:
  https://www.fico.com/fico-xpress-optimization/docs/latest/getting_started/dhtml/chap17_sec_c17s4.html

  Return the XPRS problem instance and the solution, assuming it's
  unique (which seems to be always true except in edge cases when Q =
  0? does Q being not 0 guarantee it? probably)"
  ([Q c A b bounds]
   (let [args-specs (:args (s/get-spec `solve-qp))]
     (if (s/valid? args-specs [Q c A b bounds])
       (solve-qp-validated-args Q c A b bounds)
       (throw (IllegalArgumentException.
               (s/explain-str args-specs [Q c A b bounds]))))))
  ([Q c A b]
   (solve-qp Q c A b {})))

(defn- multiply-vecs [x1 x2]
  (reduce #(+ %1 (apply * %2)) 0.0 (map vector x1 x2)))

(defn- tmm
  "Multiplies the transpose of matrix `M` by `M` itself, returns the
  result as a symmetric matrix"
  [M]
  (let [remaining-cols (fn [index col] (drop index M))
        multiply-by-remaining-cols
        (fn [index col]
          (mapv #(multiply-vecs col %) (remaining-cols index col)))]
    (vec (map-indexed multiply-by-remaining-cols M))))

(defn- mv
  "Multiplies matrix A by vector x and scales the result by alpha"
  [alpha M x]
  (mapv #(* alpha (multiply-vecs x %)) (transpose M)))

(defn solve-lcls
  "Solve the linearly constrained least squares (LCLS) problem ||`A`x-`b`||² such
  that `C`x-`d`<=0.

  This is equivalent to minimizing 0.5*xT.AT.A.x - bT.A.x with the
  same constraints, so turning it to solve-qp params we have Q=AT.A and c=-AT.b"
  ([A b C d bounds]
   (let [Q (tmm A) c (mv -1.0 (transpose A) b)]
     (solve-qp Q c C d bounds)))
  ([A b C d]
   (solve-lcls A b C d {})))
