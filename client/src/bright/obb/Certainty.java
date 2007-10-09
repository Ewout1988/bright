package bright.obb;
 
import java.awt.*;
import java.util.*;
import java.awt.event.*;

public class Certainty extends Container implements NodeDrag.Draggable {

   static Color bcol = new Color(0xFAFAFA);
   static Color hcol = new Color(0xF0F0FF);
  
   CertiePointer pointer;

   ILabel label;
   Buttcon closeButton = new Buttcon.Close();

   double xcoord, ycoord;
   double w, h;
   
   double mean, variance;
   
   int hhght;

   { setBackground(bcol);}
   { setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR)); }
   { add(closeButton); } 
   
   public Certainty(String lab) {
      this.label = new ILabel(lab);
      label.align(ILabel.CENTER);
      add(label);
   }
   
   public void setValues(double mean, double variance) {
      this.mean = mean;
      this.variance = variance;
      repaint();
   }
   
   public void paint(Graphics g) {
      
      Dimension d = getSize();
      int w = d.width-1;
      int h = d.height-1;
      
      g.setColor(getBackground());
      g.fillRect(1, hhght + 1, w-1, h-hhght-2);
      
      g.setColor(hcol);
      g.fillRect(1, 1, w-1, hhght-1);
      
      //
      
      g.setColor(Color.red);
      
      double var = variance;
      double mean = this.mean;
      
      double n = w-2;
      double sqtvar = Math.sqrt(var);
      
      double vk = 1.0/Math.sqrt(2*0.001*Math.PI);
      double k = 1.0/Math.sqrt(2*var*Math.PI);
      double kk = Math.min(1.0, Math.sqrt(k/vk));
      
      double hhh = h - hhght -1;
      
      for (int i = 1; i < w; ++i) {
         double x = (i-1) / n;
         
         double dif = (x-mean);
         double y = kk * Math.exp(-dif*dif/2/var);
         //System.err.println(x +" ; "+ y +" ; "+ dif);
         g.drawLine(i, h, i, (int)(h - hhh * y));
         
      }
      //
      
      g.setColor(Color.black);
      g.drawRect(0, 0, w, h);
      g.drawLine(0, hhght, w, hhght);
      
      super.paint(g);
   }
   
   public void paintShadow(Graphics g, Color c1, Color c2) {
      
      Dimension d = getSize();
      int w = d.width-1;
      int h = d.height-1;
      
      g.setColor(c1);
      g.fillRect(1, 1, w-1, h-1);
      g.setColor(c2);
      g.drawRect(0, 0, w, h);
   }
   
   public void setZoom(double zoom, Font font) {
      
      setFont(font);
      FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
      int fh = fm.getHeight();
      int fw = fm.stringWidth(label.name);
      int wid = Math.max( (int)(zoom * 0.9), fw + fh + 8 );
      int hgh = (int)(0.7*(wid - 2)+ fh + 3); 
      setBounds( (int)(zoom * xcoord), 
                 (int)(zoom * ycoord),
                       wid, 
                       hgh);
      
      
      closeButton.setBounds(wid-fh-1, 0, fh +2, fh +3);
      
      label.setSize(fw+4,fh, font);
      label.setLocation( (wid - fh - fw- 4) /2 , 1);
      this.hhght = fh + 1;
                 
   }
   
   public void setBounds(int x, int y, int w, int h) {
      super.setBounds(x, y, w, h);
      pointer.orient();   
   }
   
   public void moveTo(int x, int y) {
      BezierCanvas canv = (BezierCanvas)getParent();
      double zoom = canv.zoom;
      
      this.xcoord = x / zoom;
      this.ycoord = y / zoom;
      setLocation(x, y);
      canv.repaint();
   }
   
   public void putOnTop() {
      //((BezierCanvas)getParent()).putCertaintyOnTop(this);
   }
      
}
