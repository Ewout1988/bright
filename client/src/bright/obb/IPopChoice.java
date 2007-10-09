package bright.obb;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

public class IPopChoice extends ILabel {

   IChoice choice;

   Container rootcomp;

   int prevchoice;

   public IPopChoice(IChoice choice, Container rootComp) {
      super("PopChoice");
      
      this.rootcomp = rootComp;
      this.choice = choice;
      this.prevchoice = choice.cIndex;
      choice.paintback = true;
      resize();
      takeForm(choice.labels[choice.cIndex]);
      
        addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent evt) {
                    popup();
                }
            });
        choice.addActionListener(new ActionListener() {
               public void actionPerformed(ActionEvent evt) {
                  popdown();
               }
            });
         
      MouseListener list = new MouseAdapter() {
            TimeBomb bomb;
            public void mouseEntered(MouseEvent evt) {
               if (bomb != null) {
                  bomb.disarm();
               }
            }
            public void mouseExited(MouseEvent evt) {
                  bomb = new TimeBomb(800,
                  new SimpleListener() {
                     public void act(Object obj) {
                        popdown();
                     }
                  });
                  bomb.start();
               
            }
         };
          
         
      choice.addMouseListener(list);
      int n = choice.labels.length;
      for (int i = 0; i < n; ++i) {
         choice.labels[i].addMouseListener(list);
      }
   }
   
   public void resize() {
      int w = 0, h = 0;
      int n = choice.labels.length;
      for (int i = 0; i < n; ++i) {
         ILabel l = choice.labels[i];
         Dimension ls = l.getSize();
         w = Math.max(w, ls.width);
         h = Math.max(h, ls.height);
      }
      setSize(w, h);
   }
   
   public void takeForm(ILabel lab) {
      this.name = lab.name;
      setColors(lab.regcolor, lab.regbgcolor, lab.hlcolor, lab.hlbgcolor);
      setFont(lab.getFont());
      align(ILabel.CENTER);
      
   }
   
   public void select(int i) {
      if (i != prevchoice) {
         choice.select(i);
         prevchoice = i;
         takeForm(choice.labels[i]);
         choice.returnColors(this);
         repaint();
      }
   }
      
   void popup() {
   
      Point rp = rootcomp.getLocationOnScreen();
      Point p  = getLocationOnScreen();
      Point sp = choice.labels[choice.cIndex].getLocation();
      
      choice.setLocation(p.x - rp.x - sp.x, p.y - rp.y - sp.y);  
      choice.resize();
      rootcomp.add(choice, 0);
      choice.repaint();
      getParent().repaint();
      
      disableEvents(AWTEvent.MOUSE_EVENT_MASK);
      setHighlight(false);
   }
   
   void popdown() {
      select(choice.cIndex);
      rootcomp.remove(choice);
      enableEvents(AWTEvent.MOUSE_EVENT_MASK);
      Point loc = choice.getLocation();
      Dimension d = choice.getSize();
      
      rootcomp.repaint(loc.x, loc.y, d.width, d.height); 
   }
}
     
