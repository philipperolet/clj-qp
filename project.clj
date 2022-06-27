(defproject clj-qp "0.1"
  :description "Lib to solve quadratic programs in clojure, wrapping FICO xpress solver"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :min-lein-version "2.7.1"

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/test.check "1.1.0"]
                 [org.clojure/data.generators "1.0.0"]
                 [org.clojure/tools.logging "1.1.0"]
		 [org.clojure/tools.namespace "1.1.0"]
                 [trg-libs "0.3.4"]
                 [fico/xprs "6.0.0"]]

  :jvm-opts  ["-Dclojure.tools.logging.factory=clojure.tools.logging.impl/jul-factory"]
  :source-paths ["src"]
  :java-source-paths ["test/xpress"]
  :main mzero.qp.xprs
  :test-selectors {:default (fn [m & _] (not (or (:deprecated m) (:skip m))))}
  :repl-options {:init (do (require '[clojure.tools.namespace.repl :refer [refresh]])
  		       	   (set! *print-length* 10) (set! *print-level* 5))}
  :profiles {:repl {:plugins [[cider/cider-nrepl "0.25.2"]]}})