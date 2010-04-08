/* -*- mode: java; c-basic-offset: 2; indent-tabs-mode: nil -*- */

/*
  Part of the Processing project - http://processing.org

  Copyright (c) 2004-09 Ben Fry and Casey Reas
  Copyright (c) 2001-04 Massachusetts Institute of Technology

  This program is free software; you can redistribute it and/or modify
  it under the terms of the GNU General Public License version 2
  as published by the Free Software Foundation.

  This program is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program; if not, write to the Free Software Foundation,
  Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
*/

package processing.app;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.print.PageFormat;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.event.DocumentEvent;
import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.event.DocumentEvent.EventType;
import javax.swing.text.BadLocationException;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.CompoundEdit;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;

import org.w3c.dom.Document;

import processing.app.debug.Runner;
import processing.app.debug.RunnerException;
import processing.app.debug.RunnerListener;
import processing.app.graph.kCellValue;
import processing.app.graph.kGraphComponent;
import processing.app.syntax.JEditTextArea;
import processing.app.syntax.PdeKeywords;
import processing.app.syntax.PdeTextAreaDefaults;
import processing.app.syntax.SyntaxDocument;
import processing.app.syntax.TextAreaPainter;
import processing.app.tools.DiscourseFormat;
import processing.app.tools.Tool;
import processing.app.util.kConstants;
import processing.app.util.kEvent;
import processing.app.util.kUndoManager;
import processing.app.util.kUndoableEdit;
import processing.app.util.kUtils;
import processing.core.PApplet;

import com.mxgraph.io.mxCodec;
import com.mxgraph.model.mxICell;
import com.mxgraph.model.mxGraphModel.mxValueChange;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.swing.util.mxGraphActions;
import com.mxgraph.util.mxEvent;
import com.mxgraph.util.mxEventObject;
import com.mxgraph.util.mxUndoableEdit;
import com.mxgraph.util.mxUtils;
import com.mxgraph.util.mxEventSource.mxIEventListener;
import com.mxgraph.util.mxUndoableEdit.mxUndoableChange;
import com.mxgraph.view.mxGraph;

/**
 * Main editor panel for the Processing Development Environment.
 */
public class Editor extends JFrame implements RunnerListener {



  Base base;

  // otherwise, if the window is resized with the message label
  // set to blank, it's preferredSize() will be fukered
  static protected final String EMPTY =
    "                                                                     " +
    "                                                                     " +
    "                                                                     ";

  /** Command on Mac OS X, Ctrl on Windows and Linux */
  public static final int SHORTCUT_KEY_MASK =
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  /** Command-W on Mac OS X, Ctrl-W on Windows and Linux */
  public static final KeyStroke WINDOW_CLOSE_KEYSTROKE =
    KeyStroke.getKeyStroke('W', SHORTCUT_KEY_MASK);
  /** Command-Shift on Mac OS X, Ctrl-Shift on Windows and Linux */
  public static final int SHORTCUT_SHIFT_KEY_MASK = ActionEvent.SHIFT_MASK |
  Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
  /** Command-Option on Mac OS X, Ctrl-Alt on Windows and Linux */
  public static final int SHORTCUT_ALT_KEY_MASK = ActionEvent.ALT_MASK |
    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

  /**
   * true if this file has not yet been given a name by the user
   */
  boolean untitled;

  PageFormat pageFormat;
  PrinterJob printerJob;

  // file and sketch menus for re-inserting items
  EditorToolbar toolbar;
  JMenu fileMenu;
  JMenu sketchMenu;
  JMenuItem exportAppItem;
  JMenuItem saveMenuItem;
  JMenuItem saveAsMenuItem;
  // these menus are shared so that they needn't be rebuilt for all windows
  // each time a sketch is created, renamed, or moved.
  static JMenu toolbarMenu;
  static JMenu sketchbookMenu;
  static JMenu examplesMenu;
  static JMenu importMenu;

  // other editor UI content components
  EditorStatus status;
  EditorConsole console;
  EditorLineStatus lineStatus;
  JLabel lineNumberComponent;

  // editor UI layout components
  JSplitPane editConsoleSplitPane;
  JSplitPane graphCodeSplitPane;
  JPanel consolePanel;

  // text-side object & swing component
  Sketch sketch;  // currently opened program
  EditorTextHeader textHeader;
  JEditTextArea textarea;
  
  // syntax default settings shared by all code editors (i.e. code windows + text area)
  public static final PdeKeywords pdeTokenMarker = new PdeKeywords();

  // graph-side object & swing component
  EditorDrawingHeader drawingHeader;
  DrawingArea drawarea;
  
  // event handling
  TextAreaListener listener;
  FocusHandler focusHandler = new FocusHandler();
  
  // runtime information and window placement
  Point sketchWindowLocation;
  Runner runtime;

  // undo fellers
  JMenuItem undoItem, redoItem;
  protected UndoAction undoAction;
  protected RedoAction redoAction;
  kUndoManager undoManager;
  CompoundEdit compoundEdit;

  // find replace
  FindReplace find;

  // execution stuff
  boolean running;
  Runnable runHandler;
  Runnable presentHandler;
  Runnable stopHandler;
  Runnable exportHandler;
  Runnable exportAppHandler;


  public Editor(Base ibase, String path, int[] location) {
    super("Kaleido");
    this.base = ibase;

    Base.setIcon(this);

    // Install default actions for Run, Present, etc.
    resetHandlers();

    // add listener to handle window close box hit event
    addWindowListener(new WindowAdapter() {
        public void windowClosing(WindowEvent e) {
          base.handleClose(Editor.this);
        }
      });
    // don't close the window when clicked, the app will take care
    // of that via the handleQuitInternal() methods
    // http://dev.processing.org/bugs/show_bug.cgi?id=440
    setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    // When bringing a window to front, let the Base know
    addWindowListener(new WindowAdapter() {
        public void windowActivated(WindowEvent e) {
//          System.err.println("activate");  // not coming through
          base.handleActivated(Editor.this);
          // re-add the sub-menus that are shared by all windows
          fileMenu.insert(sketchbookMenu, 2);
          fileMenu.insert(examplesMenu, 3);
          sketchMenu.insert(importMenu, 4);
        }

        // added for 1.0.5
        // http://dev.processing.org/bugs/show_bug.cgi?id=1260
        public void windowDeactivated(WindowEvent e) {
//          System.err.println("deactivate");  // not coming through
          fileMenu.remove(sketchbookMenu);
          fileMenu.remove(examplesMenu);
          sketchMenu.remove(importMenu);
        }
      });

    //PdeKeywords keywords = new PdeKeywords();
    //sketchbook = new Sketchbook(this);

    buildMenuBar();
    
    
    // DRAWEDITOR BOX holds the draw tool bar and draw area (needs to be constructed before textarea)
    drawarea = new DrawingArea(this);
    drawarea.setRightClickPopup(new DrawAreaPopup());
    drawingHeader = new EditorDrawingHeader(drawarea);
    Box drawEditorBox = Box.createVerticalBox();
    drawEditorBox.add(drawingHeader);
    drawEditorBox.add(drawarea);
    
    
    // TEXTEDITOR BOX holds code file tabs and code edit area 
    textHeader = new EditorTextHeader(this);
    textarea = new JEditTextArea(new PdeTextAreaDefaults(), drawarea);
    textarea.setRightClickPopup(new TextAreaPopup()); //TODO popupFocusHandler
    // textarea.setHorizontalOffset(6); set inside JEditTextArea to give space for kTextAreaPainter
    Box textEditorBox = Box.createVerticalBox();
    textEditorBox.add(textHeader);
    textEditorBox.add(textarea);

    
    // GRAPHCODE SPLIT PANE holds the text editor box and the draw editor box
    graphCodeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, drawEditorBox, textEditorBox);
    graphCodeSplitPane.setOneTouchExpandable(true);
    graphCodeSplitPane.setContinuousLayout(true);
    graphCodeSplitPane.setResizeWeight(0.5);
    int dividerSize = Preferences.getInteger("editor.divider.size");
    if (dividerSize != 0) {
      graphCodeSplitPane.setDividerSize(dividerSize);
    }
    graphCodeSplitPane.setBorder(null);
    graphCodeSplitPane.setDividerLocation(Preferences.getInteger("default.window.width")/2);
    
    
    // EDITING PANEL as distinguished from the consolePanel
    // holds the tool bar and the graphCodeSplitPane 
    JPanel editingPanel = new JPanel(); 
    editingPanel.setLayout(new BorderLayout());

    if (toolbarMenu == null) {
      toolbarMenu = new JMenu();
      base.rebuildToolbarMenu(toolbarMenu);
    }
    toolbar = new EditorToolbar(this, toolbarMenu);
    editingPanel.add(toolbar, BorderLayout.NORTH);
    editingPanel.add(graphCodeSplitPane, BorderLayout.CENTER);
    
    
    // CONSOLE PANEL holds status area and the console itself
    consolePanel = new JPanel();
    consolePanel.setLayout(new BorderLayout());
    status = new EditorStatus(this);
    consolePanel.add(status, BorderLayout.NORTH);
    console = new EditorConsole(this);
    console.setBorder(null);     // windows puts an ugly border on this guy
    consolePanel.add(console, BorderLayout.CENTER);
    lineStatus = new EditorLineStatus(textarea);
    consolePanel.add(lineStatus, BorderLayout.SOUTH);

    
    // EDIT CONSOLE SPLIT PANE divides between the console outputs
    // and the editing areas (everything else)
    editConsoleSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, editingPanel, consolePanel);
    editConsoleSplitPane.setOneTouchExpandable(true);
    editConsoleSplitPane.setContinuousLayout(true); // repaint child panes while resizing
    // if window increases in size, give all of increase to
    // the editPanel in the upper pane
    editConsoleSplitPane.setResizeWeight(1D);
    // to fix ugliness.. normally macosx java 1.3 puts an
    // ugly white border around this object, so turn it off.
    editConsoleSplitPane.setBorder(null);
    // the default size on windows is too small and kinda ugly
    dividerSize = Preferences.getInteger("editor.divider.size");
    if (dividerSize != 0) {
      editConsoleSplitPane.setDividerSize(dividerSize);
    }
    
    
    // SET FOCUS LISTENERS
    drawarea.getGraphComponent().addFocusListener(focusHandler);
    textarea.addFocusListener(focusHandler);
    // hopefully these are no longer needed w/ swing
    // (har har har.. that was wishful thinking)
    listener = new TextAreaListener(this, textarea);
    installCodeDrawLinkListeners();
    installSelectionSyncListeners();
    installDocumentSyncListeners();

    
    // SET TEXTAREA LISTENERS
    // key/mouse/whatever: get shift down/up events so we can show the alt version of toolbar buttons
    textarea.addKeyListener(toolbar);
    // transfer handler: defines general copy-cut-paste
    textarea.setTransferHandler(new FileDropHandler()); //TODO needs testing
    // change tracker: updates the modified flag if anything in the text
    // area document changes; saves us a number of "setModified" lines from the
    // original P5 implementation
    textarea.addListener(kEvent.TEXTAREA_DOCUMENT_CHANGE, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        sketch.setModified(true);
      }
    });
    
    // UNDO MANAGEMENT: a.k.a. command history
    undoManager = new kUndoManager();
    mxIEventListener graphUndoHandler = new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt)
      {
        undoManager.undoableEditHappened((mxUndoableEdit) evt
            .getProperty("edit"));
        undoAction.updateUndoState();
        redoAction.updateRedoState();
      }
    };
    // Adds the command history to the model and view (although I'm not sure that we use the view at all)
    drawarea.getGraphComponent().getGraph().getModel().addListener(mxEvent.UNDO, graphUndoHandler);
    drawarea.getGraphComponent().getGraph().getView().addListener(mxEvent.UNDO, graphUndoHandler);    
    // Keeps the selection and the link markers in sync with the command history
    mxIEventListener undoSelectionSyncHandler = new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) 
      {
        //TODO have the undoManager throw java.UndoableEdit-friendly
        //undo events so maybe we can sync the text selection as well?
        
        List<mxUndoableChange> changes = ((mxUndoableEdit) evt
            .getProperty("edit")).getChanges();
        drawarea.getGraphComponent().getGraph()
            .setSelectionCells(
                               drawarea.getGraphComponent().getGraph()
                                   .getSelectionCellsForChanges(changes));
        // if it's a kUndoableEdit, meaning possibly a linkEdit undo,
        // then repaint the lines of the old and new links
        if (evt.getProperty("edit") instanceof kUndoableEdit)
          repaintLinesOfChanges(changes);
      }
    };
    undoManager.addListener(mxEvent.UNDO, undoSelectionSyncHandler);
    undoManager.addListener(mxEvent.REDO, undoSelectionSyncHandler);

    // ADD TO THIS JFRAME
    setContentPane(editConsoleSplitPane);
    pack();
    // Set the window bounds and the divider location before setting it visible
    setPlacement(location);
    // If the window is resized too small this will resize it again to the
    // minimums. Adapted by Chris Lonnen from comments here:
    // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4320050
    // as a fix for http://dev.processing.org/bugs/show_bug.cgi?id=25
    final int minW = Preferences.getInteger("editor.window.width.min");
    final int minH = Preferences.getInteger("editor.window.height.min");
    addComponentListener(new java.awt.event.ComponentAdapter() {
        public void componentResized(ComponentEvent event) {
          setSize((getWidth() < minW) ? minW : getWidth(),
                  (getHeight() < minH) ? minH : getHeight());
        }
      });

    // Bring back the general options for the editor
    applyPreferences();
    
    // All set, now open the document that was passed in
    boolean loaded = handleOpenInternal(path);
    if (!loaded) sketch = null;
  }



  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .

  
  /**
   * Handles files dragged & dropped from the desktop and into the editor
   * window. Dragging files into the editor window is the same as using
   * "Sketch &rarr; Add File" for each file.
   */
  class FileDropHandler extends TransferHandler {
    public boolean canImport(JComponent dest, DataFlavor[] flavors) {
      return true;
    }

    @SuppressWarnings("unchecked")
    public boolean importData(JComponent src, Transferable transferable) {
      int successful = 0;
      String type = "";

      try {
        DataFlavor uriListFlavor =
          new DataFlavor("text/uri-list;class=java.lang.String");

        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
          type = "file";
          java.util.List list = (java.util.List)
            transferable.getTransferData(DataFlavor.javaFileListFlavor);
          for (int i = 0; i < list.size(); i++) {
            File file = (File) list.get(i);
            if (sketch.addFile(file)) {
              successful++;
            }
          }
        } else if (transferable.isDataFlavorSupported(uriListFlavor)) {
          // Some platforms (Mac OS X and Linux, when this began) preferred
          // this method of moving files.
          type = "file";
          String data = (String)transferable.getTransferData(uriListFlavor);
          String[] pieces = PApplet.splitTokens(data, "\r\n");
          for (int i = 0; i < pieces.length; i++) {
            if (pieces[i].startsWith("#")) continue;

            String path = null;
            if (pieces[i].startsWith("file:///")) {
              path = pieces[i].substring(7);
            } else if (pieces[i].startsWith("file:/")) {
              path = pieces[i].substring(5);
            }
            if (sketch.addFile(new File(path))) {
              successful++;
            }
          }
        } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          type = "string";
          // support drag and drop of plain text to and from external applications
//          System.out.println("transfer: string data flavor supported");
          String str = (String) transferable.getTransferData(DataFlavor.stringFlavor);
          textarea.setSelectedText(str);
          //TODO look at jEdit's current version with addDropTargetListener()
          sketch.setModified(true);
          successful++;
        }
      } catch (Exception e) {
        e.printStackTrace();
        return false;
      }

      // print out appropriate transfer failure statements
      if (type.equals("file")) 
      {
        if (successful == 0) {
          statusError("No files were added to the sketch.");
        } else if (successful == 1) {
          statusNotice("One file added to the sketch.");
        } else {
          statusNotice(successful+ " files added to the sketch.");
        }
      }
      else if (type.equals("string")) 
      {
        if (successful != 1)
        {
          statusNotice("Invalid string data.");
        }
      }
      return true;
    }
    
    //Create a Transferable implementation that contains the
    //selected text.
    protected Transferable createTransferable(JComponent src) {
      if (src instanceof JEditTextArea)
      {
        System.out.println("transfer.createTransferable: source is instance of JEditTextArea");
        JEditTextArea source = (JEditTextArea) src;
        String data = "";
        if (source.getSelectionStart() == source.getSelectionStop()) {
            return null;
        }
        try {
          data = source.getSelectedText();
        } catch (Exception e) {
          System.out.println("Can't create position - unable to remove text from source.");
        }
        return new StringSelection(data);
      }
      else
      {
        return null;
      }
    }
    
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }
    
    //Remove the old text if the action is a MOVE.
    //However, we do not allow dropping on top of the selected text,
    //so in that case do nothing.
    protected void exportDone(JComponent src, Transferable data, int action) {
      if (src instanceof JEditTextArea)
      {
        System.out.println("transfer.export: source is instance of JEditTextArea");
        JEditTextArea source = (JEditTextArea) src;
        if (action == MOVE) {
          textarea.setSelectedText("");
          sketch.setModified(true);
        }
      }
      System.out.println("transfer: export done");
    }
  }


  protected void setPlacement(int[] location) {
    setBounds(location[0], location[1], location[2], location[3]);
    if (location[4] != 0) {
      editConsoleSplitPane.setDividerLocation(location[4]);
    }
  }


  protected int[] getPlacement() {
    int[] location = new int[5];

    // Get the dimensions of the Frame
    Rectangle bounds = getBounds();
    location[0] = bounds.x;
    location[1] = bounds.y;
    location[2] = bounds.width;
    location[3] = bounds.height;

    // Get the current placement of the divider
    location[4] = editConsoleSplitPane.getDividerLocation();

    return location;
  }


  /**
   * Hack for #@#)$(* Mac OS X 10.2.
   * <p/>
   * This appears to only be required on OS X 10.2, and is not
   * even being called on later versions of OS X or Windows.
   */
//  public Dimension getMinimumSize() {
//    //System.out.println("getting minimum size");
//    return new Dimension(500, 550);
//  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Read and apply new values from the preferences, either because
   * the app is just starting up, or the user just finished messing
   * with things in the Preferences window.
   */
  protected void applyPreferences() {

    // apply the setting for 'use external editor'
    boolean external = Preferences.getBoolean("editor.external");

    textarea.setEditable(!external);
    saveMenuItem.setEnabled(!external);
    saveAsMenuItem.setEnabled(!external);

    TextAreaPainter painter = textarea.getPainter();
    if (external) {
      // disable line highlight and turn off the caret when disabling
      Color color = Theme.getColor("editor.external.bgcolor");
      painter.setBackground(color);
      painter.setLineHighlightEnabled(false);
      textarea.setCaretVisible(false);

    } else {
      Color color = Theme.getColor("editor.bgcolor");
      painter.setBackground(color);
      boolean highlight = Preferences.getBoolean("editor.linehighlight");
      painter.setLineHighlightEnabled(highlight);
      textarea.setCaretVisible(true);
    }

    // apply changes to the font size for the editor
    //TextAreaPainter painter = textarea.getPainter();
    painter.setFont(Preferences.getFont("editor.font"));
    //Font font = painter.getFont();
    //textarea.getPainter().setFont(new Font("Courier", Font.PLAIN, 36));

    // in case tab expansion stuff has changed
    listener.applyPreferences();

    // in case moved to a new location
    // For 0125, changing to async version (to be implemented later)
    //sketchbook.rebuildMenus();
    // For 0126, moved into Base, which will notify all editors.
    //base.rebuildMenusAsync();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  protected void buildMenuBar() {
    JMenuBar menubar = new JMenuBar();
    menubar = new JMenuBar();
    menubar.add(buildFileMenu());
    menubar.add(buildEditMenu());
    menubar.add(buildSketchMenu());
    menubar.add(buildToolsMenu());
    menubar.add(buildHelpMenu());
    setJMenuBar(menubar);
  }


  protected JMenu buildFileMenu() {
    JMenuItem item;
    fileMenu = new JMenu("File");

    item = newJMenuItem("New", 'N');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleNew();
        }
      });
    fileMenu.add(item);

    item = Editor.newJMenuItem("Open...", 'O');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleOpenPrompt();
        }
      });
    fileMenu.add(item);

    if (sketchbookMenu == null) {
      sketchbookMenu = new JMenu("Sketchbook");
      base.rebuildSketchbookMenu(sketchbookMenu);
    }
    fileMenu.add(sketchbookMenu);

    if (examplesMenu == null) {
      examplesMenu = new JMenu("Examples");
      base.rebuildExamplesMenu(examplesMenu);
    }
    fileMenu.add(examplesMenu);

    item = Editor.newJMenuItem("Close", 'W');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          base.handleClose(Editor.this);
        }
      });
    fileMenu.add(item);

    saveMenuItem = newJMenuItem("Save", 'S');
    saveMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSave(false);
        }
      });
    fileMenu.add(saveMenuItem);

    saveAsMenuItem = newJMenuItemShift("Save As...", 'S');
    saveAsMenuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSaveAs();
        }
      });
    fileMenu.add(saveAsMenuItem);

    item = newJMenuItem("Export", 'E');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleExport();
        }
      });
    fileMenu.add(item);

    exportAppItem = newJMenuItemShift("Export Application", 'E');
    exportAppItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //buttons.activate(EditorButtons.EXPORT);
          //SwingUtilities.invokeLater(new Runnable() {
          //public void run() {
          handleExportApplication();
          //}});
        }
      });
    fileMenu.add(exportAppItem);

    fileMenu.addSeparator();

    item = newJMenuItemShift("Page Setup", 'P');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePageSetup();
        }
      });
    fileMenu.add(item);

    item = newJMenuItem("Print", 'P');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handlePrint();
        }
      });
    fileMenu.add(item);

    // macosx already has its own preferences and quit menu
    if (!Base.isMacOS()) {
      fileMenu.addSeparator();

      item = newJMenuItem("Preferences", ',');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            base.handlePrefs();
          }
        });
      fileMenu.add(item);

      fileMenu.addSeparator();

      item = newJMenuItem("Quit", 'Q');
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            base.handleQuit();
          }
        });
      fileMenu.add(item);
    }
    return fileMenu;
  }


  protected JMenu buildSketchMenu() {
    JMenuItem item;
    sketchMenu = new JMenu("Sketch");

    item = newJMenuItem("Run", 'R');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun(false);
        }
      });
    sketchMenu.add(item);

    item = newJMenuItemShift("Present", 'R');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleRun(true);
        }
      });
    sketchMenu.add(item);

    item = new JMenuItem("Stop");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleStop();
        }
      });
    sketchMenu.add(item);

    sketchMenu.addSeparator();

    if (importMenu == null) {
      importMenu = new JMenu("Import Library...");
      base.rebuildImportMenu(importMenu);
    }
    sketchMenu.add(importMenu);

    item = newJMenuItem("Show Sketch Folder", 'K');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openFolder(sketch.getFolder());
        }
      });
    sketchMenu.add(item);
    item.setEnabled(Base.openFolderAvailable());

    item = new JMenuItem("Add File...");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          sketch.handleAddFile();
        }
      });
    sketchMenu.add(item);

    return sketchMenu;
  }


  protected JMenu buildToolsMenu() {
    JMenu menu = new JMenu("Tools");

    addInternalTools(menu);
    addTools(menu, Base.getToolsFolder());
    File sketchbookTools = new File(Base.getSketchbookFolder(), "tools");
    addTools(menu, sketchbookTools);

    return menu;
  }


  protected void addTools(JMenu menu, File sourceFolder) {
    HashMap<String, JMenuItem> toolItems = new HashMap<String, JMenuItem>();

    File[] folders = sourceFolder.listFiles(new FileFilter() {
      public boolean accept(File folder) {
        if (folder.isDirectory()) {
          //System.out.println("checking " + folder);
          File subfolder = new File(folder, "tool");
          return subfolder.exists();
        }
        return false;
      }
    });

    if (folders == null || folders.length == 0) {
      return;
    }

    for (int i = 0; i < folders.length; i++) {
      File toolDirectory = new File(folders[i], "tool");

      try {
        // add dir to classpath for .classes
        //urlList.add(toolDirectory.toURL());

        // add .jar files to classpath
        File[] archives = toolDirectory.listFiles(new FilenameFilter() {
          public boolean accept(File dir, String name) {
            return (name.toLowerCase().endsWith(".jar") ||
                    name.toLowerCase().endsWith(".zip"));
          }
        });

        URL[] urlList = new URL[archives.length];
        for (int j = 0; j < urlList.length; j++) {
          urlList[j] = archives[j].toURI().toURL();
        }
        URLClassLoader loader = new URLClassLoader(urlList);

        String className = null;
        for (int j = 0; j < archives.length; j++) {
          className = findClassInZipFile(folders[i].getName(), archives[j]);
          if (className != null) break;
        }

        /*
        // Alternatively, could use manifest files with special attributes:
        // http://java.sun.com/j2se/1.3/docs/guide/jar/jar.html
        // Example code for loading from a manifest file:
        // http://forums.sun.com/thread.jspa?messageID=3791501
        File infoFile = new File(toolDirectory, "tool.txt");
        if (!infoFile.exists()) continue;

        String[] info = PApplet.loadStrings(infoFile);
        //Main-Class: org.poo.shoe.AwesomerTool
        //String className = folders[i].getName();
        String className = null;
        for (int k = 0; k < info.length; k++) {
          if (info[k].startsWith(";")) continue;

          String[] pieces = PApplet.splitTokens(info[k], ": ");
          if (pieces.length == 2) {
            if (pieces[0].equals("Main-Class")) {
              className = pieces[1];
            }
          }
        }
        */
        // If no class name found, just move on.
        if (className == null) continue;

        Class<?> toolClass = Class.forName(className, true, loader);
        final Tool tool = (Tool) toolClass.newInstance();

        tool.init(Editor.this);

        String title = tool.getMenuTitle();
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            SwingUtilities.invokeLater(tool);
            //new Thread(tool).start();
          }
        });
        //menu.add(item);
        toolItems.put(title, item);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    ArrayList<String> toolList = new ArrayList<String>(toolItems.keySet());
    if (toolList.size() == 0) return;

    menu.addSeparator();
    Collections.sort(toolList);
    for (String title : toolList) {
      menu.add((JMenuItem) toolItems.get(title));
    }
  }


  protected String findClassInZipFile(String base, File file) {
    // Class file to search for
    String classFileName = "/" + base + ".class";

    try {
      ZipFile zipFile = new ZipFile(file);
      Enumeration<?> entries = zipFile.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = (ZipEntry) entries.nextElement();

        if (!entry.isDirectory()) {
          String name = entry.getName();
          //System.out.println("entry: " + name);

          if (name.endsWith(classFileName)) {
            //int slash = name.lastIndexOf('/');
            //String packageName = (slash == -1) ? "" : name.substring(0, slash);
            // Remove .class and convert slashes to periods.
            return name.substring(0, name.length() - 6).replace('/', '.');
          }
        }
      }
    } catch (IOException e) {
      //System.err.println("Ignoring " + filename + " (" + e.getMessage() + ")");
      e.printStackTrace();
    }
    return null;
  }


  protected JMenuItem createToolMenuItem(String className) {
    try {
      Class<?> toolClass = Class.forName(className);
      final Tool tool = (Tool) toolClass.newInstance();

      JMenuItem item = new JMenuItem(tool.getMenuTitle());

      tool.init(Editor.this);

      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          SwingUtilities.invokeLater(tool);
        }
      });
      return item;

    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }


  protected JMenu addInternalTools(JMenu menu) {
    JMenuItem item;

    item = createToolMenuItem("processing.app.tools.AutoFormat");
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    item.setAccelerator(KeyStroke.getKeyStroke('T', modifiers));
    menu.add(item);

    menu.add(createToolMenuItem("processing.app.tools.CreateFont"));
    menu.add(createToolMenuItem("processing.app.tools.ColorSelector"));
    menu.add(createToolMenuItem("processing.app.tools.Archiver"));
    menu.add(createToolMenuItem("processing.app.tools.FixEncoding"));

    // These are temporary entries while Android mode is being worked out.
    // The mode will not be in the tools menu, and won't involve a cmd-key
    item = createToolMenuItem("processing.app.tools.android.Android");
    item.setAccelerator(KeyStroke.getKeyStroke('D', modifiers));
    menu.add(item);
    menu.add(createToolMenuItem("processing.app.tools.android.Reset"));

    return menu;
  }


  protected JMenu buildHelpMenu() {
    // To deal with a Mac OS X 10.5 bug, add an extra space after the name
    // so that the OS doesn't try to insert its slow help menu.
    JMenu menu = new JMenu("Help ");
    JMenuItem item;

    /*
    // testing internal web server to serve up docs from a zip file
    item = new JMenuItem("Web Server Test");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //WebServer ws = new WebServer();
          SwingUtilities.invokeLater(new Runnable() {
            public void run() {
              try {
                int port = WebServer.launch("/Users/fry/coconut/processing/build/shared/reference.zip");
                Base.openURL("http://127.0.0.1:" + port + "/reference/setup_.html");

              } catch (IOException e1) {
                e1.printStackTrace();
              }
            }
          });
        }
      });
    menu.add(item);
    */

    /*
    item = new JMenuItem("Browser Test");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          //Base.openURL("http://processing.org/learning/gettingstarted/");
          //JFrame browserFrame = new JFrame("Browser");
          BrowserStartup bs = new BrowserStartup("jar:file:/Users/fry/coconut/processing/build/shared/reference.zip!/reference/setup_.html");
          bs.initUI();
          bs.launch();
        }
      });
    menu.add(item);
    */

    item = new JMenuItem("Getting Started");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processing.org/learning/gettingstarted/");
        }
      });
    menu.add(item);

    item = new JMenuItem("Environment");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.showEnvironment();
        }
      });
    menu.add(item);

    item = new JMenuItem("Troubleshooting");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.showTroubleshooting();
        }
      });
    menu.add(item);

    item = new JMenuItem("Reference");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.showReference();
        }
      });
    menu.add(item);

    item = newJMenuItemShift("Find in Reference", 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (textarea.isSelectionActive()) {
            handleFindReference();
          }
        }
      });
    menu.add(item);

    item = new JMenuItem("Frequently Asked Questions");
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processing.org/faq.html");
        }
      });
    menu.add(item);

    item = newJMenuItem("Visit Processing.org", '5');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          Base.openURL("http://processing.org/");
        }
      });
    menu.add(item);

    // macosx already has its own about menu
    if (!Base.isMacOS()) {
      menu.addSeparator();
      item = new JMenuItem("About Processing");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            base.handleAbout();
          }
        });
      menu.add(item);
    }

    return menu;
  }

  protected JMenu buildEditMenu() {
    JMenu menu = new JMenu("Edit");
    JMenuItem item;

    undoItem = newJMenuItem("Undo", 'Z');
    undoItem.addActionListener(undoAction = new UndoAction());
    menu.add(undoItem);

    redoItem = newJMenuItem("Redo", 'Y');
    redoItem.addActionListener(redoAction = new RedoAction());
    menu.add(redoItem);

    menu.addSeparator();

    // TODO "cut" and "copy" should really only be enabled
    // if some text is currently selected
    item = newJMenuItem("Cut", 'X');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() instanceof JEditTextArea)
            ((JEditTextArea) getFocusOwner()).cut(); //this works for code windows
          else if (getFocusOwner() instanceof mxGraphComponent)
            TransferHandler.getCutAction().actionPerformed(new ActionEvent(getFocusOwner(), e.getID(), e.getActionCommand()));
        }
      });
    menu.add(item);

    item = newJMenuItem("Copy", 'C');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() instanceof JEditTextArea)
            ((JEditTextArea) getFocusOwner()).copy(); //this works for code windows
          else if (getFocusOwner() instanceof mxGraphComponent)
            TransferHandler.getCutAction().actionPerformed(new ActionEvent(getFocusOwner(), e.getID(), e.getActionCommand()));
        }
      });
    menu.add(item);

    //TODO disable this for code windows
    item = newJMenuItemShift("Copy for Discourse", 'C');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
//          SwingUtilities.invokeLater(new Runnable() {
//              public void run() {
          new DiscourseFormat(Editor.this).show();
//              }
//            });
        }
      });
    menu.add(item);

    item = newJMenuItem("Paste", 'V');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() instanceof JEditTextArea)
            ((JEditTextArea) getFocusOwner()).paste(); //this works for code windows
          else if (getFocusOwner() instanceof mxGraphComponent)
            TransferHandler.getCutAction().actionPerformed(new ActionEvent(getFocusOwner(), e.getID(), e.getActionCommand()));
        }
      });
    menu.add(item);

    item = newJMenuItem("Select All", 'A');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() instanceof JEditTextArea)
            ((JEditTextArea) getFocusOwner()).selectAll(); //this works for code windows
          else if (getFocusOwner() instanceof mxGraphComponent)
            mxGraphActions.getSelectAllAction().actionPerformed(new ActionEvent(getFocusOwner(), e.getID(), e.getActionCommand()));
        }
      });
    menu.add(item);

    menu.addSeparator();

    //TODO comment/uncomment etc. should work for code windows as well; alas if only it was so easy
    item = newJMenuItem("Comment/Uncomment", '/');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() == textarea)
            handleCommentUncomment();
        }
    });
    menu.add(item);

    item = newJMenuItem("Increase Indent", ']');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() == textarea)
            handleIndentOutdent(true);
        }
    });
    menu.add(item);

    item = newJMenuItem("Decrease Indent", '[');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getFocusOwner() == textarea)
            handleIndentOutdent(false);
        }
    });
    menu.add(item);

    menu.addSeparator();
    
    item = newJMenuItem("Open Code Window", 'T');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawarea.showCodeWindowOnSelected();
        }
    });
    menu.add(item);
    
    item = newJMenuItemShift("Close All Code Windows", 'T');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawarea.hideAllCodeWindows();
        }
    });
    menu.add(item);
    
    item = newJMenuItem("Zoom In", '=');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)
      {
        drawarea.getGraphComponent().zoomIn();
      }
    });
    menu.add(item);
    
    item = newJMenuItem("Zoom Out", '-');
    item.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e)
      {
        drawarea.getGraphComponent().zoomOut();
      }
    });
    menu.add(item);
    
    menu.addSeparator();
    
    item = newJMenuItem("Find...", 'F');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (find == null) {
            find = new FindReplace(Editor.this);
          }
          //new FindReplace(Editor.this).show();
          find.setVisible(true);
          //find.setVisible(true);
        }
      });
    menu.add(item);

    // TODO find next should only be enabled after a
    // search has actually taken place
    item = newJMenuItem("Find Next", 'G');
    item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (find != null) {
            //find.find(true);
            //FindReplace find = new FindReplace(Editor.this); //.show();
            find.find(true);
          }
        }
      });
    menu.add(item);

    return menu;
  }


  /**
   * A software engineer, somewhere, needs to have his abstraction
   * taken away. In some countries they jail or beat people for writing
   * the sort of API that would require a five line helper function
   * just to set the command key for a menu item.
   */
  static public JMenuItem newJMenuItem(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Like newJMenuItem() but adds shift as a modifier for the key command.
   */
  static public JMenuItem newJMenuItemShift(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    modifiers |= ActionEvent.SHIFT_MASK;
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    return menuItem;
  }


  /**
   * Same as newJMenuItem(), but adds the ALT (on Linux and Windows)
   * or OPTION (on Mac OS X) key as a modifier.
   */
  static public JMenuItem newJMenuItemAlt(String title, int what) {
    JMenuItem menuItem = new JMenuItem(title);
    //int modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    //menuItem.setAccelerator(KeyStroke.getKeyStroke(what, modifiers));
    menuItem.setAccelerator(KeyStroke.getKeyStroke(what, SHORTCUT_ALT_KEY_MASK));
    return menuItem;
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  class UndoAction extends AbstractAction {
    public UndoAction() {
      super("Undo");
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undoManager.undo();
      } catch (CannotUndoException ex) {
        //System.out.println("Unable to undo: " + ex);
        //ex.printStackTrace();
      }
      updateUndoState();
      redoAction.updateRedoState();
    }

    protected void updateUndoState() {
      if (undoManager.canUndo()) {
        this.setEnabled(true);
        undoItem.setEnabled(true);
        undoItem.setText(undoManager.getUndoPresentationName());
        putValue(Action.NAME, undoManager.getUndoPresentationName());
        if (sketch != null) {
          sketch.setModified(true);  // 0107
        }
      } else {
        this.setEnabled(false);
        undoItem.setEnabled(false);
        undoItem.setText("Undo");
        putValue(Action.NAME, "Undo");
        if (sketch != null) {
          sketch.setModified(false);  // 0107
        }
      }
    }
  }


  class RedoAction extends AbstractAction {
    public RedoAction() {
      super("Redo");
      this.setEnabled(false);
    }

    public void actionPerformed(ActionEvent e) {
      try {
        undoManager.redo();
      } catch (CannotRedoException ex) {
        //System.out.println("Unable to redo: " + ex);
        //ex.printStackTrace();
      }
      updateRedoState();
      undoAction.updateUndoState();
    }

    protected void updateRedoState() {
      if (undoManager.canRedo()) {
        redoItem.setEnabled(true);
        redoItem.setText(undoManager.getRedoPresentationName());
        putValue(Action.NAME, undoManager.getRedoPresentationName());
      } else {
        this.setEnabled(false);
        redoItem.setEnabled(false);
        redoItem.setText("Redo");
        putValue(Action.NAME, "Redo");
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  // these will be done in a more generic way soon, more like:
  // setHandler("action name", Runnable);
  // but for the time being, working out the kinks of how many things to
  // abstract from the editor in this fashion.


  public void setHandlers(Runnable runHandler, Runnable presentHandler,
                          Runnable stopHandler,
                          Runnable exportHandler, Runnable exportAppHandler) {
    this.runHandler = runHandler;
    this.presentHandler = presentHandler;
    this.stopHandler = stopHandler;
    this.exportHandler = exportHandler;
    this.exportAppHandler = exportAppHandler;
  }


  public void resetHandlers() {
    runHandler = new DefaultRunHandler();
    presentHandler = new DefaultPresentHandler();
    stopHandler = new DefaultStopHandler();
    exportHandler = new DefaultExportHandler();
    exportAppHandler = new DefaultExportAppHandler();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Gets the current sketch object.
   */
  public Sketch getSketch() {
    return sketch;
  }


  /**
   * Get the JEditTextArea object for use (not recommended). This should only
   * be used in obscure cases that really need to hack the internals of the
   * JEditTextArea. Most tools should only interface via the get/set functions
   * found in this class. This will maintain compatibility with future releases,
   * which will not use JEditTextArea.
   */
  public JEditTextArea getTextArea() {
    return textarea;
  }


  /**
   * Get the contents of the current buffer. Used by the Sketch class.
   */
  public String getText() {
    return textarea.getText();
  }


  /**
   * Get a range of text from the current buffer.
   */
  public String getText(int start, int stop) {
    return textarea.getText(start, stop - start);
  }


  /**
   * Replace the entire contents of the front-most tab.
   */
  public void setText(String what) {
    startCompoundEdit();
    textarea.setText(what);
    stopCompoundEdit();
  }


  public void insertText(String what) {
    startCompoundEdit();
    int caret = getCaretOffset();
    setSelection(caret, caret);
    textarea.setSelectedText(what);
    stopCompoundEdit();
  }


  /**
   * Called to update the text but not switch to a different set of code
   * (which would affect the undo manager).
   */
//  public void setText2(String what, int start, int stop) {
//    beginCompoundEdit();
//    textarea.setText(what);
//    endCompoundEdit();
//
//    // make sure that a tool isn't asking for a bad location
//    start = Math.max(0, Math.min(start, textarea.getDocumentLength()));
//    stop = Math.max(0, Math.min(start, textarea.getDocumentLength()));
//    textarea.select(start, stop);
//
//    textarea.requestFocus();  // get the caret blinking
//  }


  public String getSelectedText() {
    return textarea.getSelectedText();
  }


  public void setSelectedText(String what) {
    textarea.setSelectedText(what);
  }


  public void setSelection(int start, int stop) {
    // make sure that a tool isn't asking for a bad location
    start = PApplet.constrain(start, 0, textarea.getDocumentLength());
    stop = PApplet.constrain(stop, 0, textarea.getDocumentLength());

    textarea.select(start, stop);
  }


  /**
   * Get the position (character offset) of the caret. With text selected,
   * this will be the last character actually selected, no matter the direction
   * of the selection. That is, if the user clicks and drags to select lines
   * 7 up to 4, then the caret position will be somewhere on line four.
   */
  public int getCaretOffset() {
    return textarea.getCaretPosition();
  }


  /**
   * True if some text is currently selected.
   */
  public boolean isSelectionActive() {
    return textarea.isSelectionActive();
  }


  /**
   * Get the beginning point of the current selection.
   */
  public int getSelectionStart() {
    return textarea.getSelectionStart();
  }


  /**
   * Get the end point of the current selection.
   */
  public int getSelectionStop() {
    return textarea.getSelectionStop();
  }


  /**
   * Get text for a specified line.
   */
  public String getLineText(int line) {
    return textarea.getLineText(line);
  }


  /**
   * Replace the text on a specified line.
   */
  public void setLineText(int line, String what) {
    startCompoundEdit();
    textarea.select(getLineStartOffset(line), getLineStopOffset(line));
    textarea.setSelectedText(what);
    stopCompoundEdit();
  }


  /**
   * Get character offset for the start of a given line of text.
   */
  public int getLineStartOffset(int line) {
    return textarea.getLineStartOffset(line);
  }


  /**
   * Get character offset for end of a given line of text.
   */
  public int getLineStopOffset(int line) {
    return textarea.getLineStopOffset(line);
  }


  /**
   * Get the number of lines in the currently displayed buffer.
   */
  public int getLineCount() {
    return textarea.getLineCount();
  }


  /**
   * Use before a manipulating text to group editing operations together as a
   * single undo. Use stopCompoundEdit() once finished.
   */
  public void startCompoundEdit() {
    compoundEdit = new CompoundEdit();
  }


  /**
   * Use with startCompoundEdit() to group edit operations in a single undo.
   */
  public void stopCompoundEdit() {
    compoundEdit.end();
    undoManager.undoableEditHappened(compoundEdit);
    undoAction.updateUndoState();
    redoAction.updateRedoState();
    compoundEdit = null;
  }


  public int getScrollPosition() {
    return textarea.getScrollPosition();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Switch between tabs, this swaps out the Document object
   * that's currently being manipulated.
   */
  protected void setCode(SketchCode code) {
    SyntaxDocument document = (SyntaxDocument) code.getDocument();

    if (document == null) {  // this document not yet inited
      document = new SyntaxDocument();
      code.setDocument(document);

      // turn on syntax highlighting
      document.setTokenMarker(pdeTokenMarker);

      // insert the program text into the document object
      try {
        document.insertString(0, code.getProgram(), null);
      } catch (BadLocationException bl) {
        bl.printStackTrace();
      }

      // set up this guy's own undo manager
//      code.undo = new UndoManager();

      // connect the undo listener to the editor
      document.addUndoableEditListener(new UndoableEditListener() {
          public void undoableEditHappened(UndoableEditEvent e) {
            if (compoundEdit != null) {
              compoundEdit.addEdit(e.getEdit());

            } else if (undoManager != null) {
              undoManager.undoableEditHappened(e.getEdit());
              undoAction.updateUndoState();
              redoAction.updateRedoState();
            }
          }
        });
    }

    // update the document object that's in use
    textarea.setDocument(document,
                         code.getSelectionStart(), code.getSelectionStop(),
                         code.getScrollPosition());

    textarea.requestFocus();  // get the caret blinking

    // P5's original setup is that each code keeps a separate undo manager;
    // however, since the drawarea potentially maintains connections to each
    // code, we have to keep just one centralized undoManager for Kaleido
//    this.undoManager = code.getUndo();
    undoAction.updateUndoState();
    redoAction.updateRedoState();
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Implements Edit &rarr; Cut.
   */
  public void handleCut() {
    textarea.cut();
    sketch.setModified(true);
  }


  /**
   * Implements Edit &rarr; Copy.
   */
  public void handleCopy() {
    textarea.copy();
  }


  protected void handleDiscourseCopy() {
    new DiscourseFormat(Editor.this).show();
  }


  /**
   * Implements Edit &rarr; Paste.
   */
  public void handlePaste() {
    textarea.paste();
    sketch.setModified(true);
  }


  /**
   * Implements Edit &rarr; Select All.
   */
  public void handleSelectAll() {
    textarea.selectAll();
  }


  protected void handleCommentUncomment() {
    startCompoundEdit();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    int lastLineStart = textarea.getLineStartOffset(stopLine);
    int selectionStop = textarea.getSelectionStop();
    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (textarea.isSelectionActive()) {
        stopLine--;
      }
    }

    // If the text is empty, ignore the user.
    // Also ensure that all lines are commented (not just the first)
    // when determining whether to comment or uncomment.
    int length = textarea.getDocumentLength();
    boolean commented = true;
    for (int i = startLine; commented && (i <= stopLine); i++) {
      int pos = textarea.getLineStartOffset(i);
      if (pos + 2 > length) {
        commented = false;
      } else {
        // Check the first two characters to see if it's already a comment.
        String begin = textarea.getText(pos, 2);
        //System.out.println("begin is '" + begin + "'");
        commented = begin.equals("//");
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartOffset(line);
      if (commented) {
        // remove a comment
        textarea.select(location, location+2);
        if (textarea.getSelectedText().equals("//")) {
          textarea.setSelectedText("");
        }
      } else {
        // add a comment
        textarea.select(location, location);
        textarea.setSelectedText("//");
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
    stopCompoundEdit();
  }


  protected void handleIndentOutdent(boolean indent) {
    int tabSize = Preferences.getInteger("editor.tabs.size");
    String tabString = Editor.EMPTY.substring(0, tabSize);

    startCompoundEdit();

    int startLine = textarea.getSelectionStartLine();
    int stopLine = textarea.getSelectionStopLine();

    // If the selection ends at the beginning of the last line,
    // then don't (un)comment that line.
    int lastLineStart = textarea.getLineStartOffset(stopLine);
    int selectionStop = textarea.getSelectionStop();
    if (selectionStop == lastLineStart) {
      // Though if there's no selection, don't do that
      if (textarea.isSelectionActive()) {
        stopLine--;
      }
    }

    for (int line = startLine; line <= stopLine; line++) {
      int location = textarea.getLineStartOffset(line);

      if (indent) {
        textarea.select(location, location);
        textarea.setSelectedText(tabString);

      } else {  // outdent
        textarea.select(location, location + tabSize);
        // Don't eat code if it's not indented
        if (textarea.getSelectedText().equals(tabString)) {
          textarea.setSelectedText("");
        }
      }
    }
    // Subtract one from the end, otherwise selects past the current line.
    // (Which causes subsequent calls to keep expanding the selection)
    textarea.select(textarea.getLineStartOffset(startLine),
                    textarea.getLineStopOffset(stopLine) - 1);
    stopCompoundEdit();
  }


  protected void handleFindReference() {
    String text = textarea.getSelectedText().trim();

    if (text.length() == 0) {
      statusNotice("First select a word to find in the reference.");

    } else {
      String referenceFile = PdeKeywords.getReference(text);
      //System.out.println("reference file is " + referenceFile);
      if (referenceFile == null) {
        statusNotice("No reference available for \"" + text + "\"");
      } else {
        Base.showReference(referenceFile + ".html");
      }
    }
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .


  /**
   * Implements Sketch &rarr; Run.
   * @param present Set true to run in full screen (present mode).
   */
  public void handleRun(boolean present) {
    internalCloseRunner();
    running = true;
    toolbar.activate(EditorToolbar.RUN);
    statusEmpty();

    // do this to advance/clear the terminal window / dos prompt / etc
    for (int i = 0; i < 10; i++) System.out.println();

    // clear the console on each run, unless the user doesn't want to
    if (Preferences.getBoolean("console.auto_clear")) {
      console.clear();
    }

    // Cannot use invokeLater() here, otherwise it gets
    // placed on the event thread and causes a hang--bad idea all around.
    if (present) {
      new Thread(presentHandler).start();
    } else {
      new Thread(runHandler).start();
    }
  }


  class DefaultRunHandler implements Runnable {
    public void run() {
      try {
        sketch.prepare();
        String appletClassName = sketch.build();
        if (appletClassName != null) {
          runtime = new Runner(Editor.this, sketch);
          runtime.launch(appletClassName, false);
        }
      } catch (Exception e) {
        statusError(e);
      }
    }
  }


  class DefaultPresentHandler implements Runnable {
    public void run() {
      try {
        sketch.prepare();
        String appletClassName = sketch.build();
        if (appletClassName != null) {
          runtime = new Runner(Editor.this, sketch);
          runtime.launch(appletClassName, true);
        }
      } catch (Exception e) {
        statusError(e);
      }
    }
  }


  class DefaultStopHandler implements Runnable {
    public void run() {
      try {

      } catch (Exception e) {
        statusError(e);
      }
    }
  }


  /**
   * Set the location of the sketch run window. Used by Runner to update the
   * Editor about window drag events while the sketch is running.
   */
  public void setSketchLocation(Point p) {
    sketchWindowLocation = p;
  }


  /**
   * Get the last location of the sketch's run window. Used by Runner to make
   * the window show up in the same location as when it was last closed.
   */
  public Point getSketchLocation() {
    return sketchWindowLocation;
  }


  /**
   * Implements Sketch &rarr; Stop, or pressing Stop on the toolbar.
   */
  public void handleStop() {  // called by menu or buttons
    toolbar.activate(EditorToolbar.STOP);

    internalCloseRunner();

    toolbar.deactivate(EditorToolbar.RUN);
    toolbar.deactivate(EditorToolbar.STOP);

    // focus the PDE again after quitting presentation mode [toxi 030903]
    toFront();
  }


  /**
   * Deactivate the Run button. This is called by Runner to notify that the
   * sketch has stopped running, usually in response to an error (or maybe
   * the sketch completing and exiting?) Tools should not call this function.
   * To initiate a "stop" action, call handleStop() instead.
   */
  public void internalRunnerClosed() {
    running = false;
    toolbar.deactivate(EditorToolbar.RUN);
  }


  /**
   * Handle internal shutdown of the runner.
   */
  public void internalCloseRunner() {
    running = false;

    try {
      if (runtime != null) {
        runtime.close();  // kills the window
        runtime = null; // will this help?
      }
    } catch (Exception e) { }

    sketch.cleanup();
  }


  /**
   * Check if the sketch is modified and ask user to save changes.
   * @return false if canceling the close/quit operation
   */
  protected boolean checkModified() {
    if (!sketch.isModified()) return true;

    // As of Processing 1.0.10, this always happens immediately.
    // http://dev.processing.org/bugs/show_bug.cgi?id=1456

    String prompt = "Save changes to " + sketch.getName() + "?  ";

    if (!Base.isMacOS()) {
      int result =
        JOptionPane.showConfirmDialog(this, prompt, "Close",
                                      JOptionPane.YES_NO_CANCEL_OPTION,
                                      JOptionPane.QUESTION_MESSAGE);

      if (result == JOptionPane.YES_OPTION) {
        return handleSave(true);

      } else if (result == JOptionPane.NO_OPTION) {
        return true;  // ok to continue

      } else if (result == JOptionPane.CANCEL_OPTION) {
        return false;

      } else {
        throw new IllegalStateException();
      }

    } else {
      // This code is disabled unless Java 1.5 is being used on Mac OS X
      // because of a Java bug that prevents the initial value of the
      // dialog from being set properly (at least on my MacBook Pro).
      // The bug causes the "Don't Save" option to be the highlighted,
      // blinking, default. This sucks. But I'll tell you what doesn't
      // suck--workarounds for the Mac and Apple's snobby attitude about it!
      // I think it's nifty that they treat their developers like dirt.

      // Pane formatting adapted from the quaqua guide
      // http://www.randelshofer.ch/quaqua/guide/joptionpane.html
      JOptionPane pane =
        new JOptionPane("<html> " +
                        "<head> <style type=\"text/css\">"+
                        "b { font: 13pt \"Lucida Grande\" }"+
                        "p { font: 11pt \"Lucida Grande\"; margin-top: 8px }"+
                        "</style> </head>" +
                        "<b>Do you want to save changes to this sketch<BR>" +
                        " before closing?</b>" +
                        "<p>If you don't save, your changes will be lost.",
                        JOptionPane.QUESTION_MESSAGE);

      String[] options = new String[] {
        "Save", "Cancel", "Don't Save"
      };
      pane.setOptions(options);

      // highlight the safest option ala apple hig
      pane.setInitialValue(options[0]);

      // on macosx, setting the destructive property places this option
      // away from the others at the lefthand side
      pane.putClientProperty("Quaqua.OptionPane.destructiveOption",
                             new Integer(2));

      JDialog dialog = pane.createDialog(this, null);
      dialog.setVisible(true);

      Object result = pane.getValue();
      if (result == options[0]) {  // save (and close/quit)
        return handleSave(true);

      } else if (result == options[2]) {  // don't save (still close/quit)
        return true;

      } else {  // cancel?
        return false;
      }
    }
  }


  /**
   * Open a sketch from a particular path, but don't check to save changes.
   * Used by Sketch.saveAs() to re-open a sketch after the "Save As"
   */
  protected void handleOpenUnchecked(String path, int codeIndex,
                                     int selStart, int selStop, int scrollPos) {
    internalCloseRunner();
    handleOpenInternal(path);
    // Replacing a document that may be untitled. If this is an actual
    // untitled document, then editor.untitled will be set by Base.
    untitled = false;

    sketch.setCurrentCode(codeIndex);
    textarea.select(selStart, selStop);
    textarea.setScrollPosition(scrollPos);
  }


  /**
   * Second stage of open, occurs after having checked to see if the
   * modifications (if any) to the previous sketch need to be saved.
   */
  protected boolean handleOpenInternal(String path) {
    // check to make sure that this .pde file is
    // in a folder of the same name
    File file = new File(path);
    File parentFile = new File(file.getParent());
    String parentName = parentFile.getName();
    String pdeName = parentName + ".pde";
    File altFile = new File(file.getParent(), pdeName);

    if (pdeName.equals(file.getName())) {
      // no beef with this guy

    } else if (altFile.exists()) {
      // user selected a .java from the same sketch,
      // but open the .pde instead
      path = altFile.getAbsolutePath();
      //System.out.println("found alt file in same folder");

    } else if (!path.endsWith(".pde")) {
      Base.showWarning("Bad file selected",
                       "Processing can only open its own sketches\n" +
                       "and other files ending in .pde", null);
      return false;

    } else {
      String properParent =
        file.getName().substring(0, file.getName().length() - 4);

      Object[] options = { "OK", "Cancel" };
      String prompt =
        "The file \"" + file.getName() + "\" needs to be inside\n" +
        "a sketch folder named \"" + properParent + "\".\n" +
        "Create this folder, move the file, and continue?";

      int result = JOptionPane.showOptionDialog(this,
                                                prompt,
                                                "Moving",
                                                JOptionPane.YES_NO_OPTION,
                                                JOptionPane.QUESTION_MESSAGE,
                                                null,
                                                options,
                                                options[0]);

      if (result == JOptionPane.YES_OPTION) {
        // create properly named folder
        File properFolder = new File(file.getParent(), properParent);
        if (properFolder.exists()) {
          Base.showWarning("Error",
                           "A folder named \"" + properParent + "\" " +
                           "already exists. Can't open sketch.", null);
          return false;
        }
        if (!properFolder.mkdirs()) {
          //throw new IOException("Couldn't create sketch folder");
          Base.showWarning("Error",
                           "Could not create the sketch folder.", null);
          return false;
        }
        // copy the sketch inside
        File properPdeFile = new File(properFolder, file.getName());
        File origPdeFile = new File(path);
        try {
          Base.copyFile(origPdeFile, properPdeFile);
        } catch (IOException e) {
          Base.showWarning("Error", "Could not copy to a proper location.", e);
          return false;
        }

        // remove the original file, so user doesn't get confused
        origPdeFile.delete();

        // update with the new path
        path = properPdeFile.getAbsolutePath();

      } else if (result == JOptionPane.NO_OPTION) {
        return false;
      }
    }

    try {
      sketch = new Sketch(this, path);
      helloWorld();
    } catch (IOException e) {
      Base.showWarning("Error", "Could not create the sketch.", e);
      return false;
    }
    
    // Once sketch is made, if a graph file exists in the sketch folder, try to open it
    if (new File(sketch.getFolder() + "/" + getGraphFileName()).exists()) {
      try {
        String filepath = sketch.getFolder() + "/" + getGraphFileName();
        Document document = mxUtils.parse(mxUtils.readFile(filepath));
        mxCodec codec = new mxCodec(document);
        mxGraph graph = drawarea.getGraphComponent().getGraph();//just a shorthand
        codec.decode(document.getDocumentElement(), graph.getModel());
        
      } catch (IOException e) {
        Base.showWarning("Error", "Could not create the graph.", e);
        // Don't return false in this case, because it's okay to open Processing projects that don't have a graph yet.
      }
    }
    
    // Repaint the UI stuff
    textHeader.rebuild();
    updateTitle();
    // Disable untitled setting from previous document, if any
    untitled = false;

    // Store information on who's open and running
    // (in case there's a crash or something that can't be recovered)
    base.storeSketches();
    Preferences.save();

    // opening was successful
    return true;

//    } catch (Exception e) {
//      e.printStackTrace();
//      statusError(e);
//      return false;
//    }
  }

  /**
   * For development purposes only: inserts initial bunch of text
   * @author achang
   * @deprecated
   */
  private void helloWorld() {
    
    String initialString = "float[] x = new float[2];\n"
    + "float[] y = new float[2];\n"
                           + "float segLength = 50;\n"
                           + "\n"
                           + "void setup() {\n"
                           + "  size(200, 200);\n"
                           + "  smooth(); \n"
                           + "  strokeWeight(20.0);\n"
                           + "  stroke(0, 100);\n"
                           + "}\n"
                           + "\n"
                           + "void draw() {\n"
                           + "  background(226);\n"
                           + "  dragSegment(0, mouseX, mouseY);\n"
                           + "  dragSegment(1, x[0], y[0]);\n"
                           + "}\n"
                           + "\n"
                           + "void dragSegment(int i, float xin, float yin) {\n"
                           + "  float dx = xin - x[i];\n"
                           + "  float dy = yin - y[i];\n"
                           + "  float angle = atan2(dy, dx);  \n"
                           + "  x[i] = xin - cos(angle) * segLength;\n"
                           + "  y[i] = yin - sin(angle) * segLength;\n"
                           + "  segment(x[i], y[i], angle);\n" + "}\n" + "\n"
                           + "void segment(float x, float y, float a) {\n"
                           + "  pushMatrix();\n" + "  translate(x, y);\n"
                           + "  rotate(a);\n" + "  line(0, 0, segLength, 0);\n"
                           + "  popMatrix();\n"
                           + "}\n"
                           + "//aaaaaaaaaaaaaaaa\n"
                           + "//bbbbbbbbbbbbbbbb\n"
                           + "//cccccccccccccccc\n"
                           + "//dddddddddddddddd\n"
                           + "//eeeeeeeeeeeeeeee\n"
                           + "//ffffffffffffffff\n"
                           + "//gggggggggggggggg\n"
                           + "\n"
                           + "//hhhhhhhhhhhhhhhh\n"
                           + "//iiiiiiiiiiiiiiii";
    try {
      getSketch().getCurrentCode().getDocument().insertString(getTextArea().getCaretPosition(), initialString, null);
    } catch (BadLocationException e) {
      e.printStackTrace();
    }
  }

  /**
   * Updates the file name in the title bar.  If file has been modified, show
   * asterisk (and on mac show dot in the window's close button).
   * Sets the title of the window to "sketch_070752a | Processing 0126 | Kaleido 001"
   * @author achang
   */
  public void updateTitle() {
    String name = " | Processing " + Base.VERSION_NAME + " | Kaleido " + kConstants.VERSION_NAME;
    if (drawarea.isModified() || sketch.isModified()) 
    {
      setTitle(sketch.getName() + "*" + name);
      if (Base.isMacOS()) // http://developer.apple.com/qa/qa2001/qa1146.html
        getRootPane().putClientProperty("windowModified", Boolean.TRUE);
    } 
    else
    {
      setTitle(sketch.getName() + name);
      if (Base.isMacOS()) // http://developer.apple.com/qa/qa2001/qa1146.html
        getRootPane().putClientProperty("windowModified", Boolean.FALSE);
    }
  }

  /**
   * Actually handle the save command. If 'immediately' is set to false,
   * this will happen in another thread so that the message area
   * will update and the save button will stay highlighted while the
   * save is happening. If 'immediately' is true, then it will happen
   * immediately. This is used during a quit, because invokeLater()
   * won't run properly while a quit is happening. This fixes
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=276">Bug 276</A>.
   */
  public boolean handleSave(boolean immediately) {
    //stopRunner();
    handleStop();  // 0136

    if (untitled) {
      return handleSaveAs();
      // need to get the name, user might also cancel here

    } else if (immediately) {
      handleSave2();

    } else {
      SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            handleSave2();
          }
        });
    }
    return true;
  }


  protected void handleSave2() {
    toolbar.activate(EditorToolbar.SAVE);
    statusNotice("Saving...");
    try {
      if (sketch.save()) {
        // If sketch saved successfully, then get the pde folderpath and save the graph file inside.
        writeGraphToFile();
        undoManager.resetCounter();
        statusNotice("Done Saving.");
      } else {
        statusEmpty();
      }
      // rebuild sketch menu in case a save-as was forced
      // Disabling this for 0125, instead rebuild the menu inside
      // the Save As method of the Sketch object, since that's the
      // only one who knows whether something was renamed.
      //sketchbook.rebuildMenus();
      //sketchbook.rebuildMenusAsync();

    } catch (Exception e) {
      // show the error as a message in the window
      statusError(e);

      // zero out the current action,
      // so that checkModified2 will just do nothing
      //checkModifiedMode = 0;
      // this is used when another operation calls a save
    }
    //toolbar.clear();
    toolbar.deactivate(EditorToolbar.SAVE);
  }


  public boolean handleSaveAs() {
    //stopRunner();  // formerly from 0135
    handleStop();

    toolbar.activate(EditorToolbar.SAVE);

    //SwingUtilities.invokeLater(new Runnable() {
    //public void run() {
    statusNotice("Saving...");
    try {
      if (sketch.saveAs()) {
        // If sketch saved successfully, then get the pde folderpath and save the graph file inside.
        writeGraphToFile();
        undoManager.resetCounter();
        statusNotice("Done Saving.");
        // Disabling this for 0125, instead rebuild the menu inside
        // the Save As method of the Sketch object, since that's the
        // only one who knows whether something was renamed.
        //sketchbook.rebuildMenusAsync();
      } else {
        statusNotice("Save Canceled.");
        return false;
      }
    } catch (Exception e) {
      // show the error as a message in the window
      statusError(e);

    } finally {
      // make sure the toolbar button deactivates
      toolbar.deactivate(EditorToolbar.SAVE);
    }

    return true;
  }

  
  /**
   * Returns the expected name of the graph file in a Processing project folder
   * @author achang
   */
  private String getGraphFileName()
  {
    return sketch.getName()+"_graph.xml";
  }

  /**
   * Saves the graph as an XML file with the file name specified in
   * getGraphFileName();
   * @author achang
   */
  private void writeGraphToFile() throws IOException {
    String filepath = sketch.getFolder() + "/" + getGraphFileName();
    mxCodec codec = new mxCodec();
    String xml = mxUtils.getXml(codec.encode(drawarea.getGraphComponent().getGraph().getModel()));
    mxUtils.writeFile(xml, filepath);
//    System.out.println("Editor >> wrote graph to file");
  }
  
  
  /**
   * Called by Sketch &rarr; Export.
   * Handles calling the export() function on sketch, and
   * queues all the gui status stuff that comes along with it.
   * <p/>
   * Made synchronized to (hopefully) avoid problems of people
   * hitting export twice, quickly, and horking things up.
   */
  synchronized public void handleExport() {
    if (!handleExportCheckModified()) return;
    toolbar.activate(EditorToolbar.EXPORT);

    new Thread(exportHandler).start();
  }


  class DefaultExportHandler implements Runnable {
    public void run() {
      try {
        boolean success = sketch.exportApplet();
        if (success) {
          File appletFolder = new File(sketch.getFolder(), "applet");
          Base.openFolder(appletFolder);
          statusNotice("Done exporting.");
        } else {
          // error message will already be visible
        }
      } catch (Exception e) {
        statusError(e);
      }
      //toolbar.clear();
      toolbar.deactivate(EditorToolbar.EXPORT);
    }
  }


  /**
   * Handler for Sketch &rarr; Export Application
   */
  synchronized public void handleExportApplication() {
    if (!handleExportCheckModified()) return;
    toolbar.activate(EditorToolbar.EXPORT);

    // previous was using SwingUtilities.invokeLater()
    new Thread(exportAppHandler).start();
  }


  class DefaultExportAppHandler implements Runnable {
    public void run() {
      statusNotice("Exporting application...");
      try {
        if (sketch.exportApplicationPrompt()) {
          Base.openFolder(sketch.getFolder());
          statusNotice("Done exporting.");
        } else {
          // error message will already be visible
          // or there was no error, in which case it was canceled.
        }
      } catch (Exception e) {
        statusNotice("Error during export.");
        e.printStackTrace();
      }
      //toolbar.clear();
      toolbar.deactivate(EditorToolbar.EXPORT);
    }
  }


  /**
   * Checks to see if the sketch has been modified, and if so,
   * asks the user to save the sketch or cancel the export.
   * This prevents issues where an incomplete version of the sketch
   * would be exported, and is a fix for
   * <A HREF="http://dev.processing.org/bugs/show_bug.cgi?id=157">Bug 157</A>
   */
  protected boolean handleExportCheckModified() {
    if (!sketch.isModified()) return true;

    Object[] options = { "OK", "Cancel" };
    int result = JOptionPane.showOptionDialog(this,
                                              "Save changes before export?",
                                              "Save",
                                              JOptionPane.OK_CANCEL_OPTION,
                                              JOptionPane.QUESTION_MESSAGE,
                                              null,
                                              options,
                                              options[0]);

    if (result == JOptionPane.OK_OPTION) {
      handleSave(true);

    } else {
      // why it's not CANCEL_OPTION is beyond me (at least on the mac)
      // but f-- it.. let's get this shite done..
      //} else if (result == JOptionPane.CANCEL_OPTION) {
      statusNotice("Export canceled, changes must first be saved.");
      //toolbar.clear();
      return false;
    }
    return true;
  }


  /**
   * Handler for File &rarr; Page Setup.
   */
  public void handlePageSetup() {
    //printerJob = null;
    if (printerJob == null) {
      printerJob = PrinterJob.getPrinterJob();
    }
    if (pageFormat == null) {
      pageFormat = printerJob.defaultPage();
    }
    pageFormat = printerJob.pageDialog(pageFormat);
    //System.out.println("page format is " + pageFormat);
  }


  /**
   * Handler for File &rarr; Print.
   */
  public void handlePrint() {
    statusNotice("Printing...");
    //printerJob = null;
    if (printerJob == null) {
      printerJob = PrinterJob.getPrinterJob();
    }
    if (pageFormat != null) {
      //System.out.println("setting page format " + pageFormat);
      printerJob.setPrintable(textarea.getPainter(), pageFormat);
    } else {
      printerJob.setPrintable(textarea.getPainter());
    }
    // set the name of the job to the code name
    printerJob.setJobName(sketch.getCurrentCode().getPrettyName());

    if (printerJob.printDialog()) {
      try {
        printerJob.print();
        statusNotice("Done printing.");

      } catch (PrinterException pe) {
        statusError("Error while printing.");
        pe.printStackTrace();
      }
    } else {
      statusNotice("Printing canceled.");
    }
    //printerJob = null;  // clear this out?
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  /*
   * Status bar
   */

  /**
   * Show an error int the status bar.
   */
  public void statusError(String what) {
    status.error(what);
    //new Exception("deactivating RUN").printStackTrace();
    toolbar.deactivate(EditorToolbar.RUN);
  }


  /**
   * Show an exception in the editor status bar.
   */
  public void statusError(Exception e) {
    e.printStackTrace();
//    if (e == null) {
//      System.err.println("Editor.statusError() was passed a null exception.");
//      return;
//    }

    if (e instanceof RunnerException) {
      RunnerException re = (RunnerException) e;
      if (re.hasCodeIndex()) {
        sketch.setCurrentCode(re.getCodeIndex());
      }
      if (re.hasCodeLine()) {
        int line = re.getCodeLine();
        // subtract one from the end so that the \n ain't included
        if (line >= textarea.getLineCount()) {
          // The error is at the end of this current chunk of code,
          // so the last line needs to be selected.
          line = textarea.getLineCount() - 1;
          if (textarea.getLineText(line).length() == 0) {
            // The last line may be zero length, meaning nothing to select.
            // If so, back up one more line.
            line--;
          }
        }
        if (line < 0 || line >= textarea.getLineCount()) {
          System.err.println("Bad error line: " + line);
        } else {
          textarea.select(textarea.getLineStartOffset(line),
                          textarea.getLineStopOffset(line) - 1);
        }
      }
    }

    // Since this will catch all Exception types, spend some time figuring
    // out which kind and try to give a better error message to the user.
    String mess = e.getMessage();
    if (mess != null) {
      String javaLang = "java.lang.";
      if (mess.indexOf(javaLang) == 0) {
        mess = mess.substring(javaLang.length());
      }
      String rxString = "RuntimeException: ";
      if (mess.indexOf(rxString) == 0) {
        mess = mess.substring(rxString.length());
      }
      statusError(mess);
    }
//    e.printStackTrace();
  }


  /**
   * Show a notice message in the editor status bar.
   */
  public void statusNotice(String msg) {
    status.notice(msg);
  }


  /**
   * Clear the status area.
   */
  public void statusEmpty() {
    statusNotice(EMPTY);
  }


  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  /*
   * Popup menus construction
   */

  /**
   * Returns the edit popup menu.
   */
  class TextAreaPopup extends JPopupMenu {
    //String currentDir = System.getProperty("user.dir");
    String referenceFile = null;

    JMenuItem cutItem;
    JMenuItem copyItem;
    JMenuItem discourseItem;
    JMenuItem referenceItem;


    public TextAreaPopup() {
      
//      add(bindToTextComponent("Cut", TransferHandler.getCutAction()));    
//      add(bindToTextComponent("Copy", TransferHandler.getCopyAction()));    
//      add(bindToTextComponent("Paste", TransferHandler.getPasteAction()));      
//      addSeparator();
      
      JMenuItem item;

      cutItem = new JMenuItem("Cut");
      cutItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCut();
          }
      });
      this.add(cutItem);

      copyItem = new JMenuItem("Copy");
      copyItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCopy();
          }
        });
      this.add(copyItem);

      discourseItem = new JMenuItem("Copy for Discourse");
      discourseItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleDiscourseCopy();
          }
        });
      this.add(discourseItem);

      item = new JMenuItem("Paste");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handlePaste();
          }
        });
      this.add(item);

      item = new JMenuItem("Select All");
      item.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          handleSelectAll();
        }
      });
      this.add(item);

      this.addSeparator();

      item = new JMenuItem("Comment/Uncomment");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleCommentUncomment();
          }
      });
      this.add(item);

      item = new JMenuItem("Increase Indent");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleIndentOutdent(true);
          }
      });
      this.add(item);

      item = new JMenuItem("Decrease Indent");
      item.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleIndentOutdent(false);
          }
      });
      this.add(item);

      this.addSeparator();

      referenceItem = new JMenuItem("Find in Reference");
      referenceItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
            handleFindReference();
          }
        });
      this.add(referenceItem);
    }

    // if no text is selected, disable copy and cut menu items
    public void show(Component component, int x, int y) {
      if (textarea.isSelectionActive()) {
        cutItem.setEnabled(true);
        copyItem.setEnabled(true);
        discourseItem.setEnabled(true);

        String sel = textarea.getSelectedText().trim();
        referenceFile = PdeKeywords.getReference(sel);
        referenceItem.setEnabled(referenceFile != null);

      } else {
        cutItem.setEnabled(false);
        copyItem.setEnabled(false);
        discourseItem.setEnabled(false);
        referenceItem.setEnabled(false);
      }
      super.show(component, x, y);
    }
  }  

  
  class DrawAreaPopup extends JPopupMenu {
    
    JMenuItem editItem;
    JMenuItem deleteItem;
    
    JMenuItem openCodeItem;
    JMenuItem openCodesItem;
    JMenuItem closeAllCodesItem;

    
    public DrawAreaPopup() {
           
//      add(bindToGraphComponent("Edit", mxGraphActions.getEditAction()));
//      add(bindToGraphComponent("Delete", mxGraphActions.getDeleteAction()));
      
      editItem = new JMenuItem("Edit");
      editItem.addActionListener(new ActionListener () {
        public void actionPerformed(ActionEvent e)
        {
          drawarea.getGraphComponent().startEditing();
        }
      });
      add(editItem);
      
      deleteItem = new JMenuItem("Delete");
      deleteItem.addActionListener(new ActionListener () {
        public void actionPerformed(ActionEvent e)
        {
          drawarea.getGraphComponent().getGraph().removeCells();
        }
      });
      add(deleteItem);
      
      if (drawarea.isCodeWindowsEnabled())
      {
        openCodeItem = new JMenuItem("Open Code Window");
        openCodeItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)
          {
            drawarea.showCodeWindowOnSelected();
          }
        });
        add(openCodeItem);
        
        openCodesItem = new JMenuItem("Open Code Windows");
        openCodesItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)
          {
            drawarea.showCodeWindowOnSelected();
          }
        });
        add(openCodesItem);
        
        
        closeAllCodesItem = new JMenuItem("Close All Code Windows");
        closeAllCodesItem.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e)
          {
            drawarea.hideAllCodeWindows();
          }
        });
        add(closeAllCodesItem);
      }
      
      addSeparator();
      
      add(bindToGraphComponent("Cut", TransferHandler.getCutAction()));    
      add(bindToGraphComponent("Copy", TransferHandler.getCopyAction()));    
      add(bindToGraphComponent("Paste", TransferHandler.getPasteAction()));    
      add(bindToGraphComponent("Select All", mxGraphActions.getSelectAllAction()));
      
      addSeparator();
           
      JMenuItem menuItem = new JMenuItem("Zoom In");
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          drawarea.getGraphComponent().zoomIn();
        }
      });
      add(menuItem);
      
      menuItem = new JMenuItem("Zoom Out");
      menuItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e)
        {
          drawarea.getGraphComponent().zoomOut();
        }
      });
      add(menuItem);

    }
    
    public void show(Component component, int x, int y) {
      
      int selectionCount = drawarea.getGraphComponent().getGraph().getSelectionCount(); //used again for "make parent/child"
      if (selectionCount < 1)
      {
        editItem.setEnabled(false);
        deleteItem.setEnabled(false);
      }
      else
      {
        editItem.setEnabled(true);
        deleteItem.setEnabled(true);
      }
      
      if (drawarea.isCodeWindowsEnabled())
      {
        closeAllCodesItem.setEnabled(drawarea.isCodeWindowOpenOnAny());

        // note: code window actions are ignored for objects without valid code marks
        if (drawarea.getGraphComponent().getGraph().getSelectionCount() == 1)
        {
          openCodeItem.setEnabled(drawarea.isCodeWindowValidOnSelected());
          openCodeItem.setVisible(true);
          openCodesItem.setVisible(false);
        }
        else if (drawarea.getGraphComponent().getGraph().getSelectionCount() > 1)
        {
          openCodesItem.setEnabled(drawarea.isCodeWindowValidOnSelected());
          openCodeItem.setVisible(false);
          openCodesItem.setVisible(true);
        }
        else
        { //selectionCount equals zero
          openCodeItem.setVisible(true);
          openCodesItem.setVisible(false);
          openCodeItem.setEnabled(false);
        }
      }
      
      super.show(component, x, y);
    }
  }
 
  /**
   * Binds actions to the graphComponent (used to bind abstracted Transfer Actions).
   * Implementation modified from mxgraph example editor.
   */
  public Action bindToGraphComponent(String name, final Action action)
  {
    return new AbstractAction(name)
    {
      public void actionPerformed(ActionEvent e)
      {
        action.actionPerformed(new ActionEvent(drawarea.getGraphComponent(), e.getID(), e.getActionCommand()));
      }
    };
  }
  /**
   * Binds actions to the text area (used to bind abstracted Transfer Actions).
   * Implementation modified from mxgraph example editor.
   */
  public Action bindToTextComponent(String name, final Action action)
  {
    return new AbstractAction(name)
    {
      public void actionPerformed(ActionEvent e)
      {
        action.actionPerformed(new ActionEvent(textarea, e.getID(), e.getActionCommand()));
      }
    };
  }

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  /*
   * Focus handling
   */
  
  /**
   * Receives focus events so we can do repaints (otherwise events like
   * mxEvent.CHANGE fires mid-process, before the focus actually changes, and
   * so some states won't been completely processed)
   */
  class FocusHandler implements FocusListener
  {
    public void focusGained(FocusEvent e) {
   
//      System.out.println("Focus Gained >> source= " + e.getSource().getClass().getName());
      
      if (drawingHeader.getLinkButton().isLinkActiveMode()) {
//        System.out.println("selectionSync >> we're in linking mode so not sync-ing");
      } else
        if (e.getSource() instanceof JEditTextArea)
        {
            syncGraphSelectionToText();
            
            // if was in tool mode (but not link mode), end it
            if (drawarea.getToolMode() != null)
              drawarea.endToolMode(false);
        }
        else if (e.getSource() instanceof kGraphComponent)
        {
          // redraw the cell handles to reflect the focused state since it also
          // listens for mxEvent.CHANGE so it paints before focus is gained
          ((kGraphComponent) e.getSource()).getSubHandler().refresh();
          syncTextSelectionToGraph();
          drawingHeader.updateGraphButtons();
        }
      
      updateLinkButton(); // call this every time there's a focus swap
    }
    
    public void focusLost(FocusEvent e) {
          
//      System.out.println("Focus Lost >> source= " + e.getSource().getClass().getName());
      
      if (e.getSource() instanceof JEditTextArea)
      {
        // don't need to do anything?
      }
      else if (e.getSource() instanceof kGraphComponent)
      {
        // redraw the cell handles to reflect the focused state since it also
        // listens for mxEvent.CHANGE so it paints before focus is gained
        ((kGraphComponent) e.getSource()).getSubHandler().refresh();
        drawingHeader.updateGraphButtons();
      }
    }
  }
  

  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  /*
   * Selection sync
   */
  
  /**
   * Declares and installs graph listeners for synchronizing selection with
   * textarea
   */
  public void installSelectionSyncListeners() {
    drawarea.getGraphComponent().getGraph().getSelectionModel()
        .addListener(mxEvent.CHANGE, new mxIEventListener() {
          public void invoke(Object sender, mxEventObject evt) {
//            if (drawarea.getToolMode() != null) {
//              System.out.println("we're in tool mode -- not sync-ing");
//            } else {
            syncTextSelectionToGraph();
            updateLinkButton(); //update the link button for any selection change
//            }
          }
        });
    textarea.addListener(kEvent.TEXTAREA_SELECTION_CHANGE, new mxIEventListener() {
      public void invoke(Object sender, mxEventObject evt) {
//        if (drawarea.getToolMode() != null) {
//          System.out.println("we're in tool mode -- not sync-ing");
//        } else {
        syncGraphSelectionToText();
        updateLinkButton(); //update the link button for any selection change
//        }
      }
    });
  }
  
  /**
   * Checks current focus owner and its selection status, and changes the 
   * selection status of the out-of-focus component to match.
   * Called on focus change as well as fired selection change events (since
   * selection change events always fire in-progress of focus change)
   */
  public void syncGraphSelectionToText() {
    if (textarea.isFocusOwner()) {
      // then force graph selection to sync with us
        
        //crap we can't actually diable it!#&$*!#&$(# because other things depend on the firings
        //temporarily disable event firing while we make changes so we don't get an endless cycle of selection events
        drawarea.getGraphComponent().setEventsEnabled(false);
        
        drawarea.selectCellsIntersectCode(textarea.getSelectionStart(), textarea
            .getSelectionStop());
        
        drawarea.getGraphComponent().setEventsEnabled(true);
      }
  }
  
  /**
   * Checks current focus owner and its selection status, and changes the 
   * selection status of the out-of-focus component to match.
   * Called on focus change as well as fired selection change events (since
   * selection change events always fire in-progress of focus change)
   */
  public void syncTextSelectionToGraph() {
    
    if (drawarea.getGraphComponent().isFocusOwner()) { 
      // then force text selection to sync with us
      

      textarea.setEventsEnabled(false);
        
        Object cell = drawarea.getGraphComponent().getGraph().getSelectionCell();
        
        if (cell instanceof mxICell
            && ((mxICell) cell).getValue() instanceof kCellValue
            && ((kCellValue) ((mxICell) cell).getValue())
                .isValidCodeMarks())
        {
          kCellValue val = (kCellValue) ((mxICell) cell).getValue();
          sketch.setCurrentCode(val.getCodeIndex());
          setSelection(val.getStartMark(), val.getStopMark());
          System.out.println("editor >> graph listener selection sync "
                             + ((kCellValue) ((mxICell) cell).getValue())
                                 .toPrettyString());
        }
        else { // cell does not or cannot have a valid link
          textarea.selectNone();
        }
        
        textarea.setEventsEnabled(true);
      }
  }
  
  /**
   * Checks current focus owner and its selection's link status, and updates the
   * appearance of the link button appropriately.
   */
  public void updateLinkButton() {
    if (textarea.isFocusOwner())
      if (textarea.getSelectedText() == null) {
//      System.out.println("updateLinkButton >> selected text == null so disabling linkbutton");
        drawingHeader.getLinkButton().setEnabled(false);
      } else {
//        System.out.println("updateLinkButton >> text selection eligible for linking");
        drawingHeader.getLinkButton().setLinkMode();
      }
    // CASE 4  
    else if (drawarea.getGraphComponent().isFocusOwner())
      if (drawarea.isSelectionLinked()) {
//        System.out.println("updateLinkButton >> graph selection has link, changing buttons");
        drawingHeader.getLinkButton().setUnlinkMode();
      } else if (!drawarea.isSelectionContainEdge() && drawarea.getGraphComponent().getGraph().getSelectionCount() > 0) {
//        System.out.println("updateLinkButton >> graph selection doesn't have link");
        drawingHeader.getLinkButton().setLinkMode();
      } else { //case where selection is null
//        System.out.println("updateLinkButton >> nothing selected");
        drawingHeader.getLinkButton().setEnabled(false);
      }
    else { // case where drawHeader is focus owner
//      System.out.println("updateLinkButton >> not-text-not-graph is focus owner");
      drawingHeader.getLinkButton().setLinkMode();
      drawingHeader.getLinkButton().setEnabled(false);
    }
  }

  /**
   * Called by colorToolband to request repaint of line marker of cell whose
   * color was just changed
   * 
   * @param startMark
   * @param stopMark
   */
  public void repaintLinesOfOffset(int startMark, int stopMark) {
    System.out.println("editor >> repainting lines of offset >> "+startMark+" "+stopMark);
    textarea.getPainter().invalidateLineRange(textarea.getLineOfOffset(startMark), textarea.getLineOfOffset(stopMark));
  }

  /**
   * Used in undo management to request repaint of line marker of cells affected
   * in last undo/redo
   */
  public void repaintLinesOfChanges(List<mxUndoableChange> changes) {

    System.out.println("editor >> repainting lines of changes");

    Iterator<mxUndoableChange> it = changes.iterator();
    while (it.hasNext())
    {
      Object change = it.next();

      if (change instanceof mxValueChange)
      {
        // repaint both the old and new lines of code if they are in the current text view
        if (((mxValueChange) change).getValue() instanceof kCellValue) {
          kCellValue val = (kCellValue) ((mxValueChange) change).getValue();
          System.out.println("editor >> repainting lines of changes >> val="+val.toPrettyString());
          if (val.isValidCodeMarks() && val.getCodeIndex() == sketch.getCurrentCodeIndex())
            repaintLinesOfOffset(val.getStartMark(), val.getStopMark());
        }
        if (((mxValueChange) change).getPrevious() instanceof kCellValue) {
          kCellValue old = (kCellValue) ((mxValueChange) change).getPrevious();
          System.out.println("editor >> repainting lines of changes >> old="+old.toPrettyString());
          if (old.isValidCodeMarks() && old.getCodeIndex() == sketch.getCurrentCodeIndex())
            repaintLinesOfOffset(old.getStartMark(), old.getStopMark());
        }
      }
    }
  }
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  /*
   * Document sync
   */
  
  /**
   * Declares and installs document listeners for synchronizing edits between
   * the main textarea and code windows
   */
  public void installDocumentSyncListeners() {
    drawarea.addListener(kEvent.CODE_WINDOW_DOCUMENT_CHANGE, new mxIEventListener() {
      public void invoke(Object sender, mxEventObject evt) {
        textareaMirrorDocEdit(
                              ((Integer) evt
                                  .getProperty("sketchInd"))
                                  .intValue(),
                              ((Integer) evt
                                  .getProperty("sketchOffset"))
                                  .intValue(),
                              ((DocumentEvent) evt
                                  .getProperty("event")),
                              (String) evt
                                  .getProperty("change"));
        // make sure the same change is also mirrored in other code windows
        drawarea.mirrorDocEdit(
                              ((Integer) evt
                                  .getProperty("sketchInd"))
                                  .intValue(),
                              ((Integer) evt
                                  .getProperty("sketchOffset"))
                                  .intValue(),
                              ((DocumentEvent) evt
                                  .getProperty("event")),
                              (String) evt
                                  .getProperty("change"));
      }
    });
    textarea.addListener(kEvent.TEXTAREA_DOCUMENT_CHANGE, new mxIEventListener() {
      public void invoke(Object sender, mxEventObject evt) {
        drawarea.mirrorDocEdit(
                              //textarea doesn't know which sketch is current;
                              //we fill in this info before forwarding to drawarea
                              sketch.getCurrentCodeIndex(),
                              ((Integer) evt
                                  .getProperty("sketchOffset"))
                                  .intValue(),
                              ((DocumentEvent) evt
                                  .getProperty("event")),
                              (String) evt
                                  .getProperty("change"));
      }
    });
  }
  
  /**
   * Called by editor asking the textarea to mirror the given edit; its graph counterpart
   * lives in drawarea, but this lives in editor because sketch lives in editor.
   * 
   * @param sketchInd which sketchCode this change is relevant to
   * @param sketchOffset the offset adjusted to sketch (not the original offset in code window)
   * @param e
   * @param change if it is an insert event, the text that was inserted, else null
   */
  private void textareaMirrorDocEdit(int sketchInd, int sketchOffset, DocumentEvent e, String change) {

    System.out
    .println("editor.textareaMirrorDocEdit >> make sure we received everything sketchInd="
             + sketchInd+" sketchOffset="+sketchOffset+" event="+e+" change="+change);
    
    EventType type = e.getType();
    
    if (sketchInd == sketch.getCurrentCodeIndex())
    {
      textarea.documentChanged(e, sketchOffset);
      if (type == EventType.INSERT) {
        textarea.select(sketchOffset, sketchOffset);
        textarea.setSelectedText(change);
      } else if (type == EventType.REMOVE) {
        textarea.select(sketchOffset, sketchOffset+e.getLength());
        textarea.setSelectedText("");
      }
      sketch.setModified(true);
    }
    else // the change is not in the current file
    {
      SketchCode targetCode = sketch.getCode(sketchInd);
      javax.swing.text.Document targetDocument = targetCode.getDocument();
      try {
        if (type == EventType.INSERT) 
        {
          targetDocument.insertString(sketchOffset, change, null);
        }
        else if (type == EventType.REMOVE) 
        {
          targetDocument.remove(sketchOffset, e.getLength());
        }
      } catch (BadLocationException e1) {
        e1.printStackTrace();
      }
      targetCode.setModified(true);
    }
    textHeader.repaint();
  }

  
  
  // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  /*
   * Linking
   */
  
  /**
   * Declares and installs graph & textarea listeners for handling linking
   * between code and visual. Also handles setting the various states of the
   * linkButton located in drawingHeader depending on the selection.
   */
  public void installCodeDrawLinkListeners() {
    // CASE 1
    drawarea.addListener(kEvent.TOOL_END, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        // if linkButton.isSelected&&vertexTool was successful, then
        // connect(text.sel, graph.sel) this works b/c newly created
        // vertex is always the graph current selection
        if (drawingHeader.getLinkButton().isLinkActiveMode())
          if ((Boolean) evt.getProperty("success")) {
//            System.out.println("link >> link active and shape successful, so connect them");
            codeDrawLink(drawarea.getGraphComponent().getGraph().getSelectionCells(), textarea.getSelectionStart(), textarea.getSelectionStop());
          } else {
            // user must have cancelled out of linking so reset the link button
            statusNotice("Code-visual link canceled.");
            updateLinkButton();
          }
      }
    });
    // CASE 1
    drawarea.addListener(kEvent.TOOL_BEGIN, new mxIEventListener() {
      public void invoke(Object source, mxEventObject evt) {
        String toolMode = (String) evt.getProperty("tool");
        if (kUtils.arrayLinearSearch(kConstants.SHAPE_KEYS, toolMode) >= 0
            && textarea.getSelectedText() != null) {
          //user select some text and clicks to create a new shape
//          System.out.println("link >> user selected text and now to create new shape so set link button active");
          drawingHeader.getLinkButton().setLinkActiveMode();
          statusNotice("Create a shape to link it to the selected code, or press the escape key to cancel.");
          // TODO beginCompoundEdit();
        }
      }
    });
    // CASE 2 & 4
    drawarea.getGraphComponent().getGraph().getSelectionModel()
        .addListener(mxEvent.CHANGE, new mxIEventListener() {
          public void invoke(Object sender, mxEventObject evt) {
            
            // CASE 2
            if (drawingHeader.getLinkButton().isLinkActiveMode() && textarea.getSelectedText() != null) {
              //user has selected some text and clicked the link button, and now selected the cells
//              System.out.println("link >> link active, text selected, now graph selected");
              codeDrawLink(drawarea.getGraphComponent().getGraph().getSelectionCells(), textarea.getSelectionStart(), textarea.getSelectionStop());
            }
          }
        });
    // CASE 3
    textarea.addListener(kEvent.TEXTAREA_SELECTION_CHANGE, new mxIEventListener() {
      public void invoke(Object sender, mxEventObject evt) {

        if (drawingHeader.getLinkButton().isLinkActiveMode() && drawarea.getGraphComponent().getGraph().getSelectionCount() > 0) {
          //user has selected some cells and clicked the link button, and now selected text
//          System.out.println("link >> link active, cells selected, now text selected");
          //if user only clicked somewhere and didn't select an area of text, reset the link tool
          if (((Integer) evt.getProperty("newStart")).equals((Integer) evt.getProperty("newEnd")))
          {
//            System.out.println("link >> didn't properly select body of text");
            statusNotice("Code-visual link canceled.");
            drawingHeader.getLinkButton().setLinkMode();
          }
          else
            codeDrawLink(drawarea.getGraphComponent().getGraph().getSelectionCells(), textarea.getSelectionStart(), textarea.getSelectionStop());
        }
      }
    });
    // CASE 2 & 3
    addKeyListener(new KeyAdapter() { //TODO I do believe that this doesn't work AT ALL
      public void keyPressed(KeyEvent e) {
        // if while in linking mode user hits escape, cancel out of it
        if (drawingHeader.getLinkButton().isLinkActiveMode() && e.getKeyCode() == KeyEvent.VK_ESCAPE)
//          System.out.println("link >> user hit escape, cancelling link active mode");
          statusNotice("Code-visual link canceled.");
          drawingHeader.getLinkButton().setLinkMode();
      }
    });
  }
  
  //CASE 2 & 3
  /**
   * Called directly by the linkButton on click in connect mode
   */
  public void linkAction() {
//    System.out.println("link >> linkAction >> isFocusOwner textarea=" + textarea.isFocusOwner()
//                       + " drawarea=" + drawarea.isFocusOwner() + " drawingheader="+ drawingHeader.isFocusOwner());
    //actually textarea would be the previous focus owner, the current focus owner would be drawingHeader
    if (textarea.getSelectedText() != null) {
      // since I can't enable/disable the listeners, i'll jsut ahve them act
      // only when linkbutton is in some sort of active mode
//      System.out.println("link >> only text is selected and link button clicked, so set active mode");
      drawingHeader.getLinkButton().setLinkActiveMode();
      statusNotice("Click on a shape to link it to the selected code, or press the escape key to cancel.");
    } else if (drawarea.getGraphComponent().getGraph().getSelectionCount() > 0) {
      // enable graphSelection change listener on action performed codeDrawLink
      drawingHeader.getLinkButton().setLinkActiveMode();
      statusNotice("Select a body of code to link it to the selected shape, or press the escape key to cancel.");
//      System.out.println("link >> only cell is selected and link button clicked, so set active mode");
    }
  }
  
  //CASE 4
  /**
   * Called directly by the linkButton on click in disconnect mode
   */
  public void disconnectAction() {
//    System.out.println("link >> disconnect action (disconnecting directly)");
    //since we only have one case to handle we can just call the disconnector...
    codeDrawDisconnect(drawarea.getGraphComponent().getGraph().getSelectionCells());
  }

  /**
   * Makes actual model level changes by saving given codemarks into all
   * selected codemark-bearing cells
   */
  private void codeDrawLink(Object[] cells, int start, int stop) {
//    System.out.println("editor >> codeDrawLink start=" + start + " stop="
//                       + stop);
    drawarea.linkCells(cells, start, stop, sketch.getCurrentCodeIndex());
    drawingHeader.getLinkButton().setLinkMode();
    statusNotice("Code-visual link established."); 
    //TODO figure out when to empty the status; maybe after a time delay?
    //could hack it by putting a "counter" at top, which counts # of user select actions (2 or 3)
    //before clearing.  This would avoid the immediate selection problem
  }
  
  /**
   * Makes actual model level disconnection by invalidating codemarks of all
   * selected cells that have codemarks
   */
  private void codeDrawDisconnect(Object[] cells) {
//    System.out.println("editor >> codeDrawDisconnect cells.length="
//                       + cells.length);
    drawarea.unlinkCells(cells);
    drawingHeader.getLinkButton().setLinkMode();
    statusNotice("Code-visual link removed.");
    // this makes the selection active again and forces the selection sync
    // listener to repaint the link markers in textarea
    drawarea.getGraphComponent().requestFocusInWindow();
    drawarea.getGraphComponent().getGraph().setSelectionCells(cells);
  }

}