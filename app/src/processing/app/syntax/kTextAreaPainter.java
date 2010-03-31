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

/**
 * A custom version of TextAreaPainter that paints a vertical strip (i.e. the
 * line marker) on the left edge that indicates linkages to drawn elements via
 * color fill information
 * @author achang
 */
public class kTextAreaPainter extends TextAreaPainter {

  //TODO refactor into kConstants
  public static final int LINK_MARKER_WIDTH = 6;

  private static final Color LINK_MARKER_BACKGROUND_COLOR = Color.lightGray;
  
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
    System.out.println("kTAP >> painting line marker start="+textArea.getLineStartOffset(line)+" stop="+textArea.getLineStopOffset(line));
        int x = 0;
        int height = fm.getHeight();
        y += fm.getLeading() + fm.getMaxDescent();
        gfx.setColor(LINK_MARKER_BACKGROUND_COLOR);
        gfx.fillRect(x, y, LINK_MARKER_WIDTH, height);
  
        Object[] colors = drawarea.getColorsIntersectCode(textArea.getLineStartOffset(line), textArea.getLineStopOffset(line));
        
        if (colors.length > 0) {
          int stripeWidth;
          if (colors.length > LINK_MARKER_WIDTH)
            stripeWidth = 1;
          else
            stripeWidth = LINK_MARKER_WIDTH / colors.length;
          //TODO might look better if everything was just 1 px, esp. since I can't ensure consistency in width across lines
    
          for (int i = 0; i < LINK_MARKER_WIDTH/stripeWidth; i++) {
            gfx.setColor((Color) colors[i]);
            gfx.fillRect(x, y, stripeWidth, height);
            x += stripeWidth;
          }
        }
//    }
  }

}
