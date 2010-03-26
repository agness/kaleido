package processing.app.graph;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.TransferHandler;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import processing.app.Theme;
import processing.app.util.kConstants;

import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.swing.handler.mxCellHandler;
import com.mxgraph.swing.handler.mxCellMarker;
import com.mxgraph.swing.handler.mxConnectionHandler;
import com.mxgraph.swing.handler.mxEdgeHandler;
import com.mxgraph.swing.handler.mxElbowEdgeHandler;
import com.mxgraph.swing.handler.mxGraphHandler;
import com.mxgraph.swing.handler.mxGraphTransferHandler;
import com.mxgraph.swing.handler.mxVertexHandler;
import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxEdgeStyle;
import com.mxgraph.view.mxGraph;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxEdgeStyle.mxEdgeStyleFunction;

/**
 * Too many overrides and customizations of our mxGraphComponent instance
 * getting messy; I'm factoring it out into a subclass for easier coding.
 * @author achang
 */
public class kGraphComponent extends mxGraphComponent {
  
  /**
   * TODO the alternative to all the gibberish color setting on this page
   * is to subclass each of the handlers and redefine the members in the constructors
   */
  
  //TODO put all of these colors in kConstants, or make them all reference colors in kConstants
  protected static final Color CANVAS_COLOR = kConstants.UI_COLOR_INACTIVE;
 
  protected static final Color CELL_MARKER_COLOR = kConstants.UI_COLOR_ROLLOVER;

  protected static final Color SWIMLANE_MARKER_COLOR = new Color(50,255,0); //lime green

  protected static final Color HANDLE_FILLCOLOR = mxUtils.parseColor("#989898");

  protected static final Color HANDLE_BORDERCOLOR = mxUtils.parseColor("#989898");

  protected static final Color LABEL_HANDLE_FILLCOLOR = Color.CYAN;
  
  protected static final Color LOCKED_HANDLE_FILLCOLOR = Color.CYAN;

  protected static final Color CONNECT_HANDLE_FILLCOLOR = Color.magenta;

  protected static final Color CONN_MARKER_VALID_COLOR = Color.CYAN;//mxUtils.parseColor("#B9FC00");

  protected static final Color CONN_MARKER_INVALID_COLOR = Color.magenta;//mxUtils.parseColor("#7AFC00"); 
  
  protected static final Color DEFAULT_VALID_COLOR = Color.orange;

  protected static final Color DEFAULT_INVALID_COLOR = Color.yellow;//new Color(255,0,255);  
  
  protected static final Stroke PREVIEW_STROKE = new BasicStroke(1,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
      new float[] { 3, 3 }, 0.0f);

  public static Border PREVIEW_BORDER = new LineBorder(HANDLE_BORDERCOLOR) {
    public void paintBorder(Component c, Graphics g, int x, int y, int width,
                            int height) {
      ((Graphics2D) g).setStroke(VERTEX_SELECTION_STROKE);
      super.paintBorder(c, g, x, y, width, height);
    }
  };
  
  protected static final Color VERTEX_SELECTION_COLOR = Color.yellow;

  protected static final Stroke VERTEX_SELECTION_STROKE = new BasicStroke(1,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
      new float[] { 3, 3 }, 0.0f);

  protected static final Color EDGE_SELECTION_COLOR = Color.cyan;

  protected static final Stroke EDGE_SELECTION_STROKE = new BasicStroke(1,
      BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f,
      new float[] { 3, 3 }, 0.0f);

  
  /**
   * Constructor, mainly to override the cellMarker paint method,
   * specify background color, and use our custom cell editor.
   * @param graph
   */
  public kGraphComponent(mxGraph graph) 
  {
    super(graph);
    
    //Use Kaleido cell editor
    setCellEditor(new kCellEditor(this));
    
    //Set the background (must first clear viewport)
    getViewport().setOpaque(false);
    setBackground(kConstants.UI_COLOR_BACKGROUND);
    // the following setting doesn't affect the graphComponent, but affects the
    // graphOutline background color; we're using the same color as the
    // background color of inactive buttons
    setPageBackgroundColor(kConstants.UI_COLOR_INACTIVE); 
  }
  
  /**
   * Overridden to use our custom canvas.
   * @see com.mxgraph.swing.mxGraphComponent#createCanvas
   */
  public mxInteractiveCanvas createCanvas() {
    return new kCanvas();        
  }
  
  /**
   * Overridden to override mxGraphHandler's mxCellMarker to
   * change the color of swimlane cell markers.
   * 
   * if gaudenz does his thing, this could be 1 line in the kGComp constructor:
   * getGraphHandler().getMarker().setValidColor();
   */
  protected mxGraphHandler createGraphHandler()
  {
    return new mxGraphHandler(this) {
      protected mxCellMarker createMarker()
      {
        mxCellMarker marker = super.createMarker();
        marker.setValidColor(SWIMLANE_MARKER_COLOR);
        return marker;
      }
    };
  }
  
  /**
   * Overridden to override mxGraphControl to make sure that
   * drawCell can handle passing kCellValue to canvas.
   * @see com.mxgraph.swing.mxGraphComponent#createGraphControl
   */
  protected mxGraphControl createGraphControl()
  {
    return new mxGraphControl() {
      /**
       * Overriding the drawCell method here as well as mxGraph.drawCell
       * because this also only passes the String type label to be painted
       * but we need to be able to send an Object (viz. a kCellValue).
       * TODO isn't it rather retarded that I have to make this change twice?
       * think about refactoring for better organization.
       * @see com.mxgraph.swing.mxGraphComponent.mxGraphControl#drawCell
       * 
       * @param canvas Canvas onto which the cell should be drawn.
       * @param cell Cell that should be drawn onto the canvas.
       */
      public void drawCell(mxICanvas canvas, Object cell)
      {
        
//        System.out.println("graphComponent.graphControl >> drawCell");
        
        mxCellState state = graph.getView().getState(cell);

        if (isCellDisplayable(cell))
        {
          String label = getDisplayLabelForCell(cell);

          if (((mxICell) cell).getValue() instanceof kCellValue && graph instanceof kGraph) {
            ((kGraph) graph).drawStateWithNotes(canvas, state, label, ((kGraph) graph).getNotes(cell));
          } 
          else
          {
            graph.drawStateWithLabel(canvas, state, label);
          }
        }

        // Handles special ordering for edges (all in foreground
        // or background) or draws all children in order
        boolean edgesFirst = graph.isKeepEdgesInBackground();
        boolean edgesLast = graph.isKeepEdgesInForeground();

        if (edgesFirst)
        {
          drawChildren(cell, true, false);
        }

        drawChildren(cell, !edgesFirst && !edgesLast, true);

        if (edgesLast)
        {
          drawChildren(cell, true, false);
        }

        if (state != null)
        {
          cellDrawn(canvas, state);
        }
      }
    };
  }
  
  /**
   * Overriding to override the colors of handles to connect edges.
   */
  protected mxConnectionHandler createConnectionHandler()
  {
//    System.out.println("kGComp >> new mxConnectionHandler");
    mxConnectionHandler c = new mxConnectionHandler(this) {
      /**
       * PREVIEW is the component that draws the dotted line that 
       * flies around when the mouse is still dragging. The graphics 
       * are defined in the paint method.  Preview, however, is defined
       * inline <i>at the top</i> (instead of via a creator), and thus
       * cannot be overridden because Java actually creates two copies 
       * of the variable and the overridden member needs to be explicitly 
       * invoked (impossible in an anonymous class).
       * So here we cheat by redefining preview in a method and calling
       * said method immediately after we construct the object.
       * 
       * TODO gaudenz promised to update this in the next release
       * TODO probably remove our lovely ConnectorToolband in DrawingArea and use
       * mxConnectionHandler with shape-sized hotspots?
       */
      protected JComponent createPreview() {
        return new JPanel() {
          private static final long serialVersionUID = -6401041861368362818L;

          public void paint(Graphics g) {
            super.paint(g);
            ((Graphics2D) g).setStroke(PREVIEW_STROKE);

            if (start != null && current != null) {
              if (marker.hasValidState() || createTarget
                  || graphComponent.getGraph().isAllowDanglingEdges()) {
                g.setColor(DEFAULT_VALID_COLOR);
              } else {
                g.setColor(DEFAULT_INVALID_COLOR);
              }
              g.drawLine(start.x - getX(), start.y - getY(),
                         current.x - getX(), current.y - getY());
            }
          }
        };
      }
    };
    c.getMarker().setValidColor(CONN_MARKER_VALID_COLOR);
    c.getMarker().setInvalidColor(CONN_MARKER_INVALID_COLOR);
//    c.getMarker().setStroke(DEFAULT_STROKE); //TODO ask jgraph.gaudenz
    c.getMarker().setHotspot(1); //TODO test this
    return c;
  }
  
  /**
   * Overriding to override all color settings of all color-accessing 
   * methods in the the various cell handlers.
   * @see com.mxgraph.swing.mxGraphComponent#createHandler(mxCellState)
   * TODO change cursor on hotspots to be hands
   * 
   * @param state Cell state for which a handler should be created.
   * @return Returns the handler to be used for the given cell state.
   */
  public mxCellHandler createHandler(mxCellState state)
  {
//    System.out.println("kGComp >> createHandler");
    
    if (graph.getModel().isVertex(state.getCell()))
    {
//      System.out.println("kGComp >> new mxVertexHandler");
      return new mxVertexHandler(this, state) {
        protected Color getSelectionColor()
        {
          return VERTEX_SELECTION_COLOR;
        }
        protected Stroke getSelectionStroke()
        {
          return VERTEX_SELECTION_STROKE;
        }
        protected Color getHandleFillColor(int index)
        {
          if (isLabel(index))
          {
            return LABEL_HANDLE_FILLCOLOR;
          }
          return HANDLE_FILLCOLOR;
        }
        protected Color getHandleBorderColor(int index)
        {
          return HANDLE_BORDERCOLOR;
        }
      };
    }
    else if (graph.getModel().isEdge(state.getCell()))
    {
      mxEdgeStyleFunction style = graph.getView().getEdgeStyle(state,
          null, null, null);

      if (graph.isLoop(state) || style == mxEdgeStyle.ElbowConnector
          || style == mxEdgeStyle.SideToSide
          || style == mxEdgeStyle.TopToBottom)
      {
//        System.out.println("kGcomp >> new mxElbowEdgeHandler");
        return new mxElbowEdgeHandler(this, state) {
          
          protected Color getSelectionColor()
          {
            return EDGE_SELECTION_COLOR;
          }
          protected Stroke getSelectionStroke()
          {
            return EDGE_SELECTION_STROKE;
          }
          protected Color getHandleBorderColor(int index)
          {
            return HANDLE_BORDERCOLOR;
          }
          protected Color getHandleFillColor(int index) {
            boolean source = isSource(index);

            if (source || isTarget(index)) {
              mxGraph graph = graphComponent.getGraph();
              Object terminal = graph.getModel().getTerminal(state.getCell(),
                                                             source);
              if (terminal != null) {
                return (graphComponent.getGraph().isCellDisconnectable(state
                    .getCell(), terminal, source)) ? CONNECT_HANDLE_FILLCOLOR
                                                  : LOCKED_HANDLE_FILLCOLOR;
              }
            }
            // return super.getHandleFillColor(index);
            if (isLabel(index)) {
              return LABEL_HANDLE_FILLCOLOR;
            }
            return HANDLE_FILLCOLOR;
          }
          protected JComponent createPreview()
          {
            System.out.println("new mxElbowEdgeHandler >> createPreview >> is marker valid?");
            marker.setValidColor(DEFAULT_VALID_COLOR);
            marker.setInvalidColor(DEFAULT_INVALID_COLOR);
            System.out.println("new mxElbowEdgeHandler >> createPreview >> yes we can set marker colors here");
            JPanel preview = new JPanel()
            {
              private static final long serialVersionUID = -894546588972313020L;
              public void paint(Graphics g)
              {
                super.paint(g);

                if (!isLabel(index) && p != null)
                {
                  ((Graphics2D) g).setStroke(PREVIEW_STROKE);

                  if (isSource(index) || isTarget(index))
                  {
                    if (marker.hasValidState()
                        || graphComponent.getGraph()
                            .isAllowDanglingEdges())
                    {
                      g.setColor(DEFAULT_VALID_COLOR);
                    }
                    else
                    {
                      g.setColor(DEFAULT_INVALID_COLOR);
                    }
                  }
                  else
                  {
                    g.setColor(Color.BLACK);
                  }

                  Point origin = getLocation();
                  Point last = p[0];

                  for (int i = 1; i < p.length; i++)
                  {
                    g.drawLine(last.x - origin.x, last.y - origin.y, p[i].x
                        - origin.x, p[i].y - origin.y);
                    last = p[i];
                  }
                }
              }
            };
            if (isLabel(index))
            {
              preview.setBorder(mxConstants.PREVIEW_BORDER);
            }

            preview.setOpaque(false);
            preview.setVisible(false);

            return preview;
          }
      };
      }
//      System.out.println("kGcomp >> new mxEdgeHandler");
      return new mxEdgeHandler(this, state) {
        protected Color getSelectionColor()
        {
          return EDGE_SELECTION_COLOR;
        }
        protected Stroke getSelectionStroke()
        {
          return EDGE_SELECTION_STROKE;
        }
        protected Color getHandleBorderColor(int index)
        {
          return HANDLE_BORDERCOLOR;
        }
        protected Color getHandleFillColor(int index) {
          
          boolean source = isSource(index);

          if (source || isTarget(index)) {
            mxGraph graph = graphComponent.getGraph();
            Object terminal = graph.getModel().getTerminal(state.getCell(),
                                                           source);
            if (terminal != null) {
              return (graphComponent.getGraph().isCellDisconnectable(state
                  .getCell(), terminal, source)) ? CONNECT_HANDLE_FILLCOLOR
                                                : LOCKED_HANDLE_FILLCOLOR;
            }
          }

          if (isLabel(index)) {
            return LABEL_HANDLE_FILLCOLOR;
          }
          return HANDLE_FILLCOLOR;
        }
        /**
         * mxEdgeHandler apparently has its own override of 
         * mxCellHandler's preview panel so we are overriding that here
         * to be able to customize the preview image's colors.
         */
        protected JComponent createPreview()
        {
          System.out.println("new mxElbowEdgeHandler >> createPreview >> is marker valid?");
          marker.setValidColor(DEFAULT_VALID_COLOR);
          marker.setInvalidColor(DEFAULT_INVALID_COLOR);
          System.out.println("new mxElbowEdgeHandler >> createPreview >> yes we can set marker colors here");
          JPanel preview = new JPanel()
          {
            private static final long serialVersionUID = -894546588972313020L;
            public void paint(Graphics g)
            {
              super.paint(g);

              if (!isLabel(index) && p != null)
              {
                ((Graphics2D) g).setStroke(PREVIEW_STROKE);

                if (isSource(index) || isTarget(index))
                {
                  if (marker.hasValidState()
                      || graphComponent.getGraph()
                          .isAllowDanglingEdges())
                  {
                    g.setColor(DEFAULT_VALID_COLOR);
                  }
                  else
                  {
                    g.setColor(DEFAULT_INVALID_COLOR);
                  }
                }
                else
                {
                  g.setColor(Color.BLACK);
                }

                Point origin = getLocation();
                Point last = p[0];

                for (int i = 1; i < p.length; i++)
                {
                  g.drawLine(last.x - origin.x, last.y - origin.y, p[i].x
                      - origin.x, p[i].y - origin.y);
                  last = p[i];
                }
              }
            }
          };
          if (isLabel(index))
          {
            preview.setBorder(mxConstants.PREVIEW_BORDER);
          }

          preview.setOpaque(false);
          preview.setVisible(false);

          return preview;
        }
      };
    }

//    System.out.println("kGComp >> new mxCellHandler");
    return new mxCellHandler(this, state) {
      protected Color getHandleFillColor(int index)
      {
        if (isLabel(index))
        {
          return LABEL_HANDLE_FILLCOLOR;
        }
        return HANDLE_FILLCOLOR;
      }
      protected Color getHandleBorderColor(int index)
      {
        return HANDLE_BORDERCOLOR;
      }
    };
  }

}
  
