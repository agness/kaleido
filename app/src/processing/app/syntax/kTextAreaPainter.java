package processing.app.syntax;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.text.PlainDocument;
import javax.swing.text.Utilities;

import processing.app.DrawingArea;
import processing.app.util.kConstants;

/**
 * A custom version of TextAreaPainter that paints a vertical strip (i.e. the
 * line marker) on the left edge that indicates linkages to drawn elements via
 * color fill information
 * @author achang
 */
public class kTextAreaPainter extends TextAreaPainter {

  private static final boolean FIXED_STRIPE_WIDTH_STYLE = false; //each stripe is a fixed width but random order
  private static final boolean FIXED_STRIPE_LANE_STYLE = true; //each color has a lane it will always go in
  
  final DrawingArea drawarea;
  
  public kTextAreaPainter(JEditTextArea textArea, TextAreaDefaults defaults, DrawingArea drawarea) {
    super(textArea, defaults);
    this.drawarea = drawarea;
    
    System.out.println("kTAP >> constructed");
  }
  
  protected void paintLine(Graphics gfx, TokenMarker tokenMarker,
                           int line, int x)
  {
    Font defaultFont = getFont();
    Color defaultColor = getForeground();

    currentLineIndex = line;
    int y = textArea.lineToY(line);

    paintLinkMarker(gfx, line, y); //<------kEdit
    
    if (line < 0 || line >= textArea.getLineCount()) {
      if (paintInvalid) {
        paintHighlight(gfx,line,y);
        styles[Token.INVALID].setGraphicsFlags(gfx,defaultFont);
        gfx.drawString("~",0,y + fm.getHeight());
      }
    } else if(tokenMarker == null) {
      paintPlainLine(gfx,line,defaultFont,defaultColor,x,y);
    } else {
      paintSyntaxLine(gfx,tokenMarker,line,defaultFont,
                      defaultColor,x,y);
    }
  }
  
  protected void paintLinkMarker(Graphics gfx, int line, int y) {
//    if (!printing) {
//    System.out.println("kTAP >> painting line marker start="+textArea.getLineStartOffset(line)+" stop="+textArea.getLineStopOffset(line));
        int x = 0;
        int height = fm.getHeight();
        y += fm.getLeading() + fm.getMaxDescent();
        gfx.setColor(kConstants.LINK_MARKER_BACKGROUND_COLOR);
        gfx.fillRect(x, y, kConstants.LINK_MARKER_WIDTH, height);
  
        Object[] colors = drawarea.getColorsIntersectCode(textArea.getLineStartOffset(line), textArea.getLineStopOffset(line));
        
        if (colors.length > 0) {
          int stripeWidth;
          
          if (FIXED_STRIPE_LANE_STYLE) {
            Color[][] palette = kConstants.FILL_COLORS;
            boolean[] stripe = new boolean[palette[0].length];
            stripeWidth = kConstants.LINK_MARKER_WIDTH / palette[0].length;
            
            for (int i = 0; i < colors.length; i++)
              for (int j = 0; j < stripe.length; j++) {
              if (!stripe[j] && ((Color) colors[i]).equals(palette[0][j]))
                stripe[j] = true;
            }
            
            for (int i = 0; i < palette[0].length; i++)
              if (stripe[i]) {
                gfx.setColor(palette[0][i]);
                gfx.fillRect(x+stripeWidth*i, y, stripeWidth, height);
            }
          } else {
            if (FIXED_STRIPE_WIDTH_STYLE) {
              stripeWidth=3;
            } else {
              if (colors.length > kConstants.LINK_MARKER_WIDTH)
                stripeWidth = 1; //minimum stripe width, i.e. max number of stripes
              else
                stripeWidth = kConstants.LINK_MARKER_WIDTH / colors.length;
            }
      
            for (int i = 0; i < Math.min(colors.length, kConstants.LINK_MARKER_WIDTH/stripeWidth); i++) {
              gfx.setColor((Color) colors[i]);
              gfx.fillRect(x, y, stripeWidth, height);
              x += stripeWidth;
            }
            //if there was a remainder, fill it with the last color
            if (x < kConstants.LINK_MARKER_WIDTH)
              gfx.fillRect(x, y, kConstants.LINK_MARKER_WIDTH-x, height);
          }
        }
//    } //!printing if statement
  }

}
