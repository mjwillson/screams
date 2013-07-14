package screams;

public interface Iterator {
  /*
    Contract:

    .next must be threadsafe, i.e. it may be called concurrently
    without any corruption of the state of the iterator, without any
    items being skipped or duplicated etc.
    You may ask for the next item (necessarily from another thread)
    before the current item has been returned.

    Where possible, implementations should avoid taking the easy route
    to thread-safety, that is synchronizing around all of .next, since
    this will serialize all calls to it, and the main goal in
    requiring threadsafety is to allow for a concurrency speed boost
    when multiple threads are simultaneously pulling items from the
    iterator. However in some cases this be the only viable option,
    e.g. where iteration has inherently serial data dependencies (the
    next value depends on the previous one say).

    .next should throw Finished if it has exhausted all the items
    available to be iterated over. It may be called again any number
    of times after it has finished, and should throw Finished again
    on all such subsequent calls. (Note: infinite iterators are
    allowed, there's no requirement that Finished be thrown at any
    point.)

    .close from must also be implemented, and should close any
    resources associated with the iterator. If the iterator is already
    closed then invoking .close should have no further effect. In
    particular there should be no "already closed" exceptions thrown
    if .close is called multiple times.

    (The reason I didn't extend java.io.Closeable is that it throws a
    checked IOException which we'd then have to handle or suppress in
    various places or it would need to appear in the type signature of
    .next too, which would be a pain. In practise read-only Closeables
    don't seem to throw IOException on close, it's more for writable
    resources which might fail to flush a write buffer on close. This
    means you may need to catch/suppress/log IOException if
    implementing this in java based on some underlying Closeable.)

    Iterators are not required to close themselves after finishing,
    and callers must never rely on this happening and must always call
    .close even if they fully exhausted the iterator.

    .close may also be called before the iterator is finished,
    although not while a .next call is still in progress in any thread.

    Once .close has been called, .next must not be called again from
    any thread.

    Putting thw burden on the consumer to ensure this, removes any
    requirement for synchronization around .close, or for
    synchronization in .next solely for the purpose of avoiding
    clashes with .close.
  */
  Object next() throws Finished;
  void close();

  public class Finished extends Exception {
    public Finished() {}
  }
}
