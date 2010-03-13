package processing.app.util;

import java.awt.Color;

import com.mxgraph.util.mxUtils;

/**
 * Kaleido Constants
 * Extending the mxConstants with some definitions of our own.
 * @see com.mxgraph.util.mxConstants
 * @author achang
 */
public class kConstants 
{
  /*
   * Private library of color palettes that can be used in the settings.
   */
  
  /**
   * From <a href="http://www.colourlovers.com/palette/694737/Thought_Provoking">
   * http://www.colourlovers.com/palette/694737/Thought_Provoking</a>
   */
  private static Color[][] colorpalette_thought = {{ mxUtils.parseColor("#ecd078"),
    mxUtils.parseColor("#d95b43"), 
    mxUtils.parseColor("#c02942"),
    mxUtils.parseColor("#542437"), 
    mxUtils.parseColor("#53777a") },
  { Color.BLACK,
    Color.BLACK, 
    mxUtils.parseColor("#eeeeee"),
    mxUtils.parseColor("#eeeeee"), 
    Color.BLACK }};

  /**
   * From <a href="http://www.colourlovers.com/palette/1930/cheer_up_emo_kid">
   * http://www.colourlovers.com/palette/1930/cheer_up_emo_kid</a>
   */
  private static Color[][] colorpalette_cheerup = {{ mxUtils.parseColor("#556270"),
    mxUtils.parseColor("#4ECDC4"), 
    mxUtils.parseColor("#C7F464"),
    mxUtils.parseColor("#FF6B6B"), 
    mxUtils.parseColor("#C44D58") },
  { mxUtils.parseColor("#eeeeee"),
    Color.BLACK, 
    Color.BLACK,
    mxUtils.parseColor("#eeeeee"), 
    mxUtils.parseColor("#eeeeee") }};
  
  /**
   * From <a href="http://www.colourlovers.com/palette/694737/Thought_Provoking">
   * http://www.colourlovers.com/palette/694737/Thought_Provoking</a>
   */
  private static Color[][] colorpalette_creep = {{ mxUtils.parseColor("#0B8C8F"),
    mxUtils.parseColor("#FCF8BC"), 
    mxUtils.parseColor("#CACF43"),
    mxUtils.parseColor("#2B2825"), 
    mxUtils.parseColor("#D6156C") },
  { Color.BLACK,
    mxUtils.parseColor("#eeeeee"),
    mxUtils.parseColor("#eeeeee"),
    Color.BLACK, 
    Color.BLACK }};
  
  /*
   * End private members.
   */
  
  
  /**
   * Shape names.
   */
  public static String SHAPE_TEXT = "text";
  public static String SHAPE_STAR = "star";
  public static String SHAPE_AUDIO = "audio";
  public static String SHAPE_KEYBOARD = "keyboard";
  public static String SHAPE_PERSON = "person";
  
  
  /** Rollover titles for each shape-making button. */
  public static final String SHAPE_KEYS[] = {
    "rect", "circle", "diam", "star", "audio", "keyb", "pers"
  };
  
  /** Rollover titles for each connector-making button. */
  public static final String CONNECTOR_KEYS[] = {
    "line", "solid", "dotted"
  };
  
  /** Rollover titles for each color-making button. */
  //TODO color sets possible changable in some future version
  public static final String COLOR_KEYS[] = {
    "drawing.fillset1.color1", "drawing.fillset1.color2", "drawing.fillset1.color3", 
    "drawing.fillset1.color4", "drawing.fillset1.color5"
  };
  
  
  /**
   * Color themes for UI elements: buttons(paintfill), cellMarker(onMouseOver), 
   * cellHandler(selection), swimlane, etc.
   * 
   * Using this class rather than Processing's theme.txt etc. so I
   * don't have to require users to make any changes to their 
   * Processing installations.
   * Also this is easier to refactor because Eclipse doesn't deal
   * with theme.txt.
   */
  public static Color UI_COLOR_ACTIVE = mxUtils.parseColor("#ffcc00");
  public static Color UI_COLOR_ROLLOVER = Color.WHITE;
  public static Color UI_COLOR_INACTIVE = mxUtils.parseColor("#b0b0b0");
  
  /**
   * Pairs of matched fill and font colors for graph elememnts.
   */
  public static Color[][] FILL_COLORS = colorpalette_cheerup;
}
