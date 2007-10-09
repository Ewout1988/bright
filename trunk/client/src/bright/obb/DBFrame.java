package bright.obb;

import java.awt.*;
import java.util.*;

public class DBFrame extends Frame implements Runnable {

   // Double buffering
   Image offScreenImage;
   Dimension dim;
   Graphics osg;
   
   boolean doubleBuffer = true;
   
   public DBFrame(String s) {
      super(s);
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   public void run() {
      try {
         Thread.sleep(200);
         repaint();
      }
      catch (InterruptedException e) {
         e.printStackTrace();
      }
   }
   
   public synchronized void paint(Graphics g) {
      if (!doubleBuffer) {
         super.paint(g);
         return;
      }
      Dimension size = getSize();
      if (!size.equals(dim) || offScreenImage == null || osg == null) {
         if (osg != null) {
            osg.dispose();
         }
         dim = size;
         offScreenImage = createImage(dim.width, dim.height);
         osg = offScreenImage.getGraphics();
   
         new Thread(this).start();
      }
      osg.setClip(g.getClip());
      super.paint(osg);
      g.drawImage(offScreenImage, 0, 0, this);
   }
   
}
