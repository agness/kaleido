package processing.app;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractButton;
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
  
  Editor editor; //TODO drawArea instead for event handling / focus stuff?
  
  public EditorDrawingHeader(Editor eddie) {
    
    setBackground(Theme.getColor("header.bgcolor"));
    setBorder(null);
//    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setLayout(new FlowLayout(FlowLayout.TRAILING, EditorToolbar.BUTTON_GAP, (EditorToolbar.BUTTON_HEIGHT-BUTTON_WIDTH)/2+1));
    setPreferredSize(new Dimension(500, EditorToolbar.BUTTON_HEIGHT)); //TODO minor: width here should be fluid
    
     
    // CURSOR
    JToggleButton cursorButton = makeToolButton("cursor", "Cursor tool");
    cursorButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton)e.getSource()).getText());
      }
    });
    
    // SHAPES drop-down
    TrayComboBox shapeList = new TrayComboBox(shapes);
    shapeList.setSelectedIndex(0);
    shapeList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("shape selected: "+ (String) ((JComboBox)e.getSource()).getSelectedItem());
      }
    }); 
    
    // CONNECTORS drop-down
    TrayComboBox connectorList = new TrayComboBox(connectors);
    connectorList.setSelectedIndex(0);
    connectorList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("connector selected: "+ (String) ((JComboBox)e.getSource()).getSelectedItem());
      }
    });
    
    // TEXT
    JToggleButton textButton = makeToolButton("text", "Text tool");
    textButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton)e.getSource()).getText());
      }
    });
    
    // COLORS drop-down
    //TODO ohdear what are we going to do about this
    TrayComboBox colorList = new TrayComboBox(colors);
    colorList.setSelectedIndex(0);
    colorList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("color selected: "+ (String) ((JComboBox)e.getSource()).getSelectedItem());
      }
    });

    // LOCK/UNLOCK: the one and only toggle button
    JToggleButton lockButton = new JToggleButton(Base.getImageIcon("graph-inact-unlock.gif", this));
    lockButton.setRolloverIcon(Base.getImageIcon("graph-rollo-unlock.gif", this));
    lockButton.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-lock.gif", this));
    lockButton.setSelectedIcon(Base.getImageIcon("graph-inact-lock.gif", this));
    lockButton.setPressedIcon(Base.getImageIcon("graph-activ-lock.gif", this));
    // ^--there isn't a pressedSelectedIcon vs. pressedIcon unfortunately, but most users probably won't notice
    lockButton.setBorder(null); //ensures proper spacing between buttons (lord knows why)
    lockButton.setBorderPainted(false);
    lockButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("togglebutton isSelected "+((AbstractButton) e.getSource()).isSelected());
      }
    });

    // TEXT
    JToggleButton zoomInButton = makeToolButton("zoomin", "Zoom out");
    zoomInButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton)e.getSource()).getText());
      }
    });
    
    // ZOOMOUT
    JToggleButton zoomOutButton = makeToolButton("zoomout", "Zoom in");
    zoomOutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((AbstractButton)e.getSource()).getText());
      }
    });
    
    // an empty panel for layout purposes
    JPanel layoutSpacer = new JPanel();
    layoutSpacer.setBorder(null);
    layoutSpacer.setSize(EditorToolbar.BUTTON_GAP*2, EditorToolbar.BUTTON_GAP);
    layoutSpacer.setBackground(this.getBackground());
    
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
    //now to add a graphoutline here...
    
    setVisible(true);
  }
  
  /**
   * Shortcut method to make the uniform type of toggle buttons.
   * @param name, tooltip
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
  class TrayComboBox extends JComboBox {
    
    /** TODO for efficiency's sake, we should just load all 
     * the image icons once and swap between the objects.
     * Error handling will also be easier to deal with then.
     * 
     * Make a matrix of images, make some constants to refer
     * to the right row/col, yada yada yada...
     */
  
    TrayComboBoxUI trayUI = new TrayComboBoxUI();
    
    public TrayComboBox(Object[] items) {
      super(items);
      setRenderer(new ListRenderer());
      setUI(trayUI);
      setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_WIDTH));
    }
    /**
     * Shortcut to toggle main display button to OFF mode.
     */
    public void deactivate() {
      trayUI.displayedItemSetSelected(false);
    }

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
        //need to change our mouse listeners:
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
          System.out.println("updateDisplayedItem -- "+value);
        }
      }
      public void displayedItemSetSelected(boolean b) {
        if (arrowButton.isSelected() != b) {
          arrowButton.setSelected(b);
          updateDisplayedItem();
        }
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
                  System.out.println("trayHandler timer triggered");
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
          System.out.println("trayHandler mouseClicked");
          if (arrowButton.isSelected())
            arrowButton.setSelected(false);
          else
            arrowButton.setSelected(true);
          if (isPopupVisible(comboBox))
            setPopupVisible(comboBox, false);
        }
  
        public void mouseEntered(MouseEvent e) {
          System.out.println("arrowButton is "+arrowButton.isSelected());
        }
  
        public void mouseExited(MouseEvent e) {
        }
  
        public void mousePressed(MouseEvent e) {
          System.out.println("trayHandler mousePressed");
          enterTimer.start();
        }
  
        public void mouseReleased(MouseEvent e) {
          System.out.println("trayHandler mouseReleased");
          enterTimer.restart();
          enterTimer.stop();
        }
        
      }
    }
  }
}