package bright.obb;
 
import java.awt.*;
import java.util.*;
import java.awt.event.*;

public class VDotNode extends Container implements NodeDrag.Draggable {

    static final int JAVAVERSION = 
        Integer.parseInt(System.getProperty("java.version").substring(2,3));

    int index;              // the index of this node as known by the inferer
    int chosenIndex = -1;   // current observed value of this node

    int basebarw = 40;      // normalized width of a prob bar


    Font valfont;           // current font for the option names
    Font probfont;          // current font for the prob strings 

    int basew, baseh;
    
    double[] probabilities;
    float zoom;
    
    VDotNode.Open openpart;
    VDotNode.Closed closedpart;
    Tabbie tabbie;
    
    Part nowon;

    String[] values;
   
    Color vtcol = Color.black;          // value text color
    Color htcol = Color.black;          // value text color highlighetd
    Color hbcol = new Color(0xBCBCBC);  // value bg color highlighted
    Color ftcol = Color.blue.darker();  // text color for the item "FREE"
    Color hfcol = ftcol.darker();       // the same highlighted
    Color labcol = new Color(0x440044); // name text color
   
    DotGraph.Node dotnode;          // the dotgraph node corresponding to this 
   
    public VDotNode(DotGraph.Node no, String[] values, Container rootComp) {
        this.dotnode = no;
        this.values = values;
        this.closedpart = new VDotNode.Closed(rootComp);
        this.openpart = new VDotNode.Open();
        this.tabbie = new Tabbie();
        add(closedpart);
        nowon = closedpart;

        tabbie.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent evt) {
                    softNode();
                }
            });
      
      MouseAdapter backer = new MouseAdapter() {
               public void mouseClicked(MouseEvent evt) {
                  if (evt.getClickCount() < 2) return;
                  dotnode.takeOriginalPosition();
               }
            };
        openpart.addMouseListener(backer);    
        closedpart.addMouseListener(backer);    
    }
   
    public void openNode() {
        int i = closedpart.pop.prevchoice;
        openpart.histo.select(i < openpart.histo.labels.length ? i : -1);
        
        nowon = openpart; nowon.calcSize();
        removeAll();
        openpart.calcSize();
        add(tabbie);
        add(openpart);
        openpart.setLocation(0,0);
        updateLocation();
        getParent().repaint();
    }
    
    public void closeNode() {
        int i = openpart.histo.cIndex;
        closedpart.pop.select(i > -1 ? i : openpart.histo.labels.length );
        
        nowon = closedpart; nowon.calcSize();
        removeAll();
        closedpart.calcSize();
        add(closedpart);
        closedpart.setLocation(0,0);
        updateLocation();
        getParent().repaint();
    }
    
    public void softNode() { }
    
    public void updateLocation() {    
        if (nowon == openpart) {
            Dimension d = openpart.getSize();
            int h = d.height + tabbie.getSize().height;
            int tw = (int)(d.width * 0.7); 
            setSize(d.width, h);
        
            tabbie.setLocation(tw, d.height-1);
        }
        else {
            setSize(nowon.getSize());
        }
        int xoff = basew - nowon.getSize().width;
        setLocation( (int)(dotnode.x * zoom - (basew - xoff)/2)
                   , (int)(dotnode.y * zoom - baseh/2) );
    }        
   
   
   public void putOnTop() {
       BezierCanvas parent = (BezierCanvas)getParent();
       parent.putNodeOnTop(this);
   }
   
   public void orient(Font font, Font vfont, Font probfont, float zoom) {
      int w = (int)(dotnode.w * zoom);
      int h = (int)(dotnode.h * zoom);
      int x = (int)(dotnode.x * zoom - w/2);
      int y = (int)(dotnode.y * zoom - h/2);
      
      this.zoom = zoom;
      this.basew = w;
      this.baseh = h;
      
      setFont(font);
      this.probfont = probfont;
      this.valfont = vfont;
      
      nowon.calcSize();
      tabbie.calcSize();
      
      updateLocation();      
   }
   
   public void setProbs(double[] p, boolean observed) {
      this.probabilities = (double[])p.clone();
      repaint();
   }
   
    float findZoom() {
        return ((BezierCanvas)getParent()).zoom;
    }
   
   /*
    public void paint(Graphics g) {
        Dimension size = getSize();
        g.setColor(Color.red);
        g.drawRect(0,0, size.width-1, size.height-1);
        
        
        super.paint(g);
    }*/
   
    public void paintShadow(Graphics g, Color c1, Color c2) {
        nowon.paintShadow(g, c1, c2);
        if (nowon == openpart) {
            Point p = tabbie.getLocation();
            g.translate(p.x,p.y);
            tabbie.paintShadow(g, c1, c2);
            g.translate(-p.x, -p.y);
        }
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
   
   public synchronized void moveTo(int x, int y) {
      Point oloc = getLocation();
      setLocation(x, y);
      
      float zoom = findZoom();
      float dx = (x -oloc.x)/zoom;
      float dy = (y -oloc.y)/zoom;
      dotnode.translate(dx, dy);
      
      getParent().repaint();
   }
   
    abstract class Part extends Container {
        
        abstract void paintShadow(Graphics g, Color c1, Color c2);
        abstract void calcSize();
    }
   
    class Closed extends Part {
        IPopChoice pop;         // popper for the choice in closed mode 
        IChoice cho;            // the choice that pops up
        ILabel label;           // the name field
        
        public Closed(Container rootComp) {
        
            // make name label
            label = new ILabel(dotnode.label);
            label.setColors(labcol, null, Color.white, labcol);
            add(label);
            label.align(ILabel.CENTER);
            label.addActionListener(new ActionListener() {
                     public void actionPerformed(ActionEvent evt) {
                        openNode();
                        putOnTop();
                     }});

        
           // make value labels
            int n = values.length;
            ILabel[] labs = new ILabel[n+1];
            for (int i = 0; i < n; ++i) {
               labs[i] = new ILabel(values[i]);
               labs[i].setColors(vtcol, null, htcol, hbcol);
               labs[i].align(ILabel.RIGHT);
            }
            labs[n] = new ILabel("FREE");
            labs[n].setColors(ftcol, null, hfcol, hbcol);

            // make the pop choice
            this.cho = new IChoice(labs);
            this.cho.select(n);
            this.pop = new IPopChoice(cho, rootComp);
            this.add(pop);
        
            cho.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        int i = cho.cIndex < values.length ? cho.cIndex : -1;
                        if (i != chosenIndex) {
                            chosenIndex = i;
                            deliver(evt);
                        }
                    }});
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
  
        }
        
        public void paint(Graphics g) {
            Dimension size = getSize();
            g.setColor(getBackground());
            g.fillOval(0, 0, size.width-1, size.height-1);
            g.setColor(getForeground());
            g.drawOval(0, 0, size.width-1, size.height-1);
            super.paint(g);
        }
        
        public void paintShadow(Graphics g, Color c1, Color c2) {
            Dimension size = getSize();
            g.setColor(c1);
            g.fillOval(0, 0, size.width-1, size.height-1);
            g.setColor(c2);
            g.drawOval(0, 0, size.width-1, size.height-1);        
        }
        
        void calcSize() {
            int w = basew, h = baseh;
            setSize(w, h);
            Font font = VDotNode.this.getFont();
            FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
            int fonth =  fm.getHeight() - (JAVAVERSION > 1 ? 4 : 0); 
            int fh = fonth;
            int fy = (JAVAVERSION <= 1) ? 1+ fonth/3 : 2;
      
            int labw = fm.stringWidth(label.name);
            label.setSize(labw+4, fonth, font);
            label.setLocation( (w - labw- 4)/2, fy);
   
            fm = Toolkit.getDefaultToolkit().getFontMetrics(valfont);
            int vfh =  fm.getHeight() - (JAVAVERSION > 1 ? 2 : 0); 
      
            int vfw = 0;
            int n = cho.labels.length;
            for (int i = 0; i < n; ++i) {
                vfw = Math.max(vfw, fm.stringWidth(cho.labels[i].name));
            }
            int vfy = 1+ vfh/3;
            for (int i = 0; i < n; ++i) {
                cho.labels[i].setSize(vfw+2, vfh, valfont);
            }
            cho.resize();
            pop.setSize(vfw+2, vfh);
            pop.takeForm(cho.labels[cho.cIndex]);
            cho.returnColors(pop);

            pop.setLocation( (w - vfw - 2)/2, fy + fh );
        }
    }     
    
   
    class Open extends Part {
        double[] probs;
        ILabel label;           // the name field
        IChoice histo;          // the histogram menu
        
        double norm = 1.0;      // current normalization factor
        
        public Open() {
            label = new ILabel(dotnode.label);
            label.setColors(labcol, null, Color.white, labcol);
            add(label);
            label.align(ILabel.CENTER);
            label.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                        closeNode();
                        putOnTop();
                    }});
                     
            // make the histograms
            IHisto[] hitms = new IHisto[values.length];
            for (int i = 0; i < hitms.length; ++i) {
                hitms[i] = new IHisto(values[i]);
                hitms[i].setColors(vtcol, null, htcol, hbcol);
                hitms[i].setBarColor(new Color(0x96557C));
            }
            this.histo = new IChoice(hitms);
            this.histo.allownochoice = true;
            this.histo.paintback = false;
            this.histo.select(-1);  
            histo.addActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent evt) {
                       if (histo.cIndex != chosenIndex) {
                             chosenIndex = histo.cIndex;
                             deliver(evt);
                       }
                    }});
                    
            add(histo);
            
            setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        }
        
        void calcSize() {
            int w = basew, h = baseh;
            Font font = VDotNode.this.getFont();
            FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(font);
            int fonth =  fm.getHeight() - (JAVAVERSION > 1 ? 4 : 0); 
            int fh = fonth;
            int fy = (JAVAVERSION <= 1) ? 1+ fonth/3 : 2;
            int labw = fm.stringWidth(label.name);
            
            fm = Toolkit.getDefaultToolkit().getFontMetrics(valfont);
            int vfh =  fm.getHeight() - (JAVAVERSION > 1 ? 2 : 0); 

            int barw = (int)(zoom/100 * basebarw);
      
            IHisto[] hitms = (IHisto[])histo.labels;
            int vfw = 0;
            for (int i = 0; i < hitms.length; ++i) {
                vfw = Math.max(vfw, fm.stringWidth(hitms[i].name));
            }
            int hww = Math.max(vfw+2+barw, w - 2);
            for (int i = 0; i < hitms.length; ++i) {
                hitms[i].setBar(hitms[i].prob, norm, barw, probfont);
                hitms[i].setSize( hww, vfh, valfont);
            }
            histo.resize();
            Dimension hsiz = histo.getSize();
            histo.setLocation( (hww - hsiz.width)/2+1, fy + fh );
      
            setSize(hww + 2, fy + fh + hsiz.height);

            label.setSize(labw+4, fonth, font);
            label.setLocation( (hww - labw- 2)/2, fy);
        }
   
        
        public void paint(Graphics g) {
            if (probs != probabilities) {
                updateProbs();
            }
            Dimension size = getSize();
            int w = size.width -1;             
            int areah = size.height - baseh / 2;  
            g.setColor(getBackground());
            g.fillArc(0, 0, w, baseh, 0, 180);
            g.fillRect(0, baseh / 2, w, areah);
            int th = baseh / 2; 
            int nh = size.height -1;
            int nw = w;
            g.setColor(getForeground());
            g.drawArc(0, 0, w, th * 2, 0, 180);
            g.drawLine(0,  th, 0,  nh);
            g.drawLine(nw, th, nw, nh);
            g.drawLine(0,  nh, nw, nh);    
            super.paint(g);
        }
        
         public void paintShadow(Graphics g, Color c1, Color c2) {       
            Dimension size = getSize();
            g.setColor(c1);
            int w = size.width -1;
            int th = size.height /2; 
            int areah = size.height - th;
            int nh = size.height -1;
            int nw = w;
            g.fillArc(0, 0, w, nh + 1, 0, 180);
            g.fillRect(0, th, w, areah);
            g.setColor(c2);
            g.drawArc(0, 0, w, nh + 1, 0, 180);
            g.drawLine(0,  th, 0,  nh);
            g.drawLine(nw, th, nw, nh);
            g.drawLine(0,  nh, nw, nh);
        }

        void updateProbs() {
            double[] p = this.probs = probabilities;
            // Find maximum probability
            int n = p.length;
            double max = 0;
            for (int i = 0; i < n; ++i) {
                max = (max > p[i]) ? max : p[i];
            }
            // Calculate normalization factor
            this.norm = 1 / Math.sqrt(max);
            int barw = (int)(zoom/100 * basebarw);
            try {
				for (int i = 0; i < n; ++i) {
					((IHisto)histo.labels[i]).setBar(p[i], norm, barw, probfont);
				}
            } catch (Exception e) {
            	System.err.println("Problem: " + e.getMessage()
            					   + " " + dotnode.label);
            }
        }
    }
    
    int[] ib = (int[])(new int[3]).clone();

/*
    { // ib.length = 10; 
      int x = new Vector().length; }
 */
       
    
    class SoftPanel extends Part {
        
        void calcSize() {
            
        }
        public void paint(Graphics g) {
         /*   g.setColor(highlight ? hbgc : bgc);
            Dimension d = getSize();
            g.fillPolygon(xs,ys,4);
            g.setColor(getForeground());
            g.drawPolyline(xs,ys,4);
        */
        }
    
        public void paintShadow(Graphics g, Color c1, Color c2) {
          /*  g.setColor(c1);
            Dimension d = getSize();
            g.fillPolygon(xs,ys,4);
            g.setColor(c2);
            g.drawPolyline(xs,ys,4);
        */
        }
    }
    
    
    class Tabbie extends Part {
        int[] xs, ys;
        Color bgc = new Color(0xD8D8D8);
        Color hbgc = Color.black;
        boolean highlight;
        
        {   setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            addMouseListener(new MouseAdapter() {
                    public void mouseEntered(MouseEvent evt) {
                        highlight = true;
                        repaint();
                    }
                    public void mouseExited(MouseEvent evt) {
                        highlight = false;
                        repaint();
                    }
                });
        
        }
        
        void calcSize() {
            int w = (int)(zoom / 100 * 20);
            int h = (int)(zoom / 100 * 15);
            setSize(w, h);
            int os = (int)(zoom / 100 * 4);
            xs = new int[] { 0, os, w-os-1, w-1 };
            ys = new int[] { 0, h-1, h-1, 0 } ;
        }
        public void paint(Graphics g) {
            g.setColor(highlight ? hbgc : bgc);
            Dimension d = getSize();
            g.fillPolygon(xs,ys,4);
            g.setColor(getForeground());
            g.drawPolyline(xs,ys,4);
        }
        
        public void paintShadow(Graphics g, Color c1, Color c2) {
            g.setColor(c1);
            Dimension d = getSize();
            g.fillPolygon(xs,ys,4);
            g.setColor(c2);
            g.drawPolyline(xs,ys,4);
        }
    }
}
