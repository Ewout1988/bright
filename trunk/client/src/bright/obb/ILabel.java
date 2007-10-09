package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class ILabel extends Component {

   public final static int LEFT = 0;
   public final static int CENTER = 1;
   public final static int RIGHT = 2;
   
   int align = RIGHT;
   int xmarg = 2;

   Color regcolor, regbgcolor;
   Color hlcolor, hlbgcolor;
   
   boolean highlight;
   boolean paintback;

   String name;
   int strx, stry, strw;
   
    
   public ILabel(String name) {
      this.name = name;
   
      addMouseListener(new MouseAdapter() {
         public void mouseEntered(MouseEvent evt) {
            setHighlight(true);
         }
         public void mouseExited(MouseEvent evt) {
            setHighlight(false);
         }
         public void mousePressed(MouseEvent evt) {
            deliver(new ActionEvent(this, 0, null));
         }
      });
   }
   
   ILabel() {
      this.name = "Che rules";
   }
   
   public void setHighlight(boolean hl) {
      this.highlight = hl;
      updateColors();
   }
      
   private void updateColors() {   
      setForeground(highlight ? hlcolor : regcolor);
      setBackground(highlight ? hlbgcolor : regbgcolor);
      paintback = (highlight && hlbgcolor != null) || 
                 (!highlight && regbgcolor != null);
      repaint();
   }
   
   public void setColors(Color rc, Color rbc, Color hc, Color hbc) {
      this.regcolor = rc;
      this.regbgcolor = rbc;
      this.hlcolor = hc;
      this.hlbgcolor = hbc;
      updateColors();
   } 
   
   public void setSize(int w, int h, Font font) {
      super.setSize(w, h);
      setFont(font);
      align(align);
   }
   
    public void align(int align) {
        alignText(align, getSize().width);    
    }
    
    public void alignText(int align, int w) {
        this.align = align;
      
        if (getFont() == null) return;
      
        FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(getFont());
        this.strw = fm.stringWidth(name);
        this.stry = fm.getAscent();
        switch (align) {
            case (LEFT)   : this.strx = xmarg;              break; 
            case (CENTER) : this.strx = (w - strw) / 2;     break;
            case (RIGHT)  : this.strx = w - strw - xmarg;   break;
        }
    }
   
   public void paint(Graphics g) {
      if (paintback) {
         Color bgc = getBackground();
         g.setColor(bgc);
         Dimension d = getSize();
         g.fillRect(0, 0, d.width, d.height);
      }
      
      g.setColor(getForeground());
      g.setFont(getFont());
      g.drawString(name, strx, stry);
   }
   
   private Vector actionListeners = new Vector();
   
   public void addActionListener(ActionListener l) {
      actionListeners.addElement(l);
      setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
   }
   
   public void removeActionListener(ActionListener l) { 
      actionListeners.removeElement(l);  
   }
   
   void deliver(ActionEvent e) {
      Enumeration en = actionListeners.elements();
      while (en.hasMoreElements()) {
         ActionListener l = (ActionListener)en.nextElement();
         l.actionPerformed(e);
      }
   }  
}
