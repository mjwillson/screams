package screams;

import clojure.lang.IFn;

public class IteratorUtils {
  // Doing this in java rather than clojure because of "Cannot recur across
  // try" limitation in the clojure compiler:
  public static void iterate(final Iterator streamIterator, final IFn callback) {
    Object item;
    while (true) {
      try {
        item = streamIterator.next();
      }
      catch (Iterator.Finished _) {
        break;
      }
      callback.invoke(item);
    }
  }

  public static Object reduce(final Iterator streamIterator, final IFn f, Object val) {
    Object item;
    while (true) {
      try {
        item = streamIterator.next();
      }
      catch (Iterator.Finished _) {
        return val;
      }
      val = f.invoke(val, item);
    }
  }

}
