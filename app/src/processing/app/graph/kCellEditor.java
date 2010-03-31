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
   * The corner radius of editpanel's background (round rectangle)
   */
  public static final int ROUNDRECT_RADIUS = 7;
  /**
   * The fill color of edit panel (round rectangle). Half-transparent.
   */
  public static final Color EDITPANEL_FILL = Color.white;
  /**
   * The color of text in text fields (editable text); set this 
   * to null to use the cell label's original font color.
   */
  public static final Color EDITTEXT_FONTCOLOR = Color.black;
  /**
   * Border color of text field and text area
   */
  public static final Color FIELD_BORDERS = Color.lightGray;
  /**
   * Used to define the sizes of the text components such 
   * that there is a scalable spacing between them.
   */
  public static final int PADDING = 7;

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
   * Need to be able to reference the layout gap between
   * label and notes so that we can set it to invisible
   * when label is invisible.
   */
  protected transient Component gap;

  
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
    textArea.setBorder(BorderFactory.createLineBorder(FIELD_BORDERS));
    
    // Creates the label text editing field
    labelField = new JTextField();
    labelField.setBorder(BorderFactory.createLineBorder(FIELD_BORDERS));
    labelField.setOpaque(true);
    labelField.addKeyListener(keyListener);
    
    // Creates the notes text editing field and puts it in a scroll pane
    notesField = new JTextArea();
    notesField.setBorder(BorderFactory.createEmptyBorder());
    notesField.setOpaque(true);
    notesField.addKeyListener(keyListener);
    notesScrollPane = new JScrollPane(notesField);
    notesScrollPane.setBorder(BorderFactory.createLineBorder(FIELD_BORDERS));
    notesScrollPane.setOpaque(false);
    notesScrollPane.getViewport().setOpaque(false); //<--how I love how java requires you to set EVERYTHING transparent

    // Creates the panel to hold both label and notes
    kEditPanel = new JPanel() {
      public void paintComponent(Graphics g) {
        //doesn't appear to do anything, but we'll do it anyway:
        super.paintComponent(g);

        //background is a rounded rectangle in translucent gray.
        g.setColor(new Color(EDITPANEL_FILL.getRed(),
            EDITPANEL_FILL.getGreen(), EDITPANEL_FILL.getBlue(), 150));
        g.fillRoundRect(0, 0, getWidth(), getHeight(), ROUNDRECT_RADIUS,
                        ROUNDRECT_RADIUS);
      }
    };
    kEditPanel.setLayout(new BoxLayout(kEditPanel, BoxLayout.PAGE_AXIS));
    kEditPanel.setBorder(BorderFactory.createEmptyBorder(PADDING*2,PADDING*2,PADDING*2,PADDING*2));
    kEditPanel.setOpaque(false);

    kEditPanel.add(labelField);
    gap = Box.createRigidArea(new Dimension(0, PADDING));
    kEditPanel.add(gap);
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
//      JComponent currentEditor = (isKCellValue()) ? kEditPanel : mxEditPanel;
      JComponent currentEditor = kEditPanel;
      
      currentEditor.setBounds(getEditorBounds(state, scale));
      currentEditor.setVisible(true);
      
      //get color of text in cell; get black instead if none specified
      Color fontColor = mxUtils.getColor(state.getStyle(),
                                         mxConstants.STYLE_FONTCOLOR,
                                         Color.black);

      if (isKCellValue()) 
      {
        labelField.setVisible(true);
        gap.setVisible(true);
        state.getStyle()
            .put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        labelField.setFont(mxUtils.getFont(state.getStyle(), scale));
        
        //set font color if one for editor was specified
        if (EDITTEXT_FONTCOLOR != null)
          labelField.setForeground(EDITTEXT_FONTCOLOR);
        else
          labelField.setForeground(fontColor);
        
        labelField.setText(((kCellValue) ((mxICell) cell).getValue())
            .getLabel());
        labelField.setMaximumSize(new Dimension(kEditPanel.getBounds().width - PADDING*4,
            state.getLabelBounds().getRectangle().height - PADDING*4));
        labelField.setMinimumSize(new Dimension(kEditPanel.getBounds().width - PADDING*4,
            state.getLabelBounds().getRectangle().height - PADDING*4));

        state.getStyle().put(mxConstants.STYLE_FONTSTYLE, 0);
        notesField.setFont(mxUtils.getFont(state.getStyle(), scale));
        
        //set font color if one for editor was specified
        if (EDITTEXT_FONTCOLOR != null)
          notesField.setForeground(EDITTEXT_FONTCOLOR);
        else
          notesField.setForeground(fontColor);
        
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
        gap.setVisible(false);
        labelField.setVisible(false);
        state.getStyle().put(mxConstants.STYLE_FONTSTYLE, 0);
        notesField.setFont(mxUtils.getFont(state.getStyle(), scale));
        
        //set font color if one for editor was specified
        if (EDITTEXT_FONTCOLOR != null)
          notesField.setForeground(EDITTEXT_FONTCOLOR);
        else
          notesField.setForeground(fontColor);
        
        notesField.setText(getInitialValue(state, trigger));

        currentField = notesField;
      }
      
      graphComponent.getGraphControl().add(currentEditor, 0);

      if (isHideLabel(state))
      {
        graphComponent.redraw(state);
      }

      currentEditor.revalidate();
      //put the cursor in the labelField and highlight all text:
      currentField.requestFocusInWindow(); //requestFocus();
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
//      JComponent currentEditor = (isKCellValue()) ? kEditPanel : mxEditPanel;
      JComponent currentEditor = kEditPanel;
      
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
    } else {
      return notesField.getText();
    }
    
  }
  
  /**
   * A shortcut to know which mode of editor we are currently in.
   */
  public boolean isKCellValue() {
    return (((mxICell) editingCell).getValue() instanceof kCellValue);
  }
}
