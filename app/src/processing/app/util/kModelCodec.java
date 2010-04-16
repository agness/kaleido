package processing.app.util;

import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import processing.app.graph.kGraphModel;

import com.mxgraph.io.mxCodec;
import com.mxgraph.io.mxCodecRegistry;
import com.mxgraph.io.mxObjectCodec;
import com.mxgraph.model.mxICell;

public class kModelCodec extends mxObjectCodec
{

  /**
   * Constructs a new model codec.
   */
  public kModelCodec()
  {
    this(new kGraphModel());
  }

  /**
   * Constructs a new model codec for the given template.
   */
  public kModelCodec(Object template)
  {
    this(template, null, null, null);
  }

  /**
   * Constructs a new model codec for the given arguments.
   */
  public kModelCodec(Object template, String[] exclude, String[] idrefs,
      Map<String, String> mapping)
  {
    super(template, exclude, idrefs, mapping);
  }

  /**
   * Encode the given kGraphModel by writing a (flat) XML sequence
   * of cell nodes as produced by the mxCellCodec. The sequence is
   * wrapped-up in a node with the name root.
   */
  public Node encode(mxCodec enc, Object obj)
  {
    Node node = null;

    if (obj instanceof kGraphModel)
    {
      kGraphModel model = (kGraphModel) obj;
      String name = mxCodecRegistry.getName(obj);

      node = enc.getDocument().createElement(name);
      Node rootNode = enc.getDocument().createElement("root");

      enc.encodeCell((mxICell) model.getRoot(), rootNode, true);
      node.appendChild(rootNode);
    }

    return node;
  }

  /**
   * Reads the cells into the graph model. All cells are children of the root
   * element in the node.
   */
  public Node beforeDecode(mxCodec dec, Node node, Object into)
  {
    if (node instanceof Element)
    {
      Element elt = (Element) node;
      kGraphModel model = null;

      if (into instanceof kGraphModel)
      {
        model = (kGraphModel) into;
      }
      else
      {
        model = new kGraphModel();
      }

      // Reads the cells into the graph model. All cells
      // are children of the root element in the node.
      Node root = elt.getElementsByTagName("root").item(0);
      mxICell rootCell = null;

      if (root != null)
      {
        Node tmp = root.getFirstChild();

        while (tmp != null)
        {
          mxICell cell = dec.decodeCell(tmp, true);

          if (cell != null && cell.getParent() == null)
          {
            rootCell = cell;
          }

          tmp = tmp.getNextSibling();
        }

        root.getParentNode().removeChild(root);
      }

      // Sets the root on the model if one has been decoded
      if (rootCell != null)
      {
        model.setRoot(rootCell);
      }
    }

    return node;
  }

}