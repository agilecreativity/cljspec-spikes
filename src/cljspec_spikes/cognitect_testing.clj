;; From 2nd screencast by Cognitect:
;; https://youtu.be/W6crrbF7s2s?list=PLZdCLR02grLrju9ntDh3RGPpWSWBvjwXg
(ns cljspec-spikes.cognitect-testing
  (:require [clojure.spec.test :as test]
            [clojure.stacktrace :as stt]
            [clojure.string :as str]
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

;; Case where we have non-conformance
(s/conform (s/? nat-int?) [:a])  ;; :clojure.spec/invalid
(s/explain-str (s/? nat-int?) [:a])
;; => "In: [0] val: :a fails predicate: nat-int?\n"

(s/conform (s/? nat-int?) [1 2]) ;; :clojure.spec/invalid
(s/explain-data (s/? nat-int?) [1 2])
;; => #:clojure.spec{:problems [{:path [], :reason "Extra input", :pred (? nat-int?), :val (2), :via [], :in [1]}]}

;; Now we can add the validation to the optional third argument
(s/fdef my-index-of
        :args (s/cat :source string?
                     ;; Allow either string or char
                     :search (s/alt :string string?
                                    :char char?)
                     :from (s/? nat-int?))
        :ret nat-int?
        :fn #(<= (:ret %) (-> % :args :source count)))

(s/exercise-fn `my-index-of)
;; ([("" \S 0) nil] [("k" \_ 0) nil] [("" \Þ 0) nil] [("M2e" \g) nil] [("U3eN" \) nil] [("DTHQq" \< 2) nil] [("W" \O 3) nil] [("40YZ" \ö) nil] [("OF75A" "u7" 1) nil] [("imV8M4vl" "11JyA") nil])

;; Old way: traditional example based testing
;; Which is repetitive, errorprone, and ..
(assert (= 8 (my-index-of "testing with spec" "w")))

(= 8 (my-index-of "testing with spec" "w")) ;; true

;; New way: usint test/check generative testing
;; which generate the right inputs for the test automatically
(->> (test/check `my-index-of)
     test/summarize-results)
;; {:total 1, :check-failed 1}
;; The result in the REPL
;; =>
(comment
 ;; => result from the running of spec
 {:spec
  (fspec
   :args
   (cat
    :source
    string?
    :search
    (alt :string string? :char char?)
    :from
    (? nat-int?))
   :ret
   nat-int?
   :fn
   (<= (:ret %) (-> % :args :source count))),
  :sym cljspec-spikes.cognitect-testing/my-index-of,
  :failure
  {:clojure.spec/problems
   [{:path [:ret], :pred nat-int?, :val nil, :via [], :in []}],
   ;; Note: the one that fail the test
   :clojure.spec.test/args ("" \  0),
   :clojure.spec.test/val nil,
   :clojure.spec/failure :check-failed}})

;; Now let's learn about the nilable
(s/conform (s/nilable string?) "foo") ;;=> "foo"

(s/conform (s/nilable string?) nil)   ;;=> nil

;; What about incorrect type input?
(s/conform (s/nilable string?) 42)    ;;=> :clojure.spec/invalid

;; Even better explaination
(s/explain-str (s/nilable string?) 42)
;; =>
;; "val: 42 fails at: [:clojure.spec/nil] predicate: nil?\nval: 42 fails at: [:clojure.spec/pred] predicate: string?\n"

;; Now we are ready to use the nilable to make our function more robust
(s/fdef my-index-of
        :args (s/cat :source string?
                     ;; Allow either string or char
                     :search (s/alt :string string?
                                    :char char?)
                     :from (s/? nat-int?))
        ;; old definition
        ;:ret nat-int?
        ;; better definition
        :ret (s/nilable nat-int?)
        :fn #(<= (:ret %) (-> % :args :source count)))

;; Now let's run the generative test again
;; If we look at the stacktrace, the cause of error is tha `:fn` need to be
;; check against the nil value as well
(->> (test/check `my-index-of) test/summarize-results)
;; {:total 1, :check-threw 1}

;; We can improve this using the `or`
(s/fdef my-index-of
        :args (s/cat :source string?
                     ;; Allow either string or char
                     :search (s/alt :string string?
                                    :char char?)
                     :from (s/? nat-int?))
        ;; old definition
                                        ;:ret nat-int?
        ;; better definition
        :ret (s/nilable nat-int?)
        :fn (s/or
             :not-found #(nil? (:ret %))
             :found #(<= (:ret %) (-> % :args :source count))))

;; Now let's try again
(->> (test/check `my-index-of) test/summarize-results)
;; Finally
;; => {:total 1, :check-passed 1}

;; Calling this function
(defn which-came-first
  "Returns :chick or :egg, depending on which string appears
first in s, starting from position from"
  [s from]
  (let [c-idx (my-index-of s "chicken" :from from)
        e-idx (my-index-of s "egg" :from from)]
    (cond
      (< c-idx e-idx) :chicken
      (< e-idx c-idx) :egg)))

;; The result of the next expression (without the comment), result in ArityException ..
;; We can print the stacktrace using `clojure.stacktrace`
(try
  ;; Use of print-stack-trace

  (which-came-first "chicken or the egg" 0)
  (catch Exception e
    (str (.getMessage e))))
;; => "Wrong number of args (4) passed to: string/index-of"

;; Or if we wrap this one with
;; (stt/print-stack-trace (which-came-first "chicken or the egg" 0))
;;
;; (From the REPL) =>
;; 1. Unhandled clojure.lang.ArityException
;; Wrong number of args (4) passed to: string/index-of

;; AFn.java:  429  clojure.lang.AFn/throwArity
;; AFn.java:   44  clojure.lang.AFn/invoke
;; AFn.java:  165  clojure.lang.AFn/applyToHelper
;; AFn.javau:  144  clojure.lang.AFn/applyTo
;; core.clj:  661  clojure.core/apply
;; core.clj:  652  clojure.core/apply

;; Now this bring us to the use of `instrumentation`
;; to get the much better error message
(test/instrument `my-index-of)

;; If call without the comment
(comment (which-came-first "the chicken or the egg" 0))
;; From the REPL =>
;; Note that we have the line number that causing the problem
;; 1. Unhandled clojure.lang.ExceptionInfo
;; Call to #'cljspec-spikes.cognitect-testing/my-index-of did not conform to
;; spec: In: [2] val: :from fails at: [:args :from] predicate: nat-int?
;; :clojure.spec/args ("the chicken or the egg" "chicken" :from 0)
;; :clojure.spec/failure :instrument :clojure.spec.test/caller {:file
;;                                                              "cognitect_testing.clj", :line 202, :var-scope
;;                                                              cljspec-spikes.cognitect-testing/which-came-first}

;; {:clojure.spec/problems ({:path [:args :from], :pred nat-int?, :val :from, :via [], :in [2]}), :clojure.spec/args ("the chicken or the egg" "chicken" :from 0), :clojure.spec/failure :instrument, :clojure.spec.test/caller {:file "cognitect_testing.clj", :line 202, :var-scope cljspec-spikes.cognitect-testing/which-came-first}}

;; Now we can fix this with the test/instrumentation
(s/fdef which-came-first
        :args (s/cat :source string? :from nat-int?)
        ;; The return is the set of the two values
        :ret #{:chicken :egg})

(->> (test/check `which-came-first)
     test/summarize-results)
;; {:total 1, :instrument 1}

;; REPL output is very informative and concise
(comment {:spec
          (fspec
           :args
           (cat :source string? :from nat-int?)
           :ret
           #{:egg :chicken}
           :fn
           nil),
          :sym cljspec-spikes.cognitect-testing/which-came-first,
          :failure
          {:clojure.spec/problems
           ({:path [:args :from],
             :pred nat-int?,
             :val :from,
             :via [],
             :in [2]}),
           :clojure.spec/args ("" "chicken" :from 0),
           :clojure.spec/failure :instrument,
           :clojure.spec.test/caller
           {:file "cognitect_testing.clj",
            :line 202,
            :var-scope cljspec-spikes.cognitect-testing/which-came-first}}})

;; In summary
;; * Powerful specs
;;  - structural regexes
;;  - predicative via arbitrary Clojure
;; * Let spec Write Your Tests!
;;  - dev/REPL/test time tools
;;  - test/instrument, test/check
;; * Bring Your Own Workflow
;; See: http://clojure.org/about/spec
