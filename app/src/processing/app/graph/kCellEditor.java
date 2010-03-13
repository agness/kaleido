package processing.app.graph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.EventObject;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;

import processing.app.util.kConstants;

import com.mxgraph.model.mxICell;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.view.mxCellEditor;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;
import com.mxgraph.view.mxCellState;

/**
 * Extends mxCellEditor to handle cases where the value to be edited is
 * an instance of the kCellValue class.  Original mxCellEditor
 * functionality retained to handle generic mxCell editing (e.g. edges),
 * although HTML editing is left out.
 * 
 * TODO we're overriding so much already that we might as well just
 * stop extending and implement mxICellEditor
 * A note on the design: OmniGraffle's approach of WYSIWYG is probably
 * best and most intuitive; the original mxCellEditor is a little closer
 * to that, but fails miserably re:dynamic formatting; seeing as we
 * don't have that might time to implement, we'll go with this compromise:
 * @author achang
 */
public class kCellEditor extends mxCellEditor {
   
  /**
   * We'll use a text field for label editing,
   * and save the superclass' text area for note editing
   */
  protected transient JTextField labelField;
  /**
   * We'll use a text field for label editing,
   * and save the superclass' text area for note editing
   */
  protected transient JTextArea notesField;
  /**
   * A scroll panel to hold the notes' text area
   */
  protected transient JScrollPane notesScrollPane;
  /**
   * A panel to hold the two editing components
   */
  protected transient JPanel kEditPanel;
  /**
   * A reference to superclass.scrollPane, basically 
   * renaming it to be distinguishable from kEditPanel
   */
  protected transient JScrollPane mxEditPanel = scrollPane;
  /**
   * Used to define the sizes of the text components such 
   * that there is a scalable spacing between them.
   */
  public static int PADDING = 7;
  
  public static int ROUNDRECT_RADIUS = 7;
  
  /*
   * ========CONSTRUCTOR=======
   */
  public kCellEditor(mxGraphComponent graphComponent)
  {
    super(graphComponent);
    
    //make everything a little bigger TODO tweak values/layout
    minimumWidth = DEFAULT_MIN_WIDTH * 2;
    minimumHeight = DEFAULT_MIN_HEIGHT * 2;
    
    /*
     * Override escape key listener (since the only ESC key listener we have is
     * in MGraphComponent, which is not listening when this editor is active).
     * The original keylistener handles stopEditing on ENTER key
     * @param graphComponent
     */
    keyListener = new KeyAdapter()
    {
      public void keyPressed(KeyEvent e)
      {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE)
        {
          stopEditing(true);
        }
      }
    };
    
    // Styles the general-case text editing field created by the superclass
    textArea.setBorder(BorderFactory.createLineBorder(Color.lightGray));
    
    // Creates the label text editing field
    labelField = new JTextField();
    labelField.setBorder(BorderFactory.createLineBorder(Color.lightGray));
    labelField.setOpaque(true);
    labelField.addKeyListener(keyListener);
    
    // Creates the notes text editing field and puts it in a scroll pane
    notesField = new JTextArea();
    notesField.setBorder(BorderFactory.createEmptyBorder());
    notesField.setOpaque(true);
    notesField.addKeyListener(keyListener);
    notesScrollPane = new JScrollPane(notesField);
    notesScrollPane.setBorder(BorderFactory.createLineBorder(Color.lightGray));
    notesScrollPane.setVisible(true);
    notesScrollPane.setOpaque(false);
    notesScrollPane.getViewport().setOpaque(false); //<--how I love how java requires you to set EVERYTHING transparent

    // Creates the panel to hold both label and notes
    kEditPanel = new JPanel() {
      public void paintComponent(Graphics g) {
        //doesn't appear to do anything, but we'll do it anyway:
        super.paintComponent(g);

        //background is a rounded rectangle in translucent gray.
        g.setColor(new Color(kConstants.UI_COLOR_BACKGROUND.getRed(),
            kConstants.UI_COLOR_BACKGROUND.getGreen(),
            kConstants.UI_COLOR_BACKGROUND.getBlue(), 150));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), ROUNDRECT_RADIUS,
                        ROUNDRECT_RADIUS);

        System.out.println("kEditPanel >> paintComponents: bounds="
                           + getBounds() + ", w=" + getWidth() + ", h="
                           + getHeight());
      }
    };
    kEditPanel.setLayout(new BoxLayout(kEditPanel, BoxLayout.PAGE_AXIS));
    kEditPanel.setBorder(BorderFactory.createEmptyBorder(PADDING*2,PADDING*2,PADDING*2,PADDING*2));
    kEditPanel.setOpaque(false);

    kEditPanel.add(labelField);
    kEditPanel.add(Box.createRigidArea(new Dimension(0, PADDING)));
    kEditPanel.add(notesScrollPane);
  }

 
  /**
   * Experimenting with using fixed bounds... (will it crash at
   * the edge of canvas?)
   */
  public Rectangle getEditorBounds(mxCellState state, double scale)
  {
    Rectangle bounds = state.getRectangle();
    bounds.setLocation((int) state.getRectangle().getCenterX(), (int) state.getRectangle().getCenterY());  
    bounds.setSize((int) Math.round(minimumWidth * scale), (int) Math.round(minimumHeight * scale));

    return bounds;
  }
  

  /**
   * Reorganized to accommodate editing of kCellValues
   */
  public void startEditing(Object cell, EventObject trigger)
  {
//    System.out.println("MGraphElementEditor start editing >>");
  
    if (editingCell != null)
    {
      stopEditing(true);
    }

    mxCellState state = graphComponent.getGraph().getView().getState(cell);

    if (state != null)
    {
      double scale = Math.max(minimumEditorScale, graphComponent
          .getGraph().getView().getScale());
      JTextComponent currentField = null;
      this.trigger = trigger;
      editingCell = cell;
      JComponent currentEditor = (isKCellValue()) ? kEditPanel : mxEditPanel;
      
      currentEditor.setBounds(getEditorBounds(state, scale));
      currentEditor.setVisible(true);
      
      //get color of text in cell, use black if none specified
      Color fontColor = mxUtils.getColor(state.getStyle(),
                                         mxConstants.STYLE_FONTCOLOR,
                                         Color.black);

      if (isKCellValue()) 
      {
        state.getStyle()
            .put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        labelField.setFont(mxUtils.getFont(state.getStyle(), scale));
//        labelField.setForeground(fontColor);
        labelField.setText(((kCellValue) ((mxICell) cell).getValue())
            .getLabel());
        labelField.setMaximumSize(new Dimension(kEditPanel.getBounds().width - PADDING*4,
            state.getLabelBounds().getRectangle().height - PADDING*4));
        labelField.setMinimumSize(new Dimension(kEditPanel.getBounds().width - PADDING*4,
            state.getLabelBounds().getRectangle().height - PADDING*4));

        state.getStyle().put(mxConstants.STYLE_FONTSTYLE, 0);
        notesField.setFont(mxUtils.getFont(state.getStyle(), scale));
//        notesField.setForeground(fontColor);
        notesField.setText(((kCellValue) ((mxICell) cell).getValue())
            .getNotes());
        //if we want to do any formatting, we've got to do it on the notesScrollPane
//        notesScrollPane.setMaximumSize(new Dimension(kEditPanel.getBounds().width - PADDING*4,
//            kEditPanel.getBounds().height - PADDING*4
//                - state.getLabelBounds().getRectangle().height
//                - ((int) Math.round(PADDING * scale))));
//        notesScrollPane.setMinimumSize(new Dimension(kEditPanel.getBounds().width - PADDING*4,
//            kEditPanel.getBounds().height - PADDING*4
//                - state.getLabelBounds().getRectangle().height
//                - ((int) Math.round(PADDING * scale))));

        currentField = labelField;
      }
      else
      {   
        textArea.setFont(mxUtils.getFont(state.getStyle(), scale));
        textArea.setForeground(fontColor);
        textArea.setText(getInitialValue(state, trigger));

        mxEditPanel.setViewportView(textArea);
        currentField = textArea;
      }
      
      graphComponent.getGraphControl().add(currentEditor, 0);

      if (isHideLabel(state))
      {
        graphComponent.redraw(state);
      }

      currentEditor.revalidate();
      //put the cursor in the labelField and highlight all text:
      currentField.requestFocusInWindow();
      currentField.selectAll();
      //force the graphComponent to paint our editor, otherwise
      // it will get clipped in various ugggly ways:
      graphComponent.paintImmediately(currentEditor.getBounds());
    }
  }

  /**
   * If editing was not canceled, save field values.  In any case 
   * remove the editor and transfer UI focus back to graphComponent.
   */
  public void stopEditing(boolean cancel)
  {
//    System.out.println("MGraphElementEditor cancel >> "+cancel);
    
    if (editingCell != null)
    {
      JComponent currentEditor = (isKCellValue()) ? kEditPanel : mxEditPanel;
      
      currentEditor.transferFocusUpCycle();
      Object cell = editingCell;

      if (!cancel)
      {
        EventObject trig = trigger;
        trigger = null;
        graphComponent.labelChanged(cell, getFieldValues(cell), trig);
      }
      else
      {
        mxCellState state = graphComponent.getGraph().getView()
            .getState(cell);
        graphComponent.redraw(state);
      }

      if (currentEditor.getParent() != null)
      {
        currentEditor.setVisible(false);
        currentEditor.getParent().remove(currentEditor);
      }
      
      // kill editingCell here instead, so kCellEditor will work until this point
      editingCell = null;
      graphComponent.requestFocusInWindow();
    }
  }
  
  /**
   * Returns the current editing value.
   * (Whilst preserving codemark data.)
   */
  public Object getFieldValues(Object cell)
  {
    if (isKCellValue()) {
      kCellValue val = (kCellValue) ((mxICell) editingCell).getValue();
      val.setLabel(labelField.getText());
      val.setNotes(notesField.getText());
      return val;
    } else {//use original mxCellEditor implementation
      return getCurrentValue();
    }
    
  }
  
  /**
   * A shortcut to know which mode of editor we are currently in.
   */
  public boolean isKCellValue() {
    return (((mxICell) editingCell).getValue() instanceof kCellValue);
  }
}
