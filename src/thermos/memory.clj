(ns thermos.memory)
(def ^:dynamic *drop* (ref {}))

(defn mset [key value]
  (dosync (alter *drop* assoc key value) nil))

(def status @*drop*)

(defn delete [key]
  (dosync
   (alter *drop* dissoc key)))


