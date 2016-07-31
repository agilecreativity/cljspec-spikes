;; From 2nd screencast by Cognitect:
;; https://youtu.be/W6crrbF7s2s?list=PLZdCLR02grLrju9ntDh3RGPpWSWBvjwXg
(ns cljspec-spikes.cognitect-testing
  (:require [clojure.string :as str]
            [clojure.test :as t])
  (:require [clojure.spec :as s]))

;; spec testings
;; test/check
;; - generate conforming inputs
;; - checks returns

;; test/instrument
;; - check conforming inputs

;; Support Idiomatic Clojure
;; - alternatives with s/alt
;; - options with s/?
;; - s/nilable
;; - :fn Branching with :o
;; - test/instrument

;; Let's write the function we want to test
(defn my-index-of
  "Returns the index at which search appears in source"
  [source search & opts]
  (apply str/index-of source search opts))

;; See: http://cider.readthedocs.io/en/latest/using_the_repl/

;; define spec using fspec
(s/fdef my-index-of
        :args (s/cat :source string? :search string?)
        :ret nat-int?
        ;; The return result
        :fn #(<= (:ret %) (-> % :args :source count)))

;; Let's exercise example
(s/exercise-fn `my-index-of)
;; =>
;; [("" "") 0]
;; [("" "Au") nil]
;; [("L8s" "4") nil]
;; [("qNZ" "") 0]
;; [("wSK2d" "UeT9") nil]
;; [("4pN" "dow7u1") nil]
;; [("sd2q10c" "") 0]
;; [("X306" "t") nil]
;; [("2b" "DhS") nil])

;; Let's allow the parameter be either string or char
(s/conform (s/alt :string string? :char char?) ["foo"])
;; => [:string "foo"]

(s/conform (s/alt :string string? :char char?) [\f])
;; [:char \f]

;; Example of non-matching case
(s/explain (s/alt :string string? :char char?) [42])
;; => nil

(s/explain-str (s/alt :string string? :char char?) [42])
;; => "In: [0] val: 42 fails at: [:string] predicate: string?\nIn: [0] val: 42 fails at: [:char] predicate: char?\n"

;; Now let's refine our example using alt
(s/fdef my-index-of
        :args (s/cat :source string?
                     ;; Allow either string or char
                     :search (s/alt :string string?
                                    :char char?))
        :ret nat-int?
        :fn #(<= (:ret %) (-> % :args :source count)))

;; Now let's try to exercise
(s/exercise-fn `my-index-of)
;; => ([("" \) nil] [("" "") 0] [("" "77") nil] [("f5" \æ) nil] [("" \2) nil] [("" \è) nil] [("ER" \ë) nil] [("YoM3" "PFgnovT") nil] [("69h" "") 0] [("7Yu" \Ð) nil])

;; Let support the optional third argument
;; Using quantification
(s/conform (s/? nat-int?) [])    ;; nil
(s/conform (s/? nat-int?) [1])   ;; 1
(s/conform (s/? nat-int?) [:a])  ;; :clojure.spec/invalid
(s/conform (s/? nat-int?) [1 2]) ;; :clojure.spec/invalid
