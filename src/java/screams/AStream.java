package screams;

import clojure.lang.Seqable;
import clojure.lang.ISeq;
import clojure.lang.PersistentVector;
import clojure.lang.ITransientCollection;

public abstract class AStream implements IStream, Seqable {
  public PersistentVector vec() {
    Iterator it = iterator();
    ITransientCollection ret = PersistentVector.EMPTY.asTransient();
    try {
      while (true) {
        try {
          ret = ret.conj(it.next());
        }
        catch (Iterator.Finished _) {
          break;
        }
      }
    }
    finally {
      it.close();
    }
    return (PersistentVector) ret.persistent();
  }

  public ISeq seq() {
    return vec().seq();
  }
}
