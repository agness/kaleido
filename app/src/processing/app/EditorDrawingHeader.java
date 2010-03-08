package processing.app;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
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
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicComboBoxUI;


/**
 * Graph-editing toolbar at the top of the drawing window.
 */
public class EditorDrawingHeader extends JPanel {

  /** Rollover titles for each button. */
  static final String shapes[] = {
    "rect", "circle", "diam", "star", "audio", "keyb", "pers"
  };
  
  /** Rollover titles for each button. */
  static final String connectors[] = {
    "line", "solid", "dotted"
  };
  
  /** Rollover titles for each button. */
  static final String colors[] = {
    "paint", "A", "B", "C", "D", "E"
  };
  
  static final int INACTIVE = 0;
  static final int ROLLOVER = 1;
  static final int ACTIVE   = 2;
  static final int BUTTON_WIDTH = 21;
  
  ButtonGroup toolButtons;
  Editor editor; //TODO drawArea instead for event handling / focus stuff?
  
  public EditorDrawingHeader(Editor eddie) {
    
    setBackground(Theme.getColor("header.bgcolor"));
    setBorder(null);
    setLayout(new FlowLayout(FlowLayout.TRAILING, EditorToolbar.BUTTON_GAP,
                             (EditorToolbar.BUTTON_HEIGHT - BUTTON_WIDTH) / 2 + 1));
    setPreferredSize(new Dimension(500, EditorToolbar.BUTTON_HEIGHT));
    //TODO minor: width here ------^ should be fluid
    toolButtons = new ButtonGroup();
     
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
    
    /*
     * SHAPES drop-down
     */
    TrayComboBox shapeList = new TrayComboBox(shapes);
    shapeList.setSelectedIndex(0);
    shapeList.getDisplayedButton().addActionListener(new ActionListener() {
      //^--- here the displayedButton is added instead of the list directly
      //because we want the button group to toggle the display button
      public void actionPerformed(ActionEvent e) {
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
        spitButtonStates();
      }
    }); 
    toolButtons.add(shapeList.getDisplayedButton());
    
    /*
     * CONNECTORS drop-down
     */
    TrayComboBox connectorList = new TrayComboBox(connectors);
    connectorList.setSelectedIndex(0);
    connectorList.getDisplayedButton().addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        toolButtons.setSelected(((AbstractButton)e.getSource()).getModel(), true);
        spitButtonStates();
      }
    });
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
    //TODO ohdear what are we going to do about this
    TrayComboBox colorList = new TrayComboBox(colors);
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
    
    setVisible(true);
  }
  
  /**
   * A temporary debugging purpose.
   * TODO kill
   */
  public void spitButtonStates() {
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
  public JToggleButton makeToolButton(String name, String tooltip) {
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
   * Makes a drop-down tool tray that activates a tool when just clicked,
   * and opens the tray when the mouse click is held down.
   * This is mostly a wrapper class to package repeated and associated UI stuff
   * into one convenience object declaration.
   * @author achang
   */
  class TrayComboBox extends JComboBox { //implements ButtonModel {
    
    /* TODO for efficiency's sake, we should just load all 
     * the image icons once and swap between the objects.
     * Error handling will also be easier to deal with then.
     * 
     * Make a matrix of images, make some constants to refer
     * to the right row/col, yada yada yada...
     */
  
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
        ImageIcon icon;
        
        if (isSelected)
          icon = Base.getImageIcon("graph-rollo-noflag-"+value+".gif", list);
        else if (cellHasFocus)
          icon = Base.getImageIcon("graph-activ-noflag-"+value+".gif", list);
        else
          icon = Base.getImageIcon("graph-inact-noflag-"+value+".gif", list);
  
        if (icon.getIconWidth() == -1) {
          System.out.println("no "+value+" image at index "+list.getSelectedIndex());
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
        arrowButton.setAction(new AbstractAction() {
          public void actionPerformed(ActionEvent e) {
            System.out.println("action event-- "+e.getActionCommand());
          }
        });
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
        if (comboBox.getSelectedItem() != null) {
          String value = comboBox.getSelectedItem().toString();
          arrowButton.setIcon(Base.getImageIcon("graph-inact-flag-"+value+".gif", arrowButton));
          arrowButton.setRolloverIcon(Base.getImageIcon("graph-rollo-flag-"+value+".gif", arrowButton));
          arrowButton.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-flag-"+value+".gif", arrowButton));
          arrowButton.setSelectedIcon(Base.getImageIcon("graph-activ-flag-"+value+".gif", arrowButton));
          arrowButton.setPressedIcon(Base.getImageIcon("graph-activ-flag-"+value+".gif", arrowButton));
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

    /**
     * Methods from interface ButtonModel that we will never need
     * to use.  Here basically channeling everything to the UI.arrowButton
     */
    /*
    public void addChangeListener(ChangeListener l) {
      trayUI.getDisplayedItem().addChangeListener(l);
    }
    public int getMnemonic() {
      return trayUI.getDisplayedItem().getMnemonic();
    }
    public boolean isArmed() {
      return trayUI.getDisplayedItem().getModel().isArmed();
    }
    public boolean isPressed() {
     return trayUI.getDisplayedItem().getModel().isPressed();
    }
    public boolean isRollover() {
      return trayUI.getDisplayedItem().getModel().isRollover();
    }
    public void removeChangeListener(ChangeListener l) {
      trayUI.getDisplayedItem().removeChangeListener(l);
    }
    public void setArmed(boolean b) {
      trayUI.getDisplayedItem().getModel().setArmed(b);
    }
    public void setGroup(ButtonGroup group) {
      trayUI.getDisplayedItem().getModel().setGroup(group);
    }
    public void setMnemonic(int key) {
      trayUI.getDisplayedItem().setMnemonic(key);
    }
    public void setPressed(boolean b) {
      trayUI.getDisplayedItem().getModel().setPressed(b);
    }
    public void setRollover(boolean b) {
      trayUI.getDisplayedItem().getModel().setRollover(b);
    }
    */
  }
  

  /**
   * Our customization of swing.ButtonGroup to accommodate TrayComboBoxes
   * which don't subclass AbstractButton. Thus the button Vector type is
   * changed, but otherwise method implementations are 90% identical.
   * @see javax.swing.ButtonGroup
   * @author achang
   */
  class ToolGroup {

    /**
     * The list of buttons participating in this group.
     */
    protected Vector<ButtonModel> buttons = new Vector();

    /**
     * The current selection.
     */
    ButtonModel selection = null;

    /**
     * Constructor (does nothing).
     */
    public ToolGroup() {
    }

    /**
     * Adds the button to the group.
     * @param b the button to be added
     */
    public void add(ButtonModel b) {
      if (b == null) {
        return;
      }
      buttons.addElement(b);

      if (b.isSelected()) {
        if (selection == null) {
          selection = b;
        } else {
          b.setSelected(false);
        }
      }
    }

    /**
     * Removes the button from the group.
     * @param b the button to be removed
     */
    public void remove(AbstractButton b) {
      if (b == null) {
        return;
      }
      buttons.removeElement(b);
      if (b.getModel() == selection) {
        selection = null;
      }
    }

    /**
     * Returns all the buttons that are participating in this group.
     * @return an <code>Enumeration</code> of the buttons in this group
     */
    public Enumeration<ButtonModel> getElements() {
      return buttons.elements();
    }

    /**
     * Returns the model of the selected button.
     * @return the selected button model
     */
    public ButtonModel getSelection() {
      return selection;
    }

    /**
     * Sets the selected value for the <code>ButtonModel</code>. Only one
     * button in the group may be selected at a time.
     * @param m the <code>ButtonModel</code>
     * @param b <code>true</code> if this button is to be selected, 
     *          otherwise <code>false</code>
     */
    public void setSelected(ButtonModel m, boolean b) {
      if (b && m != null && m != selection) {
        ButtonModel oldSelection = selection;
        selection = m;
        if (oldSelection != null) {
          oldSelection.setSelected(false);
        }
        m.setSelected(true);
      }
    }

    /**
     * Returns whether a <code>ButtonModel</code> is selected.
     * @return <code>true</code> if the button is selected, otherwise 
     *         returns <code>false</code>
     */
    public boolean isSelected(ButtonModel m) {
      return (m == selection);
    }

    /**
     * Returns the number of buttons in the group.
     * @return the button count
     */
    public int getButtonCount() {
      if (buttons == null) {
        return 0;
      } else {
        return buttons.size();
      }
    }

  }
  
}