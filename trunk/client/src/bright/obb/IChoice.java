package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class IChoice extends Container {
   
   int cIndex = -1;
   
   Color chcolor, chbgcolor;
   Color chhlcolor, chhlbgcolor; {
      chcolor = Color.black;
      chbgcolor = new Color(0xAAAACC);
      chhlcolor = Color.white;
      chhlbgcolor = chbgcolor;
   };
   
   Color origcolor, origbgcolor;
   Color orighlcolor, orighlbgcolor;
   
   ILabel[] labels;
   
   boolean allownochoice;
   
   boolean paintback = true; {
      setBackground(Color.white);
      setForeground(Color.black);
   }
   
   public IChoice(ILabel[] labels) {
      this.labels = labels;
      int n = labels.length;
      for (int i = 0; i < n; ++i) {
         ILabel l = labels[i];
         final int j = i;
         l.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent evt) {
                  if (evt.getID() == 0) {
                     select( (cIndex == j) ? -1 : j );
                  }
                  deliver(new ActionEvent(this, evt.getID(), ""+j));
               }
            });
         add(l);
      }
   }
   
   public void select(int i) {
      if (i == -1 && !allownochoice) {
         return;
      }
      if (cIndex > -1) {
         returnColors(labels[cIndex]);
      }
      if (i > -1) {
         selectedColors(labels[i]);
      }
      cIndex = i;
   }
   
   void returnColors(ILabel lab) {
      lab.setColors(origcolor, origbgcolor, orighlcolor, orighlbgcolor);
   }
   
   private void selectedColors(ILabel lab) {
      this.origcolor       = lab.regcolor;
      this.origbgcolor     = lab.regbgcolor;
      this.orighlcolor     = lab.hlcolor;
      this.orighlbgcolor   = lab.hlbgcolor;
      lab.setColors(chcolor, chbgcolor, chhlcolor, orighlbgcolor);
   }
         
   public void resize() {   
      int n = labels.length;
      int w = 0, h = 1;
      for (int i = 0; i < n; ++i) {
         ILabel l = labels[i];
         l.setLocation(1, h);
         Dimension ls = l.getSize();
         w  = (ls.width > w) ? ls.width : w;
         h += ls.height;
      }
      for (int i = 0; i < n; ++i) {
         ILabel l = labels[i];
         Dimension ls = l.getSize();
         l.setSize(w, ls.height);
      }  
      setSize(w+2, h+1);
   }
   
   public void paint(Graphics g) {
      if (paintback) {
         Dimension d = getSize();
         g.setColor(getBackground());
         g.fillRect(1, 1, d.width-2, d.height-2);
         g.setColor(getForeground());
         g.drawRect(0, 0, d.width-1, d.height-1);
      }
      super.paint(g);
   }
   
   private Vector actionListeners = new Vector();
   
   public void addActionListener(ActionListener l) {
      actionListeners.addElement(l);  
   }
   
   public void removeActionListener(ActionListener l) { 
      actionListeners.removeElement(l);  
   }
   
   private void deliver(ActionEvent e) {
      Enumeration en = actionListeners.elements();
      while (en.hasMoreElements()) {
         ActionListener l = (ActionListener)en.nextElement();
         l.actionPerformed(e);
      }
   }  
}
