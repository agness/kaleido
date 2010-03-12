package processing.app;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import processing.app.graph.kCanvas;
import processing.app.util.kConstants;
import processing.app.util.kEvent;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;


/**
 * The Kaleido drawing JComponent, which is a desktop pane
 * that contains the graph component, and any number of internal
 * code windows.
 * @author achang
 */
public class DrawingArea extends JDesktopPane {

  JInternalFrame graphPanel;
  mxGraphComponent graphComponent;
  mxEventSource eventSource = new mxEventSource(this);
  
  //making these beforehand so we don't make infinite new objects when swapping between these two
  static final Cursor TOOL_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);
  
  /**
   * Stores which graph editing tool (e.g. "rect [shape]", 
   * "dotted [connector]", "text", or paintbucket color name) is currently active.
   * Null if in default selection mode.
   */
  String toolMode;
  
  /**
   * Stores which color is currently active on the DrawingHeader-toolbar
   * regardless of whether the color tool is active, but is updated
   * when the color tool is activated.
   */
  String currentFillColor = kConstants.COLOR_NAMES[0];

  /**
   * @deprecated
   * This array stores references to all cells that have valid code marks.  It is used to
   * update codeMarks after every textarea edit.
   * 
   * TODO instead, scan the graph each time, like this:
   * Iterator it = graphModel.getCells().values().iterator();
   * while(iterator. hasNext()) System.out.println(it.next());
   */
  protected ArrayList<Object> mideBlockRegistry = new ArrayList<Object>(20);

  mxRubberband rubberband;
  ShapeToolband shapeToolband;
  ConnectorToolband connectorToolband;
  ColorToolband colorToolband;
  
  public DrawingArea(mxGraphModel x) {
    super();
    //<!------------- hello world crap TODO remove
    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
    try
    {
      Object v1 = graph.insertVertex(parent, null, "Hello", 20, 20, 80,
          30);
      Object v2 = graph.insertVertex(parent, null, "World!", 240, 150,
          80, 30);
      graph.insertEdge(parent, null, "Edge", v1, v2);
    }
    finally
    {
      graph.getModel().endUpdate();
    }
    // hello world crap -------------<
    
    graphComponent = new mxGraphComponent(graph) {
      public mxInteractiveCanvas createCanvas() {
        return new kCanvas();        
      }
    };
    graphPanel = new JInternalFrame("Graph", false, //resizable
                                    false, //not closable
                                    false, //not maximizable
                                    false); //not iconifiable
    graphPanel.setContentPane(graphComponent);
    graphPanel.pack();
    graphPanel.setVisible(true);
    graphPanel.setLayer(0);
    graphPanel.setBorder(null);
    graphPanel.setSize(3000, 3000);  //TODO: be able to resize with the rest of them
    add(graphPanel);
    setVisible(true);
    
    //event handling
    rubberband = new mxRubberband(graphComponent);
    rubberband.setEnabled(true);
    shapeToolband = new ShapeToolband(this);
    shapeToolband.setEnabled(false);
    connectorToolband = new ConnectorToolband(this);
    connectorToolband.setEnabled(false);
    colorToolband = new ColorToolband(this);
    colorToolband.setEnabled(false);
  }
  
  public mxGraphComponent getGraphComponent() {
    return graphComponent;
  }
  
  public String getCurrentFillColor() {
    return currentFillColor;
  }
  /**
   * @deprecated
   */
  public void setCurrentFillColor(String currentFillColor) {
    this.currentFillColor = currentFillColor;
  }
  
  /**
   * Fires TOOL_BEGIN event, sets the cursor, and enables toolband
   * mouse drawing instead of rubberband selection.
   * @param toolName
   */
  public void beginToolMode (final String toolName) {
    System.out.println("BEGINtoolmode "+toolName);
    eventSource.fireEvent(new mxEventObject(kEvent.TOOL_BEGIN, "tool", toolName));
    //disable all other graph mouse events for the time being:
    rubberband.setEnabled(false);
    graphComponent.getGraphHandler().setCloneEnabled(false);
    graphComponent.getGraphHandler().setMoveEnabled(false);
    graphComponent.getGraphHandler().setSelectEnabled(false);
    graphComponent.getConnectionHandler().setEnabled(false); //gets ride of cellMarkers
    //Notes on: cursor setting:
    //turns out we can't set the cursor here because it gets overridden
    //by graphControl's mouse listeners after the 2nd time
    //but we still need to do this to cover the first time we want to
    //change the cursor.  Why?  Dun ask me. @author achang
    setCursor(TOOL_CURSOR);
    toolMode = toolName;
//    System.out.println("index of "+toolName+" in SHAPE_NAMES = "+kConstants.stringLinearSearch(kConstants.SHAPE_NAMES, toolName));
    if (kConstants.stringLinearSearch(kConstants.SHAPE_NAMES, toolName) >= 0 || toolName.equals(kConstants.SHAPE_TEXT)) 
      shapeToolband.setEnabled(true);
    else if (kConstants.stringLinearSearch(kConstants.CONNECTOR_NAMES, toolName) >= 0)
      connectorToolband.setEnabled(true);
    else {
      currentFillColor = toolName;
      System.out.println("currentFillColor = "+currentFillColor);
      colorToolband.setEnabled(true);
      graphComponent.getConnectionHandler().setEnabled(true); //put highlights back
    }
    
  }
  /**
   * Doesn't need to know which mode we are ending
   * in any case ending means we go back to default mode
   */
  public void endToolMode() {
    shapeToolband.setEnabled(false);
    connectorToolband.setEnabled(false);
    colorToolband.setEnabled(false);
    //reactivate all other mouse event systems:
    rubberband.setEnabled(true);
    graphComponent.getGraphHandler().setCloneEnabled(true);
    graphComponent.getGraphHandler().setMoveEnabled(true);
    graphComponent.getGraphHandler().setSelectEnabled(true);
    graphComponent.getConnectionHandler().setEnabled(true);
    setCursor(null); //clean up
    toolMode = null;
    eventSource.fireEvent(new mxEventObject(kEvent.TOOL_END));
    System.out.println("ENDtoolmode");
  }

  /**
   * Remembers which tool is currently activated
   * (we could check the DrawingHeader/Toolbar but that takes too long)
   * @return
   */
  public String getToolMode() {
    return toolMode;
  }
  
  /*
   * Redirected to event source
   */

  /**
   * @return Returns true if event dispatching is enabled in the event source.
   * @see com.mxgraph.util.mxEventSource#isEventsEnabled()
   */
  public boolean isEventsEnabled()
  {
    return eventSource.isEventsEnabled();
  }

  /**
   * @param eventsEnabled
   * @see com.mxgraph.util.mxEventSource#setEventsEnabled(boolean)
   */
  public void setEventsEnabled(boolean eventsEnabled)
  {
    eventSource.setEventsEnabled(eventsEnabled);
  }

  /**
   * @param eventName
   * @param listener
   * @see com.mxgraph.util.mxEventSource#addListener(java.lang.String, com.mxgraph.util.mxEventSource.mxIEventListener)
   */
  public void addListener(String eventName, mxIEventListener listener)
  {
    eventSource.addListener(eventName, listener);
  }

  /**
   * @param listener Listener instance.
   */
  public void removeListener(mxIEventListener listener)
  {
    eventSource.removeListener(listener);
  }

  /**
   * @param eventName Name of the event.
   * @param listener Listener instance.
   */
  public void removeListener(mxIEventListener listener, String eventName)
  {
    eventSource.removeListener(listener, eventName);
  }

  /**
   * This class handles drawing shapes and connectors according to 
   * mouse-specified position and dimensions.
   * It inherits from mxRubberband for the conveniently ready-made drawing 
   * and event handling implementations.  The actual actions performed 
   * upon event firings is different of course, so those methods are
   * overridden.
   * @author achang
   */
  protected static class ShapeToolband extends mxRubberband {

    /**
     * Reference to the enclosing drawing component.
     */
    DrawingArea drawingArea;
    
    /**
     * Constructor.  The super class constructor handles
     * 1. adding this to the graphComponent
     * 2. the escape key listener
     * This overrides the colors, and the paint method to remove the fill
     * @param graphComponent
     * @author achang
     */
    public ShapeToolband(final DrawingArea drawingArea) 
    {
      super(drawingArea.getGraphComponent());
      this.drawingArea = drawingArea;
      borderColor = Color.GRAY;
      fillColor = new Color(170,170,170,70); //half-transparent gray
    }
    //TODO implement shiftkey = square boundary
    //TODO our lovely cellmarker is pretty forkin' ugly

    /**
     * Overriding mouseMoved to ensure we have the right cursor.
     * @see com.mxgraph.swing.handler.mxRubberband#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e)
    {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger())
      {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        e.consume();
      }
    }
    
    /**
     * Overriding mousePressed to edit the triggering conditionals.
     * All events originate from graphComponent.graphControl
     * @see com.mxgraph.swing.handler.mxRubberband#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) 
    {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger())
      {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        start(e.getPoint());
        e.consume();
      }      
    }

    public void mouseReleased(MouseEvent e) 
    {
      Rectangle rect = bounds;
      reset();
      
      if (!e.isConsumed() && rect != null && 
          drawingArea.getToolMode() != null &&
          graphComponent.isSignificant(rect.width, rect.height))
      {
//        System.out.println("is shapetoolband.mouseReleased doing anything?");
        String style = "";
        String toolMode = drawingArea.getToolMode();
        style = mxUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, Theme.get(drawingArea.getCurrentFillColor()));
        style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, Theme.get(drawingArea.getCurrentFillColor()));
        style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Theme.get(drawingArea.getCurrentFillColor()+".text"));
        
        if (toolMode.equals(kConstants.SHAPE_NAMES[1])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, mxConstants.SHAPE_ELLIPSE);
        } else if (toolMode.equals(kConstants.SHAPE_NAMES[2])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, mxConstants.SHAPE_RHOMBUS);
        } else if (toolMode.equals(kConstants.SHAPE_NAMES[3])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, kConstants.SHAPE_STAR);
        } else if (toolMode.equals(kConstants.SHAPE_NAMES[4])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, kConstants.SHAPE_AUDIO);
        } else if (toolMode.equals(kConstants.SHAPE_NAMES[5])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, kConstants.SHAPE_KEYBOARD);
        } else if (toolMode.equals(kConstants.SHAPE_NAMES[6])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, kConstants.SHAPE_PERSON);
        }
        else
        if (toolMode.equals(kConstants.SHAPE_TEXT)) {
          //this works because the canvas will draw nothing but the label if we set style=junk
          //note this also overrides the earlier assignment of colors
          style = kConstants.SHAPE_TEXT; 
        }
        else if (!toolMode.equals(kConstants.SHAPE_NAMES[0])) //no style means rectangles
        {
          System.err.println("Magic! You managed to select a tool that doesn't exist! Please report this bug: class=ShapeToolband, toolMode="+toolMode);
        }
        
        //----------------
        mxGraph graph = graphComponent.getGraph();
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try
        {
//          System.out.println("style="+style);
          
          Object cell = graph.insertVertex(parent, null, "tool-made", 
                             rect.x, rect.y, 
                             rect.width, rect.height, style);
          
//          System.out.println("cell.style="+graph.getModel().getStyle(cell));
        }
        finally
        {
          graph.getModel().endUpdate();
        }
        //----------------
        
        e.consume();
        
        //reset tool when done
        graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
        drawingArea.endToolMode();
      }
    }
    
    /**
     * Debugging: Print out cell details onMouseOver. Stolen from mxgraph.examples.GraphEditor.
     * @param cell
     * @return
     * @deprecated
     */
    protected String getToolTipForCell(Object cell)
    {
      NumberFormat numberFormat = NumberFormat.getInstance();
      
      String tip = "<html>";
      mxGeometry geo = graphComponent.getGraph().getModel().getGeometry(cell);
      mxCellState state = graphComponent.getGraph().getView().getState(cell);

      if (graphComponent.getGraph().getModel().isEdge(cell))
      {
        tip += "points={";

        if (geo != null)
        {
          List<mxPoint> points = geo.getPoints();

          if (points != null)
          {
            Iterator<mxPoint> it = points.iterator();

            while (it.hasNext())
            {
              mxPoint point = it.next();
              tip += "[x=" + numberFormat.format(point.getX())
                  + ",y=" + numberFormat.format(point.getY())
                  + "],";
            }

            tip = tip.substring(0, tip.length() - 1);
          }
        }

        tip += "}<br>";
        tip += "absPoints={";

        if (state != null)
        {

          for (int i = 0; i < state.getAbsolutePointCount(); i++)
          {
            mxPoint point = state.getAbsolutePoint(i);
            tip += "[x=" + numberFormat.format(point.getX())
                + ",y=" + numberFormat.format(point.getY())
                + "],";
          }
          tip = tip.substring(0, tip.length() - 1);
          tip+= "style=[" + state.getStyle()+"]";

        }

        tip += "}";
      }
      else
      {
        tip += "geo=[";

        if (geo != null)
        {
          tip += "x=" + numberFormat.format(geo.getX()) + ",y="
              + numberFormat.format(geo.getY()) + ",width="
              + numberFormat.format(geo.getWidth()) + ",height="
              + numberFormat.format(geo.getHeight());
        }

        tip += "]<br>";
        tip += "state=[";

        if (state != null)
        {
          tip += "x=" + numberFormat.format(state.getX()) + ",y="
              + numberFormat.format(state.getY()) + ",width="
              + numberFormat.format(state.getWidth())
              + ",height="
              + numberFormat.format(state.getHeight())
              + ", style="
              + state.getStyle();
        }

        tip += "]";
      }

      mxPoint trans = graphComponent.getGraph().getView().getTranslate();

      tip += "<br>scale=" + numberFormat.format(graphComponent.getGraph().getView().getScale())
          + ", translate=[x=" + numberFormat.format(trans.getX())
          + ",y=" + numberFormat.format(trans.getY()) + "]";
      tip += "</html>";

      System.out.println(tip);
      return tip;
    }
    
  }

  /**
   * The difference here, from ShapeToolband, is only in the mouseDragged image
   * (i.e. paint()) and the action upon mouseRelease.
   * @author achang
   */
  protected static class ConnectorToolband extends ShapeToolband {

    /**
     * Constructor: just use the super class constructor.
     */
    public ConnectorToolband(final DrawingArea drawingArea) {
      super(drawingArea);
    }

    /**
     * We're not making a rubberband, but override the paint method anyway.
     */
    public void paintRubberband(Graphics g)
    {
      if (first != null && bounds != null
          && graphComponent.isSignificant(bounds.width, bounds.height))
      {
        Rectangle rect = new Rectangle(bounds);
        g.setColor(borderColor);
        rect.width -= 1;
        rect.height -= 1;
        g.drawLine(rect.x, rect.y, rect.x+rect.width, rect.y+rect.height);
      }
    }
    
    public void mouseReleased(MouseEvent e) 
    {
      Rectangle rect = bounds;
      reset();
      
      if (!e.isConsumed() && rect != null && 
          drawingArea.getToolMode() != null &&
          graphComponent.isSignificant(rect.width, rect.height))
      {
        String style = "";
        String toolMode = drawingArea.getToolMode();
        
        if (toolMode.equals(kConstants.CONNECTOR_NAMES[0])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
          style = mxUtils.setStyle(style, mxConstants.STYLE_ENDARROW, mxConstants.NONE);
        } else if (toolMode.equals(kConstants.CONNECTOR_NAMES[1])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
        } else if (toolMode.equals(kConstants.CONNECTOR_NAMES[2])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE, mxConstants.SHAPE_CONNECTOR);
          style = mxUtils.setStyle(style, mxConstants.STYLE_DASHED, "1");
        }
        else
        {
          System.err.println("Magic! You managed to select a tool that doesn't exist! Please report this bug: class=ConnectorToolband, toolMode="+toolMode);
        }
        
        //----------------
        mxGraph graph = graphComponent.getGraph();
        graph.getModel().beginUpdate();
        try
        {
          String value = "edge please";
          mxGeometry geometry = new mxGeometry(rect.x, rect.y, rect.width, rect.height);
          geometry.setTerminalPoint(new mxPoint(rect.x, rect.y), true);
          geometry.setTerminalPoint(new mxPoint(rect.x+rect.width, rect.y+rect.height), false);
          geometry.setRelative(true);
          //TODO this geometry gets bad if I drag from right to left

          mxCell cell = new mxCell(value, geometry, style);
          cell.setEdge(true);

          graph.addCell(cell);
        }
        finally
        {
          graph.getModel().endUpdate();
        }
        //----------------
        
        e.consume();
        
        //reset tool when done
        graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
        drawingArea.endToolMode();
      }
    }
   
  }
  
  protected static class ColorToolband extends MouseAdapter implements MouseMotionListener {
    
    /**
     * Reference to the enclosing drawing component.
     */
    DrawingArea drawingArea;
    
    /**
     * Reference to the enclosing graph container.
     */
    protected mxGraphComponent graphComponent;

    /**
     * Specifies if the handler is enabled.
     */
    protected boolean enabled = true;

    /**
     * Constructor.
     * @param drawingArea
     */
    public ColorToolband(final DrawingArea drawingArea)
    {
      this.graphComponent = drawingArea.getGraphComponent();
      this.drawingArea = drawingArea;
      this.enabled = false;
      
      // Install ourselves as a listener
      graphComponent.getGraphControl().addMouseListener(this);
      graphComponent.getGraphControl().addMouseMotionListener(this);
    }
    
    /**
     * Returns the enabled state.
     */
    public boolean isEnabled()
    {
      return enabled;
    }

    /**
     * Sets the enabled state.
     */
    public void setEnabled(boolean enabled)
    {
      this.enabled = enabled;
    }
    
    /**
     * Overriding mousePressed to edit the triggering conditionals.
     * All events originate from graphComponent.graphControl
     * @see com.mxgraph.swing.handler.mxRubberband#mousePressed(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e)
    {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger())
      {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        e.consume();
      }
    }
    
    public void mouseReleased(MouseEvent e) {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger())
      {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        mxCell cell = (mxCell) graphComponent.getCellAt(e.getX(), e.getY());
        if (cell != null) {
          
          mxGraph graph = graphComponent.getGraph();
          String style = cell.getStyle();
          style = mxUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, Theme.get(drawingArea.toolMode));
          style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, Theme.get(drawingArea.toolMode));
          style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Theme.get(drawingArea.toolMode+".text"));
          graph.setCellStyle(style, new Object[] {cell});
          
        }
        e.consume();
        
        //reset tool when done
        graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
        drawingArea.endToolMode();
      }    
    }

    public void mouseDragged(MouseEvent e) {
      //do nothing, but need this since we implement MouseMotionListener
    }
    
  }

}
