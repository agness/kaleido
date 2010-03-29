package processing.app.graph;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Shape;
import java.util.Map;

import processing.app.util.kConstants;

import com.mxgraph.canvas.mxGraphics2DCanvas;
import com.mxgraph.canvas.mxICanvas;
import com.mxgraph.canvas.mxImageCanvas;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;
import com.mxgraph.view.mxGraph;
import com.mxgraph.view.mxStylesheet;

/**
 * Extending mxGraph class in order to handle painting and editing of
 * cells that contain kCellValue.
 * @author achang
 */
public class kGraph extends mxGraph {

  /**
   * Override the isSwimlane function to make everything that is not an edge
   * act in the manner of a swimlane without actually being one.
   */
  public boolean isSwimlane(Object cell)
  {
    if (cell != null && (model.getParent(cell) != model.getRoot()))
    {
      mxCellState state = view.getState(cell);
      if (state != null && !model.isEdge(cell))
      {
        return true;
      }
    }
    return false;
  }
  
  /**
   * Returns true if the given cell may not be moved, sized, bended,
   * disconnected, edited or selected.
   * 
   * @param cell Cell whose locked state should be returned.
   * @return Returns true if the given cell is locked.
   */
  public boolean isCellLocked(Object cell)
  {
    return isCellsLocked() || mxUtils.isTrue(getCellStyle(cell), kConstants.STYLE_LOCKED,
                                             false);
  }
  
  /**
   * Override the drawCell method so we can tell our kCanvas to draw notes.
   * The superclass implementation only passes the String type label to be painted
   * but we need to be able to send an Object (viz. a kCellValue).
   * So the only changes in code, really, is an additional line to send
   * the note information to the canvas.
   * @see com.mxgraph.view.mxGraph#drawCell
   * 
   * @param canvas Canvas onto which the cell should be drawn.
   * @param cell Cell that should be drawn onto the canvas.
   */
  public void drawCell(mxICanvas canvas, Object cell)
  {
    
//    System.out.println("kGraph >> drawCell");
    
    if (((mxICell) cell).getValue() instanceof kCellValue) {
      drawStateWithNotes(canvas, getView().getState(cell), getLabel(cell), getNotes(cell));
    } 
    else
    {
      drawStateWithLabel(canvas, getView().getState(cell), getLabel(cell));
    }
    // Draws the children on top of their parent
    int childCount = model.getChildCount(cell);

    for (int i = 0; i < childCount; i++)
    {
      Object child = model.getChildAt(cell, i);
      drawCell(canvas, child);
    }
  }

  /**
   * Draws the given cell and label onto the specified canvas. No
   * children or descendants are painted here. This method invokes
   * cellDrawn after the cell, but not its descendants have been
   * painted.
   * 
   * @param canvas Canvas onto which the cell should be drawn.
   * @param state State of the cell to be drawn.
   * @param label Label of the cell to be drawn.
   * @param notes Notes of the cell to be drawn. 
   */
  public void drawStateWithNotes(mxICanvas canvas, mxCellState state,
      String label, String notes)
  {
    Object cell = (state != null) ? state.getCell() : null;
    
//  System.out.println("kGraph >> drawStateWithNotes");
  
    if (cell != null && cell != view.getCurrentRoot()
        && cell != model.getRoot())
    {
      Object obj = null;
      Object lab = null;

      int x = (int) Math.round(state.getX());
      int y = (int) Math.round(state.getY());
      int w = (int) Math.round(state.getWidth() - x + state.getX());
      int h = (int) Math.round(state.getHeight() - y + state.getY());

      if (model.isVertex(cell))
      {
        obj = canvas.drawVertex(x, y, w, h, state.getStyle());
      }
      else if (model.isEdge(cell))
      {
        obj = canvas.drawEdge(state.getAbsolutePoints(), state
            .getStyle());
      }

      // Holds the current clipping region in case the label will
      // be clipped
      Shape clip = null;
      Rectangle newClip = state.getRectangle();

      // Indirection for image canvas that contains a graphics canvas
      mxICanvas clippedCanvas = (isLabelClipped(state.getCell())) ? canvas
          : null;

      if (clippedCanvas instanceof mxImageCanvas)
      {
        clippedCanvas = ((mxImageCanvas) clippedCanvas)
            .getGraphicsCanvas();
        //mxGraph TODO: Shift newClip to match the image offset
        //Point pt = ((mxImageCanvas) canvas).getTranslate();
        //newClip.translate(-pt.x, -pt.y);
      }

      if (clippedCanvas instanceof mxGraphics2DCanvas)
      {
        Graphics g = ((mxGraphics2DCanvas) clippedCanvas).getGraphics();
        clip = g.getClip();
        g.setClip(newClip);
      }

      mxRectangle bounds = state.getLabelBounds();

      if (label != null && bounds != null)
      {
        x = (int) Math.round(bounds.getX());
        y = (int) Math.round(bounds.getY());
        w = (int) Math.round(bounds.getWidth() - x + bounds.getX());
        h = (int) Math.round(bounds.getHeight() - y + bounds.getY());

        //<!------------Kaleido edits
        //forcing the title to be in bold
        state.getStyle().put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        lab = canvas.drawLabel(label, x, y, w, h, state.getStyle(),
            isHtmlLabel(cell));
        
        if (notes != null) 
        {
          
          //draw a little lower
          y = (int) Math.round(bounds.getY()+bounds.getHeight());
          h = (int) Math.round(bounds.getHeight()*2 - y + bounds.getY());
          
          //at minimum reset the font style TODO make monospace like before?
          state.getStyle().remove(mxConstants.STYLE_FONTSTYLE);
          
          canvas.drawLabel(notes, x, y, w, h, state.getStyle(),
                           isHtmlLabel(cell));
        }
        //kEdits------------>  
      }

      // Restores the previous clipping region
      if (clippedCanvas instanceof mxGraphics2DCanvas)
      {
        ((mxGraphics2DCanvas) clippedCanvas).getGraphics()
            .setClip(clip);
      }

      // Invokes the cellDrawn callback with the object which was created
      // by the canvas to represent the cell graphically
      if (obj != null)
      {
        cellDrawn(cell, obj, lab);
      }
    }
  }

  /**
   * Overriding the super.getLabel() function to be able
   * to return labels from kCellValues.
   * @see com.mxgraph.view.mxGraph#getLabel
   * 
   * @param cell <mxCell> whose notes should be returned.
   * @return Returns the notes for the given cell.
   */
  public String getLabel(Object cell)
  {
    String result = "";

    if (cell != null)
    {
      mxCellState state = view.getState(cell);
      Map<String, Object> style = (state != null) ? state.getStyle()
          : getCellStyle(cell);

      if (labelsVisible
          && !mxUtils.isTrue(style, mxConstants.STYLE_NOLABEL, false))
      {
        Object value = ((mxICell) cell).getValue();

        if (value instanceof kCellValue)
          result = ((kCellValue) value).getLabel();
        else
          result = convertValueToString(cell); //the superclass implementation
      }
    }

    return result;
  }

  /**
   * New method, analogous to the super.getLabel() function.
   * @see com.mxgraph.view.mxGraph#getLabel
   * 
   * @param cell <mxCell> whose notes should be returned.
   * @return Returns the notes for the given cell.
   */
  public String getNotes(Object cell)
  {
    String result = "";

    if (cell != null)
    {
      mxCellState state = view.getState(cell);
      Map<String, Object> style = (state != null) ? state.getStyle()
          : getCellStyle(cell);

      if (labelsVisible
          && !mxUtils.isTrue(style, mxConstants.STYLE_NOLABEL, false))
      {
        Object value = ((mxICell) cell).getValue();
        if (value instanceof kCellValue)
          result = ((kCellValue) value).getNotes();
      }
    }

    return result;
  }
}