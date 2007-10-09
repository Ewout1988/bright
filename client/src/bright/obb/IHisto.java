package bright.obb;

import java.awt.event.*;
import java.awt.*;
import java.util.*;

public class IHisto extends ILabel {

   boolean stretchmode = false; 

   int barw;                    // maximum width of the prob bar
   int ibarw, ibarh;            // dimensions of the prob bar in pixels
   
   String probstr;              // the prob string
   int probx, proby;            // coordinates of the prob string
   
   double prob;                 // current probability 
   
   Color incol  = Color.white;  // color of the prob string inside prob bar
   Color outcol = Color.black;  // color of the prob string outside prob bar

   Font probfont;               // current font for the prob string
   Color pcolor;                // current color of the prob string
   
   Color color;                 // color of the prob bar
   Color dcolor;                // color of the prob bar shading
   
   static final double underflow = 0.001;  // probs below this are not shown
   
   public IHisto(String name) {
      this.name = name;
      
      addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) {
            if (!stretchmode)
                setHighlight(true);
         }
         public void mouseExited(MouseEvent evt) {
            if (!stretchmode)
                setHighlight(false);
         }
         public void mousePressed(MouseEvent evt) {
            if (!stretchmode)
                deliver(new ActionEvent(this, 0, null));
         }
      });
   }
   
    public void align(int align) {
        alignText(align,  getSize().width - barw);
    }
   
    public void setBarColor(Color c) {
        this.color  = c;
        this.dcolor = c.darker();
    }
   
    public void setBar(double p, double norm, int barw, Font probfont) {
      this.probfont = probfont;
      this.barw = barw;
      this.prob = p;
      
      double d = round(p * 100, 2);
      probstr = (d >= 10) ? ( (int)d + "%" ) :( d + "%" );
      ibarw =  (int)(barw * prob * norm);
      if (probfont.getSize() < 3) {
         // the font is too small -> don't try to show it!
         this.probfont = null;
         this.proby = 1;
      }
      else {
         // decide the location of the text
         FontMetrics pfm = Toolkit.getDefaultToolkit().getFontMetrics(probfont);
         int strw = pfm.stringWidth(probstr);
         this.ibarh = pfm.getHeight(); 
         this.proby = pfm.getAscent() +1;
         
         if (ibarw > strw + 1) {
            // the colored bar is big enough for the prob string to fit in
            this.probx  = ibarw - strw - 2;  
            this.pcolor = incol;
         }
         else {
            // it is not big enough -> put the prob string next to the bar
            this.probx  = ibarw + 2;
            this.pcolor = outcol;
        }
     }
   }   
   
    public void paint(Graphics g) {
        super.paint(g);
      
        if (prob > underflow) {
            int xx = getSize().width - barw;
            int yy = 1;
            g.setColor(color);
            g.fillRect(xx, yy+1, ibarw, ibarh-2); 
            g.setColor(dcolor);
            g.drawLine(xx, yy + ibarh-1, xx + ibarw, yy + ibarh -1);
            if (stretchmode) {
                g.setColor(Color.red);
            }
            g.drawLine(xx + ibarw, yy+1, xx + ibarw, yy + ibarh -2);

            if (probfont != null) {   
                g.setColor(pcolor);
                g.setFont(probfont);
                g.drawString(probstr, xx + probx, proby);
            }
        }
   }
   
   static double round(double d, int n) {
      if (d == 0) return 0;
      double m = 1;
      double k = Math.pow(10, n) / 10 ;
      while (d <= k) {
         d *= 10;
         m *= 10;
      }
      return Math.round(d) / m;
   }
}
