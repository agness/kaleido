package processing.app.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;

import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import processing.app.Theme;


import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;

/**
 * Kaleido Constants
 * Extending the mxConstants with some definitions of our own.
 * @see com.mxgraph.util.mxConstants
 * @author achang
 */
public class kConstants 
{
  /**
   * This version of Kaleido
   */
  public static final String VERSION_NAME = "001";
  
  /*
   * Private library of color palettes that can be used in these settings.
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
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * Cell/vertex/edge/shape stuff
   */
  
  /**
   * Style keys: Shape names.
   */
  public static String SHAPE_TEXT = "text";
  public static String SHAPE_STAR = "star";
  public static String SHAPE_AUDIO = "audio";
  public static String SHAPE_KEYBOARD = "keyboard";
  public static String SHAPE_PERSON = "person";
  
  /**
   * Style key: Locked or not...
   */
  public static final String STYLE_LOCKED = "locked"; //mxConstants.STYLE_SHADOW
  public static final String STYLE_LINKED = "linked";
  
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
    "color1", "color2", "color3", "color4", "color5"
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
  public static Color UI_COLOR_BACKGROUND = mxUtils.parseColor("#d3d3d3"); //the color of selected text tab
  public static Color UI_HANDLE_FILL = UI_COLOR_BACKGROUND;
  
  /**
   * Pairs of matched fill and font colors for graph elememnts.
   */
  public static Color[][] FILL_COLORS = colorpalette_cheerup;

  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * kGraphComponent handler styling stuff
   */

  public static final Color EDGE_STROKE_COLOR = Color.black;

  public static final Color EDGE_FONT_COLOR = Color.gray;
  
  public static final Color CANVAS_COLOR = UI_COLOR_INACTIVE;

  public static final Color CELL_MARKER_COLOR = Color.gray;

  public static final Color SWIMLANE_MARKER_COLOR = UI_COLOR_ACTIVE;

  public static final Color HANDLE_FILLCOLOR = UI_COLOR_ROLLOVER;

  public static final Color HANDLE_BORDERCOLOR = UI_COLOR_ROLLOVER;

  public static final Color LABEL_HANDLE_FILLCOLOR = UI_COLOR_ROLLOVER;

  public static final Color LOCKED_HANDLE_FILLCOLOR = Color.gray;

  public static final Color CONNECT_HANDLE_FILLCOLOR = Color.magenta;//what's this

  public static final Color CONN_MARKER_VALID_COLOR = Color.CYAN;//mxUtils.parseColor("#B9FC00");

  public static final Color CONN_MARKER_INVALID_COLOR = Theme.getColor("status.error.bgcolor"); 

  public static final Color DEFAULT_VALID_COLOR = Color.magenta;//what's this?

  public static final Color DEFAULT_INVALID_COLOR = Color.yellow;//new Color(255,0,255);  

  public static final Stroke PREVIEW_STROKE = new BasicStroke(1,
  BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
  new float[] { 3, 3 }, 0.0f);

  public static Border PREVIEW_BORDER = new LineBorder(HANDLE_BORDERCOLOR) {
    public void paintBorder(Component c, Graphics g, int x, int y, int width,
                            int height) {
      ((Graphics2D) g).setStroke(kConstants.VERTEX_SELECTION_STROKE);
      super.paintBorder(c, g, x, y, width, height);
    }
  };

  public static final Color VERTEX_SELECTION_COLOR = Color.yellow;//what's this?

  public static final Stroke VERTEX_SELECTION_STROKE = new BasicStroke(1,//what's this?
  BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
  new float[] { 3, 3 }, 0.0f);

  public static final Color EDGE_SELECTION_COLOR = Color.cyan;//what's this?

  public static final Stroke EDGE_SELECTION_STROKE = new BasicStroke(1,//what's this?
  BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
  new float[] { 3, 3 }, 0.0f);

  public static final Color SHAPE_PREVIEW_BORDER_COLOR = Color.GRAY;//what's this?

  public static final Color SHAPE_PREVIEW_FILL_COLOR = new Color(170, 170, 170, 70);//what's this?
  
}
