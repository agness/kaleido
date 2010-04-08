package processing.app.graph;

import processing.app.util.kUndoableEdit;

import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxIGraphModel;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;

/**
 * Extension of mxGraphModel implementation to allow firing of kUndoableEdits
 * 
 * @see processing.app.util.kUndoableEdit
 * @author achang
 * 
 */
public class kGraphModel extends mxGraphModel {
  
  /**
   * An override of {@link mxIGraphModel#endUpdate()} to enable firing of
   * "kaleido events", namely a compound mxUndoableEdit with a recognizable
   * human name instead of a mashup name of its component changes. Used for
   * linking and locking.
   */
  public void endUpdate(String name) {
    updateLevel--;

    if (!endingUpdate)
    {
      endingUpdate = updateLevel == 0;
      fireEvent(new mxEventObject(mxEvent.END_UPDATE, "edit", currentEdit));

      try
      {
        if (endingUpdate && !currentEdit.isEmpty())
        {
          fireEvent(new mxEventObject(mxEvent.BEFORE_UNDO, "edit",
              currentEdit));
          kUndoableEdit tmp = new kUndoableEdit(currentEdit, name); //<---kEdit (1 line)
          currentEdit = createUndoableEdit();
          tmp.dispatch();
          fireEvent(new mxEventObject(mxEvent.UNDO, "edit", tmp));
        }
      }
      finally
      {
        endingUpdate = false;
      }
    }
  }

}
