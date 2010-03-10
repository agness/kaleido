package processing.app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.Timer;
import javax.swing.plaf.basic.BasicComboBoxUI;

import processing.app.util.kEvent;

import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxEventSource.mxIEventListener;


/**
 * Graph-editing toolbar at the top of the drawing window.
 */
public class EditorDrawingHeader extends JPanel {

  /** Rollover titles for each button. */
  static final String shapeNames[] = {
    "rect", "circle", "diam", "star", "audio", "keyb", "pers"
  };
  
  /** Rollover titles for each button. */
  static final String connectorNames[] = {
    "line", "solid", "dotted"
  };
  
  /** Rollover titles for each button. */
  static final String colorNames[] = {
    "drawing.fillset1.color1", "drawing.fillset1.color2", "drawing.fillset1.color3", 
    "drawing.fillset1.color4", "drawing.fillset1.color5"
  };
  
  static final int INACTIVE = 0;
  static final int ROLLOVER = 1;
  static final int ACTIVE   = 2;
  static final int BUTTON_WIDTH = 21;
  
  //making all the image objects beforehand for efficiency
  //a matrix of bags for the different states [3 mouse states] [2 flag states]
  //each bag containing that variant of every icon
  static final String[] mouseState = {"rollo", "activ", "inact"};
  static final String[] flagState = {"flag", "noflag"};
  final HashMap[][] iconBag = new HashMap[mouseState.length][flagState.length];
  
  AbstractButton defaultButton;
  ButtonGroup toolButtons;
//  Editor editor; //TODO drawArea instead for event handling / focus stuff?
                  //we don't really need to access the textArea for anything
  DrawingArea drawingArea;
  
  public EditorDrawingHeader(DrawingArea dory) {
    
    drawingArea = dory;
    drawingArea.addListener(kEvent.TOOL_END,
      new mxIEventListener()
      {
        public void invoke(Object source, mxEventObject evt)
        {
          toolButtons.setSelected(defaultButton.getModel(), true);
        }
      });
    
    setBackground(Theme.getColor("header.bgcolor"));
    setBorder(null);
    setLayout(new FlowLayout(FlowLayout.TRAILING, EditorToolbar.BUTTON_GAP,
                             (EditorToolbar.BUTTON_HEIGHT - BUTTON_WIDTH) / 2 + 1));
    setPreferredSize(new Dimension(500, EditorToolbar.BUTTON_HEIGHT));
    //TODO minor: width here ------^ should be fluid
    toolButtons = new ButtonGroup();
    initializeIconBag();
     
    /*
     * CURSOR
     */
    JToggleButton cursorButton = makeToolButton("cursor", "Cursor tool");
    cursorButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
        System.out.println("action event-- cursor");
        spitButtonStates();
      }
    });
    toolButtons.add(cursorButton);
    defaultButton = cursorButton;
    
    /*
     * SHAPES drop-down
     */
    TrayComboBox shapeList = new TrayComboBox(shapeNames);
    shapeList.setSelectedIndex(0);
    shapeList.getDisplayedButton().addActionListener(new ActionListener() {
      //^--- here the displayedButton is added instead of the list directly
      //because we want the button group to toggle the display button
      public void actionPerformed(ActionEvent e) {
        //TODO first click after button press will actually create the object at that location
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
        spitButtonStates();
      }
    }); 
    toolButtons.add(shapeList.getDisplayedButton());
    
    /*
     * CONNECTORS drop-down
     */
    TrayComboBox connectorList = new TrayComboBox(connectorNames);
    connectorList.setSelectedIndex(0);
    connectorList.getDisplayedButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
//        spitButtonStates();
//        System.out.println(((AbstractButton)e.getSource()).getModel().getActionCommand());
        drawingArea.beginToolMode(((AbstractButton)e.getSource()).getModel().getActionCommand());
      }
    });
    //the alternative, instead of defining the actionListeners here separately
    //is to make a bunch of static ones at the beginning and change them in/out
    //when updating the arrowButton
    //yet another alternative is to make one static actionListener that works for all tool cases
    //which we can then just install when we instantiate the arrowButton
    toolButtons.add(connectorList.getDisplayedButton());
    
    /*
     * TEXT
     */
    JToggleButton textButton = makeToolButton("text", "Text tool");
    textButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
        System.out.println("action event-- text");
        spitButtonStates();
      }
    });
    toolButtons.add(textButton);
    
    /*
     * COLORS drop-down
     */
    TrayComboBox colorList = new TrayComboBox(colorNames);
    colorList.setSelectedIndex(0);
    colorList.getDisplayedButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
        spitButtonStates();
      }
    });
    toolButtons.add(colorList.getDisplayedButton());
    
    /*
     * ---- an empty panel for layout purposes (space divider) ----
     */
    JPanel layoutSpacer = new JPanel();
    layoutSpacer.setBorder(null);
    layoutSpacer.setSize(EditorToolbar.BUTTON_GAP*2, EditorToolbar.BUTTON_GAP);
    layoutSpacer.setBackground(this.getBackground());

    /*
     * LOCK/UNLOCK: the one and only toggle button
     */
    JToggleButton lockButton = new JToggleButton(Base.getImageIcon("graph-inact-unlock.gif", this));
    lockButton.setRolloverIcon(Base.getImageIcon("graph-rollo-unlock.gif", this));
    lockButton.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-lock.gif", this));
    lockButton.setSelectedIcon(Base.getImageIcon("graph-inact-lock.gif", this));
    lockButton.setPressedIcon(Base.getImageIcon("graph-activ-lock.gif", this));
    // ^--there isn't a pressedSelectedIcon vs. pressedIcon unfortunately, but most users probably won't notice
    lockButton.setBorder(null); //this ensures proper spacing between buttons (lord knows why)
    lockButton.setBorderPainted(false);
    lockButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("togglebutton isSelected "+((AbstractButton) e.getSource()).isSelected());
      }
    });

    /*
     * ZOOMIN
     */
    JToggleButton zoomInButton = makeToolButton("zoomin", "Zoom out");
    zoomInButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton)e.getSource()).getText());
      }
    });
    
    /*
     * ZOOMOUT
     */
    JToggleButton zoomOutButton = makeToolButton("zoomout", "Zoom in");
    zoomOutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton)e.getSource()).getText());
      }
    });

    
    // add everything
    add(cursorButton);
    add(shapeList);
    add(connectorList);
    add(textButton);
    add(colorList);
    add(layoutSpacer);  
    add(lockButton);
    add(zoomInButton);
    add(zoomOutButton);
    //now to add a graphOutline here...
    
    //highlight our default button
    toolButtons.setSelected(defaultButton.getModel(), true);
    setVisible(true);
  }

  /**
   * Make all Icon objects and fill the global final arrays.
   * This makes image access more efficient over repeated use.
   * 
   * The toggleButton icons aren't included in this icon bag 
   * system b/c they were implemented prior and it's faster to
   * keep them as they are.
   */
  private void initializeIconBag() {
    int i, j, k;
    String filename;

    for (i=0; i<mouseState.length; i++)
      for (j=0; j<flagState.length; j++) {
        //initialize the HashMap
        iconBag[i][j] = new HashMap(shapeNames.length+connectorNames.length+colorNames.length);
        //put in all the shape icons
        for (k=0; k<shapeNames.length; k++) {
          filename = "graph-"+mouseState[i]+"-"+flagState[j]+"-"+shapeNames[k]+".gif";
          iconBag[i][j].put(shapeNames[k], Base.getImageIcon(filename, this));
        }
        //put in all the connector icons
        for (k=0; k<connectorNames.length; k++) {
          filename = "graph-"+mouseState[i]+"-"+flagState[j]+"-"+connectorNames[k]+".gif";
          iconBag[i][j].put(connectorNames[k], Base.getImageIcon(filename, this));
        }
        //put in all the color icons
        filename = "graph-"+mouseState[i]+"-"+flagState[j]+"-paint.png";
        for (k=0; k<colorNames.length; k++) {
          iconBag[i][j].put(colorNames[k], new FillIcon(Theme.getColor(colorNames[k]), Base.getThemeImage(filename, this)));
        }
      }
  }
  
  /**
   * A temporary debugging purpose.
   * TODO kill
   */
  private void spitButtonStates() {
    Enumeration e = toolButtons.getElements();
    AbstractButton b;
    while (e.hasMoreElements()) {
      b = (AbstractButton) e.nextElement();
      System.out.println(b.isSelected());
    }
    System.out.println();
  }
  
  /**
   * Shortcut method to make our uniform type of toggle buttons.
   * @param name, tool tip
   * @return
   */
  private JToggleButton makeToolButton(String name, String tooltip) {
    JToggleButton bob = new JToggleButton(Base.getImageIcon("graph-inact-"+name+".gif", this));
    bob.setRolloverIcon(Base.getImageIcon("graph-rollo-"+name+".gif", this));
    bob.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-"+name+".gif", this));
    bob.setSelectedIcon(Base.getImageIcon("graph-activ-"+name+".gif", this));
    bob.setToolTipText(tooltip);
    bob.setBorder(null); //need this for proper spacing between buttons (lord knows why)
    bob.setBorderPainted(false); //borderPainted seems the only one necessary for a mac
    bob.setFocusPainted(false); //TODO minor: test these when going cross-platform
    bob.setMargin(new Insets(0,0,0,0));
    bob.setContentAreaFilled(false);
    return bob;
  }

  /**
   * Make an icon with a block of solid color painted inside.
   * Used to make fill color selection buttons.
   */
  private class FillIcon implements Icon {
    Color fillcolor;
    Image overlay;
    public FillIcon (Color c, Image img) {
      fillcolor = c;
      overlay = img;
    }
    public void paintIcon (Component c, Graphics g, int x, int y) {
      g.setColor(fillcolor);
      g.fillRect(x, y, getIconWidth(), getIconHeight());
      g.drawImage(overlay,0,0,null);
    }
    public int getIconWidth() {
      return BUTTON_WIDTH;
    }
    public int getIconHeight() { 
      return BUTTON_WIDTH;
    }
  }
  
  /**
   * Makes a drop-down tool tray that activates a tool when just clicked,
   * and opens the tray when the mouse click is held down.
   * This is mostly a wrapper class to package repeated and associated UI stuff
   * into one convenience object declaration.
   * @author achang
   */
  private class TrayComboBox extends JComboBox { //implements ButtonModel {
  
    TrayComboBoxUI trayUI = new TrayComboBoxUI();
    
    /**
     * Constructor.
     */
    public TrayComboBox(Object[] items) {
      super(items);
      setRenderer(new ListRenderer());
      setUI(trayUI);
      setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_WIDTH));
    }
    /**
     * Shortcut to set selection of main display 
     * button (arrowButton) inside the UI.
     */
    public void setSelected(boolean b) {
      trayUI.displayedItemSetSelected(b); //the UI method calls repaint
    }
    /**
     * Shortcut to get selection mode of the main display 
     * button (arrowButton) inside the UI.
     */
    public boolean isSelected() {
      return trayUI.getDisplayedItem().isSelected();
    }
    
    public AbstractButton getDisplayedButton() {
      return trayUI.getDisplayedItem();
    }

    /**
     * Custom renderer that makes icon-only JComboBox items
     * @author achang
     */
    class ListRenderer extends JLabel implements ListCellRenderer {
      
      public ListRenderer() {
        setHorizontalAlignment(CENTER); //not sure if these do anything
        setVerticalAlignment(CENTER);
      }
  
      /**
       * This method finds the image and text corresponding to the selected value
       * and returns the label, set up to display the text and image.
       */
      public Component getListCellRendererComponent(JList list, Object value,
                                                    int index, boolean isSelected,
                                                    boolean cellHasFocus) {
        Icon icon;

        if (isSelected)
          icon = (Icon) iconBag[0][1].get(value); //rollo-noflag
        else if (cellHasFocus)
          icon = (Icon) iconBag[1][1].get(value); //activ-noflag
        else
          icon = (Icon) iconBag[2][1].get(value); //inact-noflag

        if (icon == null || icon.getIconWidth() == -1) {
          System.err.println("no "+value+" image at index "+list.getSelectedIndex());
        } else {
          setIcon(icon);
        }
  
        return this;
      }
    }
    
    /**
     * Enables a Photoshop&OmniGraffle-style tool button that can toggle a 
     * functionality AND extend into a tray of selectable related tools. <br />
     * Inspired by the "HUD style combo box" code
     * <a href="http://explodingpixels.wordpress.com/2009/03/08/creating-a-hud-style-comob-box/">
     * here</a>.
     * @author achang
     */
    class TrayComboBoxUI extends BasicComboBoxUI {
  
      @Override
      protected JButton createArrowButton() {
        JButton arrowButton = new JButton("");
//        arrowButton.setAction(new AbstractAction() {
//          public void actionPerformed(ActionEvent e) {
//            System.out.println("action event-- "+e.getActionCommand());
//          }
//        });
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
        //change our mouse listeners to add longclick->opentray functionality:
        unconfigureArrowButton();
        arrowButton.addMouseListener(new TrayHandler());
        ((JPopupMenu) popup).setBorder(null);
        // ^--setting borderPainted to 0 is not enough, must remove border completely
        updateDisplayedItem();
      }
      /**
       * Updates the value displayed to match that of 
       * {@link javax.swing.JComboBox#getSelectedItem()}.
       * Creates an {@link java.awt.event.ActionListener} that updates
       * the displayed item when the {@link JComboBox}'s currently selected
       * item changes.
       */
      private void updateDisplayedItem() {
        
        if (comboBox.getSelectedItem() != null) 
        {
          String value = comboBox.getSelectedItem().toString();
          arrowButton.setIcon((Icon) iconBag[2][0].get(value)); //inact-flag
          arrowButton.setRolloverIcon((Icon) iconBag[0][0].get(value)); //rollo-flag
          arrowButton.setRolloverSelectedIcon((Icon) iconBag[0][0].get(value)); //rollo-flag
          arrowButton.setSelectedIcon((Icon) iconBag[1][0].get(value)); //activ-flag
          arrowButton.setPressedIcon((Icon) iconBag[1][0].get(value)); //activ-flag
          arrowButton.setActionCommand(value);
          arrowButton.doClick(); //perform the newly assigned action
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
       * Overwrites original arrowButton mouse listener in order to enable 
       * 1) showing popup after a delay, and 2) toggling the button state.
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
        //TODO bigger minor: connect mouse events between arrowButton and popup:
        //if mouse WAS pressed and release happens on the popup,
        //then the item under the release should be selected,
        //and that item should be active
        
        public void mouseClicked(MouseEvent e) {
          //selection is handled via added listeners when declaring the buttons
          //in order to achieve ButtonGroup effect
        }
  
        public void mouseEntered(MouseEvent e) {
//          System.out.println("arrowButton is "+arrowButton.isSelected());
        }
  
        public void mouseExited(MouseEvent e) {
          enterTimer.restart();
          enterTimer.stop();
        }
  
        public void mousePressed(MouseEvent e) {
//          System.out.println("trayHandler mousePressed");
          enterTimer.start();
        }
  
        public void mouseReleased(MouseEvent e) {
//          System.out.println("trayHandler mouseReleased");
          enterTimer.restart();
          enterTimer.stop();
        }
        
      }
    }

  }
  
}