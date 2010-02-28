package processing.app;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputListener;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.plaf.basic.BasicComboPopup;

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
    "lin", "sol", "dot", "emp"
  };
  
  /** Rollover titles for each button. */
  static final String colors[] = {
    "A", "B", "C", "D", "E"
  };
  
  static final int INACTIVE = 0;
  static final int ROLLOVER = 1;
  static final int ACTIVE   = 2;
  static final int BUTTON_WIDTH = 21;
  
  Editor editor; //TODO drawArea instead?
  
  public EditorDrawingHeader(Editor eddie) {
    // TODO Auto-generated constructor stub
    
    setBackground(Theme.getColor("header.bgcolor"));
    setBorder(null);
//    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    setLayout(new FlowLayout(FlowLayout.TRAILING, 0, 0));//can't seem to reduce gap...
    setPreferredSize(new Dimension(500, EditorToolbar.BUTTON_HEIGHT)); //TODO width here should be fluid
    
    
    /*
     * the toggle button
     */
    JButton test = new JButton(Base.getImageIcon("graph-inact-text.gif", this));
    test.setRolloverIcon(Base.getImageIcon("graph-rollo-text.gif", this));
    test.setRolloverSelectedIcon(Base.getImageIcon("graph-activ-text.gif", this));
    test.setToolTipText("Text tool");
    test.setMargin(new Insets(0,0,0,0));
    test.setContentAreaFilled(false);
    test.setBorderPainted(false); //borderPainted seems the only one necessary for a mac
    test.setFocusPainted(false); //TODO test these when going cross-platform
    add(test);
    
    //================= sandbox START ====================
    JComboBox toggleList = new JComboBox();
    
    JToggleButton t1 = new JToggleButton(Base.getImageIcon("graph-inact-unlock.gif", this));
    t1.setRolloverIcon(Base.getImageIcon("graph-rollo-unlock.gif", this));
    t1.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-lock.gif", this));
    t1.setSelectedIcon(Base.getImageIcon("graph-inact-lock.gif", this));
    t1.setPressedIcon(Base.getImageIcon("graph-activ-lock.gif", this));
    // ^--there isn't a pressedSelectedIcon vs. pressedIcon unfortunately, but most users probably won't notice
    t1.setBorderPainted(false);
    toggleList.add(t1);
    
    JToggleButton t2 = new JToggleButton(Base.getImageIcon("graph-inact-paint.gif", this));
    t2.setRolloverIcon(Base.getImageIcon("graph-rollo-paint.gif", this));
    t2.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-text.gif", this));
    t2.setSelectedIcon(Base.getImageIcon("graph-activ-paint.gif", this));
    t2.setBorderPainted(false);
    toggleList.add(t2);
    
    JToggleButton t3 = new JToggleButton(Base.getImageIcon("graph-inact-lock.gif", this));
    t3.setRolloverIcon(Base.getImageIcon("graph-rollo-lock.gif", this));
    t3.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-text.gif", this));
    t3.setSelectedIcon(Base.getImageIcon("graph-activ-lock.gif", this));
    t3.setBorderPainted(false);
    toggleList.add(t3);
    
    JToggleButton t4 = new JToggleButton(Base.getImageIcon("graph-inact-audio.gif", this));
    t4.setRolloverIcon(Base.getImageIcon("graph-rollo-audio.gif", this));
    t4.setRolloverSelectedIcon(Base.getImageIcon("graph-rollo-text.gif", this));
    t4.setSelectedIcon(Base.getImageIcon("graph-activ-audio.gif", this));
    t4.setBorderPainted(false);
    toggleList.add(t4);
    
    toggleList.setUI(new BasicComboBoxUI());
    add(t1);
//    add(toggleList);
    //================= sandbox END ====================
    
    JButton cursorButton = new JButton("<=");
    cursorButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((JButton)e.getSource()).getText());
      }
    });
    JComboBox shapeList = new JComboBox(shapes);
    shapeList.setSelectedIndex(0);
    shapeList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("shape selected: "+ (String) ((JComboBox)e.getSource()).getSelectedItem());
      }
    }); 
    JComboBox connectorList = new JComboBox(connectors);
    connectorList.setSelectedIndex(0);
    connectorList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("connector selected: "+ (String) ((JComboBox)e.getSource()).getSelectedItem());
      }
    });
    JButton textButton = new JButton("T");
    textButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((JButton)e.getSource()).getText());
      }
    });
    JComboBox colorList = new JComboBox(colors);
    colorList.setSelectedIndex(0);
    colorList.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println("color selected: "+ (String) ((JComboBox)e.getSource()).getSelectedItem());
      }
    });
    JButton lockButton = new JButton("c[]");
    lockButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((JButton)e.getSource()).getText());
      }
    });
    JButton zoomInButton = new JButton("z+");
    zoomInButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((JButton)e.getSource()).getText());
      }
    });
    JButton zoomOutButton = new JButton("z-");
    zoomOutButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        System.out.println((String) ((JButton)e.getSource()).getText());
      }
    });
    

    //================= sandbox START ====================
    ListRenderer renderer = new ListRenderer();
    shapeList.setUI(new TrayComboBoxUI());
    shapeList.setRenderer(renderer); // this gets the appropriate icon images
    shapeList.setPreferredSize(new Dimension(BUTTON_WIDTH, BUTTON_WIDTH));
    add(shapeList);
    //================= sandbox END ====================
    
    add(cursorButton);
//    add(shapeList);
    add(connectorList);
    add(textButton);
    add(colorList);
    add(lockButton);
    add(zoomInButton);
    add(zoomOutButton);
    
    setVisible(true);
  }

  class ListRenderer extends JLabel implements ListCellRenderer {
    
    public ListRenderer() {
//      setOpaque(false);  //useless
//      setSize(21, 21);  //useless
      setHorizontalAlignment(CENTER);
      setVerticalAlignment(CENTER);
    }

    /*
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

    /**
     * AGNES NOTE: since you can't cast from a JToggleButton to a JButton
     * might have to overwrite a lot of stuff??
     * either that or stick in a JButton subclass that does all the things
     * I need a JToggleButton to do 
     */
    public TrayComboBoxUI() {
      super();
    }
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
      unconfigureArrowButton();
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
        arrowButton.setSelectedIcon(Base.getImageIcon("graph-inact-flag-"+value+".gif", arrowButton));
        arrowButton.setPressedIcon(Base.getImageIcon("graph-activ-flag-"+value+".gif", arrowButton));
        System.out.println("updateDisplayedItem -- "+value);
      }
    }
  }
}