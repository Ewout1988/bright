package bright.obb;


import java.awt.*;
import java.awt.event.*;
import java.util.*;

import bright.obb.infer.Answun;
import bright.obb.infer.Inferer;

public class BezierCanvas extends Container {

   static float zoom = 100.0f;
   
   DotGraph dg;         // the network layout produced by Dot
   Bezier[] bzrs;
   VDotNode[] vnodes, vnodesinorder;
   
   Inferer inferer;
   Answun answun;
   
   Point oldp;
   Dimension psize;
   
   Color edgecol = new Color(0x663399);
   Color shadowcol2 = Color.gray.darker();
   Color shadowcol = shadowcol2.darker();
   
   Color freecol = Color.white;
   Color fixcol = new Color(0xECECF9);
   
   // the scroll pane has to be known to report zoom changes
   DBScrollPane pane;
   
   Rectangle paintBounds;
   
   int certaintyCount;
   
   public BezierCanvas(DotGraph dg, ValGraph vg, Inferer ifr, Answun answun) {
      
      this.dg = dg;
      this.inferer = ifr;
      this.answun = answun;
      
      // create node components
      int m = dg.nodes.size();
      vnodes = new VDotNode[m];
      vnodesinorder = new VDotNode[m];
      Hashtable ht = new Hashtable();
      
      for (int i = 0; i < m; ++i) {
         final DotGraph.Node no = (DotGraph.Node)dg.nodes.elementAt(i);
         String[] tab = (String[])vg.vals.get(no.label);
         final VDotNode vnod = vnodes[i] = new VDotNode(no, tab, this);
         vnod.setBackground(Color.white);
         vnod.setForeground(Color.black);
         vnod.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent evt) {
                  int ind = vnod.chosenIndex;
                  inferer.setInst(vnod.index, ind);
                  if (ind == -1) {
                     vnod.setBackground(freecol);
                  }
                  else {
                     vnod.setBackground(fixcol);
                  }
                  setProbs();
               }
            });
         add(vnod);
         new NodeDrag(vnod, vnod.openpart);
         new NodeDrag(vnod, vnod.closedpart);
         ht.put(no.label, vnod);
         no.vnode = vnod;
      }
      
      for (int i = 0; i < m; ++i) {
         String name = (String)vg.nodes.elementAt(i);
         VDotNode no = (VDotNode)ht.get(name);
         vnodesinorder[i] = no;
         no.index = i;
      }   
      
      // create edge beziers
      int n = dg.edges.size();
      bzrs = new Bezier[n];
      for (int i = 0; i < n; ++i) {
         DotGraph.Edge e = (DotGraph.Edge)dg.edges.elementAt(i);
         bzrs[i] = new Bezier(e.cx, e.cy);
         bzrs[i].dotedge = e;
         bzrs[i].setZoom( zoom );
         bzrs[i].setColor(edgecol);
         e.bezier = bzrs[i];
      }
   }
      
   synchronized void putNodeOnTop(VDotNode node) {
      remove(node);
      add(node, 0);
      node.repaint();
   }
   
   double calcGoodZoom() {
      Dimension psize = pane.getViewportSize();
      
      double woptz = psize.width / dg.width;
      double hoptz = psize.height / dg.height;
      
      return Math.min(woptz, hoptz);
   }
   
   public Dimension getPreferredSize() {
      int w = (int)(zoom * dg.width );
      int h = (int)(zoom * dg.height);
      return new Dimension(w, h);
   }
   
   public void centerAt(int x, int y) {
      Dimension size = getSize();
      psize = pane.getViewportSize();
      int xnn = Math.min(size.width - psize.width, 
                 Math.max(0, x - psize.width/2));
      int ynn = Math.min(size.height - psize.height, 
                 Math.max(0, y - psize.height/2));
      pane.setScrollPosition(xnn, ynn);
         
   }          
   
   public void setZoom(float z) {
      
      float oldz = zoom;
      
      this.zoom = z;
      float zf = zoom;
      
      int n = dg.edges.size();
      for (int i = 0; i < n; ++i) {
         bzrs[i].setZoom( zf );
      }
      int fsize = (int)(zoom/100*12);
          fsize += fsize%2;
      Font font = new Font("SansSerif", Font.BOLD, Math.max(1, fsize));
      Font vfont = new Font("SansSerif", Font.PLAIN, Math.max(1, fsize-2));
      Font sfont = new Font("SansSerif", Font.PLAIN, Math.max(1, fsize-4));
       
      for (int i = 0; i < dg.nodes.size(); ++i) {
         vnodes[i].orient(font, vfont, sfont, zoom);
      }
      
      Dimension size = getPreferredSize();
      setSize(size);
     
      if (pane != null) {
         oldp = pane.getScrollPosition();
         psize = pane.getViewportSize();
         
         int xm = oldp.x + psize.width/2;
         int ym = oldp.y + psize.height/2;
         
         int xnn = (int)(xm /oldz * zoom) - psize.width/2;
         int ynn = (int)(ym /oldz * zoom) - psize.height/2;
         
         pane.doLayout();
   
         int nx = Math.max(0, Math.min(xnn , size.width - psize.width));
         int ny = Math.max(0, Math.min(ynn , size.height - psize.height));
         
         oldp = new Point(nx, ny);
         
         if (size.width < psize.width) {
            nx = (size.width - psize.width) /2;
         }
         if (size.height < psize.height) {
            ny = (size.height - psize.height) /2;
         }
         pane.setScrollPosition(nx, ny);
         
      }
      
   }
   
   public void setBackground(Color col) {
      super.setBackground(col);
      
      float fact = 0.67f;
      float fact2 = 0.8f;
      
      this.shadowcol = new Color(
         (int)(col.getRed() * fact),
         (int)(col.getGreen() * fact),
         (int)(col.getBlue() * fact));
         
      this.shadowcol2 = new Color(
         (int)(col.getRed() * fact2),
         (int)(col.getGreen() * fact2),
         (int)(col.getBlue() * fact2));
      
      ;
   }
   
   public void paint(Graphics g) {
      
      Rectangle bob = g.getClipBounds();
      g.setColor(getBackground());
     // g.fillRect(bob.x, bob.y, bob.width, bob.height); 
    
      Dimension size = getSize();
      g.fillRect(0, 0, size.width, size.height);
      if (zoom > 50) {
         paintBezierShadows(g);
      }
      paintShadows(g);
      paintBeziers(g);
      super.paint(g);
      
    /*  
      g.setColor(Color.red);
      g.drawRect(bob.x, bob.y, bob.width-1, bob.height-1);
         System.err.println("paint");
   */
   }
      
   public void paintShadows(Graphics g) {
      int offset = (int)(zoom / 100.0 * 6);
      g.translate(offset, offset);
      Rectangle bob = g.getClipBounds();
      int n = vnodes.length;
      for (int i = 0; i < n; ++i) {
         if (vnodes[i].getBounds().intersects(bob)) {
            Point p = vnodes[i].getLocation();
            g.translate(p.x, p.y);
            vnodes[i].paintShadow(g, shadowcol, shadowcol2);
            g.translate(-p.x, -p.y);
         }
      }
      
      g.translate(-offset, -offset);
      
   }
   
   private void paintBeziers(Graphics g) {
      int n = bzrs.length;
      Rectangle bob = g.getClipBounds();
      for (int i = 0; i < n; ++i) {
         if (bzrs[i].bobox.intersects(bob)) {
            bzrs[i].paint(g);
         }
      }
   }
   private void paintBezierShadows(Graphics g) {
      g.setColor(shadowcol);
      int offset = (int)(zoom / 100.0 * 2);
      g.translate(offset, offset);
      int n = bzrs.length;
      Rectangle bob = g.getClipBounds();
      for (int i = 0; i < n; ++i) {
         if (bzrs[i].bobox.intersects(bob)) {
            bzrs[i].paintMe(g);
         }
      } 
      g.translate(-offset, -offset);
      
   }
   
    public void setProbs() {
        inferer.infer();
        int n = vnodes.length;
        for (int i = 0; i < n; ++i) {
            vnodesinorder[i].setProbs(inferer.getProbs(i), 
                                      inferer.isObserved(i));
        }
    }
}
