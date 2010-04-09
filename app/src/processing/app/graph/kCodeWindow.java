package processing.app.graph;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import processing.app.Base;
import processing.app.DrawingArea;
import processing.app.Editor;
import processing.app.Preferences;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.app.syntax.TextAreaDefaults;
import processing.app.util.kConstants;

import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.util.mxMouseControl;
import com.mxgraph.view.mxCellState;

//TODO at some future date: make right-click popup menus for code windows

/*
 * For the nth time, stop trying to make this something 
 * other than 3 internal frames
 */
public class kCodeWindow {

  private static final int TEXTAREA_HORIZ_OFFSET = 3;
  // model
  protected final String id;
  protected JDesktopPane desktop;

  // swing components
  protected JEditTextArea textarea;
  protected JInternalFrame editFrame;
  protected JInternalFrame triangleFrame;
  protected JInternalFrame buttonFrame;
  protected JButton closeButton;
  protected JButton moveButton;

  protected static final int TEXTAREA_DEFAULT_ROWS = 20;
  protected static final int TEXTAREA_DEFAULT_COLS = 200;
  protected static final int editFrame_DEFAULT_WIDTH = 150;
  protected static final int editFrame_DEFAULT_HEIGHT = 75;

  // triangle stuff
  protected String direction;
  protected static final int TRIANGLE_BASE = 15;
  protected static final int TRIANGLE_DEFAULT_HEIGHT = 25;
  private static final int BUTTON_ICON_HEIGHT = 15; //14
  private static final int BUTTON_ICON_WIDTH = 15; //12
  private static final int BUTTON_GAP = 3; //12

  /**
   * Constructor #1
   * 
   * @param cell
   * @param desktop
   */
  public kCodeWindow(mxICell cell, JDesktopPane desktop) {
    this(cell.getId(), ((kCellValue) cell.getValue()).getLabel(), desktop);
  }

  /**
   * Constructor: The one we're working on right now.
   * 
   * @param id
   * @param label
   * @param desktop
   */
  public kCodeWindow(String id, String label, JDesktopPane desktop) {

    this.id = id;
    this.desktop = desktop;

    // make the editor portions

    TextAreaDefaults editareaSettings = new PdeTextAreaDefaults();
    editareaSettings.rows = TEXTAREA_DEFAULT_ROWS;
    editareaSettings.cols = TEXTAREA_DEFAULT_COLS;
    textarea = new JEditTextArea(editareaSettings);
    textarea.getDocument().setTokenMarker(Editor.pdeTokenMarker);
    textarea.setEventsEnabled(false); //suppress JEditTextArea events (not that anyone is listening to it)
    textarea.setEditable(true);
    textarea.setHorizontalOffset(TEXTAREA_HORIZ_OFFSET);
    textarea.getPainter().setLineHighlightEnabled(false); // else looks funny
    textarea.getPainter().setBackground(kConstants.CODE_WINDOW_COLOR);
    setShortcutKeystrokes();
    JScrollPane scrollPane = new JScrollPane(textarea);
    scrollPane.setBorder(null);
//    scrollPane.setOpaque(true);

    editFrame = new JInternalFrame(label, true, false, false, false);
    editFrame.setContentPane(textarea);
    editFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    editFrame.setSize(editFrame_DEFAULT_WIDTH, editFrame_DEFAULT_HEIGHT);
    editFrame.setBorder(null);
    editFrame.setOpaque(true);

    // make the triangle
    triangleFrame = new JInternalFrame("", false, false, false, false);
    triangleFrame.setOpaque(false);
    triangleFrame.setContentPane(new Triangle("SE", 0, TRIANGLE_BASE,
        TRIANGLE_DEFAULT_HEIGHT, TRIANGLE_BASE));
    triangleFrame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    triangleFrame.setSize(TRIANGLE_BASE, TRIANGLE_DEFAULT_HEIGHT);
    triangleFrame.setBorder(null);
    triangleFrame.getContentPane().setBackground(new Color(0,0,0,0));
    triangleFrame.setBackground(new Color(0,0,0,0));

    // remove the ability to move the triangle iframe
    MouseMotionListener[] actions = (MouseMotionListener[]) triangleFrame
        .getListeners(MouseMotionListener.class);
    for (int i = 0; i < actions.length; i++)
      triangleFrame.removeMouseMotionListener(actions[i]);

    // make the buttons
    moveButton = new JButton(Base.getImageIcon("codewindow-activ-move.gif", desktop));
    moveButton.setDisabledIcon(Base.getImageIcon("codewindow-inact-move.gif", desktop));
    moveButton.setVisible(true);
    moveButton.setBorder(null);
    moveButton.setOpaque(false);
    moveButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    closeButton = new JButton(Base.getImageIcon("codewindow-activ-close.gif", desktop));
    closeButton.setDisabledIcon(Base.getImageIcon("codewindow-inact-close.gif", desktop));
    closeButton.setVisible(true);
    closeButton.setBorder(null);
    closeButton.setOpaque(false);
    closeButton.setCursor(new Cursor(Cursor.HAND_CURSOR));
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 1, 0));
    buttonPanel.setBorder(null);
    buttonPanel.setOpaque(false);
    buttonPanel.add(moveButton);
    buttonPanel.add(closeButton);

    buttonFrame = new JInternalFrame("", false, false, false, false);
    buttonFrame.setContentPane(buttonPanel);
    buttonFrame.setOpaque(false);
    buttonFrame.setSize(BUTTON_ICON_WIDTH*2+BUTTON_GAP, BUTTON_ICON_HEIGHT);
    buttonFrame.setBorder(null);

    // myriad event handling

    installFocusHandlers(buttonPanel);
    
    // hide code window when escape key is hit
    textarea.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
          setVisible(false);
        if (e.getKeyCode() == KeyEvent.VK_ENTER) { //HACK force enter key to work (cause i dunno why it doesn't) TODO dun hack.
          textarea.setSelectedText("\n");
        }
      }
    });
    // add dragging function of the move button,
    // reset the triangle after mouse release, and shift the editFrame
    // along with the mouse when the user is moving the window
    mxMouseControl moveListener = createMoveListener();
    moveButton.addMouseListener(moveListener);
    moveButton.addMouseMotionListener(moveListener);
    closeButton.addMouseListener(createCloseListener());
    // listens to resizing of editFrame and adjusts the position of the
    // buttons and the shape of the triangle accordingly
    editFrame.addComponentListener(createResizeListener());
    // when code windows are on top of each other, layers them correctly
    // such that when the user clicks on any part of a code window
    // all three component internal frames are brought to the top
    // so they appear "focused" also
    InternalFrameListener iframeListener = new InternalFrameAdapter() {
      public void internalFrameActivated(InternalFrameEvent e) {
        moveToFrontLayer();
      }

      public void internalFrameDeactivated(InternalFrameEvent e) {
        moveToBackLayer();
      }
    };
    editFrame.addInternalFrameListener(iframeListener);
    buttonFrame.addInternalFrameListener(iframeListener);
    triangleFrame.addInternalFrameListener(iframeListener);

    // add everything to desktop

    desktop.add(editFrame);
    desktop.add(buttonFrame);
    desktop.add(triangleFrame);
    moveToBackLayer();

  }

  /**
   * @param buttonPanel
   */
  protected void installFocusHandlers(JPanel buttonPanel) {
    // disable buttons when this "frame" is defocused
    FocusListener focusListener = new FocusListener() {
      public void focusGained(FocusEvent e) {
        moveButton.setEnabled(true);
        closeButton.setEnabled(true);
      }
      public void focusLost(FocusEvent e) {
        moveButton.setEnabled(false);
        closeButton.setEnabled(false);
      }
    };
    textarea.addFocusListener(focusListener);
    //have to put the focus listener on every component else it doesn't work every time
    buttonPanel.addFocusListener(focusListener);
    moveButton.addFocusListener(focusListener);
    closeButton.addFocusListener(focusListener);
  }
  
  /**
   * Add close function for the close button because of focus issues the
   * normal ActionPerformed route requires too many clicks.
   */
  protected MouseAdapter createCloseListener() {
    return new MouseAdapter() {
      public void mouseReleased(MouseEvent e) {
//        System.out.println("kCW >> mouse clicked on close window");
        setVisible(false);
      }
    };
  }

  /**
   * Add dragging function of the move button, reset the triangle after mouse
   * release, and shift the editFrame along with the mouse when the user is
   * moving the window
   * 
   * @return
   */
  protected mxMouseControl createMoveListener() {
    return new mxMouseControl() {
      public void mouseDragged(MouseEvent e) {
        Point buttonLocation = buttonFrame.getLocation();
        Point realLocation = new Point(e.getX() + buttonLocation.x,
            e.getY() + buttonLocation.y);
        Dimension editFrameSize = editFrame.getSize();

        triangleFrame.setVisible(false);
        editFrame.setLocation(e.getX(), e.getY());

        setFrameGeometry("SE", 0, realLocation.x - editFrameSize.width + buttonFrame.getWidth()*3/4,
                    realLocation.y - buttonFrame.getHeight());
      }

      public void mouseReleased(MouseEvent e) {
        updateTriangle(e.getX() + buttonFrame.getLocation().x,
            e.getY() + buttonFrame.getLocation().y);
        triangleFrame.setVisible(true);
      }
    };
  }

  /**
   * Listens to resizing of editFrame and adjusts the position of the buttons
   * and the shape of the triangle accordingly
   * 
   * @author susiefu
   * @return
   */
  protected ComponentListener createResizeListener() {
    return new ComponentAdapter() {

      public void componentResized(ComponentEvent e) {
        moveButtons();
        moveTriangle();
      }

      private void moveButtons() {
        Point windowloc = new Point(editFrame.getLocation());
        Dimension windowsize = new Dimension(editFrame.getSize());

        buttonFrame.setLocation(windowloc.x + windowsize.width - buttonFrame.getWidth() + 1,
                                windowloc.y - buttonFrame.getHeight());
      }

      /**
       * Moves the triangle according to the size of the editFrame
       */
      private void moveTriangle() {

        Point windowloc = new Point(editFrame.getLocation());
        Dimension windowsize = editFrame.getSize();

        int X_FUDGE = 15;
        int Y_FUDGE = 5;
        int tribase = (Math.round((Math
            .min(windowsize.width, windowsize.height))) / 10 > 15) ? Math
            .round((Math.min(windowsize.width, windowsize.height))) / 10 : 15; // How
                                                                               // wide
                                                                               // the
                                                                               // base
                                                                               // of
                                                                               // the
                                                                               // triangle
                                                                               // is
        int tilt = -1;
        int cellX = getCellCenter().x;
        int cellY = getCellCenter().y;

        if (direction == "SE") { // Right , bottom (DEFAULT)
          // DO NOTHING
        } else if (direction == "S") { // Middle, bottom
          // DO NOTHING
        }

        else if (direction == "W") { // Left, middle

          if (windowloc.x + windowsize.width > cellX) {
            direction = "NONE";
          } else {
            int height;
            int width = Math.abs(windowloc.x + windowsize.width - cellX);
            if (windowloc.y >= cellY) {
              tilt = 0; // Pointing up
              height = Math.abs(windowloc.y - cellY) + tribase;
            } else {
              tilt = 1; // Pointing down
              if (Math.abs(windowloc.y - cellY) > tribase) {
                height = Math.abs(windowloc.y - cellY);
              } else {
                height = tribase;
              }
            }
            triangleFrame.setSize(width, height);
            ((Triangle) triangleFrame.getContentPane()).setGeometry(direction, tilt, width, height, tribase);
            triangleFrame.setLocation(windowloc.x + windowsize.width,
                                      windowloc.y);
            triangleFrame.setVisible(true);
          }

        } else if (direction == "NW") { // Left, top

          if (windowloc.y + windowsize.height > cellY) {
            direction = "NONE";
          }

          else {

            int height = Math.abs(windowloc.y + windowsize.height - cellY);
            int width;
            if (windowloc.x + windowsize.width > cellX) {
              tilt = 0; // Pointing left
              width = Math.abs(windowloc.x + windowsize.width - cellX);
            } else {
              tilt = 1; // Pointing right
              width = Math.abs(windowloc.x + windowsize.width - cellX)
                      + tribase;
            }
            triangleFrame.setSize(width, height);
            ((Triangle) triangleFrame.getContentPane()).setGeometry(direction, tilt, width, height, tribase);
            if (tilt == 0) {
              triangleFrame.setLocation(windowloc.x + windowsize.width - width,
                                        windowloc.y + windowsize.height);
            } else {
              triangleFrame.setLocation(windowloc.x + windowsize.width
                                        - tribase, windowloc.y
                                                   + windowsize.height);
            }
            triangleFrame.setVisible(true);

          }

        } else if (direction == "N") { // Middle, top

          if (windowloc.y + windowsize.height > cellY) {
            direction = "NONE";
          }

          else {

            int height = Math.abs(windowloc.y + windowsize.height - cellY);
            int width;
            if (windowloc.x >= cellX) {
              tilt = 0; // Pointing left
              width = Math.abs(windowloc.x - cellX) + tribase;
            } else {
              tilt = 1; // Pointing right
              if (Math.abs(windowloc.x - cellX) > tribase) {
                width = Math.abs(windowloc.x - cellX);
              } else {
                width = tribase;
              }
            }
            triangleFrame.setSize(width, height);
            ((Triangle) triangleFrame.getContentPane()).setGeometry(direction, tilt, width, height, tribase);
            
            if (tilt == 0) {
              // There is a fudge factor of 2 b/c of the space between the
              // border of the editFrame and the line that shows the border
              triangleFrame.setLocation(windowloc.x + tribase - width,
                                        windowloc.y + windowsize.height - 2);
            } else {
              // There is a fudge factor of 2 b/c of the space between the
              // border of the editFrame and the line that shows the border
              triangleFrame.setLocation(windowloc.x, windowloc.y
                                                     + windowsize.height - 2);
            }
            triangleFrame.setVisible(true);
          }

        } else if (direction == "E") { // Right, middle
          // DO NOTHING
        } else { // DIRECTION = "NONE"

          triangleFrame.setVisible(false);

          if (windowloc.x + windowsize.width / 2 >= cellX
              && windowloc.y >= cellY + Y_FUDGE) { // Right , bottom (DEFAULT)
            direction = "SE";

          } else if (windowloc.x + windowsize.width / 2 < cellX
                     && windowloc.x + windowsize.width > cellX
                     && windowloc.y >= cellY + Y_FUDGE) { // Middle, bottom
            direction = "S"; // SAME CODE AS "SE" DIRECTION BECAUSE OF
                             // OVERLAPPING OF BUTTONS AND TRIANGLE

          } else if (windowloc.x + windowsize.width <= cellX
                     && windowloc.y + windowsize.height + 10 * Y_FUDGE >= cellY) { // Left,
                                                                                   // middle
            direction = "W";

          } else if (windowloc.x + windowsize.width / 2 <= cellX
                     && windowloc.y + windowsize.height + 10 * Y_FUDGE < cellY) { // Left,
                                                                                  // top
            direction = "NW";

          } else if (windowloc.x + windowsize.width / 2 > cellX
                     && windowloc.x < cellX + X_FUDGE
                     && windowloc.y + windowsize.height + Y_FUDGE < cellY) { // Middle,
                                                                             // top
            direction = "N";

          } else if (windowloc.x >= cellX + X_FUDGE
                     && windowloc.y - Y_FUDGE < cellY) { // Right, middle
            direction = "E";

          } else {
            direction = "NONE";

          }
        }
      }

      public void componentShown(ComponentEvent e) {
        triangleFrame.setVisible(true);
      }

    };
  }

  /**
   * To support drag-drop of strings, and cut-copy-paste actions in the code window text editor.
   */
  protected void setShortcutKeystrokes() {
    final int SHORTCUT_KEY_MASK = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    
    InputMap imap = textarea.getInputMap();
    imap.put(KeyStroke.getKeyStroke('X', SHORTCUT_KEY_MASK),
        TransferHandler.getCutAction().getValue(Action.NAME));
    imap.put(KeyStroke.getKeyStroke('C', SHORTCUT_KEY_MASK),
        TransferHandler.getCopyAction().getValue(Action.NAME));
    imap.put(KeyStroke.getKeyStroke('V', SHORTCUT_KEY_MASK),
        TransferHandler.getPasteAction().getValue(Action.NAME));
    imap.put(KeyStroke.getKeyStroke('A', SHORTCUT_KEY_MASK), "selectAll");
    imap.put(KeyStroke.getKeyStroke('/', SHORTCUT_KEY_MASK), "commentUncomment");
    imap.put(KeyStroke.getKeyStroke(']', SHORTCUT_KEY_MASK), "increaseIndent");
    imap.put(KeyStroke.getKeyStroke('[', SHORTCUT_KEY_MASK), "decreaseIndent");
    
    ActionMap amap = textarea.getActionMap();
    amap.put(TransferHandler.getCutAction().getValue(Action.NAME), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> cut "+e.getSource());
        ((JEditTextArea) e.getSource()).cut();
      }
    });
    amap.put(TransferHandler.getCopyAction().getValue(Action.NAME), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> copy "+e.getSource());
        ((JEditTextArea) e.getSource()).copy();
      }
    });
    amap.put(TransferHandler.getPasteAction().getValue(Action.NAME), new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> paste "+e.getSource());
        ((JEditTextArea) e.getSource()).paste();
      }
    });
    amap.put("selectAll", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> select all "+e.getSource());
        ((JEditTextArea) e.getSource()).selectAll();
      }
    });
    amap.put("commentUncomment", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> comment uncomment"+e.getSource());
        handleCommentUncomment();
      }
    });
    amap.put("increaseIndent", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> increaseIndent"+e.getSource());
        handleIndentOutdent(true);
      }
    });
    amap.put("decreaseIndent", new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow ActionMap >> decreaseIndent"+e.getSource());
        handleIndentOutdent(false);
      }
    });
  }
  
  /**
   * @see processing.app.Editor#handleCommentUncomment
   * @author fry
   */
  public void handleCommentUncomment() {
    //TODO startCompoundEdit();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    int lastLineStart = textarea.getLineStartOffset(stopLine);
    int selectionStop = textarea.getSelectionStop();
    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (textarea.isSelectionActive()) {
        stopLine--;
      }
    }

    // If the text is empty, ignore the user.
    // Also ensure that all lines are commented (not just the first)
    // when determining whether to comment or uncomment.
    int length = textarea.getDocumentLength();
    boolean commented = true;
    for (int i = startLine; commented && (i <= stopLine); i++) {
      int pos = textarea.getLineStartOffset(i);
      if (pos + 2 > length) {
        commented = false;
      } else {
        // Check the first two characters to see if it's already a comment.
        String begin = textarea.getText(pos, 2);
        //System.out.println("begin is '" + begin + "'");
        commented = begin.equals("//");
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartOffset(line);
      if (commented) {
        // remove a comment
        textarea.select(location, location+2);
        if (textarea.getSelectedText().equals("//")) {
          textarea.setSelectedText("");
          //pseudo-code:
          //find open code windows
          //find localLocation
          //insert/remove
          //update all codeblocks after
        }
      } else {
        // add a comment
        textarea.select(location, location);
        textarea.setSelectedText("//");
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
    
//    stopCompoundEdit();
  }
  
  /**
   * @see processing.app.Editor#handleIndentOutdent
   * @author fry
   */
  public void handleIndentOutdent(boolean indent) {
    int tabSize = Preferences.getInteger("editor.tabs.size");
    String tabString = "                        ".substring(0, tabSize);

//    TODO startCompoundEdit();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    int lastLineStart = textarea.getLineStartOffset(stopLine);
    int selectionStop = textarea.getSelectionStop();
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (textarea.isSelectionActive()) {
        stopLine--;
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartOffset(line);

      if (indent) {
        textarea.select(location, location);
        textarea.setSelectedText(tabString);

      } else {  // outdent
        textarea.select(location, location + tabSize);
        // Don't eat code if it's not indented
        if (textarea.getSelectedText().equals(tabString)) {
          textarea.setSelectedText("");
        }
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
//    stopCompoundEdit();
  }
  
  /**
   * Get the id of this code window (matches the id of its associated cell)
   * 
   * @return
   */
  public String getId() {
    return id;
  }

  /**
   * Return the text editor component; used by Editor to monitor code window
   * document updates
   * 
   * @return
   */
  public JEditTextArea getTextArea() {
    return textarea;
  }

  /**
   * Show the code window and all its component parts
   * @param b
   */
  public void setVisible(boolean b) {
    System.out.println("kCodeWindow opaque >> editFrame="+editFrame.isOpaque()+ " buttonFrame="+buttonFrame.isOpaque()+" triangleFrame="+triangleFrame.isOpaque());
    editFrame.setVisible(b);
    buttonFrame.setVisible(b);
    triangleFrame.setVisible(b);
    ((DrawingArea) desktop).fireCodeWindowEvent();
    if (b)
      textarea.requestFocus();
  }

  /**
   * Returns if the code window (represented by its textarea) is visible
   * @param b
   */
  public boolean isVisible() {
    return editFrame.isVisible();
  }

  /**
   * Fills in default direction and tilt for a code 
   * window popping out of x,y location.
   * 
   * @param x
   * @param y
   */
  public void setLocation(int x, int y) {
    resetTriangleToDefault();
    setFrameGeometry("NONE", 0, x, y);
  }
   
  /**
   * Will update the triangle assuming that the cell center has moved but
   * that the edit window and button panel has not.
   * @author achang
   */
  public void updateTriangle() {
    Point buttonLocation = buttonFrame.getLocation();
    updateTriangle(buttonFrame.getWidth()/4 + buttonLocation.x, buttonFrame.getHeight()/2 + buttonLocation.y);
  }
  
  /**
   * Will update the triangle given the absolute location of the center of
   * the moveButton.
   * @author achang, adapted from code by susiefu
   */
  public void updateTriangle(int x, int y) {

    Point realLocation = new Point(x,y);
    Dimension editFrameSize = editFrame.getSize();
    Point editFrameLocation = editFrame.getLocation();
//        new Point(realLocation.x - editFrameSize.width + buttonFrame.getWidth()*3/4,
//        realLocation.y + buttonFrame.getHeight()/2);
    int X_FUDGE = 15;
    int Y_FUDGE = 5;
    int tribase = (Math.round((Math.min(editFrameSize.width, 
                                        editFrameSize.height))) / 10 > TRIANGLE_BASE) ?
                  Math.round((Math.min(editFrameSize.width, editFrameSize.height))) / 10 
                  : TRIANGLE_BASE;
    int tilt = -1;
    int cellX = getCellCenter().x;
    int cellY = getCellCenter().y;
    System.out.println("kCW >> updateTriangle >> cellCenter="+getCellCenter());
    int width, height;

    if (editFrameLocation.x + editFrameSize.width / 2 >= cellX
        && editFrameLocation.x - X_FUDGE <= cellX
        && editFrameLocation.y >= cellY) { // Right , bottom (DEFAULT)
      direction = "SE";
      height = editFrameLocation.y - cellY;
      if (editFrameLocation.x >= cellX) {
        tilt = 0; // Pointing left
        width = Math.abs(editFrameLocation.x - cellX) + tribase;
      } else {
        tilt = 1; // Pointing right
        if (Math.abs(editFrameLocation.x - cellX) > tribase) {
          width = Math.abs(editFrameLocation.x - cellX);
        } else {
          width = tribase;
        }
      }
    } else if (editFrameLocation.x + editFrameSize.width / 2 < cellX
               && editFrameLocation.x + editFrameSize.width > cellX
               && editFrameLocation.y >= cellY + Y_FUDGE) { // Middle,
                                                             // bottom
      direction = "S"; // SAME CODE AS "SE" DIRECTION BECAUSE OF OVERLAPPING
                       // OF BUTTONS AND TRIANGLE
      height = editFrameLocation.y - cellY;
      if (editFrameLocation.x >= cellX) {
        tilt = 0; // Pointing left
        width = Math.abs(editFrameLocation.x - cellX) + tribase;
      } else {
        tilt = 1; // Pointing right
        if (Math.abs(editFrameLocation.x - cellX) > tribase) {
          width = Math.abs(editFrameLocation.x - cellX);
        } else {
          width = tribase;
        }
      }

    } else if (editFrameLocation.x + editFrameSize.width <= cellX
               && editFrameLocation.y + editFrameSize.height + 3 * Y_FUDGE >= cellY
               || editFrameLocation.x + 3 * editFrameSize.width / 2 < cellX
               && editFrameLocation.y + editFrameSize.height < cellY

    ) { // Left, middle

      direction = "W";
      width = Math.abs(editFrameLocation.x + editFrameSize.width
                           - cellX);
      if (editFrameLocation.y >= cellY) {
        tilt = 0; // Pointing up
        height = Math.abs(editFrameLocation.y - cellY) + tribase;
      } else {
        tilt = 1; // Pointing down
        if (Math.abs(editFrameLocation.y - cellY) > tribase) {
          height = Math.abs(editFrameLocation.y - cellY);
        } else {
          height = tribase;
        }
      }

    } else if (editFrameLocation.x + editFrameSize.width / 2 <= cellX
               && editFrameLocation.x + 3 * editFrameSize.width / 2 >= cellX
               && editFrameLocation.y + editFrameSize.height + 3 * Y_FUDGE < cellY) { // Left,
                                                                                       // top
      direction = "NW";
      height = Math.abs(editFrameLocation.y + editFrameSize.height
                            - cellY);
      if (editFrameLocation.x + editFrameSize.width > cellX) {
        tilt = 0; // Pointing left
        if (Math.abs(editFrameLocation.x + editFrameSize.width - cellX) > tribase) {
          width = Math.abs(editFrameLocation.x + editFrameSize.width
                           - cellX);
        } else {
          width = tribase;
        }

      } else {
        tilt = 1; // Pointing right
        width = Math
            .abs(editFrameLocation.x + editFrameSize.width - cellX)
                + tribase;
      }

    } else if (editFrameLocation.x + editFrameSize.width / 2 > cellX
               && editFrameLocation.x - editFrameSize.width / 2 <= cellX
               && editFrameLocation.y + editFrameSize.height + Y_FUDGE < cellY) { // Middle,
                                                                                   // top
      direction = "N";
      height = Math.abs(editFrameLocation.y + editFrameSize.height
                            - cellY);
      if (editFrameLocation.x >= cellX) {
        tilt = 0; // Pointing left
        width = Math.abs(editFrameLocation.x - cellX) + tribase;
      } else {
        tilt = 1; // Pointing right
        if (Math.abs(editFrameLocation.x - cellX) > tribase) {
          width = Math.abs(editFrameLocation.x - cellX);
        } else {
          width = tribase;
        }

      }

    } else if (editFrameLocation.x - X_FUDGE > cellX
               || editFrameLocation.y - editFrameSize.height / 2 >= cellY
               && editFrameLocation.x - X_FUDGE <= cellX) {// Right, middle
      direction = "E";
      width = Math.abs(editFrameLocation.x - cellX);
      if (editFrameLocation.y >= cellY) {
        tilt = 0; // Pointing up
        height = Math.abs(editFrameLocation.y - cellY) + tribase;
      } else {
        tilt = 1; // Pointing down
        if (Math.abs(editFrameLocation.y - cellY) > tribase) {
          height = Math.abs(editFrameLocation.y - cellY);
        } else {
          height = tribase;
        }
      }

    } else {
      direction = "NONE";
      height = editFrameLocation.y - cellY;
      if (editFrameLocation.x >= cellX) {
        tilt = 0;
        width = Math.abs(editFrameLocation.x - cellX) + tribase;
      } else {
        tilt = 1;
        if (Math.abs(editFrameLocation.x - cellX) > tribase) {
          width = Math.abs(editFrameLocation.x - cellX);
        } else {
          width = tribase;
        }
      }

    }
    triangleFrame.setSize(width, height);
    ((Triangle) triangleFrame.getContentPane()).setGeometry(direction, tilt, width, height, tribase);
    setFrameGeometry(direction, tilt, realLocation.x - editFrameSize.width
                                 + buttonFrame.getWidth()*3/4, realLocation.y - buttonFrame.getWidth()/2);
  }

  /**
   * Gets the coordinate location of the center of this code window's associated
   * cell TODO if we ever have to refer to the cell one more time it'll make
   * sense to save a reference to it as a member variable instead of saving the id...
   * Updated to use the cell state so this still works when graph is scaled/panned
   * 
   * @author achang
   */
  protected Point getCellCenter() {
    Object cell = ((mxGraphModel) ((DrawingArea) desktop)
        .getGraphComponent().getGraph().getModel()).getCell(id);
    mxCellState state = ((DrawingArea) desktop).getGraphComponent().getGraph().getView().getState(cell);
    return new Point((int) state.getCenterX(), (int) state.getCenterY());
  }

  /**
   * Move internal frames to front so it isn't covered by other code windows.
   * 
   * @author susiefu
   */
  protected void moveToFrontLayer() {
    editFrame.setLayer(2);
    buttonFrame.setLayer(2);
    triangleFrame.setLayer(2);
    editFrame.moveToFront();
    triangleFrame.moveToFront();
    buttonFrame.moveToFront();
  }

  /**
   * Allow internal frames to go behind other frames (i.e. code windows).
   * 
   * @author susiefu
   */
  protected void moveToBackLayer() {
    editFrame.setLayer(1);
    buttonFrame.setLayer(1);
    triangleFrame.setLayer(1);
    editFrame.moveToFront();
    triangleFrame.moveToFront();
    buttonFrame.moveToFront();
  }

  /**
   * Sets the location for the 3 internal frames
   * 
   * @author susiefu
   * @param direction
   * @param tilt
   * @param x
   * @param y
   */
  protected void setFrameGeometry(String direction, int tilt, int x, int y) {

    Dimension editwindowsize = editFrame.getSize();
    Dimension trisize = triangleFrame.getSize();
    int tribase = (Math.round((Math.min(editwindowsize.width,
                                        editwindowsize.height))) / 10 > 15) ? Math
                                                                               .round((Math
                                                                                   .min(
                                                                                        editwindowsize.width,
                                                                                        editwindowsize.height))) / 10
                                                                           : 15; // How
                                                                                 // wide
                                                                                 // the
                                                                                 // base
                                                                                 // of
                                                                                 // the
                                                                                 // triangle
                                                                                 // is
    int fudge = 1;
    
    // For the different directions
    if (direction == "SE") { // Default location
      if (tilt == 0) {
        triangleFrame.setLocation(x + tribase - trisize.width, y + 24
                                                               - trisize.height
                                                               + fudge);
      } else {
        triangleFrame.setLocation(x, y + 24 - trisize.height + fudge);
      }

    } else if (direction == "S") { // SAME AS "SE" DIRECTION
      if (tilt == 0) {
        triangleFrame.setLocation(x + tribase - trisize.width, y + 24
                                                               - trisize.height
                                                               + fudge);
      } else {
        triangleFrame.setLocation(x, y + 24 - trisize.height + fudge);
      }

    } else if (direction == "W") {
      if (tilt == 0) {
        triangleFrame.setLocation(x + editwindowsize.width - fudge,
                                  y + 24 - trisize.height + tribase);
      } else {
        triangleFrame.setLocation(x + editwindowsize.width - fudge, y + 24);
      }

    } else if (direction == "NW") {
      if (tilt == 0) {
        triangleFrame.setLocation(x + editwindowsize.width - trisize.width,
                                  y + 24 + editwindowsize.height - 2 * fudge);
      } else {
        triangleFrame.setLocation(x + editwindowsize.width - tribase,
                                  y + 25 + editwindowsize.height - 2 * fudge);
      }

    } else if (direction == "N") {
      if (tilt == 0) {
        triangleFrame.setLocation(x - trisize.width + tribase,
                                  y + 24 + editwindowsize.height - 2 * fudge);
      } else {
        triangleFrame
            .setLocation(x, y + 24 + editwindowsize.height - 2 * fudge);
      }

    } else if (direction == "E") {
      if (tilt == 0) { // Pointing up
        triangleFrame.setLocation(x - trisize.width + fudge, y + 24
                                                             - trisize.height
                                                             + tribase);
      } else {
        triangleFrame.setLocation(x - trisize.width + fudge, y + 24);
      }

    } else if (direction == "NONE") {
      if (tilt == 0) {
        triangleFrame.setLocation(x + tribase - trisize.width, y + 24
                                                               - trisize.height
                                                               + fudge);
      } else {
        triangleFrame.setLocation(x, y + 24 - trisize.height + fudge);
      }

    }
    editFrame.setLocation(x, y + 24);
    buttonFrame.setLocation(x + editwindowsize.width - buttonFrame.getWidth()+fudge, y + buttonFrame.getHeight()/2+fudge*2);

  }

  /**
   * Resets the triangle graphic to the fixed default location
   * 
   * @author susiefu
   */
  protected void resetTriangleToDefault() {
    int tribase = (Math.round((Math.min(editFrame.getSize().width, editFrame
        .getSize().height))) / 10 > 15) ? Math.round((Math.min(editFrame
        .getSize().width, editFrame.getSize().height))) / 10 : 15; // How wide
                                                                   // the base
                                                                   // of the
                                                                   // triangle
                                                                   // is
    triangleFrame.setSize(tribase, TRIANGLE_DEFAULT_HEIGHT);
    ((Triangle) triangleFrame.getContentPane()).setGeometry("SE", 0, tribase, TRIANGLE_DEFAULT_HEIGHT, tribase);
  }

  /**
   * Triangle graphic
   * 
   * @author susiefu
   */
  private class Triangle extends JComponent {

    private String triDirection;

    private int tilt, width, height, tribase;

    public Triangle(String direction, int tilt, int width, int height,
                    int tribase) {

      setGeometry(direction, tilt, width, height, tribase);
      setOpaque(false);
    }
    
    public void setGeometry(String direction, int tilt, int width, int height,
                       int tribase) {
      this.triDirection = direction;
      this.tilt = tilt;
      this.width = width;
      this.height = height;
      this.tribase = tribase; // How wide the base of the triangle is;
    }

    // Paints the triangle onto the iframe
    public void paintComponent(Graphics g) {

      super.paintComponent(g);

      Point p1, p2, p3;

      p1 = new Point(0, 0);
      p2 = new Point(width - tribase, height);
      p3 = new Point(width, height);

      if (triDirection == "SE" || triDirection == "S") {
        if (tilt == 0) {
          p1 = new Point(0, 0);
          p2 = new Point(width - tribase, height);
          p3 = new Point(width, height);
        } else if (tilt == 1) {
          p1 = new Point(width - 1, 0);
          p2 = new Point(0, height);
          p3 = new Point(tribase - 1, height);
        }
      }

      else if (triDirection == "W") {
        if (tilt == 0) {
          p1 = new Point(width, 0);
          p2 = new Point(0, height - tribase);
          p3 = new Point(0, height);
        } else if (tilt == 1) {
          p1 = new Point(width, height - 1);
          p2 = new Point(0, 0);
          p3 = new Point(0, tribase - 1);
        }
      }

      else if (triDirection == "NW" || triDirection == "N") {
        if (tilt == 0) { // Left tilt
          p1 = new Point(0, height);
          p2 = new Point(width, 0);
          p3 = new Point(width - tribase, 0);
        } else if (tilt == 1) { // Right tilt
          p1 = new Point(width - 1, height);
          p2 = new Point(tribase - 1, 0);
          p3 = new Point(0, 0);
        }
      }

      else if (triDirection == "E") {
        if (tilt == 0) {
          p1 = new Point(0, 0);
          p2 = new Point(width, height - tribase);
          p3 = new Point(width, height);
        } else if (tilt == 1) {
          p1 = new Point(0, height - 1);
          p2 = new Point(width, 0);
          p3 = new Point(width, tribase - 1);
        }
      }

      else {
        p1 = new Point(0, 0);
        p2 = new Point(0, TRIANGLE_DEFAULT_HEIGHT);
        p3 = new Point(tribase, TRIANGLE_DEFAULT_HEIGHT);
      }

      int[] xs = { p1.x, p2.x, p3.x };
      int[] ys = { p1.y, p2.y, p3.y };
      Polygon triangle = new Polygon(xs, ys, xs.length);
      g.setColor(kConstants.CODE_WINDOW_COLOR);

      g.fillPolygon(triangle);

      // g.setColor(Color.LIGHT_GRAY);
      // g.drawLine(p1.x,p1.y, p2.x, p2.y);
      // g.drawLine(p3.x,p3.y, p1.x, p1.y);

    }
  }
}
