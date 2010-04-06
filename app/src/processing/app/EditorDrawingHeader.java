package processing.app;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyboardFocusManager;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.Timer;
import javax.swing.border.LineBorder;
import javax.swing.plaf.basic.BasicComboBoxUI;

import processing.app.graph.kGraphComponent;
import processing.app.syntax.JEditTextArea;
import processing.app.util.kConstants;
import processing.app.util.kEvent;
import processing.app.util.kUtils;

import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.mxGraphOutline;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.view.mxGraphSelectionModel;

/**
 * Graph-editing toolbar at the top of the drawing window.
 */
public class EditorDrawingHeader extends JSplitPane {

  private static final Color HEADER_BACKGROUND = Theme.getColor("header.bgcolor");

  static final int INACTIVE = 0;

  static final int ROLLOVER = 1;

  static final int ACTIVE = 2;

  static final int BUTTON_WIDTH = 21;

  static final String[] mouseState = { "rollo", "activ", "inact" };

  static final String[] flagState = { "flag", "noflag" };
  
  private static final int HEADER_HEIGHT = EditorToolbar.BUTTON_HEIGHT;

  private static final int BUTTON_GAP = EditorToolbar.BUTTON_GAP;

  private static final int GRAPH_OUTLINE_WIDTH = HEADER_HEIGHT*4/3;

  /**
   * We make all the image objects beforehand for efficiency a matrix of bags
   * for the different states [3 mouse states] [2 flag states] each bag
   * containing that variant of every icon
   */
  final HashMap[][] iconBag = new HashMap[mouseState.length][flagState.length];

  /**
   * This is the shared by all tool buttons. It activates the appropriate tool
   * in DrawingArea component.
   */
  final ActionListener toolActionListener;

  JToggleButton codeWindowButton;

  JToggleButton lockButton;

  LinkButton linkButton;

  mxGraphOutline graphOutline;

  ButtonGroup toolButtons;

  DrawingArea drawarea;

  protected TrayComboBox shapeList;

  protected TrayComboBox connectorList;

  protected TrayComboBox colorList;

  protected JToggleButton textButton;

  public EditorDrawingHeader(DrawingArea dory) {

    drawarea = dory;

    // drawarea tool end listener
    drawarea.addListener(kEvent.TOOL_END, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        // deselect all the tool buttons
        // BUG: this does not work in the case of "text" although it should,
        // so we are forcing selection of the shape button instead and 
        // deselecting that to get the result of none being selected
//         System.out.println("resetting on TOOL_END: "+toolButtons.getSelection().getActionCommand());
        if (toolButtons.getSelection() != null) {
        toolButtons.setSelected(shapeList.getDisplayedButton().getModel(), true);
        toolButtons.getSelection().setSelected(false);
        }
//         spitButtonStates();
      }
    });

    // graph selection change listener
    drawarea.getGraphComponent().getGraph().getSelectionModel()
        .addListener(mxEvent.CHANGE, new mxIEventListener() {
          public void invoke(Object sender, mxEventObject evt) {
            updateLockButton();
            updateCodeWindowButton();
          }
        });

    // code window change listener
    drawarea.addListener(kEvent.CODE_WINDOW_VISIBILITY_CHANGE, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        updateCodeWindowButton();
      }
    });
        

    /*
     * Initialize the tool button group stuff
     */
    toolButtons = new ButtonGroup();
    initializeIconBag();
    toolActionListener = new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        
        // put focus here so we can deal with escape events
        // (graphComponent deals with tool mode escape events separately)
        requestFocusInWindow();
        
//        closeButtonTrays();
        
        drawarea.beginToolMode(((AbstractButton) e.getSource()).getModel()
            .getActionCommand());
        
        toolButtons.setSelected(((AbstractButton) e.getSource()).getModel(),
                                true);
        // ^--- we technically don't need to do this for every tool select
        // event, but it's easier to code
        //TODO something else is turning this off???? only SOMETIMES?
//        spitButtonStates();
        System.out.println("drawHeader >> set tool button selected");
      }
    };
    
    //===================TESTING=======================
    addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
          System.out.println("drawHeader >> i hear escape pressed");

        closeButtonTrays();
        
        if (drawarea.getToolMode() != null)
          drawarea.endToolMode(false);
      }
    });
//    addFocusListener(new FocusListener() {
//      public void focusGained(FocusEvent e) {
//        System.out.println("drawHeader >> focus gained");
//      }
//      public void focusLost(FocusEvent e) {
//        System.out.println("drawHeader >> focus lost");
//      }
//    });
//    addMouseListener(new MouseAdapter() {
//      public void mouseReleased(MouseEvent e) {
//        System.out.println("buttonPanel mouse released");
//      }
//    });
    //===================TESTING=======================
    

    // TODO implement grab when I'm grabbing on canvas instead of mouseOverCell

    /*
     * SHAPES drop-down
     */
    shapeList = new TrayComboBox(kConstants.SHAPE_KEYS);
    shapeList.setSelectedIndex(0);
    shapeList.getDisplayedButton().addActionListener(toolActionListener);
    // ^--- here the displayedButton is added instead of the list directly
    // because we want the button group to toggle the display button
    toolButtons.add(shapeList.getDisplayedButton());

    /*
     * CONNECTORS drop-down
     */
    connectorList = new TrayComboBox(kConstants.CONNECTOR_KEYS);
    connectorList.setSelectedIndex(0);
    connectorList.getDisplayedButton().addActionListener(toolActionListener);
    toolButtons.add(connectorList.getDisplayedButton());

    /*
     * TEXT
     */
    textButton = makeToolButton(kConstants.SHAPE_TEXT,
                                              "Text tool");
    textButton.addActionListener(toolActionListener);
    toolButtons.add(textButton);

    /*
     * COLORS drop-down
     */
    colorList = new TrayComboBox(kConstants.COLOR_KEYS);
    colorList.setSelectedIndex(0);
    colorList.getDisplayedButton().addActionListener(toolActionListener);
    toolButtons.add(colorList.getDisplayedButton());

    /*
     * ---- an empty panel for layout purposes (space divider) ----
     */
    JPanel layoutSpacer = new JPanel();
    layoutSpacer.setBorder(null);
    layoutSpacer.setSize(BUTTON_GAP * 2, BUTTON_GAP);
    layoutSpacer.setOpaque(true);
    layoutSpacer.setBackground(HEADER_BACKGROUND);

    /*
     * OPEN/CLOSE CODE WINDOW:
     */
    codeWindowButton = new JToggleButton(Base
        .getImageIcon("graph-inact-opencodew.gif", this));
    codeWindowButton.setRolloverIcon(Base
        .getImageIcon("graph-rollo-opencodew.gif", this));
    codeWindowButton.setRolloverSelectedIcon(Base
        .getImageIcon("graph-rollo-closecodew.gif", this));
    codeWindowButton.setSelectedIcon(Base
        .getImageIcon("graph-inact-closecodew.gif", this));
    codeWindowButton.setPressedIcon(Base
        .getImageIcon("graph-activ-closecodew.gif", this));
    // ^--there isn't a pressedSelectedIcon vs. pressedIcon unfortunately, but
    // most users probably won't notice
    codeWindowButton.setBorder(null); // this ensures proper spacing between
                                      // buttons (lord knows why)
    codeWindowButton.setBorderPainted(false);
    codeWindowButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        AbstractButton source = (AbstractButton) e.getSource();
        if (source.isSelected())
          drawarea.showCodeWindowOnSelected();
        else
          drawarea.hideCodeWindowOnSelected();
      }
    });
    codeWindowButton.setEnabled(false); //start false, assuming nothing is selected at the beginning

    /*
     * LOCK/UNLOCK:
     */
    lockButton = new JToggleButton(Base.getImageIcon("graph-inact-unlock.gif",
                                                     this));
    lockButton.setRolloverIcon(Base
        .getImageIcon("graph-rollo-unlock.gif", this));
    lockButton.setRolloverSelectedIcon(Base
        .getImageIcon("graph-rollo-lock.gif", this));
    lockButton.setSelectedIcon(Base.getImageIcon("graph-inact-lock.gif", this));
    lockButton.setPressedIcon(Base.getImageIcon("graph-activ-lock.gif", this));
    // ^--there isn't a pressedSelectedIcon vs. pressedIcon unfortunately, but
    // most users probably won't notice
    lockButton.setBorder(null); // this ensures proper spacing between buttons
                                // (lord knows why)
    lockButton.setBorderPainted(false);
    lockButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("lockButton isSelected "
                           + ((AbstractButton) e.getSource()).isSelected());
        AbstractButton source = (AbstractButton) e.getSource();
        if (source.isSelected())
          drawarea.unlockSelected();
        else
          drawarea.lockSelected();
      }
    });
    lockButton.setEnabled(false); //start false, assuming nothing is selected at the beginning
    
    /*
     * ZOOMIN
     */
    JToggleButton zoomInButton = makeToolButton("zoomin", "Zoom out");
    zoomInButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton) e.getSource()).getText());
      }
    });

    /*
     * ZOOMOUT
     */
    JToggleButton zoomOutButton = makeToolButton("zoomout", "Zoom in");
    zoomOutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton) e.getSource()).getText());
      }
    });

    /*
     * ---- an empty panel for layout purposes (space divider) ----
     */
    JPanel layoutSpacerJr = new JPanel();
    layoutSpacerJr.setBorder(null);
    layoutSpacerJr.setSize(BUTTON_GAP * 2,
                           BUTTON_GAP);
    layoutSpacerJr.setOpaque(true);
    layoutSpacerJr.setBackground(HEADER_BACKGROUND);

    /*
     * LINK (partially responsible as a status indicator, partially responsible
     * as a button)
     */
    linkButton = new LinkButton();
    linkButton.setEnabled(false);

    /*
     * BUTTON PANEL add everything
     */
    JPanel buttonPanel = new JPanel();
    buttonPanel.setLayout(new FlowLayout(FlowLayout.LEADING, BUTTON_GAP,
        (HEADER_HEIGHT - BUTTON_WIDTH) / 2 + 1));
    buttonPanel.setOpaque(false);
    buttonPanel.setBorder(null);
    buttonPanel.add(shapeList);
    buttonPanel.add(connectorList);
    buttonPanel.add(textButton);
    buttonPanel.add(colorList);
    buttonPanel.add(layoutSpacer);
    buttonPanel.add(codeWindowButton);
    buttonPanel.add(lockButton);
    // buttonPanel.add(zoomInButton); //temporarily removing these because it
                                        // feels less important
    // buttonPanel.add(zoomOutButton); //and our toolbar is getting too long
    buttonPanel.add(linkButton);
    buttonPanel.add(layoutSpacerJr);

    /*
     * GRAPH OUTLINE
     */
    // note that the graphOutline background color is determined by the
    // graphComponent.pageBackgroundColor (which we have set in kGraphComponent)
    graphOutline = new mxGraphOutline(drawarea.getGraphComponent()) {
      /**
       * @see com.mxgraph.swing.mxGraphOutline#paintForeground
       */
      protected void paintForeground(Graphics g) {
        if (graphComponent != null) {
          Graphics2D g2 = (Graphics2D) g;

          Stroke stroke = g2.getStroke();
          g.setColor(kConstants.OUTLINE_HANDLE_COLOR);
          g2.setStroke(new BasicStroke(1));
          g.drawRect(finderBounds.x, finderBounds.y, finderBounds.width,
                     finderBounds.height);

          g2.setStroke(stroke);
          g.setColor(kConstants.OUTLINE_HANDLE_COLOR);
          g.fillRect(finderBounds.x + finderBounds.width - 2,
                     finderBounds.y + finderBounds.height - 2, 5, 5);
        }
      }
      /**
       * Returns true if the scale or translate has changed.
       */
      public boolean updateScaleAndTranslate()
      {
        outlineBorder = kConstants.OUTLINE_BORDER_WIDTH;
        return super.updateScaleAndTranslate();
      }
    };
    graphOutline.setPreferredSize(new Dimension(GRAPH_OUTLINE_WIDTH, HEADER_HEIGHT));
    graphOutline.setMinimumSize(new Dimension(GRAPH_OUTLINE_WIDTH, HEADER_HEIGHT));

    /*
     * SPLIT PANE: no clue why, but graphOutline only shows in JInternalFrames
     * and JSplitPanes and the like, but not in JPanels so we are stuck with a
     * JSplitPane that we've customized to look like it neither exists nor
     * functions
     */
    setOrientation(JSplitPane.HORIZONTAL_SPLIT);
    setLeftComponent(buttonPanel);
    setRightComponent(graphOutline);
    setOpaque(true);
    setBackground(HEADER_BACKGROUND); //TODO dunno why this doesn't work when packaged
    setBorder(null);
    setDividerSize(0);
    setResizeWeight(1.0);
    setMaximumSize(new Dimension(3000,HEADER_HEIGHT));    
  }
 

  /**
   * Simply forces all JComboBox popups to hide; used when escape key or button is selected
   */
  private void closeButtonTrays() {
    shapeList.hidePopup();
    colorList.hidePopup();
    connectorList.hidePopup();
  }
  
  /**
   * Refreshes all buttons whose state depends on the graph selection
   * (link button, which also depends on the text selection, is handled
   * by editor)
   */
  public void updateGraphButtons() {
    updateLockButton();
    updateCodeWindowButton();
  }
  
  /**
   * Refreshes the lock button state depending on the graph selection
   */
  private void updateLockButton() {
    if (drawarea.getGraphComponent().getGraph().getSelectionCount() == 0) {
      lockButton.setEnabled(false);
    } else {
      lockButton.setEnabled(true);
      lockButton.setSelected(!drawarea.isSelectionLocked());
    }
  }

  /**
   * Refreshes the code window button state depending on the graph selection
   */
  private void updateCodeWindowButton() {
    codeWindowButton.setEnabled(drawarea.isSelectionLinked());
    codeWindowButton.setSelected(drawarea.isCodeWindowOpenOnSelected());
  }

  public LinkButton getLinkButton() {
    return linkButton;
  }

  /**
   * Make all Icon objects and fill the global final arrays. This makes image
   * access more efficient over repeated use.
   * 
   * The toggleButton icons aren't included in this icon bag system b/c they
   * were implemented prior and it's faster to keep them as they are.
   */
  private void initializeIconBag() {
    int i, j, k;
    String filename;

    for (i = 0; i < mouseState.length; i++)
      for (j = 0; j < flagState.length; j++) {
        // initialize the HashMap
        iconBag[i][j] = new HashMap(kConstants.SHAPE_KEYS.length
                                    + kConstants.CONNECTOR_KEYS.length
                                    + kConstants.COLOR_KEYS.length);
        // put in all the shape icons
        for (k = 0; k < kConstants.SHAPE_KEYS.length; k++) {
          filename = "graph-" + mouseState[i] + "-" + flagState[j] + "-"
                     + kConstants.SHAPE_KEYS[k] + ".gif";
          iconBag[i][j].put(kConstants.SHAPE_KEYS[k], Base
              .getImageIcon(filename, this));
        }
        // put in all the connector icons
        for (k = 0; k < kConstants.CONNECTOR_KEYS.length; k++) {
          filename = "graph-" + mouseState[i] + "-" + flagState[j] + "-"
                     + kConstants.CONNECTOR_KEYS[k] + ".gif";
          iconBag[i][j].put(kConstants.CONNECTOR_KEYS[k], Base
              .getImageIcon(filename, this));
        }
        // put in all the color icons
        filename = "graph-" + mouseState[i] + "-" + flagState[j] + "-paint.png";
        for (k = 0; k < kConstants.COLOR_KEYS.length; k++) {
          iconBag[i][j].put(kConstants.COLOR_KEYS[k], new FillIcon(kUtils
              .getFillColorFromKey(kConstants.COLOR_KEYS[k]), Base
              .getThemeImage(filename, this)));
        }
      }
  }

  /**
   * Debugging: print out all button selected states in button bag.
   * 
   * @deprecated
   */
  private void spitButtonStates() {
    Enumeration e = toolButtons.getElements();
    AbstractButton b;
    while (e.hasMoreElements()) {
      b = (AbstractButton) e.nextElement();
      System.out.println(b.getModel()+" "+b.isSelected());
    }
    System.out.println();
  }

  /**
   * Shortcut method to make our uniform type of toggle buttons, used for TEXT,
   * ZOOM_IN, and ZOOM_OUT buttons.
   * 
   * @param name
   *          , tool tip
   * @return
   */
  private JToggleButton makeToolButton(String name, String tooltip) {
    JToggleButton bob = new JToggleButton(Base.getImageIcon("graph-inact-"
                                                            + name + ".gif",
                                                            this));
    bob
        .setRolloverIcon(Base
            .getImageIcon("graph-rollo-" + name + ".gif", this));
    bob.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-" + name
                                                  + ".gif", this));
    bob
        .setSelectedIcon(Base
            .getImageIcon("graph-activ-" + name + ".gif", this));
    bob.setToolTipText(tooltip);
    bob.setActionCommand(name);
    bob.setBorder(null); // need this for proper spacing between buttons (lord
    // knows why)
    bob.setBorderPainted(false); // borderPainted seems the only one necessary
    // for a mac
    bob.setFocusPainted(false); // TODO minor: test these when going
    // cross-platform
    bob.setMargin(new Insets(0, 0, 0, 0));
    bob.setContentAreaFilled(false);
    return bob;
  }

  protected class LinkButton extends JButton {

    private Icon[][] icons = {
        { Base.getImageIcon("graph-inact-link.gif", this),
            Base.getImageIcon("graph-inact-unlink.gif", this) },
        { Base.getImageIcon("graph-rollo-link.gif", this),
            Base.getImageIcon("graph-rollo-unlink.gif", this) },
        { Base.getImageIcon("graph-activ-link.gif", this),
            Base.getImageIcon("graph-activ-unlink.gif", this) } };

    public LinkButton() {
      super();
      setLinkMode();
      setBorderPainted(false);
      addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("link >> "
                             + ((AbstractButton) e.getSource()).getModel()
                                 .getActionCommand() + " button pressed");
          String command = ((AbstractButton) e.getSource()).getModel()
              .getActionCommand();
          if (command.equals("link")) {
            drawarea.editor.linkAction();
          } else if (command.equals("unlink")) {
            drawarea.editor.disconnectAction();
          }
        }
      });
    }

    /**
     * Returns whether or not the button is currently in non-clickable
     * "linking..." status
     */
    public boolean isLinkActiveMode() {
      return (getActionCommand()).equals("linkActive");
    }

    /**
     * Switches to regular Link button
     */
    public void setLinkMode() {
      setIcon(icons[0][0]);
      setRolloverIcon(icons[1][0]);
      setPressedIcon(icons[2][0]);
      setDisabledIcon(null); // attempt at reset
      setActionCommand("link");
      // setting border to null ensures proper spacing between buttons (lord
      // knows why) but also setting the icon appears to reset the border to
      // something not null so we have to do this every time
      setBorder(null);  
      setEnabled(true);
    }

    /**
     * Switches to regular Disconnect
     */
    public void setUnlinkMode() {
      setIcon(icons[0][1]);
      setRolloverIcon(icons[1][1]);
      setPressedIcon(icons[2][1]);
      setDisabledIcon(null); // attempt at reset
      setActionCommand("unlink");
      // setting border to null ensures proper spacing between buttons (lord
      // knows why) but also setting the icon appears to reset the border to
      // something not null so we have to do this every time
      setBorder(null);
      setEnabled(true);
      System.out.println("unlink button.border=" + getBorder());
    }

    /**
     * Switches to a non-clickable "linking..." status indicator
     */
    public void setLinkActiveMode() {
      setDisabledIcon(icons[2][0]);
      setActionCommand("linkActive");
      setEnabled(false);
    }

    /**
     * Switches to a non-clickable "disconnecting..." status indicator
     */
    public void setUnlinkActiveMode() {
      setDisabledIcon(icons[2][1]);
      setActionCommand("unlinkActive");
      setEnabled(false);
    }
  }

  /**
   * Make an icon with a block of solid color painted inside/underneath. Used to
   * make fill color selection buttons.
   */
  private class FillIcon implements Icon {
    Color fillcolor;
    Image overlay;

    public FillIcon(Color c, Image img) {
      fillcolor = c;
      overlay = img;
    }

    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(fillcolor);
      g.fillRect(x+1, y+1, getIconWidth()-2, getIconHeight()-2);
      if (kConstants.BUILD_FOR_RELEASE)
        g.drawImage(overlay, 1, 1, null); //for release
      else
        g.drawImage(overlay, 0, 0, null); //for development
    }

    public int getIconWidth() {
      return BUTTON_WIDTH;
    }

    public int getIconHeight() {
      return BUTTON_WIDTH;
    }
  }

  /**
   * Makes a drop-down tool tray that activates a tool when just clicked, and
   * opens the tray when the mouse click is held down. This is mostly a wrapper
   * class to package repeated and associated UI stuff into one convenience
   * object declaration.
   * 
   * @author achang
   */
  private class TrayComboBox extends JComboBox { // implements ButtonModel {

    TrayComboBoxUI trayUI = new TrayComboBoxUI();

    /**
     * Constructor.
     */
    public TrayComboBox(Object[] items) {
      super(items);
      setOpaque(true);
      setBackground(kConstants.UI_COLOR_BUTTONFILL);
      setRenderer(new ListRenderer());
      setUI(trayUI);
      setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_WIDTH));
    }

    /**
     * Shortcut to set selection of main display button (arrowButton) inside the
     * UI.
     */
    public void setSelected(boolean b) {
      trayUI.displayedItemSetSelected(b); // the UI method calls repaint
    }

    /**
     * Shortcut to get selection mode of the main display button (arrowButton)
     * inside the UI.
     */
    public boolean isSelected() {
      return trayUI.getDisplayedItem().isSelected();
    }

    public AbstractButton getDisplayedButton() {
      return trayUI.getDisplayedItem();
    }

    /**
     * Custom renderer that makes icon-only JComboBox items
     * 
     * @author achang
     */
    class ListRenderer extends JLabel implements ListCellRenderer {

      public ListRenderer() {
        setHorizontalAlignment(CENTER); // not sure if these do anything
        setVerticalAlignment(CENTER);
      }

      /**
       * This method finds the image and text corresponding to the selected
       * value and returns the label, set up to display the text and image.
       */
      public Component getListCellRendererComponent(JList list, Object value,
                                                    int index,
                                                    boolean isSelected,
                                                    boolean cellHasFocus) {
        Icon icon;

        if (isSelected)
          icon = (Icon) iconBag[0][1].get(value); // rollo-noflag
        else if (cellHasFocus)
          icon = (Icon) iconBag[1][1].get(value); // activ-noflag
        else
          icon = (Icon) iconBag[2][1].get(value); // inact-noflag

        if (icon == null || icon.getIconWidth() == -1) {
          System.err.println("no " + value + " image at index "
                             + list.getSelectedIndex());
        } else {
          setIcon(icon);
        }

        return this;
      }
    }

    /**
     * Enables a Photoshop&OmniGraffle-style tool button that can toggle a
     * functionality AND extend into a tray of selectable related tools. <br />
     * Inspired by the "HUD style combo box" code <a href="http://explodingpixels.wordpress.com/2009/03/08/creating-a-hud-style-comob-box/"
     * > here</a>.
     * 
     * @author achang
     */
    class TrayComboBoxUI extends BasicComboBoxUI {

      @Override
      protected JButton createArrowButton() {
        JButton arrowButton = new JButton("");
        // arrowButton.setAction(new AbstractAction() {
        // public void actionPerformed(ActionEvent e) {
        // System.out.println("action event-- "+e.getActionCommand());
        // }
        // });
        arrowButton.setBounds(0, 0, BUTTON_WIDTH, BUTTON_WIDTH);
        arrowButton.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_WIDTH));
        return arrowButton;
      }

      @Override
      protected void installDefaults() {
        super.installDefaults();
        comboBox.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            updateDisplayedItem();
          }
        });
      }

      @Override
      protected void installComponents() {
        super.installComponents();
        // change our mouse listeners to add longclick->opentray functionality:
        unconfigureArrowButton();
        arrowButton.addMouseListener(new TrayHandler());
        ((JPopupMenu) popup).setOpaque(true);
        ((JPopupMenu) popup).setBackground(kConstants.UI_COLOR_BUTTONFILL);
        ((JPopupMenu) popup).setBorder(null);
        // ^--setting borderPainted to 0 is not enough, must remove border
        // completely
        updateDisplayedItem();
      }

      /**
       * Updates the value displayed to match that of
       * {@link javax.swing.JComboBox#getSelectedItem()}. Creates an
       * {@link java.awt.event.ActionListener} that updates the displayed item
       * when the {@link JComboBox}'s currently selected item changes.
       */
      private void updateDisplayedItem() {

        if (comboBox.getSelectedItem() != null) {
          String value = comboBox.getSelectedItem().toString();
          arrowButton.setIcon((Icon) iconBag[2][0].get(value)); // inact-flag
          arrowButton.setRolloverIcon((Icon) iconBag[0][0].get(value)); // rollo-flag
          arrowButton.setRolloverSelectedIcon((Icon) iconBag[0][0].get(value)); // rollo-flag
          arrowButton.setSelectedIcon((Icon) iconBag[1][0].get(value)); // activ-flag
          arrowButton.setPressedIcon((Icon) iconBag[1][0].get(value)); // activ-flag
          arrowButton.setActionCommand(value);
          arrowButton.doClick(); // perform the newly assigned action
        }
      }

      public void displayedItemSetSelected(boolean b) {
        if (arrowButton.isSelected() != b) {
          arrowButton.setSelected(b);
          updateDisplayedItem();
        }
      }

      public AbstractButton getDisplayedItem() {
        return arrowButton;
      }

      /**
       * Overwrites original arrowButton mouse listener in order to enable 1)
       * showing popup after a delay, and 2) toggling the button state.
       * 
       * @author achang
       */
      private class TrayHandler implements MouseListener {

        Timer enterTimer;

        public TrayHandler() {
          enterTimer = new Timer(300, new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              toggleOpenClose();
            }
          });
          enterTimer.setRepeats(false);
        }

        // TODO bigger minor: connect mouse events between arrowButton and
        // popup:
        // if mouse WAS pressed and release happens on the popup,
        // then the item under the release should be selected,
        // and that item should be active

        public void mouseClicked(MouseEvent e) {
          // selection is handled via added listeners when declaring the buttons
          // in order to achieve ButtonGroup effect
        }

        public void mouseEntered(MouseEvent e) {
          // System.out.println("arrowButton is "+arrowButton.isSelected());
        }

        public void mouseExited(MouseEvent e) {
          enterTimer.restart();
          enterTimer.stop();
        }

        public void mousePressed(MouseEvent e) {
          // System.out.println("trayHandler mousePressed");
          enterTimer.start();
        }

        public void mouseReleased(MouseEvent e) {
          // System.out.println("trayHandler mouseReleased");
          enterTimer.restart();
          enterTimer.stop();
        }

      }
    }

  }

}