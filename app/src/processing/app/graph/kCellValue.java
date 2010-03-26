package processing.app.graph;

import java.io.Serializable;

/**
 * An Object that is made up of two strings and a pair of integer
 * code marks; used to simultaneously store a label and notes inside a cell.
 * We also store codemarks here instead of extending mxCell for convenience
 * in coding (one less class to mess with).
 * @author achang
 */
public class kCellValue implements Serializable {

  private static final long serialVersionUID = -3237429105695890369L;
  
  /**
   * The name of the cell.
   */
  private String label;
  /**
   * A longer field of description for the cell.
   */
  private String notes;
  /**
   * The code sketch (tab/file) to which this cell refers.
   * TODO double check that the codeIndex doesn't mess up if I reorder tabs.
   */
  private int codeIndex;
  /**
   * The document character index that marks the beginning of the code section
   * that this cell refers to.  This number is always less than stopMark.
   */
  private int startMark;
  /**
   * The document character index that marks the end of the code section
   * that this cell refers to.  This number is always greater than or equal to stopMark.
   */
  private int stopMark;
  
  /*
   * Constructors
   */
  public kCellValue() {
    this("<unlabeled>","",-1,-1,-1);
  }
  public kCellValue(String label) {
    this(label,"",-1,-1,-1);
  }
  public kCellValue(String label, String notes) {
    this(label, notes,-1,-1,-1);
  }
  public kCellValue(String label, String notes, int index, int start, int stop) {
    this.label = label;
    this.notes = notes;
    this.codeIndex = index;
    this.startMark = start;
    this.stopMark = stop;
  }

   /*
   * Accessors (getters and setters)
   */
  public String getLabel() {
    return label;
  }
  public void setLabel(String s) {
    label = s;
  }
  public String getNotes() {
    return notes;
  }
  public void setNotes(String s) {
    notes = s;
  }
  public int getCodeIndex() {
    return codeIndex;
  } 
  public void setCodeIndex(int ind) {
    codeIndex = ind;
  }
  public int getStartMark() {
    return startMark;
  }
  public int getStopMark() {
    return stopMark;
  }
  public void setStartMark(int start) {
    if (startMark != -1 && start > stopMark) { //then turn it around
      startMark = stopMark;
      stopMark = start;
    } else //like normal
      startMark = start;
  }
  public void setStopMark(int stop) {
    if (stopMark != -1 && stop < startMark) { //then turn it around
      stopMark = startMark;   
      startMark = stop;   
    } else //like normal
      stopMark = stop;  
  }
  
   /*
   * Other Methods
   */
  public void shiftCodeMarks(int offset) {
    startMark = startMark + offset;
    stopMark = stopMark + offset;
  }
  public void setCodeMark(int start, int stop, int ind) {
    setStartMark(start);
    setStopMark(stop);
    setCodeIndex(ind);
  }
  public boolean hasValidCodeMarks() {
    return (isValidCodeIndex() && (startMark != -1) && (stopMark != -1));
  }
  public boolean isValidCodeIndex() {
    return (codeIndex >= 0);
  }
  public void invalidateCodeMarks() {
    setCodeMark(-1,-1,-1);
  }
  public String toPrettyString() {
    String s = "['"+label+"', '"+notes+"', "+codeIndex+", "+startMark+", "+stopMark+"]";
    return s;
  }

}
