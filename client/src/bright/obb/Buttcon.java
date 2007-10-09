package bright.obb;
 
import java.awt.*;
import java.util.*;
import java.awt.event.*;

class Buttcon extends Component implements MouseListener, Runnable {
      
      boolean stateDown = false;
      
      TimeBomb bomb;
      ActionListener listener;
      boolean repeat = false;
      
      { addMouseListener(this); }
      { setBackground(Color.black); }
      
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
      if (repeat) {
         bomb = new TimeBomb(350,
            new SimpleListener() {
               public void act(Object nil) {
                  new Thread(Buttcon.this).run();
               }
            });
         bomb.start();
      }
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
         g.setColor(getBackground());
         g.fillRect(0, 0, w-1, h-1);
      }
   
   public static class Close extends Buttcon {
      static Color bc = new Color(0xFFAAAA);
    
      { setBackground(bc);
        setForeground(Color.black); 
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
      }
      
      public void paint(Graphics g) {
         Dimension d = getSize();
         int w = d.width;
         int h = d.height;
         g.setColor(getBackground());
         g.fillRect(1, 1, w-3, h-3);
         g.setColor(getForeground());
         g.drawRect(0, 0, w-2, h-2);
         
      }
   }
   
}
