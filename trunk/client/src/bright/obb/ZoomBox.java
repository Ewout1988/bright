package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ZoomBox extends Container {

   Birdie birdie;
   ZoomStrip strip;
   
   BezierCanvas canv;

   double zoom;
   int mark;
   
   double hzo = 25;
   double lzo = 250;

   double qpo = 100;
   
  
   Color[] colors;
   
   
   public ZoomBox(Image bird) {
      this.strip = new ZoomStrip();
      add(strip);
      strip.addMouseListener(new MouseAdapter() {
         public void mouseClicked(MouseEvent evt) {
            int x = evt.getX();
            int y = evt.getY();
            
            setZoom(calcZoom(y));
            canv.setZoom((float)zoom);
         }
      });
      
      strip.addMouseMotionListener(new MouseMotionAdapter() {
         public void mouseDragged(MouseEvent evt) {
            int x = evt.getX();
            int y = Math.min(strip.getSize().height -1, 
                     Math.max(0, evt.getY()));
            
            setZoom(calcZoom(y));
         }
      });
   
      this.birdie = new Birdie(bird);
      add(birdie);
      birdie.addMouseListener(
         new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
               double z = hzo;
               setZoom(z);
            }
         });
      setZoom(100);
      
      Cursor hand = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
      strip.setCursor(hand);
      birdie.setCursor(hand);
   }
   
   void setMinZoom(double hzo) {
      this.hzo = hzo;
      setZoom(Math.max(hzo, zoom));
   }
   
   void setZoom(double zoom) {
      this.zoom = zoom;
      Dimension d = strip.getSize();
      int th = d.height;
      
      double quarter = th /4.0;
      
      if (zoom < qpo) {
         mark = (int)((zoom - hzo)/(qpo - hzo) * 3 * quarter);
      }
      else {
         mark = (int)(3 * quarter + (zoom - qpo)/(lzo - qpo) * quarter);
      }
      if (canv != null) {
         canv.setZoom((float)zoom);
      }
      repaint();
   }
   
   
   public double calcZoom(int mrk) {
      Dimension d = strip.getSize();
      int th = d.height;
      double quarter = th /4.0;
      if (mrk < 3 * quarter) {
         return mrk/3.0/quarter*(qpo - hzo) + hzo;
      }
      else {
         return (mrk /quarter - 3) * (lzo - qpo) + qpo;
      }
   }
   
   public void setBounds(int x, int y, int w, int h) {
      super.setBounds(x, y, w, h);
      
      int wid = 30;
      int xx = (w-wid)/2;
      int yy = birdie.getSize().height;
      int bx = (w- birdie.getSize().width) /2;
      birdie.setLocation(bx, 0);
      strip.setBounds(xx, yy, wid, h - yy);
      strip.calcCols();
   }
   
   
   
   class Birdie extends Component {
      
      Image bird;
      
      Birdie(Image bird) {
         this.bird = bird;
         setSize(bird.getWidth(this), bird.getHeight(this));
      }
      
      public void paint(Graphics g) {
         g.drawImage(bird, 0, 0, this);
      }
   }
   
   class ZoomStrip extends Component {
   
      Color bord = new Color(0xDDCCAA);
   
      Color low = new Color(0xBBB088);
      Color high  = new Color(0x9999B8);
 
      { calcCols(); }
      
      public void calcCols() {
         int ncol = Math.max(2, getSize().height/10 +1);
         colors = new Color[ncol];
         
         int lowr = low.getRed();
         int lowg = low.getGreen();
         int lowb = low.getBlue();
         int hir = high.getRed();
         int hig = high.getGreen();
         int hib = high.getBlue();
      
         int nn = ncol -1;
         for (int i = 0; i< ncol; ++i) {
            colors[i] = new Color(hir + (lowr-hir)*i/nn,
                                  hig + (lowg-hig)*i/nn,
                                  hib + (lowb-hib)*i/nn);
        }
      }
   
      public void paint(Graphics g) {
   
         Dimension d = getSize();
         int w = d.width -1;
         int h = d.height -1;
   
         int n = colors.length;
         for (int i = 0; i < n; ++i) { 
            g.setColor(colors[i]);
            g.fillRect(0, 10*i, w, 10);
         }
      
         /*
         g.setXORMode(colors[0]);
         g.setFont(getFont());
         g.drawString("Z", tx + 10, ty + 30);
         g.drawString("O", tx + 10, ty + 50);
         g.drawString("O", tx + 10, ty + 70);
         g.drawString("M", tx + 10, ty + 90);
      
         g.setPaintMode();
         */
         
         g.setColor(Color.black);
         g.drawRect(0, 0, w, h);
    
         g.setColor(bord);
         g.drawRect(1, 1, w-2, h-2);
         g.setColor(Color.black);
         g.drawLine(0, mark,   w, mark);
         g.drawLine(0, mark-2, w, mark-2);
         g.drawLine(0, mark+2, w, mark+2);
      
         super.paint(g);  
      }
   }
   
}
