package screams;

import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.PersistentList;

public class EmptyStream implements IStream, Seqable {
  public static final EmptyStream INSTANCE = new EmptyStream();

  EmptyStream() {}
  public Iterator iterator() {
    return new Iterator() {
      public Object next() throws Iterator.Finished {
        throw new Iterator.Finished();
      }
      public void close() {}
    };
  }

  public ISeq seq() {
    return PersistentList.EMPTY;
  }
}
