(ns thermos.utils)

(defn to-int [s]
  (cond 
   (string? s) (Integer/parseInt s)
   (instance? Integer s) s
   (instance? Long s) (.intValue ^Long s)
   :else 0))

