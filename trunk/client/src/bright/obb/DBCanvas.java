package bright.obb;

import java.awt.*;
import java.util.*;

public class DBCanvas extends Container {

   // Double buffering
   Image offScreenImage;
   Dimension dim;
   Graphics osg;
   
   boolean doubleBuffer = true;
   
   public void update(Graphics g) {
      paint(g);
   }
   
   public void paint(Graphics g) {
      if (!doubleBuffer) {
         super.paint(g);
         return;
      }
      Dimension size = getSize();
      if (!size.equals(dim) || offScreenImage == null || osg == null) {
         dim = size;
         offScreenImage = createImage(dim.width, dim.height);
         osg = offScreenImage.getGraphics();
      }
      super.paint(osg);
      g.drawImage(offScreenImage, 0, 0, this);
   }
   

}
