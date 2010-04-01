package processing.app.util;

import java.awt.Color;

/**
 * Contains various static helper methods for use with Kaleido.
 * @author achang
 */
public class kUtils {

  /**
   * Can't believe I need to write this myself, but I guess
   * it's just 7 lines of code and infinitely more trustworthy
   * than that java.Arrays.binarySearch crap.
   * TODO this probably should go into some sort of kUtil class
   */
  public static int arrayLinearSearch(Object [] a, Object s) {
    for (int i = 0; i < a.length; i++) {
      if (s.equals(a[i]))
        return i;
    }
    return -1;
  }

  /**
   * For use in converting our color keys (which are String)
   * into actual Colors whose value is set in kConstants.
   * @param s
   * @return
   */
  public static Color getFillColorFromKey(String s){
    int index = arrayLinearSearch(kConstants.COLOR_KEYS, s);
    if (index >= 0)
      return kConstants.FILL_COLORS[0][index];
    else
      return null;
  }

  /**
   * For use in converting our color keys (which are String)
   * into actual Colors whose value is set in kConstants.
   * @param s
   * @return
   */
  public static Color getFontColorFromKey(String s){
    int index = arrayLinearSearch(kConstants.COLOR_KEYS, s);
    if (index >= 0)
      return kConstants.FILL_COLORS[1][index];
    else
      return null;
  }

}
