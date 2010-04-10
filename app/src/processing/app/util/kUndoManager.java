package processing.app.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.undo.UndoableEdit;

import com.mxgraph.model.mxGraphModel.mxChildChange;
import com.mxgraph.model.mxGraphModel.mxCollapseChange;
import com.mxgraph.model.mxGraphModel.mxGeometryChange;
import com.mxgraph.model.mxGraphModel.mxStyleChange;
import com.mxgraph.model.mxGraphModel.mxTerminalChange;
import com.mxgraph.model.mxGraphModel.mxValueChange;
import com.mxgraph.model.mxGraphModel.mxVisibleChange;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;

/**
 * Re-implementing mxUndoManager to accommodate javax.swing.undo.UndoableEdits
 * @author achang
 */
public class kUndoManager extends mxEventSource {

  /**
   * Replacing the history list in the super class so we can hold Object types.
   * Original: List that contains the steps of the command history.
   */
  protected List<Object> history;

  /**
   * Maximum command history size. 0 means unlimited history. Default is 100.
   */
  protected int size;
  
  /**
   * Index of the element to be added next.
   */
  protected int indexOfNextAdd;
  
  /**
   * Used to track of number of edits since the last save.
   * @author achang
   */
  protected int counter;
  
  /**
   * Constructs a new undo manager with a default history size.
   */
  public kUndoManager()
  {
    this(100);
  }

  /**
   * Constructs a new undo manager for the specified size.
   */
  public kUndoManager(int size)
  {
    this.size = size;
    clear();
  }
  
  /**
   * Returns whether or not the command history is empty.
   */
  public boolean isEmpty()
  {
    return history.isEmpty();
  }
  
  /**
   * Clears the command history.
   */
  public void clear()
  {
//    System.out.println("undoMan >> clear");
    
    history = new ArrayList<Object>(size);
    indexOfNextAdd = 0;
    resetCounter();
    fireEvent(new mxEventObject(mxEvent.CLEAR));
  }
  

  /**
   * Returns true if an undo is possible.
   */
  public boolean canUndo()
  {
    return indexOfNextAdd > 0;
  }
  
  /**
   * Undoes the last change.
   */
  public void undo()
  {   
    while (indexOfNextAdd > 0)
    {
      Object e = history.get(--indexOfNextAdd);
      counter--;
      
//      System.out.println("undoMan >> UNDO edit="+e+" indexOfNextAdd="+indexOfNextAdd + " counter="+counter);
      
      if (e instanceof mxUndoableEdit)  //case of mxUndoableEdit
      {
        mxUndoableEdit edit = (mxUndoableEdit) e;
        edit.undo();
  
        if (edit.isSignificant()) //TODO i wonder if these should be fired regardless of mxUndo or javaxUndo
        {
          fireEvent(new mxEventObject(mxEvent.UNDO, "edit", edit));
          break;
        }
      }
      else  //case of javax.undoableEdit
      {
        UndoableEdit edit = (UndoableEdit) e;
        edit.undo();
        break;
      }
    }
  }
  
  /**
   * Returns true if a redo is possible.
   */
  public boolean canRedo()
  {
    return indexOfNextAdd < history.size();
  }
  
  /**
   * Redoes the last change.
   */
  public void redo()
  {
    int n = history.size();
    
    while (indexOfNextAdd < n)
    {
      Object e = history.get(indexOfNextAdd++);
      counter++;
      
//      System.out.println("undoMan >> REDO edit="+e+" indexOfNextAdd="+indexOfNextAdd + " counter="+counter);
      
      if (e instanceof mxUndoableEdit)  //case of mxUndoableEdit
      {
        mxUndoableEdit edit = (mxUndoableEdit) e;
        edit.redo();
  
        if (edit.isSignificant())
        {
          fireEvent(new mxEventObject(mxEvent.REDO, "edit", edit));
          break;
        }
      }
      else  //case of javax.undoableEdit
      {
        UndoableEdit edit = (UndoableEdit) e;
        edit.redo();
        break;
      }
    }
  }
  
  /**
   * Method to be called to add new undoable edits to the history.
   */
  public void undoableEditHappened(Object undoableEdit)
  {
    trim();
    counter++;

    if (size > 0 && size == history.size())
    {
      history.remove(0);
    }
   
    history.add(undoableEdit);
    indexOfNextAdd = history.size();
    
//    System.out.println("undoMan >> undoableEditHappened edit="+undoableEdit+" indexOfNextAdd="+indexOfNextAdd + " counter="+counter);
    
    fireEvent(new mxEventObject(mxEvent.ADD, "edit", undoableEdit));
  }
  
  /**
   * Removes all pending steps after indexOfNextAdd from the history,
   * invoking die on each edit. This is called from undoableEditHappened.
   */
  protected void trim()
  {
    while (history.size() > indexOfNextAdd)
    {
//      System.out.println("undoMan >> trim removing "+indexOfNextAdd);
      
      Object e = history.remove(indexOfNextAdd);
      
      if (e instanceof mxUndoableEdit)  //case of mxUndoableEdit
      {
        mxUndoableEdit edit = (mxUndoableEdit) e;
        edit.die();
      }
      else  //case of javax.undoableEdit
      {
        UndoableEdit edit = (UndoableEdit) e;
        edit.die();
      }
    }
  }
  
  /**
   * Returns the number of edits since the last save.
   */
  public int getCounter()
  {
    return counter;
  }
  
  /**
   * Resets the number of edits since the last save to zero.
   */
  public void resetCounter()
  {
    counter = 0;
  }
  
  /**
   * Returns getUndoPresentationName of the next edit (significant or not) that
   * will be undone when undo() is invoked. If there is none, returns
   * AbstractUndoableEdit.undoText from the defaults table.
   * 
   * @see java.swing.undo.UndoManager#getUndoPresentationName
   */
  public synchronized String getUndoPresentationName() {
      if (canUndo()) {
        Object e = history.get(indexOfNextAdd-1);
        
        if (e instanceof kUndoableEdit)  //case of kaleido link edit
        {
          return ((kUndoableEdit) e).getUndoPresentationName();
        }
        else if (e instanceof mxUndoableEdit)  //case of mxUndoableEdit
        {
          List<mxUndoableChange> changes = ((mxUndoableEdit) e).getChanges();
          String changeDescription = "generic";
          Iterator<mxUndoableChange> it = changes.iterator();
          while (it.hasNext())
          {
            Object change = it.next();
            if (change instanceof mxChildChange)
            {
              changeDescription="element";
            }
            else if (change instanceof mxTerminalChange)
            {
              changeDescription="connection";
            }
            else if (change instanceof mxValueChange)
            {
              changeDescription="label";
            }
            else if (change instanceof mxStyleChange)
            {
              changeDescription="color fill";
            }
            else if (change instanceof mxGeometryChange)
            {
              changeDescription="layout";
            }
            else if (change instanceof mxCollapseChange)
            {
              changeDescription="collapse";
            }
            else if (change instanceof mxVisibleChange)
            {
              changeDescription="visible";
            }
          }          
          return "Undo drawing "+changeDescription;
        }
        else if (e instanceof UndoableEdit)  //case of javax.undoableEdit
        {
          return ((UndoableEdit) e).getUndoPresentationName();
        }
      }
      return UIManager.getString("AbstractUndoableEdit.undoText");
  }

  /**
   * Returns getRedoPresentationName of the next edit (significant or not) that
   * will be redone when redo() is invoked. If there is none, returns
   * AbstractUndoableEdit.redoText from the defaults table.
   * 
   * @see java.swing.undo.UndoManager#getRedoPresentationName
   */
  public synchronized String getRedoPresentationName() {
    if (canRedo()) {
      Object e = history.get(indexOfNextAdd);
      
      if (e instanceof kUndoableEdit)  //case of kaleido link edit
      {
        return ((kUndoableEdit) e).getRedoPresentationName();
      }
      else if (e instanceof mxUndoableEdit)  //case of mxUndoableEdit
      {
        List<mxUndoableChange> changes = ((mxUndoableEdit) e).getChanges();
        String changeDescription = "generic";
        Iterator<mxUndoableChange> it = changes.iterator();
        while (it.hasNext())
        {
          Object change = it.next();
          if (change instanceof mxChildChange)
          {
            changeDescription="nesting";
          }
          else if (change instanceof mxTerminalChange)
          {
            changeDescription="connection";
          }
          else if (change instanceof mxValueChange)
          {
            changeDescription="label";
          }
          else if (change instanceof mxStyleChange)
          {
            changeDescription="color fill";
          }
          else if (change instanceof mxGeometryChange)
          {
            changeDescription="geometry";
          }
          else if (change instanceof mxCollapseChange)
          {
            changeDescription="collapse";
          }
          else if (change instanceof mxVisibleChange)
          {
            changeDescription="visible";
          }
        }          
        return "Redo drawing "+changeDescription;
      }
      else if (e instanceof UndoableEdit)  //case of javax.undoableEdit
      {
        return ((UndoableEdit) e).getRedoPresentationName();
      }
    }
    return UIManager.getString("AbstractUndoableEdit.redoText");
  }
}
