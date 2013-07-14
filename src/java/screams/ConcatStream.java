package screams;

// A stream based on concatenating a stream of streams.
// This is entirely lazy and doesn't need to serialize calls to .next
// on each inner stream.
// Iterators for each inner stream are only opened when required and
// closed before starting the next one.
//
// Particularly suited to, for example, concatenating together
// a bunch of long file-backed streams.
//
// It does necessarily serialize iteration over the outer stream
// though, so may not be the best choice if the inner streams are
// short (in which case a concatenator which eagerly consumes each
// inner stream may be better).
public class ConcatStream extends AStream {
  private final IStream streamOfStreams;

  public ConcatStream(IStream streamOfStreams) {
    this.streamOfStreams = streamOfStreams;
  }

  public Iterator iterator() {
    return new ConcatIterator(streamOfStreams.iterator());
  }

  class ConcatIterator implements Iterator {
    private Iterator currentIterator = null;
    private final Iterator iteratorOfStreams;

    ConcatIterator(Iterator iteratorOfStreams) {
      this.iteratorOfStreams = iteratorOfStreams;
      this.currentIterator = EmptyStream.INSTANCE.iterator();
    }

    // Making this threadsafe without serializing access completely is quite subtle.
    public Object next() throws Iterator.Finished {
      Iterator currentIteratorInitially;

      // We need to synchronize reading this as other threads may
      // update it inside a lock.
      synchronized (this) {
        currentIteratorInitially = currentIterator;
      }

      // The fast path -- try returning the next value from the
      // current iterator outside of any lock. This is OK since the
      // underlying iterators must be threadsafe by contract.
      // doing it this way avoids serializing all access to the
      // underlying iterator (which would rather defeat the point of
      // trying to make all our iterators threadsafe...).
      //
      // Note someone else may already have exhausted it since we
      // exited the lock above, in which case it'll just throw
      // Finished again which we can catch.
      try {
        return currentIteratorInitially.next();
      }
      catch (Iterator.Finished _) {
        // The current iterator (as we read it just a while ago) is exhausted.
        // We need to synchronize when replacing it with the next one,
        // all other threads will need to block until this is done.
        synchronized (this) {
          // Now we're inside the lock again, we need to check nobody else
          // got there first before we try to replace the current
          // iterator.
          // We're assuming the iterator instances are never reused
          // (which would be a silly thing to do anyway since they can't
          // be rewound).
          if (this.currentIterator == currentIteratorInitially) {
            nextIterator();
          }
          // else: someone else got there first, we just use
          // their new value of currentIterator below.

          // Keep trying many new iterators until we
          // find a non-empty one which will yield something:
          while (true) {
            try {return currentIterator.next();}
            catch (Iterator.Finished __) {nextIterator();}
          }

        }
      }
    }

    // Private & only to be called within a synchronized block.
    private void nextIterator() throws Iterator.Finished {
      // Make sure we close the
      // iterator which just finished:
      currentIterator.close();
      // If there are no streams left on iteratorOfStreams,
      // .next here will throw Finished which we let bubble
      // up, since the overall stream is then done.
      currentIterator = ((IStream) iteratorOfStreams.next()).iterator();
    }

    // Because of the contract around next/close we don't technically
    // need to synchronize here, but (Postel's principle) it doesn't
    // hurt to be safe here.
    public synchronized void close() {
        currentIterator.close();
      }
  }
}
