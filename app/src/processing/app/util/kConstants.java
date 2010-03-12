package processing.app.util;

/**
 * Kaleido Constants
 * Extending the mxConstants with some definitions of our own.
 * @see com.mxgraph.util.mxConstants
 * @author achang
 */
public class kConstants 
{
  /**
   * 
   */
  public static String SHAPE_TEXT = "text";
  public static String SHAPE_STAR = "star";
  public static String SHAPE_AUDIO = "audio";
  public static String SHAPE_KEYBOARD = "keyboard";
  public static String SHAPE_PERSON = "person";
  
  
  /** Rollover titles for each shape-making button. */
  public static final String SHAPE_NAMES[] = {
    "rect", "circle", "diam", "star", "audio", "keyb", "pers"
  };
  
  /** Rollover titles for each connector-making button. */
  public static final String CONNECTOR_NAMES[] = {
    "line", "solid", "dotted"
  };
  
  /** Rollover titles for each color-making button. */
  //TODO color sets possible changable in some future version
  public static final String COLOR_NAMES[] = {
    "drawing.fillset1.color1", "drawing.fillset1.color2", "drawing.fillset1.color3", 
    "drawing.fillset1.color4", "drawing.fillset1.color5"
  };
 
  
  /**
   * Can't believe I need to write this myself, but I guess
   * it's just 7 lines of code and infinitely more trustworthy
   * than that java.Arrays.binarySearch crap.
   * TODO this probably should go into some sort of kUtil class
   */
  public static int stringLinearSearch(String [] a, String s) {
    for (int i = 0; i < a.length; i++) {
      if (s.equals(a[i]))
        return i;
    }
    return -1;
  }
}
