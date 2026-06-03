(ns katas.2-coding.sort)

(defn quicksort
  "Simple quicksort implementation using tail recursion. Returns a sorted vector."
  ([coll]
   (quicksort coll []))
  ([coll acc]
   (if (empty? coll)
     acc
     (let [pivot (first coll)
           rest-items (rest coll)
           smaller (filter #(<= % pivot) rest-items)
           larger (filter #(> % pivot) rest-items)]
       (recur larger
              (vec (concat (quicksort smaller) [pivot] acc)))))))
