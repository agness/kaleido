package processing.app;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.BadLocationException;

import processing.app.graph.kCellValue;
import processing.app.graph.kCodeWindow;
import processing.app.graph.kGraph;
import processing.app.graph.kGraphComponent;
import processing.app.util.kConstants;
import processing.app.util.kEvent;
import processing.app.util.kUtils;

import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.swing.handler.mxCellMarker;
import com.mxgraph.swing.handler.mxConnectionHandler;
import com.mxgraph.swing.handler.mxRubberband;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxGraphSelectionModel;
import com.mxgraph.view.mxGraphView;
import com.mxgraph.view.mxPerimeter.mxPerimeterFunction;

/**
 * The Kaleido drawing JComponent, which is a desktop pane that contains the
 * graph component, and any number of internal code windows. This class
 * comprises the bulk mechanisms for the code window system.
 * 
 * @author achang
 */
public class DrawingArea extends JDesktopPane {

  Editor editor;

  JInternalFrame graphPanel;

  mxGraphComponent graphComponent;

  mxEventSource eventSource = new mxEventSource(this);

  boolean modified = false;

  /**
   * Making this beforehand so we don't make infinite new objects when swapping
   * between this and the default cursor.
   */ 
  static final Cursor TOOL_CURSOR = new Cursor(Cursor.CROSSHAIR_CURSOR);

  /**
   * Stores which graph editing tool (e.g. "rect [shape]", "dotted [connector]",
   * "text", or paintbucket color name) is currently active. Null if in default
   * selection mode.
   */
  String toolMode;

  /**
   * Stores which color is currently active on the DrawingHeader-toolbar
   * regardless of whether the color tool is active, but is updated when the
   * color tool is activated.
   */
  String currentFillColor = kConstants.COLOR_KEYS[0];

  protected ArrayList<kCodeWindow> codeWindows;

  boolean codeWindowsEnabled = true;

  boolean lockEnabled = true;

  /**
   * Tool and selection handling.
   */
  mxRubberband rubberband;
  ShapeToolband shapeToolband;
  kConnectionHandler connectorToolband;
  ColorToolband colorToolband;
  
  /**
   * Code window document listener, used to mirror edits between code windows and main textarea
   */
  CodeWindowDocListener codeWindowDocListener;

  public DrawingArea(Editor editor) {
    super();
    this.editor = editor;
    mxGraph graph = new kGraph();
    graph.setEdgeLabelsMovable(false); //vertexLabels are by default not movable
    graph.setVertexLabelsMovable(true);//DEBUGGING

    // <!------------- hello world crap TODO remove
    helloWorld(graph);
    // hello world crap -------------<

    graphComponent = new kGraphComponent(graph);
    
    // helloWorld2();

    // change tracker: updates the modified flag if anything in the graph model
    // changes
    graph.getModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        setModified(true);
      }
    });
    // also a change tracker, but if any graph change required repainting (i.e.
    // cells moved, resized, deleted) then update the codeWindows
    graph.addListener(mxEvent.REPAINT, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        repaintCodeWindows();
      }
    });
    // in the particular case of a cell with a code window being deleted
    // on the data layer we need to remove the associated code window
    graph.addListener(mxEvent.CELLS_REMOVED, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        Object[] cells = (Object[]) evt.getProperty("cells");
        kCodeWindow cw;
        for (int i = 0; i < cells.length; i++) {
          cw = getCodeWindow((mxICell) cells[i]);
          if (cw != null) {
            System.out.println("drawarea >> cell removed, removing code window id="+cw.getId());
            codeWindows.remove(cw);
          }
        }
      }
    });
    
    // tool handling
    rubberband = new mxRubberband(graphComponent);
    rubberband.setEnabled(true);
    shapeToolband = new ShapeToolband(this);
    shapeToolband.setEnabled(false);
    connectorToolband = new kConnectionHandler(this);
    connectorToolband.setEnabled(false);
    //hereafter we won't reference graphComponent.connectionHandler 
    //and instead will just treat it as "connectorToolband"
    ((kGraphComponent) graphComponent).setConnectionHandler(connectorToolband); 
    colorToolband = new ColorToolband(this);
    colorToolband.setEnabled(false);
    
    codeWindowDocListener = new CodeWindowDocListener();

    // compose the swing components
    graphPanel = new JInternalFrame("Graph", false, // resizable
        false, // not closable
        false, // not maximizable
        false); // not iconifiable
    graphPanel.setContentPane(graphComponent);
    graphPanel.pack();
    graphPanel.setVisible(true);
    graphPanel.setLayer(0);
    graphPanel.setBorder(null);
    graphPanel.setSize(3000, 3000); // TODO: be able to resize with the rest of
                                    // them
    add(graphPanel);
    setVisible(true);
  }

  /**
   * For development purposes only.
   * 
   * @deprecated
   * @param graph
   */
  private void helloWorld(mxGraph graph) {
    Object parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
    try {
      String style = "";
      style = mxUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, Integer
          .toHexString(kUtils.getFillColorFromKey(getCurrentFillColorKey())
              .getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, Integer
          .toHexString(kUtils.getFillColorFromKey(getCurrentFillColorKey())
              .getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Integer
          .toHexString(kUtils.getFontColorFromKey(getCurrentFillColorKey())
              .getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_FONTFAMILY,
                               kConstants.DEFAULT_FONTFAMILY);
                           

      Object v1 = graph.insertVertex(parent, null, new kCellValue("superstar",
          "VCR"), 20, 20, 80, 30, style);
      Object v2 = graph.insertVertex(parent, null, new kCellValue("hello",
      ""), 50, 220, 80, 30, style);
      
      style = "";
      style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, Integer
          .toHexString((kConstants.EDGE_STROKE_COLOR).getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Integer
          .toHexString((kConstants.EDGE_FONT_COLOR).getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_FONTFAMILY,
                               kConstants.DEFAULT_FONTFAMILY);
                           

      graph.insertEdge(parent, null, "happy edge", v1, v2, style);
    } finally {
      graph.getModel().endUpdate();
    }

    graph.getSelectionModel().addListener(mxEvent.CHANGE, new mxIEventListener() {
      public void invoke(Object sender, mxEventObject evt) {
        Object[] cells = ((mxGraphSelectionModel) sender).getCells();
        for (int i = 0; i < cells.length; i++) {
          
          //testing for whether cells are locked/lockable...
//          System.out.println("drawingArea >> graph selection changed: isCellLocked="+graphComponent.getGraph().isCellLocked(cells[i]));
//          System.out.println("drawingArea >> graph selection changed: isCellLinked="+((kGraph) graphComponent.getGraph()).isCellLinked(cells[i]));
          System.out.println("drawingArea >> graph selection changed: cellCenter="+graphComponent.getGraph().getView().getState(cells[i]).getCenterX()+","+graphComponent.getGraph().getView().getState(cells[i]).getCenterY());
          
          if (cells[i] instanceof mxICell
              && ((mxICell) cells[i]).getValue() instanceof kCellValue) {
//            System.out.println("drawingArea >> graph selection changed: "+((kCellValue) ((mxICell) cells[i]).getValue()).toPrettyString());
//            System.out.println("drawingArea >> graph selection bounsd: "+graphComponent.getGraph().getCellBounds(cells[i]).getRectangle());
          }
        }
      }
    });
  }
  /**
   * @deprecated
   */
  private void helloWorld2() {

    mxGraphOutline graphOutline = new mxGraphOutline(graphComponent);
//    System.out.println("drawingArea >> graphOutline is fit page >> "
//                       + graphOutline.isFitPage());
    graphOutline.setFitPage(false);
    graphOutline.setFinderVisible(false);
    JInternalFrame newFrame = new JInternalFrame("tester", true, false, false,
        false);
    newFrame.setSize(100, 50); // graphOutline will fit itself into its
                               // container...
    newFrame.setLocation(50, 50);
    newFrame.setVisible(true);
    newFrame.getContentPane().add(graphOutline);
    add(newFrame);
  }

  /**
   * Debugging: Print out cell details onMouseOver. Stolen from
   * mxgraph.examples.GraphEditor.
   * 
   * @param cell
   * @return
   * @deprecated
   */
  private String getToolTipForCell(Object cell) {
    NumberFormat numberFormat = NumberFormat.getInstance();
  
    String tip = "<html>";
    mxGeometry geo = graphComponent.getGraph().getModel().getGeometry(cell);
    mxCellState state = graphComponent.getGraph().getView().getState(cell);
  
    if (graphComponent.getGraph().getModel().isEdge(cell)) {
      tip += "points={";
  
      if (geo != null) {
        List<mxPoint> points = geo.getPoints();
  
        if (points != null) {
          Iterator<mxPoint> it = points.iterator();
  
          while (it.hasNext()) {
            mxPoint point = it.next();
            tip += "[x=" + numberFormat.format(point.getX()) + ",y="
                   + numberFormat.format(point.getY()) + "],";
          }
  
          tip = tip.substring(0, tip.length() - 1);
        }
      }
  
      tip += "}<br>";
      tip += "absPoints={";
  
      if (state != null) {
  
        for (int i = 0; i < state.getAbsolutePointCount(); i++) {
          mxPoint point = state.getAbsolutePoint(i);
          tip += "[x=" + numberFormat.format(point.getX()) + ",y="
                 + numberFormat.format(point.getY()) + "],";
        }
        tip = tip.substring(0, tip.length() - 1);
        tip += "style=[" + state.getStyle() + "]";
  
      }
  
      tip += "}";
    } else {
      tip += "geo=[";
  
      if (geo != null) {
        tip += "x=" + numberFormat.format(geo.getX()) + ",y="
               + numberFormat.format(geo.getY()) + ",width="
               + numberFormat.format(geo.getWidth()) + ",height="
               + numberFormat.format(geo.getHeight());
      }
  
      tip += "]<br>";
      tip += "state=[";
  
      if (state != null) {
        tip += "x=" + numberFormat.format(state.getX()) + ",y="
               + numberFormat.format(state.getY()) + ",width="
               + numberFormat.format(state.getWidth()) + ",height="
               + numberFormat.format(state.getHeight()) + ", style="
               + state.getStyle();
      }
  
      tip += "]";
    }
  
    mxPoint trans = graphComponent.getGraph().getView().getTranslate();
  
    tip += "<br>scale="
           + numberFormat.format(graphComponent.getGraph().getView()
               .getScale()) + ", translate=[x="
           + numberFormat.format(trans.getX()) + ",y="
           + numberFormat.format(trans.getY()) + "]";
    tip += "</html>";
  
    System.out.println(tip);
    return tip;
  }

  public mxGraphComponent getGraphComponent() {
    return graphComponent;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * Tool handling
   */
  
  /**
   * Returns the current state of the color selection; used by ShapeToolband
   * to determine what cell style to create
   * @return
   */
  public String getCurrentFillColorKey() {
    return currentFillColor;
  }

  /**
   * Used only internally, so technically an unnecessary method.
   * @deprecated
   */
  protected void setCurrentFillColor(String currentFillColor) {
    this.currentFillColor = currentFillColor;
  }
  
  /**
   * Fires TOOL_BEGIN event, sets the cursor, and enables toolband mouse drawing
   * instead of rubberband selection.
   * 
   * @param toolName
   */
  public void beginToolMode(final String toolName) {
    System.out.println("BEGINtoolmode " + toolName);
    eventSource
        .fireEvent(new mxEventObject(kEvent.TOOL_BEGIN, "tool", toolName));
    // disable all other graph mouse events for the time being:
    rubberband.setEnabled(false);
    graphComponent.getGraphHandler().setCloneEnabled(false);
    graphComponent.getGraphHandler().setMoveEnabled(false);
    graphComponent.getGraphHandler().setSelectEnabled(false);
//    graphComponent.getConnectionHandler().setEnabled(false); // gets ride of
                                                             // cellMarkers
    // Notes on: cursor setting:
    // turns out we can't set the cursor here because it gets overridden
    // by graphControl's mouse listeners after the 2nd time
    // but we still need to do this to cover the first time we want to
    // change the cursor. Why? Dun ask me. @author achang
    setCursor(TOOL_CURSOR);
    toolMode = toolName;
//TODO enable the ESCAPE_KEY listener that endsToolMode
    if (kUtils.stringLinearSearch(kConstants.SHAPE_KEYS, toolName) >= 0
        || toolName.equals(kConstants.SHAPE_TEXT))
      shapeToolband.setEnabled(true);
    else if (kUtils.stringLinearSearch(kConstants.CONNECTOR_KEYS, toolName) >= 0)
      connectorToolband.setEnabled(true);
    else {
      // update the FillColor
      setCurrentFillColor(toolName);
      System.out.println("currentFillColor = " + currentFillColor);
      colorToolband.setEnabled(true);
//      graphComponent.getConnectionHandler().setEnabled(true); // TODO put highlights
                                                              // back
    }

  }

  /**
   * Fires TOOL_END event, sets all mouse event handling back to normal,
   * and reports which tool just ended, as well as whether the tool ended because
   * it was canceled or because the job completed
   */
  public void endToolMode(boolean success) {
    shapeToolband.setEnabled(false);
    connectorToolband.setEnabled(false);
    colorToolband.setEnabled(false);
    // reactivate all other mouse event systems:
    rubberband.setEnabled(true);
    graphComponent.getGraphHandler().setCloneEnabled(true);
    graphComponent.getGraphHandler().setMoveEnabled(true);
    graphComponent.getGraphHandler().setSelectEnabled(true);
//    graphComponent.getConnectionHandler().setEnabled(true);
    setCursor(null); // clean up
    eventSource.fireEvent(new mxEventObject(kEvent.TOOL_END, "tool", toolMode, "success", (success ? Boolean.TRUE : Boolean.FALSE)));
    toolMode = null;
    System.out.println("ENDtoolmode "+toolMode+" >> success="+success);
  }

  /**
   * Remembers which tool is currently activated (we could check the
   * DrawingHeader/Toolbar but that takes too long)
   * 
   * @return
   */
  public String getToolMode() {
    return toolMode;
  }

  /**
   * Sets the modified value for the drawarea
   */
  public void setModified(boolean state) {
//    System.out.println("drawArea >> setModified >> oldVal = " + modified
//                       + ", newVal = " + state);
    modified = state;
//    editor.updateTitle();
  }

  public boolean isModified() {
    return modified;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /*
   * Code window methods
   */

  /**
   * Enables/disables code windows. TODO verify that only public codewindow
   * methods check this
   */
  public void setCodeWindowsEnabled(boolean value) {
    codeWindowsEnabled = value;
  }

  /**
   * Returns if code windows are enabled.
   */
  public boolean isCodeWindowsEnabled() {
    return codeWindowsEnabled;
  }
   
  /**
   * Repaints all currently open code windows.
   * TODO not super efficient, since we have a way of knowing which cells were changed
   * and only those code windows need to be repainted, not all...
   */
  protected void repaintCodeWindows() {
    if (codeWindows != null) {
      System.out.println("drawarea >> repainting code windows");
      Iterator it = codeWindows.iterator();
      while (it.hasNext())
      {
        kCodeWindow next = (kCodeWindow) it.next();
        if (next.isVisible())
          next.updateTriangle();
      }
    }    
  }

  /**
   * Shortcut method to get a cell when given a String id
   */
  protected Object getCell(String id)
  {
    return ((mxGraphModel) graphComponent.getGraph().getModel()).getCell(id);
  }

  /**
   * Shortcut method to determine if a given cell has valid code marks and can
   * therefore have a valid code window
   * 
   * @param cell
   * @return
   */
  protected boolean hasValidCodeMarks(Object cell) {
    if ((cell instanceof mxCell)
        && (((mxCell) cell).getValue() instanceof kCellValue)
        && (((kCellValue) ((mxCell)cell).getValue()).hasValidCodeMarks()))
      return true;
    else
      return false;
  }

  /**
   * Returns the code window associated with the given cell by performing a
   * linear search through the array of codewindows to match the cell id.
   * Returns null if not found.
   * 
   * @param cell
   * @return
   */
  protected kCodeWindow getCodeWindow(mxICell cell) {
    return getCodeWindow(cell.getId());
  }

  /**
   * Returns the code window associated with the given cell by performing a
   * linear search through the array of codewindows to match the cell id.
   * Returns null if not found.
   * 
   * @param cell
   * @return
   */
  protected kCodeWindow getCodeWindow(String id) {
    if (codeWindows != null) {
      Iterator it = codeWindows.iterator();
      while (it.hasNext()) // System.out.println(it.next());
      {
        kCodeWindow next = (kCodeWindow) it.next();
        if (next.getId().equals(id))
          return next;
      }
    }
    return null; // return null if not found
  }

  /**
   * Shows code window at default (cell center) location. Overridden method
   * creates a code window if one doesn't already exist.
   * 
   * @param cell
   * @param locX
   * @param locY
   */
  protected void showCodeWindow(mxICell cell) {
    showCodeWindow(cell, (int) cell.getGeometry().getCenterX(), (int) cell
        .getGeometry().getCenterY());
  }

  /**
   * Creates a code window if one doesn't already exist.
   * 
   * @param cell
   * @param locX
   * @param locY
   */
  protected void showCodeWindow(mxICell cell, int locX, int locY) {
//    System.out.println("drawarea.showCodeWindow >> id=" + cell.getId() + " val=" + cell.getValue());
    if (hasValidCodeMarks(cell)) {
//      System.out.println("drawarea.showCodeWindow >> hasValidCodeMarks");
      kCodeWindow codewindow = getCodeWindow(cell.getId());
      if (codewindow == null)
        codewindow = addCodeWindow(cell);

      refreshCodeWindowContent(cell);
      codewindow.setLocation(locX, locY);
      codewindow.setVisible(true);
    }
  }
  
  /**
   * Updates the content of the code window textarea regardless of whether the
   * code window is currently visible. This method is called each time the code
   * window is opened
   * 
   * @param cell
   */
  protected void refreshCodeWindowContent(mxICell cell) {
    if (cell.getValue() instanceof kCellValue) {
      kCellValue val = (kCellValue) cell.getValue();
//      System.out.println("drawarea.refreshCodeWindowContent >> id=" + cell.getId() + " val=" + cell.getValue());
      try {
        String content = editor.getSketch().getCode(val.getCodeIndex())
            .getDocument().getText(val.getStartMark(),
                                   val.getStopMark() - val.getStartMark());
//        System.out.println("drawarea.refreshCodeWindowContent >> cw="+getCodeWindow(cell).getId()+" textarea="+getCodeWindow(cell).getTextArea().getClass().getName() + '@' + Integer.toHexString(hashCode()));
        getCodeWindow(cell).getTextArea().setText(content);
      } catch (BadLocationException bl) {
//        System.err.println("drawarea.refreshCodeWindowContent error with cell "+val.toPrettyString());
        bl.printStackTrace();
      }
    }
  }

  /**
   * Hide the code window if it exists for the given cell.
   * 
   * @param cell
   */
  protected void hideCodeWindow(mxICell cell) {
//    System.out.println("drawarea >> hideCodeWindow id="+cell.getId()+" val=" + cell.getValue());
    if (hasValidCodeMarks(cell)) {
//      System.out.println("drawarea >> hideCodeWindow >> hasValidCodeMarks");
      kCodeWindow codewindow = getCodeWindow(cell.getId());
      if (codewindow != null)
        codewindow.setVisible(false);
    }
  }

  /**
   * Creates a new code window internal frame belonging to the specific cell
   * 
   * @param cell
   * @return
   */
  protected kCodeWindow addCodeWindow(mxICell cell) {
    kCodeWindow newWindow = new kCodeWindow(cell, this);
//    System.out.println("drawarea >> addCodeWindow id="+cell.getId()+" val="+cell.getValue()+" textarea="+newWindow.getTextArea().getClass().getName() + '@' + Integer.toHexString(hashCode()));
    
    if (codeWindows == null) {
      codeWindows = new ArrayList<kCodeWindow>();
//      System.out
//          .println("drawarea >> addCodeWindow >> added new code windows ArrayList");
    }
    codeWindows.add(newWindow);
    newWindow.getTextArea().getDocument().addDocumentListener(codeWindowDocListener);
    return newWindow;
  }
  
  /**
   * Given a swing document, this method returns the code window in which 
   * the document is contained, if said code window exists and is open.
   * It does a linear search, and returns null if not found.
   * @param document
   * @return
   */
  protected kCodeWindow getCodeWindowOfDocument(javax.swing.text.Document document) {
    if (codeWindows != null) {
      Iterator it = codeWindows.iterator();
      while (it.hasNext()) // System.out.println(it.next());
      {
        kCodeWindow next = (kCodeWindow) it.next();
        if (next.getTextArea().getDocument() == document && next.isVisible()) 
          return next;
      }
    }
    return null; // return null if not found
  }

  /**
   * Returns if a given cell's code window exists and is open
   * 
   * @return if any code window is open
   */
  protected boolean isCodeWindowOpen(mxICell cell) {
    if (hasValidCodeMarks(cell)) {
//      System.out.println("drawingArea >> isCodeWindowOpen >> hasValidCodeMarks");
      kCodeWindow codewindow = getCodeWindow(cell.getId());
      if (codewindow != null)
        return codewindow.isVisible();
    }
    return false;
  }

  /**
   * Returns if a given cell's code window exists and is open
   * 
   * @return if any code window is open
   */
  public boolean isCodeWindowOpenOnSelected() {
    if (codeWindowsEnabled) {
      Object[] selected = graphComponent.getGraph().getSelectionCells();
      for (int i = 0; i < selected.length; i++)
        if (isCodeWindowOpen((mxICell) selected[i]))
          return true;
    }
    return false;
  }

  /**
   * Makes visible the associated code window of each selected cell.
   */
  public void openCodeWindowOnSelected() {
    if (codeWindowsEnabled) {
      Object[] selected = graphComponent.getGraph().getSelectionCells();
      for (int i = 0; i < selected.length; i++)
        showCodeWindow((mxICell) selected[i]);
    }
  }

  /**
   * Makes visible the associated code window of each selected cell.
   */
  public void closeCodeWindowOnSelected() {
    if (codeWindowsEnabled) {
      Object[] selected = graphComponent.getGraph().getSelectionCells();
      for (int i = 0; i < selected.length; i++)
        hideCodeWindow((mxICell) selected[i]);
    }
  }
  
  /**
   * Returns whether or not the current selection contains an edge
   * @return
   */
  public boolean isSelectionContainEdge() {
    Object[] selected = graphComponent.getGraph().getSelectionCells();
    for (int i = 0; i < selected.length; i++)
      if (((mxICell) selected[i]).isEdge())
        return true;
    return false;
  }

  /**
   * Closes all code windows.
   */
  public void closeAllCodeWindows() {
    if (codeWindowsEnabled && codeWindows != null) {
      Iterator it = codeWindows.iterator();
      while (it.hasNext())
        ((kCodeWindow) it.next()).setVisible(false);
    }
  }

  /**
   * Called by codewindows to trigger an event that drawingHeader can listen for
   * and thus update the codeWindowButton's status
   */
  public void fireCodeWindowEvent() {
    eventSource.fireEvent(new mxEventObject(kEvent.CODE_WINDOW_VISIBILITY_CHANGE));
  }

  /**
   * Called by editor to have this drawingArea to find the relevant code
   * window(s) and mirror the given edit.
   * 
   * General algorithm:
   * go thru all cells
   * if hasvalidCodeMarks && same sketch index
   *    if intersects
   *      update code marks
   *    else if later in document than event
   *      shift code marks
   *    if either of these cases have have open windows
   *      refresh code window content
   */
  public void mirrorDocEdit(int sketchInd, int sketchOffset, DocumentEvent e,
                            String change) {
    System.out
    .println("drawarea.mirrorDocEdit >> make sure we received everything sketchInd="
             + sketchInd+" sketchOffset="+sketchOffset+" event="+e+" change="+change);
    
    EventType type = e.getType();
    
    Object[] cells = mxGraphModel.getChildren(graphComponent.getGraph()
        .getModel(), graphComponent.getGraph().getDefaultParent());
    
    for (int i = 0; i < cells.length; i++)
      if (cells[i] instanceof mxICell
          && ((mxICell) cells[i]).getValue() instanceof kCellValue
          && ((kCellValue) ((mxICell) cells[i]).getValue()).hasValidCodeMarks()
          && ((kCellValue) ((mxICell) cells[i]).getValue()).getCodeIndex() == sketchInd) {
        
        kCellValue val = ((kCellValue) ((mxICell) cells[i]).getValue());
        
        if (type == EventType.INSERT) {
          System.out.println("drawarea.mirrorDocEdit >> type=INSERT");
          
          //if the insertion point falls into the range of the cell,
          //then expand the code mark range to include the edit
          if ((val.getStartMark() <= sketchOffset) && (val.getStopMark() >= sketchOffset)) {
            val.setStopMark(val.getStopMark()+e.getLength());
            refreshCodeWindowContent((mxICell) cells[i]);
            
          //else if the insertion point is before the cell
          //shift both code marks to accommodate the extra characters
          } else if (val.getStartMark() > sketchOffset) {
            val.shiftCodeMarks(e.getLength());
            refreshCodeWindowContent((mxICell) cells[i]);
          }
        } else if (type == EventType.REMOVE) {
          int changeStop = sketchOffset+e.getLength();
   
          //if the entire contents of the cell is contained in the
          //range of edit, disconnect the cell: invalidate its code 
          //marks and close/delete the code window if there is one
          if ((val.getStartMark() >= sketchOffset) && (val.getStopMark() <= changeStop)) {
            val.invalidateCodeMarks();
            //TODO figure out how to repaint just one cell
            kCodeWindow cw = getCodeWindow((mxICell) cells[i]);
            if (cw != null) {
              cw.setVisible(false);
              codeWindows.remove(cw);
            }
          }  
          //if the latter half of the cell range overlaps with the first
          //half of the range of edit, truncate the tail cell code mark to
          //the beginning of range of edit
          else if ((val.getStartMark() < sketchOffset) && (val.getStopMark() > sketchOffset) && (val.getStopMark() <= changeStop)) {
            val.setStopMark(sketchOffset);
            refreshCodeWindowContent((mxICell) cells[i]);
          }
          //if the first half of the cell range overlaps with the latter
          //half of the range of edit, trim the front cell code mark to
          //the end of the range of edit
          else if ((val.getStartMark() >= sketchOffset) && (val.getStartMark() < changeStop) && (val.getStopMark() > changeStop)) {
            int newLength = val.getStopMark()-changeStop;
            val.setStartMark(sketchOffset);
            val.setStopMark(sketchOffset+newLength);
            refreshCodeWindowContent((mxICell) cells[i]);
          }
          //if the entire contents of the cell is larger than the range 
          //of edit at both head and tail, then shorten the code marks with
          //the corresponding number of characters removed
          else if ((val.getStartMark() < sketchOffset) && (val.getStopMark() > changeStop)) {
            val.setStopMark(val.getStopMark()-e.getLength());
            refreshCodeWindowContent((mxICell) cells[i]);
          }
          //if the entire contents of the cell is located later in the
          //document, then shift both code marks forward
          else if (val.getStartMark() > changeStop) {
            val.shiftCodeMarks(0-e.getLength());
            refreshCodeWindowContent((mxICell) cells[i]);
          }
        }
      }
    
  }
  
  /**
   * Listens for edits inside all code windows. If the source code window has the
   * UI focus, then this drawArea will fire a kEvent.CODE_WINDOW_DOCUMENT_CHANGE
   * which will notify the editor to mirror the change in the main textarea.
   */
  public class CodeWindowDocListener implements DocumentListener {

    public void insertUpdate(DocumentEvent e) {
        handleUpdate(e);
    }
    
    public void removeUpdate(DocumentEvent e) {
        handleUpdate(e);
    }

    public void changedUpdate(DocumentEvent e) {
      //never called by simple text components
    }  
    
    private void handleUpdate(DocumentEvent e) {
      javax.swing.text.Document document = (javax.swing.text.Document) e.getDocument();
      kCodeWindow codeWindow = getCodeWindowOfDocument(document);
      // TODO dunno why but the first time this DocListener is added it
      // executes/handles update, which means it finds a null codeWindow (when
      // initially refreshing content?)
      // System.out.println("drawArea.CodeWindowDocListener >> codeWindow="+codeWindow);
      
//      System.out.println("drawarea.handleUpdate >> "+e+" cw.id="+((codeWindow != null)? codeWindow.getId() : codeWindow));
      
      if (codeWindow != null && codeWindow.getTextArea().isFocusOwner()) { 

        kCellValue val = (kCellValue) ((mxICell) getCell(codeWindow.getId())).getValue();
        int sketchInd = val.getCodeIndex();
        int sketchOffset = val.getStartMark() + e.getOffset();
        String change;
        if (e.getType() == EventType.INSERT)
          try {
            change = document.getText(e.getOffset(), e.getLength());
//            System.out.println("drawarea.handleUpdate >> string change="+change);
          } catch (BadLocationException e1) {
            e1.printStackTrace();
            change = null;
          }
        else
          change = null;
        
        //update codemarks here
        val.setStopMark(val.getStartMark()+document.getLength());
        
        System.out
            .println("drawArea.CodeWindowDocListener >> fire kEvent.CODE_WINDOW_DOCUMENT_CHANGE sketchInd="
                     + sketchInd+" sketchOffset="+sketchOffset+" event="+e+" change="+change);
        eventSource.fireEvent(new mxEventObject(
            kEvent.CODE_WINDOW_DOCUMENT_CHANGE, "sketchInd", sketchInd,
            "sketchOffset", sketchOffset, "event", e, "change", change));
      }
        
    }
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * Lock methods
   */

  /**
   * Returns if a given cell's code window exists and is open
   * 
   * @return if any code window is open
   */
  public boolean isSelectionLocked() {
    if (lockEnabled) {
      Object[] selected = graphComponent.getGraph().getSelectionCells();
      for (int i = 0; i < selected.length; i++)
        if (graphComponent.getGraph().isCellLocked(selected[i]))
          return true;
    }
    return false;
  }

  /**
   * Locks all of the selected cells.
   * 
   * @see com.mxgraph.view.mxGraph#isCellsLocked()
   */
  public void lockSelected() {
    System.out.println("drawarea >> locking selected");
    if (lockEnabled) {
      lockCells(null);
    }
  }

  /**
   * Unlocks all of the selected cells.
   * 
   * @see com.mxgraph.view.mxGraph#isCellsLocked()
   */
  public void unlockSelected() {
    System.out.println("drawarea >> UN-locking selected");
    if (lockEnabled) {
      unlockCells(null);
    }
  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * Linking business
   */
  
  /**
   * Links the specified cells by setting the code marks and updating the label
   * object, as well as synchronizing the style state to the state of the code
   * marks. If no cells are given, then the selection cells are changed.
   */
  public Object[] linkCells(Object[] cells, int start, int stop, int ind) {
    if (cells == null) {
      cells = graphComponent.getGraph().getSelectionCells();
    }
    
    System.out.println("drawarea.linkCells >>");

    for (int i = 0; i < cells.length; i++)
      if (cells[i] instanceof mxICell
          && ((mxICell) cells[i]).getValue() instanceof kCellValue) {

        ((kCellValue) ((mxICell) cells[i]).getValue()).setCodeMark(start, stop,
                                                                   ind);
        graphComponent.labelChanged(cells[i], ((mxICell) cells[i]).getValue(),
                                    null);

        String value = ((kCellValue) ((mxICell) cells[i]).getValue())
            .hasValidCodeMarks() ? "true" : "false";
        graphComponent.getGraph().setCellStyles(kConstants.STYLE_LINKED, value,
                                                new Object[] {cells[i]});
      }

    return cells;
  }
  
  /**
   * Disconnects the link of the given cells by using linkCells to set all code
   * marks to invalid marks.
   * @param cells
   * @return
   */
  public Object[] unlinkCells(Object[] cells) {
    System.out.println("drawarea.unlinkCells >>");
    return linkCells(cells, -1, -1, -1);
  }
  
  /**
   * "Locks" given cells by setting their styles to locked, which will draw them
   * in that way. Based on the implementation in mxGraph.setCellStyles, if no
   * cells are given, then the selection cells are changed.
   * 
   * @param cells
   * @return
   */
  public Object[] lockCells(Object[] cells) {
    System.out.println("drawarea.lockCells >>");
    return graphComponent.getGraph().setCellStyles(kConstants.STYLE_LOCKED,
                                                   "true", cells);
  }
  
  /**
   * "Locks" given cells by setting their locked styles to false, which will
   * draw them in that way. Based on the implementation in
   * mxGraph.setCellStyles, if no cells are given, then the selection cells are
   * changed.
   * 
   * @param cells
   * @return
   */
  public Object[] unlockCells(Object[] cells) {
    System.out.println("drawarea.unlockCells >>");
    return graphComponent.getGraph().setCellStyles(kConstants.STYLE_LOCKED,
                                                   "false", cells);
  }
  
  /**
   * Returns whether or not any of the selected cells is connected to code
   */
  public boolean isSelectionLinked() {
    Object[] selected = graphComponent.getGraph().getSelectionCells();
    for (int i = 0; i < selected.length; i++)
      if (((kGraph) graphComponent.getGraph()).isCellLinked(selected[i]))
        return true;
    return false;
  }
  
  /**
   * Selects all cells that have codemarks that intersect with the specified
   * code range of the given code index.
   */
  public void selectCodeIntersection(int start, int stop, int ind) {
    graphComponent.getGraph().getSelectionModel().clear();
    Object[] cells = mxGraphModel.getChildren(graphComponent.getGraph()
        .getModel(), graphComponent.getGraph().getDefaultParent());
    System.out.println("drawArea >> selectCodeIntersection allCells.length="
                       + cells.length + " start=" + start + " stop=" + stop);
    for (int i = 0; i < cells.length; i++)
      if (cells[i] instanceof mxICell
          && ((mxICell) cells[i]).getValue() instanceof kCellValue) {
        kCellValue val = ((kCellValue) ((mxICell) cells[i]).getValue());
        System.out.println("drawArea >> selectCodeIntersection val="
                           + val.toPrettyString());
        if (val.hasValidCodeMarks()
            && val.getCodeIndex() == ind
            && ((start > val.getStartMark() && start < val.getStopMark())
                || (stop > val.getStartMark() && stop < val.getStopMark())
                || (start == val.getStartMark() && stop == val.getStopMark())
                || (stop == val.getStartMark() && start == val.getStopMark())))
          graphComponent.getGraph().getSelectionModel().addCell(cells[i]);
      }
    System.out
        .println("drawArea >> selectCodeIntersection selectionCells.length="
                 + graphComponent.getGraph().getSelectionCount());
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  
  /*
   * Redirected to event source
   */

  /**
   * @return Returns true if event dispatching is enabled in the event source.
   * @see com.mxgraph.util.mxEventSource#isEventsEnabled()
   */
  public boolean isEventsEnabled() {
    return eventSource.isEventsEnabled();
  }

  /**
   * @param eventsEnabled
   * @see com.mxgraph.util.mxEventSource#setEventsEnabled(boolean)
   */
  public void setEventsEnabled(boolean eventsEnabled) {
    eventSource.setEventsEnabled(eventsEnabled);
  }

  /**
   * @param eventName
   * @param listener
   * @see com.mxgraph.util.mxEventSource#addListener(java.lang.String,
   *      com.mxgraph.util.mxEventSource.mxIEventListener)
   */
  public void addListener(String eventName, mxIEventListener listener) {
    eventSource.addListener(eventName, listener);
  }

  /**
   * @param listener
   *          Listener instance.
   */
  public void removeListener(mxIEventListener listener) {
    eventSource.removeListener(listener);
  }

  /**
   * @param eventName
   *          Name of the event.
   * @param listener
   *          Listener instance.
   */
  public void removeListener(mxIEventListener listener, String eventName) {
    eventSource.removeListener(listener, eventName);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  /*
   * Tool handling classes
   */
  
  /**
   * This class handles drawing shapes and connectors according to
   * mouse-specified position and dimensions. It inherits from mxRubberband for
   * the conveniently ready-made drawing and event handling implementations. The
   * actual actions performed upon event firings is different of course, so
   * those methods are overridden.
   * 
   * @author achang
   */
  protected static class ShapeToolband extends mxRubberband {

    /**
     * Reference to the enclosing drawing component.
     */
    DrawingArea drawingArea;

    /**
     * Constructor. The super class constructor handles 1. adding this to the
     * graphComponent 2. the escape key listener This overrides the colors, and
     * the paint method to remove the fill
     * 
     * @param graphComponent
     * @author achang
     */
    public ShapeToolband(final DrawingArea drawingArea) {
      super(drawingArea.getGraphComponent());
      this.drawingArea = drawingArea;
      borderColor = kConstants.SHAPE_PREVIEW_BORDER_COLOR;
      fillColor = kConstants.SHAPE_PREVIEW_FILL_COLOR; // half-transparent gray
    }

    // TODO minor: implement shiftkey = square boundary
    // TODO our lovely cellmarker is pretty forkin' ugly
    // TODO public void paintRubberband() could use kConstants.PREVIEW_STROKE for visual consistency?
    // or the other way around...

    /**
     * TODO we can tell graphcomponent to set its cursor in beginToolMode (and
     * reset it in endToolMode)
     * 
     * Overriding mouseMoved to ensure we have the right cursor.
     * 
     * @see com.mxgraph.swing.handler.mxRubberband#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger()) {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        e.consume();
      }
    }

    /**
     * Overriding mousePressed to edit the triggering conditionals. All events
     * originate from graphComponent.graphControl
     * 
     * @see com.mxgraph.swing.handler.mxRubberband#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger()) {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        start(e.getPoint());
        e.consume();
      }
    }

    public void mouseReleased(MouseEvent e) {
      Rectangle rect = bounds;
      // super.reset(); //task performed later with this.reset()
      boolean success = false;
      
      if (!e.isConsumed() && rect != null && drawingArea.getToolMode() != null
          && graphComponent.isSignificant(rect.width, rect.height)) {
        // System.out.println("is shapetoolband.mouseReleased doing anything?");
        String style = "";
        String toolMode = drawingArea.getToolMode();
        style = mxUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, Integer
            .toHexString(kUtils.getFillColorFromKey(
                                                    drawingArea
                                                        .getCurrentFillColorKey())
                .getRGB()));
        style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, Integer
            .toHexString(kUtils.getFillColorFromKey(
                                                    drawingArea
                                                        .getCurrentFillColorKey())
                .getRGB()));
        style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Integer
            .toHexString(kUtils.getFontColorFromKey(
                                                    drawingArea
                                                        .getCurrentFillColorKey())
                .getRGB()));
        style = mxUtils.setStyle(style, mxConstants.STYLE_FONTFAMILY,
                                 kConstants.DEFAULT_FONTFAMILY);
//        style = mxUtils.setStyle(style, mxConstants.STYLE_ALIGN,
//                                 mxConstants.ALIGN_LEFT);
       
        if (toolMode.equals(kConstants.SHAPE_KEYS[1])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                   mxConstants.SHAPE_ELLIPSE);
        } else if (toolMode.equals(kConstants.SHAPE_KEYS[2])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                   mxConstants.SHAPE_RHOMBUS);
        } else if (toolMode.equals(kConstants.SHAPE_KEYS[3])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                   kConstants.SHAPE_STAR);
        } else if (toolMode.equals(kConstants.SHAPE_KEYS[4])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                   kConstants.SHAPE_AUDIO);
        } else if (toolMode.equals(kConstants.SHAPE_KEYS[5])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                   kConstants.SHAPE_KEYBOARD);
        } else if (toolMode.equals(kConstants.SHAPE_KEYS[6])) {
          style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                   kConstants.SHAPE_PERSON);
        } else if (toolMode.equals(kConstants.SHAPE_TEXT)) {
          // this works because the canvas will draw nothing but the label if we
          // set style=junk
          // note this also overrides the earlier assignment of colors
          style = kConstants.SHAPE_TEXT;
        } else if (!toolMode.equals(kConstants.SHAPE_KEYS[0])) // no style means
                                                               // rectangles
        {
          System.err
              .println("Ut oh! You managed to select a tool that doesn't exist! Please report this bug: class=ShapeToolband, toolMode="
                       + toolMode);
        }

        // ----------------
        mxGraph graph = graphComponent.getGraph();
        Object parent = graph.getDefaultParent();
        graph.getModel().beginUpdate();
        try {
          // System.out.println("style="+style);

          Object cell = graph.insertVertex(parent, null, new kCellValue(), rect.x,
                                           rect.y, rect.width, rect.height,
                                           style);

          // System.out.println("cell.style="+graph.getModel().getStyle(cell));
        } finally {
          graph.getModel().endUpdate();
          success = true;
        }
        // ----------------

        e.consume();

        // reset and stop editing when done
        super.reset();
        graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
        drawingArea.endToolMode(success);
      }
    }

    /**
     * the superclass's KeyListener handles escape keystrokes and performs this
     */
    public void reset() {
      super.reset();
      graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
      drawingArea.endToolMode(false);
    }

  }

  protected static class ColorToolband extends MouseAdapter implements
      MouseMotionListener {

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
     * A cell marker to highlight then mouseOver cells
     */
    protected mxCellMarker marker;

    /**
     * Constructor.
     * 
     * @param drawingArea
     */
    public ColorToolband(final DrawingArea drawingArea) {
      this.graphComponent = drawingArea.getGraphComponent();
      this.drawingArea = drawingArea;
      this.enabled = false;
      marker = new mxCellMarker(drawingArea.getGraphComponent()) {
        /**
         * Don't mark edges because we don't want to allow coloring of edges
         * This returns null if cell is not found or cell is an edge.
         */
        protected Object getCell(MouseEvent e)
        {
          mxICell cell = (mxICell) graphComponent.getCellAt(e.getX(), e.getY(),
              swimlaneContentEnabled);
          return ((cell != null) ? ((!cell.isEdge()) ? cell : null) : null);
        }
      };
      marker.setValidColor(kConstants.UI_COLOR_ROLLOVER);

      // Install ourselves as a listener
      graphComponent.getGraphControl().addMouseListener(this);
      graphComponent.getGraphControl().addMouseMotionListener(this);
      // Handle escape keystrokes
      graphComponent.addKeyListener(new KeyAdapter() {
        public void keyPressed(KeyEvent e) {
          if (e.getKeyCode() == KeyEvent.VK_ESCAPE
              && graphComponent.isEscapeEnabled()) {
            reset();
          }
        }
      });
    }

    /**
     * Returns the enabled state.
     */
    public boolean isEnabled() {
      return enabled;
    }

    /**
     * Sets the enabled state.
     */
    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    /**
     * Overriding mousePressed to edit the triggering conditionals. All events
     * originate from graphComponent.graphControl
     * 
     * @see com.mxgraph.swing.handler.mxRubberband#mousePressed(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {
      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger()) {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        marker.process(e);
        e.consume();
      }
    }

    public void mouseReleased(MouseEvent e) {
      boolean success = false;

      if (!e.isConsumed() && isEnabled() && !e.isPopupTrigger()) {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
        // getting cell from the marker ensures that we don't grab edges
        mxCell cell = (mxCell) ((marker.hasValidState()) ? marker
            .getValidState().getCell() : null);
        if (cell != null) {
          mxGraph graph = graphComponent.getGraph();
          String style = cell.getStyle();
          style = mxUtils.setStyle(style, mxConstants.STYLE_FILLCOLOR, Integer
              .toHexString(kUtils
                  .getFillColorFromKey(drawingArea.getToolMode()).getRGB()));
          style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR,
                                   Integer.toHexString(kUtils
                                       .getFillColorFromKey(
                                                            drawingArea
                                                                .getToolMode())
                                       .getRGB()));
          style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Integer
              .toHexString(kUtils
                  .getFontColorFromKey(drawingArea.getToolMode()).getRGB()));
          graph.setCellStyle(style, new Object[] { cell });
          success = true;
        }
        e.consume();

        // end tool when success done
        marker.reset();
        graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
        drawingArea.endToolMode(success);
      }
    }

    public void reset() {
      marker.reset();
      graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
      drawingArea.endToolMode(false);
    }

    public void mouseDragged(MouseEvent e) {
      // do nothing, but need this stub since we implement MouseMotionListener
    }

  }

  /**
   * Opportunistically stealing mxConnectionHandler and tweaking it to be
   * our ConnectorTool -- ideally I'd like one that has functionalities of
   * both but for now mxConnectionHandler is slightly more sophisticated and
   * less buggy.
   */
  public static class kConnectionHandler extends mxConnectionHandler {

    /**
     * Reference to the enclosing drawing component.
     */
    DrawingArea drawingArea;
    
    /**
     * Constructor
     * @param drawingArea
     */
    public kConnectionHandler(final DrawingArea drawingArea) {
      super(drawingArea.getGraphComponent());
      this.drawingArea = drawingArea;
      getMarker().setHotspot(1);
      getMarker().setValidColor(kConstants.CONN_MARKER_VALID_COLOR);
      getMarker().setInvalidColor(kConstants.CONN_MARKER_INVALID_COLOR);
    }
    
    //set marker colors+stroke
    //click on empty space draws disconnected arrows
    //reset tool(success=true) when done
    //reset tool(false) when click popupTrigger || escape
    
  /**
  * PREVIEW is the component that draws the dotted line that 
  * flies around when the mouse is still dragging. The graphics 
  * are defined in the paint method.  
  */
 protected JComponent createPreview() {
   return new JPanel() {
     private static final long serialVersionUID = -6401041861368362818L;

     public void paint(Graphics g) {
       super.paint(g);
       ((Graphics2D) g).setStroke(kConstants.PREVIEW_STROKE);

       if (start != null && current != null) {
         if (marker.hasValidState() || createTarget
             || graphComponent.getGraph().isAllowDanglingEdges()) {
           g.setColor(kConstants.DEFAULT_VALID_COLOR);
         } else {
           g.setColor(kConstants.DEFAULT_INVALID_COLOR);
         }
         g.drawLine(start.x - getX(), start.y - getY(),
                    current.x - getX(), current.y - getY());
       }
     }
   };
 }
    
    /**
     * Creates, inserts and returns a new edge using mxGraph.insertEdge.
     * Modified to take connector style from current toolMode in drawingArea
     */
    protected Object insertEdge(Object parent, String id, Object value,
                                Object source, Object target) {
      String style = "";
      String toolMode = drawingArea.getToolMode();

      if (toolMode.equals(kConstants.CONNECTOR_KEYS[0])) {
        style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                 mxConstants.SHAPE_CONNECTOR);
        style = mxUtils.setStyle(style, mxConstants.STYLE_ENDARROW,
                                 mxConstants.NONE);
      } else if (toolMode.equals(kConstants.CONNECTOR_KEYS[1])) {
        style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                 mxConstants.SHAPE_CONNECTOR);
      } else if (toolMode.equals(kConstants.CONNECTOR_KEYS[2])) {
        style = mxUtils.setStyle(style, mxConstants.STYLE_SHAPE,
                                 mxConstants.SHAPE_CONNECTOR);
        style = mxUtils.setStyle(style, mxConstants.STYLE_DASHED, "1");
      } else {
        System.err
            .println("Ut oh! You managed to select a tool that doesn't exist! Please report this bug: class=ConnectorToolband, toolMode="
                     + toolMode);
      }
      style = mxUtils.setStyle(style, mxConstants.STYLE_STROKECOLOR, Integer
          .toHexString((kConstants.EDGE_STROKE_COLOR).getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_FONTCOLOR, Integer
          .toHexString((kConstants.EDGE_FONT_COLOR).getRGB()));
      style = mxUtils.setStyle(style, mxConstants.STYLE_FONTFAMILY,
                               kConstants.DEFAULT_FONTFAMILY);
      

      return graphComponent.getGraph().insertEdge(parent, id, value, source,
                                                  target, style);
    }

    /**
     * 
     */
    public void reset()
    {
      super.reset();
      graphComponent.getGraphControl().setCursor(Cursor.getDefaultCursor());
      //if success didn't already reset the toolmode first
      if (drawingArea.getToolMode() != null)
        drawingArea.endToolMode(false);
    }
    /**
     * Override to be able to create edges that start with a dangling point
     * rather than a source cell, by adding the store start point as source
     * point in the new edge's geometry (same way a dangling target point is
     * added as edge end). After everything is done reset drawArea.toolMode.
     */
    protected void connect(Object source, Object target, MouseEvent e)
    {
      mxGraph graph = graphComponent.getGraph();
      mxIGraphModel model = graph.getModel();

      Object newTarget = null;
      Object edge = null;

      if (target == null && createTarget)
      {
        newTarget = createTargetVertex(e, source);
        target = newTarget;
      }

      if (target != null || graph.isAllowDanglingEdges())
      {
        model.beginUpdate();
        try
        {
          Object dropTarget = graph.getDropTarget(
              new Object[] { newTarget }, e.getPoint(),
              graphComponent.getCellAt(e.getX(), e.getY()));

          if (newTarget != null)
          {
            // Disables edges as drop targets if the target cell was created
            if (dropTarget == null
                || !graph.getModel().isEdge(dropTarget))
            {
              mxCellState pstate = graph.getView().getState(
                  dropTarget);

              if (pstate != null)
              {
                mxGeometry geo = model.getGeometry(newTarget);

                mxPoint origin = pstate.getOrigin();
                geo.setX(geo.getX() - origin.getX());
                geo.setY(geo.getY() - origin.getY());
              }
            }
            else
            {
              dropTarget = graph.getDefaultParent();
            }

            graph.addCells(new Object[] { newTarget }, dropTarget);
          }

          Object parent = graph.getDefaultParent();

          if (model.getParent(source) == model.getParent(target))
          {
            parent = model.getParent(source);
          }

          edge = insertEdge(parent, null, "", source, target);

          if (edge != null)
          {
            // Makes sure the edge has a non-null, relative geometry
            mxGeometry geo = model.getGeometry(edge);

            if (geo == null)
            {
              geo = new mxGeometry();
              geo.setRelative(true);

              model.setGeometry(edge, geo);
            }
            
            if (source == null)
            {
              mxPoint pt = new mxPoint(start);
              geo.setSourcePoint(pt);
            }
            
            if (target == null)
            {
              mxPoint pt = graphComponent.getPointForEvent(e);
              geo.setTerminalPoint(pt, false);
            }
          }

        }
        finally
        {
          model.endUpdate();
        }
      }

      if (select)
      {
        graph.setSelectionCell(edge);
      }
      
      // end tool when success done
      drawingArea.endToolMode(true);
    }
    /**
     * Overriding just to make sure that the cursor is in tool mode (i.e. crosshair)
     */
    public void mouseMoved(MouseEvent e)
    {
      if (!e.isConsumed() && graphComponent.isEnabled() && isEnabled())
      {
        graphComponent.getGraphControl().setCursor(TOOL_CURSOR);
      }
      super.mouseMoved(e);
    }
    /**
     * Override to allow making edges from dangling points as well as source cells
     */
    public void mousePressed(MouseEvent e)
    {
      if (!graphComponent.isForceMarqueeEvent(e) &&
        !graphComponent.isPanningEvent(e))
      {
        if (!e.isConsumed() && !e.isPopupTrigger()) //&& source != null 
        {
            /* 
             * the following is the original mxgraph implementation
             * I don't know what it was trying to constrain
             * but in the event removing it causes bugs later, we'll
             * keep it here for reference
             */
//          if ((isHighlighting() && marker.hasValidState())
//              || (!isHighlighting() && getBounds().contains(
//                  e.getPoint())))
          {
            start = e.getPoint();
            preview.setOpaque(false);
            preview.setVisible(false);
            graphComponent.getGraphControl().add(preview, 0);
            getParent().setComponentZOrder(this, 0);
            graphComponent.getGraphControl().setCursor(TOOL_CURSOR); //crosshair
            marker.reset();
            e.consume();
          }
        }
      }
    }
    /**
     * Override to allow making edges from dangling points. If source==null it
     * is still sent to the connect method anyway, and then the bounds are
     * set the same way the dangling end (i.e. target==null) is set.
     */
    public void mouseReleased(MouseEvent e)
    {
      if (!e.isConsumed() && isConnecting())
      {
        // Does not connect if there is an error
        if (error != null)
        {
          if (error.length() > 0)
          {
            JOptionPane.showMessageDialog(graphComponent, error);
          }
        }
        else //if (source != null)
        {
          Object src = (source != null) ? source.getCell() : null;
          Object trg = (marker.hasValidState()) ? marker.getValidState()
              .getCell() : null;
          connect(src, trg, e);
        }

        e.consume();
      }
      else if (source != null && !e.isPopupTrigger())
      {
        graphComponent.selectCellForEvent(source.getCell(), e);
      }

      if (isEnabled())
        reset(); //only do this if enabled otherwise it will fire a TOOL_END event
    }

    /**
     * Override to accept current mousePoint for updating preview even if
     * source==null. SourcePerimeter point calculations are only performed if
     * source==null and ignored if source is a dangling point.
     */
    public void mouseDragged(MouseEvent e)
    {
      if (!e.isConsumed() && start != null) //&& source != null 
      {
        int dx = e.getX() - start.x;
        int dy = e.getY() - start.y;

        if (!preview.isVisible() && graphComponent.isSignificant(dx, dy))
        {
          preview.setVisible(true);
          marker.reset();
        }

        current = e.getPoint();
        mxGraph graph = graphComponent.getGraph();
        mxGraphView view = graph.getView();
        double scale = view.getScale();
        mxPoint trans = view.getTranslate();

        current.x = (int) Math.round((graph.snap(current.x / scale
            - trans.getX()) + trans.getX())
            * scale);
        current.y = (int) Math.round((graph.snap(current.y / scale
            - trans.getY()) + trans.getY())
            * scale);

        marker.process(e);

        // Checks if a color was used to highlight the state
        mxCellState state = marker.getValidState();

        if (state != null)
        {
          current.x = (int) state.getCenterX();
          current.y = (int) state.getCenterY();

          // Computes the target perimeter point
          mxPerimeterFunction targetPerimeter = view
              .getPerimeterFunction(state);

          if (targetPerimeter != null)
          {
            mxPoint next;
            if (source != null)
              next = new mxPoint(source.getCenterX(), source
                  .getCenterY());
            else
              next = new mxPoint(start);
            
            mxPoint tmp = targetPerimeter.apply(view
                .getPerimeterBounds(state, null, false), null,
                state, false, next);

            if (tmp != null)
            {
              current = tmp.getPoint();
            }
          }
        }
          
        if (source != null) {
          // Computes the source perimeter point
          mxPerimeterFunction sourcePerimeter = view
              .getPerimeterFunction(source);
  
          if (sourcePerimeter != null)
          {
            mxPoint pt = sourcePerimeter.apply(view.getPerimeterBounds(
                source, null, true), null, source, true, new mxPoint(
                current));
  
            if (pt != null)
            {
              start = pt.getPoint();
            }
          }
          else
          {
            start = new Point((int) Math.round(source.getCenterX()),
                (int) Math.round(source.getCenterY()));
          }
        }

        // Hides the connect icon or handle
        setVisible(false);

        // Updates the bounds of the previewed line
        Rectangle bounds = new Rectangle(current);
        bounds.add(start);
        bounds.grow(1, 1);
        preview.setBounds(bounds);

        e.consume();
      }
    }
    
  }

}
