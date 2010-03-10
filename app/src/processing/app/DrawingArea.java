package processing.app;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import processing.app.util.kEvent;

import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxEventSource.mxIEventListener;
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
  ToolHandler toolband;
  
  public DrawingArea(mxGraphModel x) {
    super();
    // TODO Auto-generated constructor stub
    //<!------------- hello world crap
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
    
    graphComponent = new mxGraphComponent(graph);
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
    toolband = new ToolHandler(this);
    toolband.setEnabled(false);
  }
  
  public mxGraphComponent getGraphComponent() {
    return graphComponent;
  }

  public void beginToolMode (String name) {
    //change cursor
    //set mode = name
    eventSource.fireEvent(new mxEventObject(kEvent.TOOL_BEGIN, "tool", name));
    System.out.println("BEGINtoolmode "+name);
    setCursor(new Cursor(Cursor.CROSSHAIR_CURSOR));
    toolband.setEnabled(true);
    rubberband.setEnabled(false);
//    graphComponent.getGraphControl().addMouseListener(toolband);
//    graphComponent.getGraphControl().addMouseMotionListener(toolband);
  }
  /**
   * Doesn't need to know which mode we are ending
   * in any case ending means we go back to default mode
   */
  public void endToolMode() {
    //change cursor
    //set mode = null
    //removeListener(whicheverTool's adaptor)
    //fire Event.TOOL_END
//    graphComponent.getGraphControl().removeMouseListener(toolband);
//    graphComponent.getGraphControl().removeMouseMotionListener(toolband);
    toolband.setEnabled(false);
    rubberband.setEnabled(true);
    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
    eventSource.fireEvent(new mxEventObject(kEvent.TOOL_END));
    System.out.println("ENDtoolmode");
  }
  
  //
  // Redirected to event source
  //

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
  private static class ToolHandler extends mxRubberband {
    
    DrawingArea drawingArea;
    
    /**
     * Constructor.  The super class constructor handles
     * 1. adding this to the graphComponent
     * 2. the escape key listener
     * This overrides the colors, and the paint method to remove the fill
     * @param graphComponent
     * @author achang
     */
    public ToolHandler(DrawingArea drawingArea) 
    {
      super(drawingArea.getGraphComponent());
      this.drawingArea = drawingArea;
      borderColor = Color.GRAY;
      fillColor = new Color(170,170,170,70); //half-transparent gray
    }

    /**
     * Overriding mousePressed to edit the triggering conditionals. 
     * @see com.mxgraph.swing.handler.mxRubberband#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) 
    {
      System.out.println("mousePressed-- "+e.getSource().getClass());
      if (!e.isConsumed() && isEnabled())// && isRubberbandTrigger(e) && !e.isPopupTrigger())
      {
        System.out.println(e.getSource().getClass());
        start(e.getPoint());
        e.consume();
      }      
    }

    public void mouseReleased(MouseEvent e) 
    {
      System.out.println("mouseReleased-- "+e.getSource().getClass());
      Rectangle rect = bounds;
      reset();
      if (!e.isConsumed() && rect != null && 
          graphComponent.isSignificant(rect.width, rect.height))
      {
        mxGraph graph = graphComponent.getGraph();
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try
        {
          graph.insertVertex(parent, null, "tool-made", 
                             rect.getX(), rect.getY(), 
                             rect.getWidth(), rect.getHeight());
        }
        finally
        {
          graph.getModel().endUpdate();
        }
        
        e.consume();
      }
      
      drawingArea.endToolMode();
    }

    //I think this only handles repaint while dragging so we don't need
    //to worry about it actually triggering anything
    //this is called continuously while the mouse is dragging
//    public void mouseDragged(MouseEvent e) {
//      System.out.println("mouseDragged-- "+e.getSource().getClass());
//      if (!e.isConsumed() && first != null)
//      {
//        bounds = new Rectangle(first);
//        bounds.add(e.getPoint());
//
//        System.out.println(bounds);
//        
//        e.consume();
//      }
//    }
    
  }
}
