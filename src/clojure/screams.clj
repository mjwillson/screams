(ns screams
  (:import [screams IStream Iterator Iterator$Finished ConcatStream IteratorUtils EmptyStream]
           [java.io Closeable])
  (:require [clojure.java.io :as io]
            [clojure.core.protocols :refer [coll-reduce CollReduce]])
  (:refer-clojure :exclude [map filter take apply]))


(defn finish
  []
  (throw (Iterator$Finished.)))

(def to-seq)

(defmacro stream
  [& body]
  `(reify
     IStream
     (iterator [_] ~@body)
     clojure.lang.Seqable
     (seq [this#] (to-seq this#))))

(defn file-line-stream
  [file]
  (stream
    (let [reader (io/reader file)]
      (reify Iterator
        (close [_] (.close reader))
        (next [_] (or (.readLine reader) (finish)))))))

(defn map
  [f s]
  (stream
    (let [iterator (.iterator s)]
      (reify Iterator
        (close [_] (.close iterator))
        (next [_] (f (.next iterator)))))))

(defn filter
  [f s]
  (stream
    (let [iterator (.iterator s)]
      (reify Iterator
        (close [_] (.close iterator))
        (next [_] (let [item (.next iterator)]
                    (if (f item) item (recur))))))))

(defn take
  [n s]
  (stream
    (let [iterator (.iterator s)
          counter (atom n)]
      (reify Iterator
        (close [_] (.close iterator))
        (next [_] (if (< (swap! counter dec) 0)
                    (finish)
                    (.next iterator)))))))

;; This makes streams work efficiently with clojure.core/reduce,
;; and other functions which use it like clojure.core/into
(extend-protocol CollReduce
  IStream
  (coll-reduce
    ([stream f] (coll-reduce stream f (f)))
    ([stream f val]
       (with-open [iterator (.iterator stream)]
         (IteratorUtils/reduce iterator f val)))))

(def empty-stream EmptyStream/INSTANCE)

(defprotocol StreamFactory (^IStream to-stream [s]))

(extend-protocol StreamFactory
  nil
  (to-stream [_] empty-stream)

  IStream
  (to-stream [s] s)

  ;; Most java and clojure data structures implement Iterable. Note
  ;; thread-safety under concurrent .next is not part of the contract
  ;; for java.util.Iterator, so for safety we need to synchronize on
  ;; the iterator here. (We could try calling .next without checking
  ;; .hasNext and handling NoSuchItemException, but even then we'd
  ;; still need to synchronize in general.)
  ;;
  ;; TODO: iterators of some specific iterables are documented
  ;; threadsafe, such as e.g.
  ;; http://docs.oracle.com/javase/6/docs/api/java/util/concurrent/CopyOnWriteArrayList.html#iterator()
  ;; for these we could avoid locking.
  java.lang.Iterable
  (to-stream [java-iterable]
    (stream
     (let [java-iterator (.iterator java-iterable)]
       (reify Iterator
         (next [_] (locking java-iterator
                     (if (.hasNext java-iterator)
                       (.next java-iterator)
                       (finish))))
         (close [_]
           (when (instance? Closeable java-iterator)
             (.close ^Closeable java-iterator)))))))

  java.lang.CharSequence
  (to-stream [char-seq]
    (stream
     (let [i (atom -1)
           n (.length char-seq)]
       (reify Iterator
         (next [_]
           (let [i-val (swap! i inc)]
             (if (< i-val n)
               (.charAt char-seq i-val)
               (finish))))
         (close [_])))))

  ;; TODO: add support for java arrays (could be a bit repetitive to
  ;; support all primitive array types though)
  ;;
  ;; Could also add lockfree implementations based on java.util.List#get and an
  ;; AtomicInteger counter, for immutable lists supporting concurrent .get
  ;; (although careful:
  ;; http://jeremymanson.blogspot.co.uk/2008/04/immutability-in-java.html )
  ;;
  ;; Also TODO: anything Seqable whose seq is an IChunkedSeq we could
  ;; potentially do faster via this interface and a counter within
  ;; each chunk.
  ;; Also we could write our own seq-stream which directly iterates
  ;; over a seq rather than going via its java iterator.
  )

(defn concat
  "Concatenates streams into a stream. Iterators for each stream will
  be opened and closed in turn."
  [& seq-of-streams]
  (ConcatStream. (to-stream seq-of-streams)))

(defn cat
  "Like concat, but takes a stream of streams as a single argument,
  rather than a seq of streams via varargs."
  [stream-of-streams]
  (ConcatStream. stream-of-streams))

(defn mapcat
  "Like clojure.core/mapcat, except the function f should return a stream rather than a seq."
  [f stream]
  (cat (map f stream)))

(defmacro dostream
  [[item stream] & body]
  `(with-open [iterator# (.iterator ~stream)]
     (IteratorUtils/iterate iterator# (fn [~item] ~@body))))

;; interaction with clojure Seq

(defn stream-iterator-seq
  "Create a lazy seq from an underlying iterator obtained from a
   stream. Warning: the iterator could end up closed before the seq is
   realised, or equally you might forget to close it which would be
   bad. Recommended to use with-lazy-seq if you want a seq from a
   stream."
  [^Iterator iterator]
  (lazy-seq (try (cons (.next iterator) (stream-iterator-seq iterator))
                 (catch Iterator$Finished _))))

(defmacro with-lazy-seq
  "Binds a lazy seq based on the given stream, within the body, finally
  closing the underlying iterator. Warning: you should not leak the
  seq or any derived (via map, filter etc) seqs outside of the block
  unless they're fully realised, since the iterator will be closed.

  This should only be needed for compatibility reasons -- you can
  reduce a stream directly, or iterate over it directly with dostream."
  [[seq stream] & body]
  `(with-open [it# (.iterator ~(with-meta stream {:tag `IStream}))]
     (let [~seq (stream-iterator-seq it#)]
       ~@body)))

(defn to-seq
  "Converts a stream eagerly into a fully-realised ISeq. All streams
  created with the stream macro will implement Seqable via this
  function, and so can be used anywhere a Seqable is allowed, for
  example as an argument to clojure.core/apply.

  Warning: this will bring the whole stream into memory, so it's not
  recommended to rely on this Seqable implementation. Use
  with-lazy-seq instead, or use our apply function if you want to
  apply lazily."
  [stream]
  (with-lazy-seq [s stream] (doall s)))

(defn apply
  "Like clojure.core/apply, but takes a stream and applies the
  function to a *lazy* seq of it, finally closing the underlying
  iterator.
  (Note streams will work with clojure.core/apply too, but they'll be
  eagerly brought into memory first, so better to use this, or better
  yet, use reduce if you can)."
  [f stream]
  (with-lazy-seq [s stream]
    (clojure.core/apply f s)))