package processing.app.graph;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.WindowConstants;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.InternalFrameListener;

import processing.app.Base;
import processing.app.DrawingArea;
import processing.app.Editor;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.TextAreaDefaults;

import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.swing.util.mxMouseControl;

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

    TextAreaDefaults editareaSettings = Editor.pdeTextAreaDefaults;
    editareaSettings.rows = TEXTAREA_DEFAULT_ROWS;
    editareaSettings.cols = TEXTAREA_DEFAULT_COLS;
    textarea = new JEditTextArea(editareaSettings);
    textarea.getDocument().setTokenMarker(Editor.pdeTokenMarker);
    textarea.setEditable(true);
    textarea.setHorizontalOffset(TEXTAREA_HORIZ_OFFSET);
    textarea.getPainter().setLineHighlightEnabled(false); // else looks funny
    JScrollPane scrollPane = new JScrollPane(textarea);
    scrollPane.setBorder(null);
    scrollPane.setOpaque(true);

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
//    triangleFrame.setBorder(new LineBorder(Color.cyan, 1)); //for debugging
    triangleFrame.setBorder(null);

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
//    buttonPanel.setBackground(Color.blue); //for debugging
    buttonPanel.setOpaque(false);
    buttonPanel.add(moveButton);
    buttonPanel.add(closeButton);

    buttonFrame = new JInternalFrame("", false, false, false, false);
    buttonFrame.setContentPane(buttonPanel);
    buttonFrame.setOpaque(false);
    buttonFrame.setSize(BUTTON_ICON_WIDTH*2+BUTTON_GAP, BUTTON_ICON_HEIGHT);
//    buttonFrame.setBorder(new LineBorder(Color.magenta, 1)); //for debugging
    buttonFrame.setBorder(null);

    // myriad event handling

    // stop showing buttons when this "frame" is defocused
    textarea.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        System.out.println("kCodeWindow >> textarea FocusGained");
        moveButton.setEnabled(true);
        closeButton.setEnabled(true);
      }
      public void focusLost(FocusEvent e) {
        System.out.println("kCodeWindow >> textarea focusLost");
        moveButton.setEnabled(false);
        closeButton.setEnabled(false);
      }
    });
    moveButton.addFocusListener(new FocusListener() {
      public void focusGained(FocusEvent e) {
        System.out.println("kCodeWindow >> moveButton FocusGained");
        moveButton.setEnabled(true);
        closeButton.setEnabled(true);
         textarea.requestFocus(); //put the cursor in the textarea
      }
      public void focusLost(FocusEvent e) {
        System.out.println("kCodeWindow >> moveButton focusLost");
        moveButton.setEnabled(false); //redundant probably
        closeButton.setEnabled(false);
      }
    });
    closeButton.addFocusListener(new FocusListener() { //need to do this or else closeButton action won't work
      public void focusGained(FocusEvent e) {
        System.out.println("kCodeWindow >> closeButton FocusGained");
        moveButton.setEnabled(true);
        closeButton.setEnabled(true);
         textarea.requestFocus(); //put the cursor in the textarea
      }
      public void focusLost(FocusEvent e) {
        System.out.println("kCodeWindow >> closeButton focusLost");
        moveButton.setEnabled(false);
        closeButton.setEnabled(false);
      }
    });
    // hide code window when close button is clicked
    closeButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("kCodeWindow >> closeButton actionPerformed");
        setVisible(false);
      }
    });
    textarea.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
          setVisible(false);
        if (e.getKeyCode() == KeyEvent.VK_ENTER)
          System.out.println("TEXT AREA HEARS ENTER KEY PRESSED");
      }
    });
    // add dragging function of the move button,
    // reset the triangle after mouse release, and shift the editFrame
    // along with the mouse when the user is moving the window
    mxMouseControl moveListener = createMoveListener();
    moveButton.addMouseListener(moveListener);
    moveButton.addMouseMotionListener(moveListener);
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
   * Gets the coordinate location of the center of this code window's associated
   * cell TODO if we ever have to refer to the cell one more time it'll make
   * sense to save a reference to it as a member variable
   * 
   * @return
   */
  protected Point getCellCenter() {
    mxGeometry geo = ((mxICell) ((mxGraphModel) ((DrawingArea) desktop)
        .getGraphComponent().getGraph().getModel()).getCell(id)).getGeometry();
    return new Point((int) geo.getCenterX(), (int) geo.getCenterY());
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
        Point buttonLocation = buttonFrame.getLocation();
        
        //TODO minor: what the heck is "18"? remove all hardwiring in triangle painting
        
        Point realLocation = new Point(e.getX() + buttonLocation.x,
            e.getY() + buttonLocation.y);
        Dimension editFrameSize = editFrame.getSize();

        Point editwindowlocation = new Point(realLocation.x
                                             - editFrameSize.width + 18,
            realLocation.y + 6);
        int X_FUDGE = 15;
        int Y_FUDGE = 5;
        int tribase = (Math.round((Math.min(editFrameSize.width,
                                            editFrameSize.height))) / 10 > 15) ? Math
                                                                                  .round((Math
                                                                                      .min(
                                                                                           editFrameSize.width,
                                                                                           editFrameSize.height))) / 10
                                                                              : 15; // How
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
        int width, height;

        if (editwindowlocation.x + editFrameSize.width / 2 >= cellX
            && editwindowlocation.x - X_FUDGE <= cellX
            && editwindowlocation.y >= cellY) { // Right , bottom (DEFAULT)
          direction = "SE";
          height = editwindowlocation.y - cellY;
          if (editwindowlocation.x >= cellX) {
            tilt = 0; // Pointing left
            width = Math.abs(editwindowlocation.x - cellX) + tribase;
          } else {
            tilt = 1; // Pointing right
            if (Math.abs(editwindowlocation.x - cellX) > tribase) {
              width = Math.abs(editwindowlocation.x - cellX);
            } else {
              width = tribase;
            }
          }
        } else if (editwindowlocation.x + editFrameSize.width / 2 < cellX
                   && editwindowlocation.x + editFrameSize.width > cellX
                   && editwindowlocation.y >= cellY + Y_FUDGE) { // Middle,
                                                                 // bottom
          direction = "S"; // SAME CODE AS "SE" DIRECTION BECAUSE OF OVERLAPPING
                           // OF BUTTONS AND TRIANGLE
          height = editwindowlocation.y - cellY;
          if (editwindowlocation.x >= cellX) {
            tilt = 0; // Pointing left
            width = Math.abs(editwindowlocation.x - cellX) + tribase;
          } else {
            tilt = 1; // Pointing right
            if (Math.abs(editwindowlocation.x - cellX) > tribase) {
              width = Math.abs(editwindowlocation.x - cellX);
            } else {
              width = tribase;
            }
          }

        } else if (editwindowlocation.x + editFrameSize.width <= cellX
                   && editwindowlocation.y + editFrameSize.height + 3 * Y_FUDGE >= cellY
                   || editwindowlocation.x + 3 * editFrameSize.width / 2 < cellX
                   && editwindowlocation.y + editFrameSize.height < cellY

        ) { // Left, middle

          direction = "W";
          width = Math.abs(editwindowlocation.x + editFrameSize.width
                               - cellX);
          if (editwindowlocation.y >= cellY) {
            tilt = 0; // Pointing up
            height = Math.abs(editwindowlocation.y - cellY) + tribase;
          } else {
            tilt = 1; // Pointing down
            if (Math.abs(editwindowlocation.y - cellY) > tribase) {
              height = Math.abs(editwindowlocation.y - cellY);
            } else {
              height = tribase;
            }
          }

        } else if (editwindowlocation.x + editFrameSize.width / 2 <= cellX
                   && editwindowlocation.x + 3 * editFrameSize.width / 2 >= cellX
                   && editwindowlocation.y + editFrameSize.height + 3 * Y_FUDGE < cellY) { // Left,
                                                                                           // top
          direction = "NW";
          height = Math.abs(editwindowlocation.y + editFrameSize.height
                                - cellY);
          if (editwindowlocation.x + editFrameSize.width > cellX) {
            tilt = 0; // Pointing left
            if (Math.abs(editwindowlocation.x + editFrameSize.width - cellX) > tribase) {
              width = Math.abs(editwindowlocation.x + editFrameSize.width
                               - cellX);
            } else {
              width = tribase;
            }

          } else {
            tilt = 1; // Pointing right
            width = Math
                .abs(editwindowlocation.x + editFrameSize.width - cellX)
                    + tribase;
          }

        } else if (editwindowlocation.x + editFrameSize.width / 2 > cellX
                   && editwindowlocation.x - editFrameSize.width / 2 <= cellX
                   && editwindowlocation.y + editFrameSize.height + Y_FUDGE < cellY) { // Middle,
                                                                                       // top
          direction = "N";
          height = Math.abs(editwindowlocation.y + editFrameSize.height
                                - cellY);
          if (editwindowlocation.x >= cellX) {
            tilt = 0; // Pointing left
            width = Math.abs(editwindowlocation.x - cellX) + tribase;
          } else {
            tilt = 1; // Pointing right
            if (Math.abs(editwindowlocation.x - cellX) > tribase) {
              width = Math.abs(editwindowlocation.x - cellX);
            } else {
              width = tribase;
            }

          }

        } else if (editwindowlocation.x - X_FUDGE > cellX
                   || editwindowlocation.y - editFrameSize.height / 2 >= cellY
                   && editwindowlocation.x - X_FUDGE <= cellX) {// Right, middle
          direction = "E";
          width = Math.abs(editwindowlocation.x - cellX);
          if (editwindowlocation.y >= cellY) {
            tilt = 0; // Pointing up
            height = Math.abs(editwindowlocation.y - cellY) + tribase;
          } else {
            tilt = 1; // Pointing down
            if (Math.abs(editwindowlocation.y - cellY) > tribase) {
              height = Math.abs(editwindowlocation.y - cellY);
            } else {
              height = tribase;
            }
          }

        } else {
          direction = "NONE";
          height = editwindowlocation.y - cellY;
          if (editwindowlocation.x >= cellX) {
            tilt = 0;
            width = Math.abs(editwindowlocation.x - cellX) + tribase;
          } else {
            tilt = 1;
            if (Math.abs(editwindowlocation.x - cellX) > tribase) {
              width = Math.abs(editwindowlocation.x - cellX);
            } else {
              width = tribase;
            }
          }

        }
        triangleFrame.setSize(width, height);
        ((Triangle) triangleFrame.getContentPane()).setGeometry(direction, tilt, width, height, tribase);
        setFrameGeometry(direction, tilt, realLocation.x - editFrameSize.width
                                     + buttonFrame.getWidth()*3/4, realLocation.y - buttonFrame.getWidth()/2);
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

        buttonFrame.setLocation(windowloc.x + windowsize.width - buttonFrame.getWidth(),
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
   * Return the text editor document model; used by Editor to monitor code
   * window document updates
   * 
   * @return
   */
  public javax.swing.text.Document getDocument() {
    return textarea.getDocument();
  }

  public void setVisible(boolean b) {
    editFrame.setVisible(b);
    buttonFrame.setVisible(b);
    triangleFrame.setVisible(b);
    ((DrawingArea) desktop).fireCodeWindowEvent();
    if (b)
      textarea.requestFocus();
  }

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
    resetTriangle();
    setFrameGeometry("NONE", 0, x, y);
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

  // Resets the triangle
  protected void resetTriangle() {
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
  protected class Triangle extends JComponent {

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
      g.setColor(Color.WHITE);

      g.fillPolygon(triangle);

      // g.setColor(Color.LIGHT_GRAY);
      // g.drawLine(p1.x,p1.y, p2.x, p2.y);
      // g.drawLine(p3.x,p3.y, p1.x, p1.y);

    }
  }
}
