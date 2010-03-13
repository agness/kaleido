package processing.app.graph;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
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
  protected transient JPanel editPanel;
  /**
   * Used to define the sizes of the text components such 
   * that there is a scalable spacing between them.
   */
  public static int DEFAULT_SPACING = 2;
  
  /*
   * ========CONSTRUCTOR=======
   */
  public kCellEditor(mxGraphComponent graphComponent)
  {
    super(graphComponent);
    /*
     * Override escape key listener (since the only ESC key listener we have is
     * in MGraphComponent, which is not listening when this editor is active.
     * I have no idea what the original keyListener was supposed to do (maybe
     * it only works for PCs).
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
    //TODO superclass keyhandler that handles enter, shift, control, and alt
    //should be seriously considered.
    //Meanwhile we still need escape key activity, so maybe override
    
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

    // Creates the panel to hold both label and notes
    editPanel = new JPanel();
    editPanel.setLayout(new BoxLayout(editPanel, BoxLayout.PAGE_AXIS));
    editPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    editPanel.setOpaque(false);

    editPanel.add(labelField);
//    editPanel.add(Box.createVerticalGlue());
    editPanel.add(Box.createRigidArea(new Dimension(0, 5)));
    editPanel.add(notesScrollPane);
  }

  /**
   * 
   * @param width
   * @param height
   */
  public void resize(int width, int height) {
    //TODO YO WRITE THIS
    System.out.println("kCellEditor >> resize");
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
      JComponent currentEditor = (iskCellValue()) ? editPanel : scrollPane;

      currentEditor.setBounds(getEditorBounds(state, scale));
      currentEditor.setVisible(true);
      
      Color fontColor = mxUtils.getColor(state.getStyle(), mxConstants.STYLE_FONTCOLOR, Color.black);
      
      if (iskCellValue()) {
        Object v1 = state.getStyle().put(mxConstants.STYLE_FONTSTYLE, mxConstants.FONT_BOLD);
        labelField.setFont(mxUtils.getFont(state.getStyle(), scale));
        labelField.setForeground(fontColor);
        labelField.setText(((kCellValue) ((mxICell) cell).getValue()).getLabel());
          labelField.setMaximumSize(new Dimension(editPanel.getBounds().width,state.getLabelBounds().getRectangle().height));
          labelField.setMinimumSize(new Dimension(editPanel.getBounds().width,state.getLabelBounds().getRectangle().height));
        
        Object v2 = state.getStyle().put(mxConstants.STYLE_FONTSTYLE, 0);
        notesField.setFont(mxUtils.getFont(state.getStyle(), scale));
        notesField.setForeground(fontColor);
        notesField.setText(((kCellValue) ((mxICell) cell).getValue()).getNotes());
        notesField.setMaximumSize(new Dimension(editPanel.getBounds().width, editPanel.getBounds().height-state.getLabelBounds().getRectangle().height-((int) Math.round(DEFAULT_SPACING * scale))));
          notesField.setMinimumSize(new Dimension(editPanel.getBounds().width, editPanel.getBounds().height-state.getLabelBounds().getRectangle().height-((int) Math.round(DEFAULT_SPACING * scale))));
          
        currentField = labelField;

        //TODO clean up this set max/min size mess... isn't there a better way to do this?
        // can fix it with the resizing thing I suppose
//        System.out.println("MGraphElementEditor bounds >> "+getEditorBounds(state, scale).toString());
//        System.out.println("MGraphElementEditor labelField actual size  >> "+labelField.getSize().toString());
//        System.out.println("MGraphElementEditor labelField max size >> "+labelField.getMaximumSize().toString());
//        System.out.println("MGraphElementEditor notesField actual size  >> "+notesField.getSize().toString());
//        System.out.println("MGraphElementEditor notesField max size >> "+notesField.getMaximumSize().toString());
      }
      else
      {
        textArea.setFont(mxUtils.getFont(state.getStyle(), scale));
        textArea.setForeground(fontColor);
        textArea.setText(getInitialValue(state, trigger));

        scrollPane.setViewportView(textArea);
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
      JComponent currentEditor = (iskCellValue()) ? editPanel : scrollPane;
      
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
    if (iskCellValue()) {
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
  public boolean iskCellValue() {
    return (((mxICell) editingCell).getValue() instanceof kCellValue);
  }
}
