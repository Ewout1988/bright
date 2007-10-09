package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class DBScrollbar extends Panel {

   boolean horiz;
   Bar bar;
   int buttsize = 18;
   int bw = 12;
   int step = 10;
   
   SimpleListener listener;
   
   ActionListener decrl = new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            decrease();
         }
      };

   ActionListener incrl = new ActionListener() {
         public void actionPerformed(ActionEvent evt) {
            increase();
         }
      };
   
   Button b1 = new Button(); {
         b1.listener = decrl;   
      } 
   Button b2 = new Button(); {
         b2.listener = incrl;   
      }
   
   Button b3 = new Button(); {
         b3.listener = decrl;   
      }
   Button b4 = new Button(); {
         b4.listener = incrl;   
      }
      
   public DBScrollbar(boolean horiz) {
      this.horiz = horiz;
      
      if (horiz) {
         bar = new Horizontal();
      }
      else {
         bar = new Vertical();
      }
      bar.master = this;
      setLayout(null);
      setLocations();
      add(b1);
      add(b2);
      add(b3);
      add(b4);
      add(bar);
   }
   
   
   public void update(Graphics g) {
      paint(g);
   }
   
   void setLocations() {
      Dimension d = getSize(); 
      if (!horiz) {
         bar.setLocation(0, 2 * bw);
         b1.setLocation(0, 0);
         b2.setLocation(0, bw);
         b3.setLocation(0, d.height - bw * 2);
         b4.setLocation(0, d.height - bw);
         
         bar.setSize(d.width, d.height - 4 * bw);
         b1.setSize(buttsize, bw);
         b2.setSize(buttsize, bw);
         b3.setSize(buttsize, bw);
         b4.setSize(buttsize, bw);
      }
      else {
         bar.setLocation(2 * bw, 0);
         b1.setLocation(0, 0);
         b2.setLocation(bw, 0);
         b3.setLocation(d.width - bw * 2, 0);
         b4.setLocation(d.width - bw, 0);
      
         bar.setSize(d.width - 4 * bw, d.height);
         b1.setSize(bw, buttsize);
         b2.setSize(bw, buttsize);
         b3.setSize(bw, buttsize);
         b4.setSize(bw, buttsize);
      }
       
   }
   
   public int getPotential() {
      return bar.getPotential();
   }
   
   public void setSize(Dimension d) {
      super.setSize(d);
      setLocations();
   }
   
   public void setSize(int x, int y) {
      super.setSize(x, y);
      setLocations();
   }
   
   public void setThumb(int loc, int len) {
      bar.startpos = loc;
      bar.length = len;
      repaint();
   }
   
   public Dimension getPreferredSize() {
      if (horiz) {
         return new Dimension(getSize().width, buttsize);
      }
      else {
         return new Dimension(buttsize, getSize().height);
      }
   }
   
   public void increase() {
      int maxpos = bar.getMaxpos();
      if (bar.startpos == maxpos) {
         return;
      }
      bar.startpos = Math.min(maxpos, bar.startpos + step);
      bar.repaint();
      deliver();
   }
   
   public void decrease() {
      if (bar.startpos == 0) {
         return;
      }
      bar.startpos = Math.max(0, bar.startpos - step);
      bar.repaint();
      deliver();
   }
   
   void deliver() {
      if (listener != null) {
         listener.act(this);
      }
   }
   
   public static void main(String[] args) {
   
      DBScrollbar hscroba = new DBScrollbar(false);
      hscroba.bar.length = 200;
      hscroba.bar.setForeground(new Color(0x9999CC));
      
      
      DBScrollbar scroba = new DBScrollbar(true);
      scroba.setBackground(Color.white);
      scroba.bar.length = 100;
      scroba.bar.setForeground(new Color(0x666633));
      
      final Frame f = new Frame("O.B.B.");
      f.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent evt) {
            f.dispose();
            System.exit(0);
         }});
      f.setLayout(new BorderLayout());
      f.add(hscroba, "East");
      f.add(scroba, "South");
      f.setSize(640, 400);
      f.setVisible(true);
   }   

   public static abstract class Bar
                        extends Canvas 
                     implements MouseListener, MouseMotionListener {
   
      DBScrollbar master;
   static Color shacolor = new Color(0x666666);

   
      int startpos;
      int length;
      
      int drax, dray;
      int dpos;
      int marg = 3;
      
      { addMouseListener(this);
        addMouseMotionListener(this);
        setBackground(Color.green);
      }
      
      public void mouseClicked(MouseEvent evt) { }
      public void mouseEntered(MouseEvent evt) { }
      public void mouseExited(MouseEvent evt)  { }
      public void mouseReleased(MouseEvent evt)  { }
      public void mouseMoved(MouseEvent evt) { }
      
      public void mousePressed(MouseEvent evt) {  
         this.drax = evt.getX();
         this.dray = evt.getY();
         
         if (!inThumb(drax, dray)) {
            placeMiddle(drax, dray);
            repaint();
            deliver();
         }
            
         this.dpos = startpos;
      }
      
      public void mouseDragged(MouseEvent evt) {
         int nx = evt.getX();
         int ny = evt.getY();
         drag(nx - drax, ny - dray);
      }
      
      abstract void drag(int dx, int dy);
      abstract int getMaxpos();
      abstract boolean inThumb(int x, int y);
      abstract void placeMiddle(int x, int y);
      abstract int getPotential();
      
      void adjust(int d) {
         int newstart = Math.max(0, Math.min(dpos + d, getMaxpos()));
         if (newstart != startpos) {
            startpos = newstart;
            repaint();
            deliver();
         }
      }
      
      void deliver() {
         master.deliver();
      }
      
    
   }
   
   static class Vertical extends Bar {

      public void drag(int dx, int dy) {
         adjust(dy);
      }
  
      public void paint(Graphics g) {
         Dimension d = getSize();
         g.setColor(shacolor);
         g.fillRect(3, marg + startpos, d.width-3, length);
         g.setColor(getForeground());
         g.fillRect(1, startpos+1, d.width-5, length -2);
         g.setColor(Color.black);
         g.drawRect(0, startpos, d.width -3, length);
      } 
      
      public Dimension getPreferredSize() {
         return new Dimension(18, getSize().height);
      }
      
      int getMaxpos() {
         return getSize().height - length - marg;
      }
      
      boolean inThumb(int x, int y) {
         return y > startpos && y < startpos + length;
      }
      
      void placeMiddle(int x, int y) {
         startpos = Math.max(0, Math.min(y - length/2, getMaxpos()));
      }
      
      int getPotential() {
         return getSize().height;
      }
      
   }
  
   static class Horizontal extends Bar {
  
      public void drag(int dx, int dy) {
         adjust(dx);
      }
      
      public void paint(Graphics g) {
         Dimension d = getSize();
         g.setColor(shacolor);
         g.fillRect(marg + startpos, 3, length, d.height-3);
         g.setColor(getForeground());
         g.fillRect(startpos+1, 1, length-2, d.height-5);
         g.setColor(Color.black);
         g.drawRect(startpos, 0, length, d.height-3);
      } 
      
      public Dimension getPreferredSize() {
         return new Dimension(getSize().width, 18);
      }
    
      int getMaxpos() {
         return getSize().width - length - marg;
      }
      
      boolean inThumb(int x, int y) {
         return x > startpos && x < startpos + length;
      }
      
      void placeMiddle(int x, int y) {
         startpos = Math.max(0, Math.min(x - length/2, getMaxpos()));
      }
      
      int getPotential() {
         return getSize().width;
      }
   }
  
   class Button extends Component implements MouseListener, Runnable {
      
      boolean stateDown = false;
      
      TimeBomb bomb;
      ActionListener listener;
      
      { addMouseListener(this); }
      
      
      public void mouseClicked(MouseEvent evt) { }
      public void mouseEntered(MouseEvent evt) { }
      
      public void mouseExited(MouseEvent evt)  { 
         stateDown = false;
         if (bomb != null) {
            bomb.disarm();
         }
      }
      
      public void mouseReleased(MouseEvent evt)  {
         stateDown = false;
         if (bomb != null) {
            bomb.disarm();
         }
      }
   
      public void mousePressed(MouseEvent evt) {
         stateDown = true;
         deliver();
         bomb = new TimeBomb(350,
            new SimpleListener() {
               public void act(Object nil) {
                  new Thread(Button.this).run();
               }
            });
         bomb.start();
      }
      
      public void run() {
         long time = 100;
         while (stateDown) {
            deliver();
            try {
               Thread.sleep(time);
            }
            catch (InterruptedException ie) { }
         }
      }
      
      private void deliver() {
         if (listener != null) {
            listener.actionPerformed(new ActionEvent(this, 0, null));
         }
      }
      
      public void paint(Graphics g) {
         Dimension d = getSize();
         int w = d.width;
         int h = d.height;
         g.setColor(getForeground());
         g.fillRect(0, 0, w-1, h-1);
      }
   }

}
