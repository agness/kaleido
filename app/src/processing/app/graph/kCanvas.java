package processing.app.graph;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.util.Hashtable;
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
   * Overriding superclass method that dictates which shape to draw.
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
    boolean locked = mxUtils.isTrue(style, kConstants.STYLE_LOCKED,
                                     false);
    style.put(mxConstants.STYLE_SHADOW, (!locked) ? "true" : "false");
    
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
          drawAudio(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
        else if (shape.equals(kConstants.SHAPE_KEYBOARD))
        {  
          drawKeyboard(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
        else if (shape.equals(kConstants.SHAPE_PERSON))
        {
          drawPerson(x, y, w, h, fillColor, fillPaint, penColor, shadow);
        }
        else if (shape.equals(kConstants.SHAPE_STAR))
        {
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
    g.setColor(kConstants.CANVAS_COLOR);    
    for (int i=0; i < rows; i++) {
      for (int j=0; j < cols; j++) {
        g.fillRect(x_temp+j*w_incr, y_temp+i*h_incr, w_incr/2, h_incr/2);
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
   
  /**
   * Takes the "notes", or description, of the kCell and fits the text within the 
   * boundaries of the kCell. Takes care of multiple lines scenarios. 
   * @author susiefu
   * 
   * @param text HTML markup to be painted.
   * @param x X-coordinate of the text.
   * @param y Y-coordinate of the text.
   * @param w Width of the text.
   * @param h Height of the text.
   * @param style Style to be used for painting the text.
   */
  protected void drawPlainTextEllipsis(String text, int x, int y, int w, int h,
      Hashtable style)
  {
    if (g.hitClip(x, y, w, h))
    {
      FontMetrics fm = g.getFontMetrics();
      
      // Stores the original transform
      AffineTransform at = g.getTransform();

      // Rotates the canvas if required
      boolean horizontal = mxUtils.isTrue(style,
          mxConstants.STYLE_HORIZONTAL, true);

      if (!horizontal)
      {
        g.rotate(-Math.PI / 2, x + w / 2, y + h / 2);
        g.translate(w / 2 - h / 2, h / 2 - w / 2);
      }

      // Shifts the y-coordinate down by the ascent plus a workaround
      // for the line not starting at the exact vertical location

      y += 2 * fm.getMaxAscent() - fm.getHeight()
      + mxConstants.LABEL_INSET * scale;

      // Gets the alignment settings
      Object align = mxUtils.getString(style, mxConstants.STYLE_ALIGN,
          mxConstants.ALIGN_CENTER);

      if (align.equals(mxConstants.ALIGN_LEFT))
      {
        x += mxConstants.LABEL_INSET;
      }
      else if (align.equals(mxConstants.ALIGN_RIGHT))
      {
        x -= mxConstants.LABEL_INSET;
      }

      // Sets the color
      Color fontColor = mxUtils.getColor(style,
          mxConstants.STYLE_FONTCOLOR, Color.black);
      g.setColor(fontColor);

      // Draws the text line by line
      String[] lines = text.split("\n");

      int originaly = y;
      
      for (int i = 0; i < lines.length; i++)
      {
        int numlines = 1;
        int dx = 0;
        

        if (align.equals(mxConstants.ALIGN_CENTER))
        {
          int sw = fm.stringWidth(lines[i]);

          if (horizontal)
          {
            dx = (w - sw) / 2;
          }
          else
          {
            dx = (h - sw) / 2;
          }
        }
        else if (align.equals(mxConstants.ALIGN_RIGHT))
        {
          int sw = fm.stringWidth(lines[i]);
          dx = ((horizontal) ? w : h) - sw;
        }
        
        //<!----------begin susie edits
        
        //if the width of the string is contained within the block
        if (fm.stringWidth(lines[i])<w)
        { 
          g.drawString(lines[i], x + dx, y);
        } 
        else //width of string not contained within the block
        { 
          String cuttext = lines[i];
          String formattedstring = new String(wordwrap((int) Math.round(0.155*w),cuttext));
          System.out.println(formattedstring);
          
          //Draws the formatted string into lines
          
          String[] formattedstringlines = formattedstring.split("\n");
          numlines = formattedstringlines.length;
          
          int formaty = y;
          
          //checks if any of the lines overflow vertically
          if (formaty + numlines*fm.getHeight() > originaly + h - 11) 
          { 
            for (int z = 0; z < formattedstringlines.length-1; z++) {
              if (!(formaty + (z+2)*fm.getHeight() > originaly + h - 11)) { 
                //if the next line does NOT overflow
                g.drawString(formattedstringlines[z], x+dx, formaty);
                formaty += fm.getHeight() + mxConstants.LINESPACING;   
              }
              else { //if the next line DOES overflow, 
                //draw a substring of the current line and stop drawing the 
                //rest of the lines
                String substring = formattedstringlines[z]
                    .substring(0, formattedstringlines[z].length() - 3);
                g.drawString(substring, x + dx, formaty);  
                g.drawString("...", x+dx+fm.stringWidth(substring), formaty);
                //formaty += fm.getHeight() + mxConstants.LINESPACING;   
                break;
              }
            }
          } 
          else { // none of the lines overflow vertically
            
            System.out.println("formaty: " + formaty
                               + ", numlines*fm.getHeight()" + numlines
                               * fm.getHeight() + ", originaly" + originaly
                               + ", h" + h);
            for (int z = 0; z < formattedstringlines.length; z++) {
              g.drawString(formattedstringlines[z], x + dx, formaty);  
              formaty += fm.getHeight() + mxConstants.LINESPACING;   
            }
          }
        }
        
        y += numlines*fm.getHeight() + mxConstants.LINESPACING;
        
        if (y + fm.getHeight() > originaly + h - 11) {
          //TODO: FAILED ATTEMPT AT TRYING TO "..." THE LAST LINE IF THERE ARE
          //MORE LINES AFTERWARDS... THE "..." SEEMS TO OVERLAP THE TEXT! :(
//          if (i > 0) {
//            g.drawString("...", x+dx+fm.stringWidth(lines[i-1]), y - numlines*fm.getHeight() - mxConstants.LINESPACING);
//          } else if (i == 0) {
//            g.drawString("...", x+dx+fm.stringWidth(lines[i]), y - numlines*fm.getHeight() - mxConstants.LINESPACING);
//          }
          break;
        }
        
        //end susie edits---------->
        
      }

      // Resets the transformation
      g.setTransform(at);
    } 
  }

  private static String wordwrap(int width,String st) {
    StringBuffer buf = new StringBuffer(st);
    int lastspace = -1;
    int linestart = 0;
    int i = 0;

    while (i < buf.length()) {
      if ( buf.charAt(i) == ' ' ) lastspace = i;
      if ( buf.charAt(i) == '\n' ) {
        lastspace = -1;
        linestart = i+1;
      }
      if (i > linestart + width - 1 ) {
        if (lastspace != -1) {
          buf.setCharAt(lastspace,'\n');
          linestart = lastspace+1;
          lastspace = -1;
        }
        else {
          buf.insert(i,'\n');
          linestart = i+1;
        }
      }
      i++;
    }
    return buf.toString();

  }
}
