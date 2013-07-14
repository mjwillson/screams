package screams;

// A stream is, broadly speaking, an iterable whose iterators are
// are closeable and support concurrent consumption.

// The iterators are based on a python-like single .next method, with no
// hasNext-style lookahead required.

public interface IStream {
  // Contract: should return a new Iterator over the stream. See the
  // contract for Iterator which is important, in particular it needs
  // to be threadsafe.
  //
  // Iterator instances should never be re-used, i.e. each call to
  // .iterator should return a distinct instance (not that it would make
  // much sense to reuse them even if you were allowed to).
  //
  // Streams should ideally support calling .iterator unlimited times,
  // although if they are of an inherently one-shot nature they can throw an
  // exception to this effect on any calls to .iterator after the
  // first call.
  //
  // No restrictions are placed on the consumption of multiple
  // iterators returned from calls to .iterator, in particular they
  // could be consumed with interleaved .next calls or concurrent
  // .next calls. If any synchronization at the stream level is
  // required to make this safe (hopefully it isn't) then the stream
  // is responsible for ensuring it.
  //
  // Note our Iterator interface extends Closeable, but Stream itself
  // doesn't. Where possible Streams should wait until .iterator is
  // called to open any underlying resources (like a Reader or
  // InputStream say), and let the .close method of the returned
  // iterator handle closing them.
  Iterator iterator();
}
