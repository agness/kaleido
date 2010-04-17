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

  /**
   * Which version to compile, for versions of code that are different
   * (debugging, initial graph & text, etc.)
   */  
  public static final boolean BUILD_FOR_RELEASE = true;
  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
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
   * he</a>
   */
  private static Color[][] colorpalette_cheerup = {{ mxUtils.parseColor("#556270"),
    mxUtils.parseColor("#4ECDC4"), 
    mxUtils.parseColor("#C7F464"),
    mxUtils.parseColor("#FF6B6B"), 
    mxUtils.parseColor("#C44D58") },
  { mxUtils.parseColor("#eeeeee"),
    Color.BLACK, 
    Color.BLACK,
    Color.BLACK, 
    mxUtils.parseColor("#eeeeee") }};
  
  /**
   * From <a href="http://www.colourlovers.com/palette/663167/Conspicuous_Creep">
   * http://www.colourlovers.com/palette/663167/Conspicuous_Creep</a>
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
  
  /**
   * From <a href="http://zeroperzero.com/crs/seoul-.html">
   * http://zeroperzero.com/crs/seoul-.html</a>
   */
  private static Color[][] colorpalette_seoul = {{ mxUtils.parseColor("#EF5A6F"),
    mxUtils.parseColor("#82469F"),
    mxUtils.parseColor("#00A1F4"),
    mxUtils.parseColor("#4FB84E"),
    mxUtils.parseColor("#A07E46") },
  { mxUtils.parseColor("#ffffff"),
    mxUtils.parseColor("#ffffff"),
    mxUtils.parseColor("#ffffff"),
    mxUtils.parseColor("#ffffff"), 
    mxUtils.parseColor("#ffffff") }};
  
  
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
  public static Color UI_COLOR_BUTTONFILL = new Color(0xb0b0b0); //background color of drawingHeader button elements, medium gray
  public static Color UI_COLOR_CANVAS = new Color(0x333333); //the color of drawing area canvas, very dark gray
  private static Color UI_COLOR_ACTIVE = new Color(0xffcc00);//p5 yellow highlight
  private static Color UI_COLOR_ROLLOVER = Color.white;
  private static Color UI_COLOR_ACTIVETAB = new Color(0xd3d3d3); //the color of selected text tab, light gray
  private static Color UI_COLOR_SWINGGRAY = new Color(0xe8e8e8); //very nearly white, the color of java swing borders
  private static Color UI_COLOR_BUTTONBAR = new Color(0x686868); //equals Theme.getColor(buttons.bgcolor)
  public static Color UI_COLOR_HEADERBKGD = new Color(0x989898); //equals Theme.getColor(header.bgcolor)
  private static Stroke UI_DOTTED_STROKE = new BasicStroke(1,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
      new float[] { 1, 3 }, 0.0f);
  public static Stroke SHAPE_DOTTED_STROKE = new BasicStroke(4,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
      new float[] { 5, 5 }, 0.0f);
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * Drawing Area stuff
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
  
  /**
   * Default font for labels
   * TODO read theme.getFont or something and set this instead of hardwiring?
   */
  public static final String DEFAULT_FONTFAMILY = "SansSerif";
  
  
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
   * Pairs of matched fill and font colors for graph elements
   */
  public static Color[][] FILL_COLORS = colorpalette_seoul;
  
  /**
   * The uniform color of all connectors created in Kaleido
   */
  public static final Color EDGE_STROKE_COLOR = UI_COLOR_BUTTONFILL;

  /**
   * The uniform font color of all connector labels
   */
  public static final Color EDGE_FONT_COLOR = UI_COLOR_BUTTONFILL;
  
  /**
   * The font color of text areas (should be decided upon depending on color of canvas)
   */
  public static final Color SHAPE_TEXT_FONT_COLOR = EDGE_FONT_COLOR;
  
  /**
   * The color of triangles and text area of code windows (no control over
   * scroll bars or move/close buttons)
   */
  public static final Color CODE_WINDOW_COLOR = Color.white;
  
  /**
   * Graph outline viewfinder handle color & stroke color
   */
  public static final Color OUTLINE_HANDLE_COLOR = UI_COLOR_ACTIVE;

  /**
   * Padding between graph outline image and the component it's in
   */
  public static final int OUTLINE_BORDER_WIDTH = 3;
  
  /**
   * Color of padding between graph outline image and the component
   * (i.e. color of container background)
   */
  public static final Color OUTLINE_BORDER_COLOR = UI_COLOR_BUTTONFILL;
  
  /**
   * Color of drop shadow of shapes
   */
  public static final Color SHADOW_COLOR = Color.black;

  /**
   * Dimensions of drop shadow of shapes
   */
  public static final int SHADOW_OFFSETX = 3;

  public static final int SHADOW_OFFSETY = 4;

  /**
   * Color of graphComponent "background" that has ramifications
   * only on the little corner square in between scroll bars
   */
  public static final Color DRAWAREA_SCROLL_CORNER_COLOR = UI_COLOR_SWINGGRAY;
  
  /**
   * Amount of padding between cells dropped into other cells (swimlanes)
   */
  public static final int CELL_NESTING_PADDING = 10;

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * kGraphComponent handler styling stuff
   */
  public static final Color RUBBERBAND_COLOR = new Color(UI_COLOR_ACTIVE.getRed(), UI_COLOR_ACTIVE.getGreen(), UI_COLOR_ACTIVE.getBlue(), 90);
  
  public static final Color SECONDARY_SELECTION_COLOR = UI_COLOR_ACTIVETAB;

  /**
   * @deprecated
   */
  public static final Color CELL_MARKER_COLOR = Color.gray;

  public static final Color SWIMLANE_MARKER_COLOR = UI_COLOR_ROLLOVER;

  public static final Color HANDLE_FILLCOLOR = UI_COLOR_ACTIVE;

  public static final Color HANDLE_BORDERCOLOR = UI_COLOR_ACTIVE;

  public static final Color LABEL_HANDLE_FILLCOLOR = UI_COLOR_ACTIVE;//we're not going to move labels, but make this anyway
  
  public static final Color EDGE_HANDLE_BORDERCOLOR = UI_COLOR_ACTIVE;

  public static final Color EDGE_HANDLE_FILLCOLOR = UI_COLOR_ACTIVE;//color of dangling edge handles
  
  public static final Color LOCKED_HANDLE_FILLCOLOR = UI_COLOR_ROLLOVER;//what's this

  public static final Color CONNECT_HANDLE_FILLCOLOR = Color.black;//color of edge handle that is connected to a target
  
  public static final Color DEFAULT_VALID_COLOR = UI_COLOR_ROLLOVER;//used to indicate a valid target when dragging edges over vertices

  public static final Color DEFAULT_INVALID_COLOR = new Color(0xff3000);//equal to Theme.getColor(console.error.bgcolor);

  public static final Color VERTEX_SELECTION_COLOR = UI_COLOR_ACTIVE;

  public static final Stroke VERTEX_SELECTION_STROKE = UI_DOTTED_STROKE;
  
  public static final Color EDGE_SELECTION_COLOR = UI_COLOR_ACTIVE; //color of line along connectors when selected

  public static final Stroke EDGE_SELECTION_STROKE = UI_DOTTED_STROKE;

  public static final Color PREVIEW_COLOR = DEFAULT_VALID_COLOR;
  
  public static final Stroke PREVIEW_STROKE = UI_DOTTED_STROKE;
  
  /**
   * Defines the border used for painting the preview when vertices are being
   * resized, or cells and labels are being moved.
   */
  public static Border PREVIEW_BORDER = new LineBorder(PREVIEW_COLOR)
  {
    public void paintBorder(Component c, Graphics g, int x, int y,
        int width, int height)
    {
      ((Graphics2D) g).setStroke(UI_DOTTED_STROKE);
      super.paintBorder(c, g, x, y, width, height);
    }
  };

  public static final Color SHAPE_PREVIEW_BORDER_COLOR = PREVIEW_COLOR;//used to draw the border when creating new shapes, using this color to match edge preview handles

  public static final Color SHAPE_PREVIEW_FILL_COLOR = new Color(UI_COLOR_BUTTONBAR.getRed(), UI_COLOR_BUTTONBAR.getGreen(), UI_COLOR_BUTTONBAR.getBlue(), 90);



  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  
  /*
   * Text area stuff
   */
  
  public static final Color LINK_MARKER_BACKGROUND_COLOR = Color.white;

  public static final int LINK_MARKER_WIDTH = 10;








}
