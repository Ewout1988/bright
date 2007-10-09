package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class NodeDrag extends MouseAdapter implements MouseMotionListener {

   int dragx;
   int dragy;
   
   Draggable node;
   
   boolean initi;
   
   public NodeDrag(Draggable node) {
      this(node, (Component)node);
   }
   
   public NodeDrag(Draggable node, Component handle) {
      this.node = node;
      handle.addMouseListener(this);
      handle.addMouseMotionListener(this);
   }
   
   public void mousePressed(MouseEvent evt) {
      dragx = evt.getX();
      dragy = evt.getY();
      initi = true;
      
      node.putOnTop();
   }
   
   public void mouseDragged(MouseEvent evt) {
      int x = evt.getX();
      int y = evt.getY();
      
      if (initi) {
         double dx = dragx - x;
         double dy = dragy - y;
         double dist2 = dx*dx + dy*dy;
         if (dist2 < 36) {
            return;
         }
         initi = false;
      }
      
      BezierCanvas ca = (BezierCanvas)node.getParent();
      Dimension casize = ca.getSize();
      Dimension nosize = node.getSize();
      Point p = node.getLocation();
      int nx = Math.min(casize.width - nosize.width, 
               Math.max(0, p.x + x - dragx));
      int ny = Math.min(casize.height - nosize.height, 
               Math.max(0, p.y + y - dragy));
      node.moveTo(nx, ny);
   }
   
   public void mouseMoved(MouseEvent evt) {
   
   }
   
   public interface Draggable {
      public void putOnTop();
      public void moveTo(int x, int y);
      public void addMouseListener(MouseListener l);
      public void addMouseMotionListener(MouseMotionListener l);
      public Container getParent();
      public Dimension getSize();
      public Point getLocation();
   }
}
