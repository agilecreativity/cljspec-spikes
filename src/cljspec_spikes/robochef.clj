;; From the talk by Arne Brasseur - Introducing clojure.spec
;; @plexus
(ns cljspec-spikes.robochef
  (:require [clojure.spec :as s]
            [clojure.test.check :as tc]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]))

;; Five "Regex" operators: *, +, ?, cat, alt
(s/conform (s/* keyword?) [])      ;; []
(s/conform (s/* keyword?) [:a])    ;; [:a]
(s/conform (s/* keyword?) [:a :b]) ;; [:a :b]

(s/conform (s/+ keyword?) [])      ;; :clojure.spec/invalid
(s/conform (s/+ keyword?) [:a])    ;; [:a]
(s/conform (s/+ keyword?) [:a :b]) ;; [:a :b]

(s/conform (s/? keyword?) [])      ;; nil
(s/conform (s/? keyword?) [:a])    ;; :a
(s/conform (s/? keyword?) [:a :b]) ;; :clojure.spec/invalid

;; cat: catenate sequence of items
(s/conform (s/cat :num number?
                  :key keyword?) [5 :b]) ;; {:num 5, :key :b}

;; alt: alternate
(s/conform (s/alt :num number?
                  :key keyword?) [5]) ;; [:num 5]

(s/conform (s/alt :num number?
                  :key keyword?) [:b]) ;; [:key :b]

(s/conform (s/alt :num number?
                  :key keyword?) [:b 5]) ;; :clojure.spec/invalid

;; When this fail we can see why
(s/explain-str (s/alt :num number?
                      :key keyword?) [:b 5])
;; "In: [1] val: (5) fails predicate: (alt :num number? :key keyword?),  Extra input\n"

(comment (def positive-recipe-prop
           (prop/for-all [r (s/gen ::recipe)]
                         (>= (cook! r) 0))))

(comment (tc/quick-check 100 positive-recipe-prop))
