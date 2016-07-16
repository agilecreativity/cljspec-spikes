(ns cljspec-spikes.core
  (:require [clojure.spec :as s]
            [clojure.spec.test :as test]
            [clojure.string :as str]))

(def fish-numbers {0 "Zero"
                   1 "One"
                   2 "Two"})

(s/def ::fish-number (set (keys fish-numbers)))

(s/valid? ::fish-number 1) ;; true

(s/valid? ::fish-number 5) ;; false

(s/explain ::fish-number 5) ;; nil

(s/def ::color #{"Red" "Blue" "Dun"})

;; Specifying the sequences of the values
(s/def ::first-line (s/cat :n1 ::fish-number
                           :n2 ::fish-number
                           :c1 ::color
                           :c2 ::color))

(s/explain ::first-line [1 2 "Red" "Black"])

(defn one-bigger? [{:keys [n1 n2]}]
  (= n2 (inc n1)))

(s/def ::first-line (s/and (s/cat :n1 ::fish-number
                                  :n2 ::fish-number
                                  :c1 ::color
                                  :c2 ::color)
                           one-bigger?
                           #(not= (:c1 %) (:c2 %))))

(s/valid? ::first-line [1 2 "Red" "Blue"]) ;; true

(s/explain ::first-line [1 2 "Red" "Blue"]) ;; => Success! in REPL

(s/conform ::first-line [1 2 "Red" "Blue"]) ;; {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}

(s/valid? ::first-line [2 1 "Red" "Blue"]) ;; false
(s/explain ::first-line [2 1 "Red" "Blue"])
;; val: {:n1 2, :n2 1, :c1 "Red", :c2 "Blue"} failes spec: :cljspec-spikes.core/first-line ..)

;; Generating test data - and poetry with specification
(s/exercise ::first-line 5)
;; ([(0 1 "Dun" "Red") {:n1 0, :n2 1, :c1 "Dun", :c2 "Red"}]
;; [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}]
;; [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}]
;; [(0 1 "Red" "Blue") {:n1 0, :n2 1, :c1 "Red", :c2 "Blue"}]
;; [(1 2 "Dun" "Blue") {:n1 1, :n2 2, :c1 "Dun", :c2 "Blue"}])

(defn fish-number-rhymes-with-color? [{n :n2 c :c2}]
  (or
   (= [n c] [2 "Blue"])
   (= [n c] [1 "Dun"])))

(s/def ::first-line (s/and
                     (s/cat :n1 ::fish-number
                            :n2 ::fish-number
                            :c1 ::color
                            :c2 ::color
                            )
                     one-bigger?
                     #(not= (:c1 %) (:c2 %))
                     fish-number-rhymes-with-color?))

(s/valid? ::first-line [1 2 "Red" "Blue"]) ;; true

(s/explain ::first-line [1 2 "Red" "Dun"]) ;; val: {:n1 ...} fails predicate: fish-number-rhymes-with-color?

(s/exercise ::first-line) ;; ([(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}] [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}] [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}] [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}] [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}] [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}] [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}] [(0 1 "Red" "Dun") {:n1 0, :n2 1, :c1 "Red", :c2 "Dun"}] [(1 2 "Red" "Blue") {:n1 1, :n2 2, :c1 "Red", :c2 "Blue"}] [(0 1 "Blue" "Dun") {:n1 0, :n2 1, :c1 "Blue", :c2 "Dun"}])

;; Using spec with functions

(defn fish-line [n1 n2 c1 c2]
  (clojure.string/join " "
                       (map #(str % " fish.")
                            [(get fish-numbers n1)
                             (get fish-numbers n2)
                             c1
                             c2])))

(s/fdef fish-line
        :args ::first-line
        :ret string?)

;; Finally
(fish-line 2 1 "Red" "Blue") ;; "Two fish. One fish. Red fish. Blue fish."

;; From Clojure spec screencast by Alex Millers
(defn my-index-of
  "Returns the index at which search appears in source"
  [source search]
  (str/index-of source search))

(my-index-of "foobar" "b") ;; 3

(apply my-index-of ["foobar" "b"]) ;; 3

(s/def ::index-of-args (s/cat :source string?
                              :search string?))

;; validation
(s/valid? ::index-of-args ["foo" "f"]) ;; true
(s/valid? ::index-of-args ["foo" 3]) ;; false

;; Conformance & destructuring
(s/conform ::index-of-args ["foo" "f"])
;; {:source "foo", :search "f"}

(s/conform ::index-of-args ["foo" 3])
;; :clojure.spec/invalid

(s/unform ::index-of-args ["foo" 3]) ;; ()

;; precise errors
(s/explain ::index-of-args ["foo" 3])

(s/explain-str ::index-of-args ["foo" 3])
;; "In: [1] val: 3 fails spec: :cljspec-spikes.core/index-of-args at: [:search] predicate: string?\n"

(s/explain-data ::index-of-args ["foo" 3])
;; #:clojure.spec{:problems [{:path [:search],
;;                            :pred string?,
;;                            :val 3,
;;                            :via [:cljspec-spikes.core/index-of-args],
;;                            :in [1]}]}

;; Composition
(s/explain-str (s/every ::index-of-args) [["good" "a"]
                                          ["ok" "b"]
                                          ["bad" 42]])
;; "In: [2 1] val: 42 fails spec: :cljspec-spikes.core/index-of-args at: [:search] predicate: string?\n"

;; example data generation
(s/exercise ::index-of-args)
;; ([("" "") {:source "", :search ""}]
;;  [("7" "2") {:source "7", :search "2"}]
;;  [("d" "Qc") {:source "d", :search "Qc"}]
;;  [("Mr" "V") {:source "Mr", :search "V"}]
;;  [("3Coc" "") {:source "3Coc", :search ""}]
;;  [("4MM" "5ah") {:source "4MM", :search "5ah"}]
;;  [("" "T1q0") {:source "", :search "T1q0"}]
;;  [("Qh32w2" "54bJ") {:source "Qh32w2", :search "54bJ"}]
;;  [("7wzM" "559") {:source "7wzM", :search "559"}]
;;  [("f" "oz5t3360") {:source "f", :search "oz5t3360"}])

;; assertion
(s/check-asserts true) ;; true

(s/assert ::index-of-args ["foo" "f"])
;; ["foo" "f"]

(s/assert ::index-of-args ["foo" 42])  ;;
;; Spec assertion failed In: [1] val: 42 failes at: [:search] predicate: string?
;; :clojure.spec/failure :assertion-failed

;; specing a function
(s/fdef my-index-of
        :args (s/cat :source string?
                     :search string?)
        :ret nat-int?
        :fn #(<= (:ret %) (-> % :args :source count)))

;; Documentation (try this in REPL)
(doc cljspec-spikes.core/my-idex-of)

;; generative testing
(->> (test/check `my-index-of) test/summarize-results) ;; {:total 1, :check-failed 1}
;; =>
;;  {:spec
;; (fspec
;;  :args
;;  (cat :source string? :search string?)
;;  :ret
;;  nat-int?
;;  :fn
;;  (<= (:ret %) (-> % :args :source count))),
;; :sym cljspec-spikes.core/my-index-of,
;; :failure
;; {:clojure.spec/problems
;;  [{:path [:ret], :pred nat-int?, :val nil, :via [], :in []}],
;;  :clojure.spec.test/args ("" "0"),
;;  :clojure.spec.test/val nil,
;;  :clojure.spec/failure :check-failed}}
;; user>

;; Instrumentation
(test/instrument `my-index-of)

(my-index-of "foo" 42)
;; =>
;; 1. Unhandled clojure.lang.ExceptionInfo
;; Call to #'cljspec-spikes.core/my-index-of did not conform to spec: In: [1]
;; val: 42 fails at: [:args :search] predicate: string? :clojure.spec/args
;; ("foo" 42) :clojure.spec/failure :instrument :clojure.spec.test/caller {:file
;;                                                                         "form-init6112948548877571226.clj", :line 194, :var-scope
;;                                                                         cljspec-spikes.core/eval21878}

;; {:clojure.spec/problems [{:path [:args :search], :pred string?, :val 42, :via [], :in [1]}], :clojure.spec/args ("foo" 42), :clojure.spec/failure :instrument, :clojure.spec.test/caller {:file "form-init6112948548877571226.clj", :line 194, :var-scope cljspec-spikes.core/eval21878}}
