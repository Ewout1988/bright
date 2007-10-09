package bright.obb;
 
import java.awt.*;
import java.util.*;
import java.awt.event.*;

public class CertiePointer extends ComponentAdapter {

   Certainty cert;
   Component source;
   
   int srcx, srcy;
   int lefx, lefy;
   int rigx, rigy;
   
   Rectangle bobox;
   
   BezierCanvas canvas;
   
   boolean seeme = true;
   
   ComponentListener moveWatch = this;
   
   Color color = Color.black;
   
   public CertiePointer(Certainty cert, Component source, BezierCanvas canv) {
      this.cert = cert;
      cert.pointer = this;
      this.source = source;
      this.canvas = canv;
   }
   
   public void paint(Graphics g) {
      if (!seeme) {
         return;
      }
      g.setColor(color);
      
      g.drawLine(srcx, srcy, lefx, lefy);
      g.drawLine(srcx, srcy, rigx, rigy);
   }
   
   public void orient() {
      if (canvas == null) {
         return;
      }
      Point bloc = canvas.getLocationOnScreen();
      Point cloc =   cert.getLocationOnScreen();
      Point sloc = source.getLocationOnScreen();
   
      Dimension ssiz = source.getSize();
      Dimension csiz = cert.getSize();
      
      cloc.translate(-bloc.x, -bloc.y);
      sloc.translate(-bloc.x, -bloc.y);
      
      int sx = sloc.x + ssiz.width;
      int sy = sloc.y + ssiz.height/2;
      
      this.srcx = sx;
      this.srcy = sy;
      
      int cx1 = cloc.x;
      int cy1 = cloc.y;
      int cx2 = cloc.x + csiz.width;
      int cy2 = cloc.y + csiz.height;
      
      seeme = true;
      if (sx < cx1) {
         if (sy < cy1) {
            this.lefx = cx1;
            this.lefy = cy2;
            this.rigx = cx2;
            this.rigy = cy1;
         }
         else if (sy < cy2) {
            this.lefx = cx1;
            this.lefy = cy1;
            this.rigx = cx1;
            this.rigy = cy2;
         }
         else {
            this.lefx = cx1;
            this.lefy = cy1;
            this.rigx = cx2;
            this.rigy = cy2;
         }
      }
      else if (sx < cx2) {
         if (sy < cy1) {
            this.lefx = cx1;
            this.lefy = cy1;
            this.rigx = cx2;
            this.rigy = cy1;
         }
         else if (sy < cy2) {
            seeme = false;
         }
         else {
            this.lefx = cx1;
            this.lefy = cy2;
            this.rigx = cx2;
            this.rigy = cy2;
         }
      }
      else {
         if (sy < cy1) {
            this.lefx = cx1;
            this.lefy = cy1;
            this.rigx = cx2;
            this.rigy = cy2;
         }
         else if (sy < cy2) {
            this.lefx = cx2;
            this.lefy = cy1;
            this.rigx = cx2;
            this.rigy = cy2;
         }
         else {
            this.lefx = cx2;
            this.lefy = cy1;
            this.rigx = cx1;
            this.rigy = cy2;
         }
      }
      
      int xx = Math.min(Math.min(lefx, rigx), srcx);
      int yy = Math.min(Math.min(lefy, rigy), srcy);
      
      lefx -= xx;
      lefy -= yy;
      rigx -= xx;
      rigy -= yy;
      srcx -= xx;
      srcy -= yy;
      
      int ww = Math.max(Math.max(lefx, rigx), srcx) +1;
      int hh = Math.max(Math.max(lefy, rigy), srcy) +1;
      
      bobox = new Rectangle (xx, yy, ww, hh);
   }
   
   public void componentMoved(ComponentEvent evt) {
      orient();
   }
   
}
