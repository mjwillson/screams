(ns screams.generators
  (:require [clojure.core.async :refer :all]
            [clojure.walk :refer [macroexpand-all]]))

(def ^:private ^:dynamic *compiling-generator-chan-sym* nil)

(defmacro yield
  ([]
     `(yield nil))
  ([body]
      (if *compiling-generator-chan-sym*
        `(>! ~*compiling-generator-chan-sym* ~body)
        (throw (IllegalArgumentException. "yield can only be used inside a generator form")))))

(defmacro generator
  [& body]
  (binding [*compiling-generator-chan-sym* (gensym 'generator-channel)]
    `(let [~*compiling-generator-chan-sym* (chan)]
       (go
        ~@(doall (map macroexpand-all body))
        (close! ~*compiling-generator-chan-sym*))
       (fn [] (<!! ~*compiling-generator-chan-sym*)))))

(defn range
  [n]
  (let [c (chan)]
    (go
     (loop [i 0]
       (>! c i)
       (if (< i n)
         (recur (inc i))
         (close! c))))
    (fn [] (<!! c))))

(defn range [n]
  (generator
   (loop [i 0]
     (yield i)
     (when (< i n)
       (recur (inc i))))))