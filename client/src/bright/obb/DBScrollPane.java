package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class DBScrollPane extends Container {

   DBScrollbar horiz;
   DBScrollbar verti;
   
   Dimension viewportDim = new Dimension(0,0);
   
   Component compo;
   
   public DBScrollPane(Component compo) {
      this.compo = compo;
      
      Dimension prfs = compo.getPreferredSize();
      compo.setSize(prfs);
      
      compo.setLocation(-20, -40);
   
      horiz = new DBScrollbar(true);
      verti = new DBScrollbar(false);
      
      
      Color niceco = new Color(0x9999B8);
      horiz.bar.setForeground(niceco);
      verti.bar.setForeground(niceco);
      Color nikeco = new Color(0x666688);
      horiz.bar.setBackground(nikeco);
      verti.bar.setBackground(nikeco);
      
      SimpleListener lst = new SimpleListener() {
            public void act(Object obj) {
               updateCompo();
            }
         };
         
      horiz.listener = lst;
      verti.listener = lst;
      
      add(horiz);
      add(verti);
      add(compo);
   }
   
   public Point getScrollPosition() {
      Point p = compo.getLocation();
      p.x = -p.x;
      p.y = -p.y;
      return p;
   }
   
   public Dimension getViewportSize() {
      return new Dimension(viewportDim.width, viewportDim.height);
   }
   
   public void setScrollPosition(int x, int y) {
      updateCompo();
      compo.setLocation(-x, -y);
      
      int scrw = horiz.getPotential();
      int scrh = verti.getPotential();
      Dimension d = compo.getSize();
      
      double xscale = (double)scrw / d.width;
      int wbar = (int)(scrw * xscale);
      int xbar = (int)(x * xscale);
    
      double yscale = (double)scrh / d.height;
      int hbar = (int)(scrh * yscale);
      int ybar = (int)(y * yscale);
      
      horiz.setThumb(xbar, wbar);
      verti.setThumb(ybar, hbar);
      compo.setLocation(0, 0);
      updateCompo();
      repaint();     
   }
   
   
   public void doLayout() {
      Dimension sz = getSize();
      
      int horih = horiz.getPreferredSize().height;
      int vertw = verti.getPreferredSize().width;
      
      horiz.setSize(sz.width - vertw, horih);
      horiz.setLocation(0, sz.height - horih);
      
      verti.setSize(vertw, sz.height - horih);
      verti.setLocation(sz.width - vertw, 0);
      
      viewportDim = new Dimension(sz.width - vertw, sz.height - horih);
      calcScrollbars();
      
      Point p = compo.getLocation();
      Dimension d = compo.getSize();
      boolean change = false;
      if (viewportDim.width > Math.min(0, p.x) + d.width) {
         int xx = Math.max(0, (viewportDim.width - d.width) / 2);
         p.x = Math.min(xx, viewportDim.width - d.width);
         change = true;
      }
      else if (p.x > 0) {
         p.x = 0;
         change = true;
      }  
      if (viewportDim.height > Math.min(0, p.y) + d.height) {
         int yy = Math.max(0, (viewportDim.height - d.height) / 2);
         p.y = Math.min(yy, viewportDim.height - d.height);
         change = true;
      }
      else if (p.y > 0) {
         p.y = 0;
         change = true;
      }
      if (change) {
         compo.setLocation(p);
         calcScrollbars();
      }
      updateCompo();
      repaint();     
   }
   
   public void calcScrollbars() {
      Dimension d = compo.getSize();
      Point p = compo.getLocation();
      Point q = p;
      
      int scrw = horiz.getPotential();
      int scrh = verti.getPotential();
      
      int wbar, hbar, xbar, ybar;
      if (viewportDim.width >= d.width) {
         wbar = scrw;
         xbar = 0;
         q.x = (viewportDim.width - d.width) / 2;
      }
      else {
         double scale = (double)viewportDim.width / d.width;
         wbar = (int)(scrw * scale);
         if (q.x + d.width < viewportDim.width) {
            q.x = viewportDim.width -d.width;
         }
         xbar = Math.min((int)(-q.x * scale), scrw -wbar);
      }
      if (viewportDim.height >= d.height) {
         hbar = scrh;
         ybar = 0;
         q.y = (viewportDim.height - d.height) / 2;
      }
      else {
         double scale = (double)viewportDim.height / d.height;
         hbar = (int)(scrh * scale);
         
         if (q.y + d.height < viewportDim.height) {
            q.y = viewportDim.height -d.height;
         }
         ybar = Math.min((int)(-q.y * scale), scrh - hbar);
      }
      
      horiz.setThumb(xbar, wbar);
      verti.setThumb(ybar, hbar);
      
      if (!p.equals(q)) {
         compo.setLocation(q);
      }
      repaint();
      horiz.bar.repaint();
      verti.bar.repaint();
   }
   
   void updateCompo() {
      Dimension d = compo.getSize();
      Point p = compo.getLocation();
      int xbar = horiz.bar.startpos;
      int ybar = verti.bar.startpos;
      
      int www = horiz.getPotential();
      int hhh = verti.getPotential();
      
      if (viewportDim.width < d.width) {
         double wscale = (double)www / d.width;
         p.x = - (int)(xbar /wscale);
      }
      else {
         p.x = (viewportDim.width - d.width) /2;
      }
      if (viewportDim.height < d.height) {
         double hscale = (double)hhh / d.height;
         p.y = - (int)(ybar /hscale);
      }
      else {
         p.y = (viewportDim.height - d.height) /2;
      }
      compo.setLocation(p);
      repaint();
   }
   
   
   public void update(Graphics g) {
      paint(g);
   }
   
   public void paint(Graphics g) {
            
      Point p = horiz.getLocation();
      g.translate(p.x, p.y);
      horiz.paint(g);
      
      Point q = verti.getLocation();
      g.translate(q.x - p.x, q.y - p.y);
      verti.paint(g);
      Point cloc = compo.getLocation();
      g.translate( cloc.x-q.x, cloc.y-q.y );
      
      Rectangle rec = g.getClipBounds();
      g.setColor(getBackground());
      g.fillRect(rec.x, rec.y, rec.width, rec.height);
      compo.paint(g);
   }
}
