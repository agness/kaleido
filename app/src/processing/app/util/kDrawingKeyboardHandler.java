package processing.app.util;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;

import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIManager;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxGraphActions;

/**
 * This basically differs from the original mxKeyBoardHandler in that it uses
 * the key mask for cross-platform use. Whatever is already covered by menu
 * accelerators is not covered here. Was originally going to make a keyboard map
 * for the text side as well, but menu accelerators seem sufficient there, and
 * no one has time to change what is already working fine.
 * 
 * @author achang
 */
public class kDrawingKeyboardHandler {

  /** Command on Mac OS X, Ctrl on Windows and Linux */
  public static final int SHORTCUT_KEY_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  /** Command-W on Mac OS X, Ctrl-W on Windows and Linux */
  public static final KeyStroke WINDOW_CLOSE_KEYSTROKE =
    KeyStroke.getKeyStroke('W', SHORTCUT_KEY_MASK);
  /** Command-Shift on Mac OS X, Ctrl-Shift on Windows and Linux */
  public static final int SHORTCUT_SHIFT_KEY_MASK = ActionEvent.SHIFT_MASK |
  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  /** Command-Option on Mac OS X, Ctrl-Alt on Windows and Linux */
  public static final int SHORTCUT_ALT_KEY_MASK = ActionEvent.ALT_MASK |
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  /**
   * Constructor: installs input map and action map on given graphComponent.
   * @param graphComponent
   */
  public kDrawingKeyboardHandler(mxGraphComponent graphComponent)
  {
    installKeyboardActions(graphComponent);
  }

  /**
   * Invoked as part from the boilerplate install block.
   * @author mxgraph
   */
  protected void installKeyboardActions(mxGraphComponent graphComponent)
  {
    InputMap inputMap = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    SwingUtilities.replaceUIInputMap(graphComponent,
        JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, inputMap);

    inputMap = getInputMap(JComponent.WHEN_FOCUSED);
    SwingUtilities.replaceUIInputMap(graphComponent,
        JComponent.WHEN_FOCUSED, inputMap);
    SwingUtilities.replaceUIActionMap(graphComponent, createActionMap());
  }

  /**
   * Return JTree's input map.
   */
  protected InputMap getInputMap(int condition)
  {
    InputMap map = null;

    if (condition == JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    {
      map = (InputMap) UIManager.get("ScrollPane.ancestorInputMap");
    }
    else if (condition == JComponent.WHEN_FOCUSED)
    {
      map = new InputMap();

      map.put(KeyStroke.getKeyStroke("F2"), "edit");
      map.put(KeyStroke.getKeyStroke("DELETE"), "delete");
      map.put(KeyStroke.getKeyStroke("UP"), "selectParent");
      map.put(KeyStroke.getKeyStroke("DOWN"), "selectChild");
      map.put(KeyStroke.getKeyStroke("RIGHT"), "selectNext");
      map.put(KeyStroke.getKeyStroke("LEFT"), "selectPrevious");
      map.put(KeyStroke.getKeyStroke("PAGE_DOWN"), "enterGroup");
      map.put(KeyStroke.getKeyStroke("PAGE_UP"), "exitGroup");
      map.put(KeyStroke.getKeyStroke("HOME"), "home");
      map.put(KeyStroke.getKeyStroke("ENTER"), "expand");
      map.put(KeyStroke.getKeyStroke("BACK_SPACE"), "collapse");  
//      map.put(KeyStroke.getKeyStroke("control A"), "selectAll");
//      map.put(KeyStroke.getKeyStroke("control D"), "selectNone");
      map.put(KeyStroke.getKeyStroke((int) 'D', SHORTCUT_KEY_MASK), "selectNone");
//      map.put(KeyStroke.getKeyStroke("control X"), "cut");
//      map.put(KeyStroke.getKeyStroke("CUT"), "cut");
//      map.put(KeyStroke.getKeyStroke("control C"), "copy");
//      map.put(KeyStroke.getKeyStroke("COPY"), "copy");
//      map.put(KeyStroke.getKeyStroke("control V"), "paste");
//      map.put(KeyStroke.getKeyStroke("PASTE"), "copy");
//      map.put(KeyStroke.getKeyStroke("control G"), "group");
//      map.put(KeyStroke.getKeyStroke("control U"), "ungroup");
//      map.put(KeyStroke.getKeyStroke("control ADD"), "zoomIn");
//      map.put(KeyStroke.getKeyStroke("control SUBTRACT"), "zoomOut");
    }

    return map;
  }

  /**
   * Return the mapping between JTree's input map and JGraph's actions.
   */
  protected ActionMap createActionMap()
  {
    ActionMap map = (ActionMap) UIManager.get("ScrollPane.actionMap");

    map.put("edit", mxGraphActions.getEditAction());
    map.put("delete", mxGraphActions.getDeleteAction());
    map.put("home", mxGraphActions.getHomeAction());
    map.put("enterGroup", mxGraphActions.getEnterGroupAction());
    map.put("exitGroup", mxGraphActions.getExitGroupAction());
    map.put("collapse", mxGraphActions.getCollapseAction());
    map.put("expand", mxGraphActions.getExpandAction());
    map.put("toBack", mxGraphActions.getToBackAction());
    map.put("toFront", mxGraphActions.getToFrontAction());
    map.put("selectNone", mxGraphActions.getSelectNoneAction());
//    map.put("selectAll", mxGraphActions.getSelectAllAction());
    map.put("selectNext", mxGraphActions.getSelectNextAction());
    map.put("selectPrevious", mxGraphActions.getSelectPreviousAction());
    map.put("selectParent", mxGraphActions.getSelectParentAction());
    map.put("selectChild", mxGraphActions.getSelectChildAction());
//    map.put("cut", TransferHandler.getCutAction());
//    map.put("copy", TransferHandler.getCopyAction());
//    map.put("paste", TransferHandler.getPasteAction());
//    map.put("group", mxGraphActions.getGroupAction());
//    map.put("ungroup", mxGraphActions.getUngroupAction());
//    map.put("zoomIn", mxGraphActions.getZoomInAction());
//    map.put("zoomOut", mxGraphActions.getZoomOutAction());

    return map;
  }
}
