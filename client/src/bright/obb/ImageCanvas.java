package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ImageCanvas extends Component {

   Image image;

   public ImageCanvas(Image img) {
      this.image = img;
   }
   
   public Dimension getPreferredSize() {
      return new Dimension(image.getWidth(null), image.getHeight(null));
   }
   
   public void update(Graphics g) {
      paint(g);
   }
   
   public void paint(Graphics g) {
      g.drawImage(image, 0, 0, null);
   }
   
   public static void main(String[] args) throws Exception {
      String imagename = args[0];
      
      Image img = Toolkit.getDefaultToolkit().getImage(imagename);
      MediaTracker tr = new MediaTracker(new Panel());
      tr.addImage(img, 0);
      tr.waitForAll();
      
      
      ImageCanvas canvas = new ImageCanvas(img);
      
      DBScrollPane pane = new DBScrollPane(canvas);
      
      final Frame f = new DBFrame("Fuck Java.");
      f.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent evt) {
            f.dispose();
            System.exit(0);
         }});
      f.setLayout(new BorderLayout());
      f.add(pane, "Center");
      f.setSize(640, 400);
      f.setVisible(true);
   }
}   
