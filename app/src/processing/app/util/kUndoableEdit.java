package processing.app.util;

import com.mxgraph.util.mxUndoableEdit;

/**
 * Exactly the same in operation as mxUndoableEdit, except with addition of
 * presentation names: we assume that no other Kaleido edit is using this
 * except for linking (because linking is the only Kaleido edit that exists)
 * 
 * @author achang
 * 
 */
public class kUndoableEdit extends mxUndoableEdit {

  protected String presentationName = "kaleido edit";
  
  public kUndoableEdit(Object source, boolean significant) {
    super(source, significant);
  }

  public kUndoableEdit(Object source) {
    super(source);
  }
  
  public kUndoableEdit(mxUndoableEdit origEdit, String name) {
    super(origEdit.getSource(), origEdit.isSignificant());
    this.changes = origEdit.getChanges();
    this.undone = origEdit.isUndone();
    this.redone = origEdit.isRedone();
    this.presentationName = name;
  }
  /**
   * Provides a localized, human readable description of this edit
   * suitable for use in, say, a change log.
   */
  public String getPresentationName() {
    return presentationName;
  }

  /**
   * Provides a localized, human readable description of the undoable
   * form of this edit, e.g. for use as an Undo menu item. Typically
   * derived from <code>getDescription</code>.
   */
  public String getUndoPresentationName() {
    return "Undo "+getPresentationName();
  }

  /**
   * Provides a localized, human readable description of the redoable
   * form of this edit, e.g. for use as a Redo menu item. Typically
   * derived from <code>getPresentationName</code>.
   */
  public String getRedoPresentationName() {
    return "Redo "+getPresentationName();
  }
}
