package bright.obb;

import java.awt.*;
import java.util.*;

public class Bezier {

   // the control point locations
   float[] cx;
   float[] cy;
   
   // draw-through points, current zoom
   int[] px;
   int[] py;
   int[] aheadx, aheady;
  
   // the bounding box of the curve, current zoom
   Rectangle bobox;
   
   Color color = new Color(0x440044);
   Color bcolor = color;
   
   float baw = 5f, bam = 12f;
   float baseaw =  6.0f;
   float baseml = 12.0f;
   int lwdth = 2;
   
   // array of coefficients
   double[] coeff;
   
   DotGraph.Edge dotedge;
   
   boolean origplace = true;
   double zoomi;
   int nopi;
   
   public Bezier(float[] cx, float[] cy) {
      this.cx = cx;
      this.cy = cy;
  
      // calculate coefficients
      int n = cx.length;
      coeff = new double[n];
      for (int k = 0; k < n; ++k) {
         double c = 1;
         for (int i = n-1; i >= k+1; --i) 
            c *= i;
         for (int i = n-k-1; i >= 2; --i) 
            c /= i;
         coeff[k] = c;
      }
   }
   
   public void setZoom(double zoom) {
      this.baseaw =(float)( zoom / 100  *  baw );
      this.baseml =(float)( zoom / 100  *  bam );
      this.lwdth = (zoom > 50) ? 2 : 1;
   
      int nop =  1+ ((cx.length-1)/3) *4;
      calculateWTPoints(nop, zoom);
      calculateBoundingBox();
   }
   
   void calcStraight(double zoom) {
   
      px = new int[2];
      py = new int[2];
      
      DotGraph.Node sour = dotedge.source;
      DotGraph.Node targ = dotedge.target;
      
      float sx = sour.x;
      float sy = sour.y;
      px[0] = (int)(sx * zoom);
      py[0] = (int)(sy * zoom);
      
      Doint doint = intersection(sx, sy, targ.x, targ.y, targ.w, targ.h);
     
      px[1] = (int)(doint.x * zoom);
      py[1] = (int)(doint.y * zoom);
   
      calcArrow( (float)doint.x, (float)doint.y, sx, sy, zoom);
   }
   
   void updateForm() {
      calculateWTPoints(nopi, zoomi);
      calculateBoundingBox();
   }
   
   void calculateWTPoints(int nop, double zoom) {
      
      this.zoomi = zoom;
      this.nopi = nop;
      if (dotedge.moved) {
         calcStraight(zoom);
         return;
      }
      
      int n = cx.length;
      DotGraph.Node nod = dotedge.target;
      Doint doint = intersection(cx[n-2], cy[n-2], nod.x, nod.y, nod.w, nod.h);
      
      cx[n-1] = (float)doint.x;
      cy[n-1] = (float)doint.y;
      
      px = new int[nop];
      py = new int[nop];
      
      for (int i = 0; i < nop; ++i) {
         float u = (float)i / (nop -1);
         float x = 0, y = 0;
         for (int k = 0; k < n; ++k) {
            float blend = (float)(
               coeff[k] * Math.pow(u, k) * Math.pow(1-u, n-k-1));
            x += cx[k] * blend;
            y += cy[k] * blend;
         }
         px[i] = (int)(x * zoom);
         py[i] = (int)(y * zoom);
      }
      
     
      
      float x1 = (float)doint.x;
      float y1 = (float)doint.y;
      float x2 = cx[n-2];
      float y2 = cy[n-2];
      
      calcArrow(x1, y1, x2, y2, zoom);
   }
   
   void calcArrow(float x1, float y1, float x2, float y2, double zoom) {
      aheadx = new int [3];
      aheady = new int [3];
      
      double ml = baseml / zoom;
      double w =  baseaw / zoom;
      
      double dx = x1 - x2;
      double dy = y1 - y2;
     
      double alpha = Math.atan2(dy, dx);
      double beta  = alpha + Math.PI/2;
      double ddx = (w * Math.cos(beta) );
      double ddy = (w * Math.sin(beta) );
      double mx = x1 - (ml *Math.cos(alpha));
      double my = y1 - (ml *Math.sin(alpha));
      
      aheadx[0] = (int)((mx + ddx)* zoom);
      aheady[0] = (int)((my + ddy)* zoom);
      
      aheadx[2] = (int)((mx - ddx)* zoom);
      aheady[2] = (int)((my - ddy)* zoom);
      
      aheadx[1] = (int)(x1 *zoom);
      aheady[1] = (int)(y1 *zoom);
   }
   
   static Doint intersection(double stx, double sty, double nx, 
      double ny, double nw, double nh) {
    
      double x0 = stx - nx;
      double y0 = sty - ny;
      if (x0 == 0) {
         Doint doint = new Doint();
         doint.x = nx;
         doint.y = (y0 > 0) ? ny + nh/2 : ny - nh/2; 
         return doint;
      }
      
      double k = (double)y0/x0;
      double a = (double)nw / 2;
      double b = (double)nh / 2;
      
      double a2 = a*a;
      double b2 = b*b;
      double k2 = k*k;
      
      double x2 = (a2*b2) / (a2*k2 + b2);
      
      double x = Math.sqrt(x2);
      
      double xp = (x0 < 0) ? -x : x; 
      
      double yp = xp * k;
      
      Doint doint = new Doint();
      doint.x = nx + xp;
      doint.y = ny + yp;
      return doint;
   }
   
   private static class Doint {
      double x, y;
   }
   
   
   void calculateBoundingBox() {
      int maxx, maxy, minx, miny;
      maxx = maxy = Integer.MIN_VALUE;
      minx = miny = Integer.MAX_VALUE;
      int n = px.length;
      for (int i = 0; i < n; ++i) {
         minx = Math.min(minx, px[i]);
         maxx = Math.max(maxx, px[i]);
         miny = Math.min(miny, py[i]);
         maxy = Math.max(maxy, py[i]);
      }
      int m = aheadx.length;
      for (int i = 0; i < m; ++i) {
         minx = Math.min(minx, aheadx[i]);
         maxx = Math.max(maxx, aheadx[i]);
         miny = Math.min(miny, aheady[i]);
         maxy = Math.max(maxy, aheady[i]);
      }
      this.bobox = new Rectangle(minx, miny, maxx-minx, maxy-miny);
   }
   
   

   public void paint(Graphics g) {
      g.setColor(color);
      paintMe(g);
   }
   
   void paintMe(Graphics g) {
      int n = px.length;
      for (int i = 0; i < n -1; ++i) {
         g.drawLine(px[i], py[i], px[i+1], py[i+1]);
         if (lwdth > 1) {
            g.drawLine(px[i], py[i]+1, px[i+1], py[i+1]+1);
            g.drawLine(px[i]+1, py[i], px[i+1]+1, py[i+1]);
            g.drawLine(px[i]+1, py[i]+1, px[i+1]+1, py[i+1]+1);
         }
      }
      g.fillPolygon(aheadx, aheady, 3);
   }
   
   public void setColor(Color color) {
      this.color = color;
      this.bcolor = color.darker();
   }
         
}
