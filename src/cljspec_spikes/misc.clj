(ns cljspec-spikes.misc)
;; Understanding reduce
(def xf (comp
         (filter odd?)
         (map inc)
         (take 5)))

;; Then use transduce
(transduce xf + (range 10))     ;;=> 30
(transduce xf + 100 (range 10)) ;;=> 130

(defn xf-with-threaded
  [coll]
  (->> coll
       (filter odd?)
       (map inc)
       (take 5)))

(xf-with-threaded (range 10)) ;; (2 4 6 8 10)

;; To get the same effect as transduce
(reduce + (xf-with-threaded (range 10)))     ;;=> 30
(reduce + 100 (xf-with-threaded (range 10))) ;;=> 130

;; eduction
(def iter (eduction xf (range 5)))
(reduce + 0 iter) ;; 6

;; into
(into [] (range 10)) ;; [0 1 2 3 4 5 6 7 8 9]

;; sequence
(sequence xf (range 10)) ;; (2 4 6 8 10)
