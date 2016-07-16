(ns cljspec-spikes.core
  (:require [clojure.spec :as s]
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
