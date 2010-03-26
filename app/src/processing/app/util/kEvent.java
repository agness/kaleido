package processing.app.util;

/**
 * Kaleido Event Handling
 * Extending the mxEvent system with some event definitions of our own.
 * @see com.mxgraph.util.mxEvent
 * @author achang
 */
public class kEvent {

  /**
   * Fired by drawingArea when entering of one of the drawing tools mode
   */
  public static final String TOOL_BEGIN = "toolBegin";

  /**
   * Fired by drawingArea when coming out of one of the drawing tools mode
   */
  public static final String TOOL_END = "toolEnd";

  /**
   * Fired by drawingArea whenever a code window is opened or closed (i.e. visibility changes)
   */
  public static final String CODE_WINDOW_VISIBLE = "codeWindowVisible";

  /**
   * Fired by JEditTextArea.select(int, int)
   */
  public static final String TEXT_SELECTED = "selectionChange";
  
}
