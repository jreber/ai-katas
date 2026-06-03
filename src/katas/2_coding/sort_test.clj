(ns katas.2-coding.sort-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [katas.2-coding.sort :refer [quicksort]]))

;; Spec definitions
(s/def ::sortable-coll (s/coll-of number? :min-count 0 :max-count 100))

(defn sorted? [coll]
  (or (empty? coll)
      (apply <= coll)))

(deftest quicksort-test
  (testing "edge cases"
    (is (= [] (quicksort [])))
    (is (= [1] (quicksort [1])))
    (is (= [1 2] (quicksort [2 1]))))
  
  (testing "basic sorting"
    (is (= [1 1 2 3 4 5 6 9] (quicksort [3 1 4 1 5 9 2 6])))
    (is (= [-3 -1 0 2 5] (quicksort [5 -1 0 2 -3]))))
  
  (testing "duplicates"
    (is (= [1 1 2 2 3] (quicksort [2 1 3 1 2]))))
  
  (testing "already sorted"
    (is (= [1 2 3 4] (quicksort [1 2 3 4]))))
  
  (testing "reverse sorted"
    (is (= [1 2 3 4] (quicksort [4 3 2 1]))))
  
  (testing "property: output is sorted"
    (doseq [input (gen/sample (s/gen ::sortable-coll) 20)]
      (is (sorted? (quicksort input))
          (str "Failed for input: " input))))
  
  (testing "property: output has same elements"
    (let [test-colls [[3 1 4 1 5]
                      [9 7 5 3 1]
                      [1 1 1 1]
                      []]]
      (doseq [coll test-colls]
        (is (= (sort coll) (quicksort coll))
            (str "Failed for input: " coll))))))
