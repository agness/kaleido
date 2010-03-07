package processing.app;

import java.awt.Color;
import java.util.ArrayList;

import javax.swing.JDesktopPane;
import javax.swing.JInternalFrame;

import com.mxgraph.model.mxGraphModel;
import com.mxgraph.swing.mxGraphComponent;
import com.mxgraph.view.mxGraph;


/**
 * The Kaleido drawing JComponent, which is a desktop pane
 * that contains the graph component, and any number of internal
 * code windows.
 * @author achang
 */
public class DrawingArea extends JDesktopPane {

  JInternalFrame graphPanel;
  mxGraphComponent graphComponent;
  
  /**
   * @deprecated
   * This array stores references to all cells that have valid code marks.  It is used to
   * update codeMarks after every textarea edit.
   * 
   * TODO instead, scan the graph each time, like this:
   * Iterator it = graphModel.getCells().values().iterator();
   * while(iterator. hasNext()) System.out.println(it.next());
   */
  protected ArrayList<Object> mideBlockRegistry = new ArrayList<Object>(20);

  
  public DrawingArea(mxGraphModel x) {
    super();
    // TODO Auto-generated constructor stub
    //<!------------- hello world crap
    mxGraph graph = new mxGraph();
    Object parent = graph.getDefaultParent();
    graph.getModel().beginUpdate();
    try
    {
      Object v1 = graph.insertVertex(parent, null, "Hello", 20, 20, 80,
          30);
      Object v2 = graph.insertVertex(parent, null, "World!", 240, 150,
          80, 30);
      graph.insertEdge(parent, null, "Edge", v1, v2);
    }
    finally
    {
      graph.getModel().endUpdate();
    }
    // hello world crap ------------->
    
    graphComponent = new mxGraphComponent(graph);
    graphPanel = new JInternalFrame("Graph", false, //resizable
                                    false, //not closable
                                    false, //not maximizable
                                    false); //not iconifiable
    graphPanel.setContentPane(graphComponent);
    graphPanel.pack();
    graphPanel.setVisible(true);
    graphPanel.setLayer(0);
    graphPanel.setBorder(null);
    graphPanel.setSize(3000, 3000);  //TODO: be able to resize with the rest of them
    add(graphPanel);
    setVisible(true);
  }

}
