package processing.app.graph;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.GeneralPath;
import java.util.Map;

import processing.app.util.kConstants;

import com.mxgraph.swing.view.mxInteractiveCanvas;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxUtils;

/**
 * Custom canvas in order to draw Kaleido's custom shapes.
 * @author achang
 */
public class kCanvas extends mxInteractiveCanvas {

  public kCanvas() {
    // TODO Auto-generated constructor stub
  }
  
  /**
   * Overriding supersuperclass method that dictates which shape to draw.
   * @see {@link com.mxgraph.canvas.mxGraphics2DCanvas#drawShape}
   * @author achang
   * 
   * @param x X-coordinate of the shape.
   * @param y Y-coordinate of the shape.
   * @param w Width of the shape.
   * @param h Height of the shape.
   * @param style Style of the the shape.
   */
  public void drawShape(int x, int y, int w, int h, Map<String, Object> style)
  {
    String shape = mxUtils.getString(style, mxConstants.STYLE_SHAPE, "");
    
    if (shape.equals(kConstants.SHAPE_AUDIO)
        || shape.equals(kConstants.SHAPE_KEYBOARD)
        || shape.equals(kConstants.SHAPE_PERSON)
        || shape.equals(kConstants.SHAPE_STAR))
    {
      // Same procedures of preparation as super class
      Color penColor = mxUtils.getColor(style, mxConstants.STYLE_STROKECOLOR);
      float penWidth = mxUtils.getFloat(style, mxConstants.STYLE_STROKEWIDTH, 1);
      int pw = (int) Math.ceil(penWidth * scale);

      if (g.hitClip(x - pw, y - pw, w + 2 * pw, h + 2 * pw))
      {
        // Prepares the background
        boolean shadow = mxUtils.isTrue(style, mxConstants.STYLE_SHADOW,
            false);
        Color fillColor = mxUtils.getColor(style,
            mxConstants.STYLE_FILLCOLOR);
        Paint fillPaint = getFillPaint(new Rectangle(x, y, w, h),
            fillColor, style);

        if (penWidth > 0)
        {
          setStroke(penWidth, style);
        }
          
        if (shape.equals(kConstants.SHAPE_AUDIO))
        {
          System.out.println("drawing audio shape");
          drawAudio(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
        else if (shape.equals(kConstants.SHAPE_KEYBOARD))
        {
          System.out.println("drawing keyboard shape");    
          drawKeyboard(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
        else if (shape.equals(kConstants.SHAPE_PERSON))
        {
          System.out.println("drawing person shape");
          drawPerson(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
        else if (shape.equals(kConstants.SHAPE_STAR))
        {
          System.out.println("drawing star shape");
          drawStar(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
      } // end if hitClip
    }
    else
      super.drawShape(x, y, w, h, style);
  }

  
  /**
   * Draws a star shape for the given parameters.
   * Uses source code from <a href="http://java-sl.com/shapes.html">here</a>.
   * 
   * @param x X-coordinate of the shape.
   * @param y Y-coordinate of the shape.
   * @param w Width of the shape.
   * @param h Height of the shape.
   * @param fillColor Optional fill color of the shape.
   * @param fillPaint Optional paint of the shape.
   * @param penColor Optional stroke color.
   * @param shadow Boolean indicating if a shadow should be painted.
   */
  protected void drawStar(int x, int y, int w, int h, Color fillColor,
      Paint fillPaint, Color penColor, boolean shadow)
  {
    int vertexCount = 5;
    double startAngle = 0-(Math.PI/2); //start with point to the top
    int halfWidth = w / 2;
    int halfHeight = h / 2;
    int centerx = x+halfWidth;
    int centery = y+halfHeight;
    
    int xcoords[]=new int[vertexCount*2];
    int ycoords[]=new int[vertexCount*2];
    double addAngle=2*Math.PI/vertexCount;
    double innerAngle=startAngle+Math.PI/vertexCount;
    double angle=startAngle;
    
    for (int i=0; i<vertexCount; i++) {
      xcoords[i*2]=(int)Math.round(halfWidth*Math.cos(angle))+centerx;
      ycoords[i*2]=(int)Math.round(halfHeight*Math.sin(angle))+centery;
      angle+=addAngle;
      xcoords[i*2+1]=(int)Math.round(halfWidth/2*Math.cos(innerAngle))+centerx;
      ycoords[i*2+1]=(int)Math.round(halfHeight/2*Math.sin(innerAngle))+centery;
      innerAngle+=addAngle;
    }
    
    Polygon star = new Polygon(xcoords, ycoords, vertexCount*2);

    drawPolygon(star, fillColor, fillPaint, penColor, shadow);
  }
  
  /**
   * Draws a humanoid shape for the given parameters.
   * 
   * @param x X-coordinate of the shape.
   * @param y Y-coordinate of the shape.
   * @param w Width of the shape.
   * @param h Height of the shape.
   * @param fillColor Optional fill color of the shape.
   * @param fillPaint Optional paint of the shape.
   * @param penColor Optional stroke color.
   * @param shadow Boolean indicating if a shadow should be painted.
   */
  protected void drawPerson(int x, int y, int w, int h, Color fillColor,
      Paint fillPaint, Color penColor, boolean shadow)
  {
    GeneralPath path = new GeneralPath();

    path.moveTo(x + w/2, y);
    path.curveTo(x + w*2/3 + w/20, y, x + w*2/3 + w/5, y + h/8, x + w*5/8, y + h/4); //end @ right neck
    path.lineTo(x + w, y + h*3/5);
    path.lineTo(x + w*9/10, y + h*3/5 + h/15);
    path.lineTo(x + w*2/3, y + h/2); // right armpit
    path.lineTo(x + w*3/4, y + h*29/30);
    path.lineTo(x + w*6/10, y + h);
    
    path.lineTo(x + w/2, y + h*2/3); // crotch
    
    path.lineTo(x + w*4/10, y + h);
    path.lineTo(x + w/4, y + h*29/30);
    path.lineTo(x + w/3, y + h/2); // left armpit
    path.lineTo(x + w/10, y + h*3/5 + h/15);
    path.lineTo(x, y + h*3/5);
    path.lineTo(x + w*3/8, y + h/4);
    path.curveTo(x + w/3 - w/5, y + h/8, x + w/3 - w/20, y, x + w/2, y); //end @ top of head
    
    path.closePath();

    drawPath(path, fillColor, fillPaint, penColor, shadow);
  }
  
  /**
   * Draws a keyboard shape for the given parameters.
   * 
   * @param x X-coordinate of the shape.
   * @param y Y-coordinate of the shape.
   * @param w Width of the shape.
   * @param h Height of the shape.
   * @param fillColor Optional fill color of the shape.
   * @param fillPaint Optional paint of the shape.
   * @param penColor Optional stroke color.
   * @param shadow Boolean indicating if a shadow should be painted.
   */
  protected void drawKeyboard(int x, int y, int w, int h, Color fillColor,
      Paint fillPaint, Color penColor, boolean shadow)
  {
    //TODO purrty ugly keyboard in both filled and unfilled mode
    fillPaint = null;
    
    GeneralPath path = new GeneralPath();

    path.moveTo(x + w*2/5, y); //start slightly left of center
    path.curveTo(x + w*2/5, y + h/6, x + w*6/10, y + h*3/16, x + w/2, y + h/3); //weave down
    path.lineTo(x + w*7/15, y + h/3); //go left
    path.curveTo(x + w*6/10 - w/15, y + h*3/16, x + w*2/5 - w/15, y + h/6, x + w*2/5 - w/30, y); //weave back up    
    path.closePath();
    drawPath(path, fillColor, fillPaint, penColor, shadow);
    
    drawRect(x, y + h/3, w, h*2/3, fillColor, fillPaint, penColor, shadow, false);
    
    //draw keys as empty white boxes
    final int cols = 7, rows = 3;
    final int w_incr = w / (cols + 2);
    final int h_incr = h*2/3 / (rows + 2);
    int x_temp = x + w_incr*3/2;
    int y_temp = y + h/3 + h_incr*3/2;
    g.setColor(Color.LIGHT_GRAY);    
    for (int i=0; i < rows; i++) {
      for (int j=0; j < cols; j++) {
        g.drawRect(x_temp+j*w_incr, y_temp+i*h_incr, w_incr/2, h_incr/2);
      }
    }
  }
  
  /**
   * Draws a speaker audio shape for the given parameters.
   * 
   * @param x X-coordinate of the shape.
   * @param y Y-coordinate of the shape.
   * @param w Width of the shape.
   * @param h Height of the shape.
   * @param fillColor Optional fill color of the shape.
   * @param fillPaint Optional paint of the shape.
   * @param penColor Optional stroke color.
   * @param shadow Boolean indicating if a shadow should be painted.
   */
  protected void drawAudio(int x, int y, int w, int h, Color fillColor,
      Paint fillPaint, Color penColor, boolean shadow)
  {
    GeneralPath path = new GeneralPath();

    //speaker:
    path.moveTo(x + w/3, y);
    path.lineTo(x + w*2/3, y + h/4);
    path.lineTo(x + w, y + h/4); //top right butt end
    path.lineTo(x + w, y + h*3/4);
    path.lineTo(x + w*2/3, y + h*3/4);
    path.lineTo(x + w/3, y + h);
    path.closePath();
    drawPath(path, fillColor, fillPaint, penColor, shadow);
    
    //inner sound curve:
    path = new GeneralPath();
    path.moveTo(x + w*6/24, y + h/6);
    path.curveTo(x + w*4/24, y + h*5/12, x + w*4/24, y + h*7/12, x + w*6/24, y + h*5/6);
    path.lineTo(x + w*6/24 - w/20, y + h*5/6);
    path.curveTo(x + w*4/24 - w/20, y + h*5/12 + h/7, x + w*4/24 - w/20, y + h*7/12 - h/7, x + w*6/24 - w/20, y + h/6);
    path.closePath();
    drawPath(path, fillColor, fillPaint, penColor, shadow);
    
    //outer sound curve:
    path = new GeneralPath();
    path.moveTo(x + w*4/24, y);
    path.curveTo(x + w/20, y + h/3, x + w/20, y + h*2/3, x + w*4/24, y + h);
    path.lineTo(x + w*4/24 - w/20, y + h);
    path.curveTo(x, y + h*2/3 + h/20, x, y + h/3 - h/20, x + w*4/24 - w/20, y);
    path.closePath();
    drawPath(path, fillColor, fillPaint, penColor, shadow);
  }

}
