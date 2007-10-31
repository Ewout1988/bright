package bright.obb;

import bright.gui.Bright;
import bright.obb.infer.*;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.*;
import java.net.*;
import java.applet.*;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import bright.obb.infer.Answun;
import bright.obb.infer.Inferer;
import bright.obb.infer.JoinTree;

public class IApplet extends Applet {

   Cursor on = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);
   Cursor off = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
   String command = "Launch the Bayes Browser";
   String running = "Bayes Browser running";
   String label = command;
   Color linkc, basicc;
   
   Panel zbp;
   DBScrollPane pane;
   ZoomBox zoombox;
   BezierCanvas canv;
   
   boolean exit;
   
   public static void main(String[] args) throws IOException {
   
      // Read .pla file
      BufferedReader r = new BufferedReader(new FileReader(args[0]+".pla"));
      DotGraph dg = new DotGraph(r);
      
      // read .vd file
      BufferedReader vr = new BufferedReader(new FileReader(args[0]+".vd"));
      ValGraph vg = new ValGraph(vr);
   
      // read .qjt file
      BufferedReader jr = new BufferedReader(new FileReader(args[0]+".qjt"));
      Inferer ifr = new Inferer(new JoinTree(jr));
   
      // read .dpa file and .str file
      BufferedReader dr = new BufferedReader(new FileReader(args[0]+".dpa"));
      BufferedReader sr = new BufferedReader(new FileReader(args[0]+".str"));
      Answun answun = new Answun(ifr, dr, sr);
   
      // read bird.gif
      Image bird = createImage("bird.gif");
                  
   
      IApplet app = new IApplet();
      app.setup(dg, vg, ifr, bird, answun);
      app.launch();
      app.exit = true;
   }

   private static Image createImage(String path) throws IOException {
       path = "images/" + path;
       java.net.URL imgURL = Bright.class.getResource("/" + path);

       if (imgURL != null) {
           return ImageIO.read(imgURL);
       } else {
           return ImageIO.read(new File(path));
       }
   }

   public static void run(File vdFile, File strFile, File plaFile, File qjtFile, File dpaFile)
       throws IOException
   {
       // Read .pla file
       BufferedReader r = new BufferedReader(new FileReader(plaFile));
       DotGraph dg = new DotGraph(r);
       
       // read .vd file
       BufferedReader vr = new BufferedReader(new FileReader(vdFile));
       ValGraph vg = new ValGraph(vr);
    
       // read .qjt file
       BufferedReader jr = new BufferedReader(new FileReader(qjtFile));
       Inferer ifr = new Inferer(new JoinTree(jr));
    
       // read .dpa file and .str file
       BufferedReader dr = new BufferedReader(new FileReader(dpaFile));
       BufferedReader sr = new BufferedReader(new FileReader(strFile));
       Answun answun = new Answun(ifr, dr, sr);
    
       // read bird.gif
       Image bird = Toolkit.getDefaultToolkit().getImage("images" + File.separator + "bird.gif");
    
       IApplet app = new IApplet();
       app.setup(dg, vg, ifr, bird, answun);
       app.launch();
       app.exit = false;
   }
   
   public void init() {
      try {
         String cols;
         cols = getParameter("bgcolor");
         if (cols == null) cols = "0xFFFFFF";
         setBackground(new Color(Integer.valueOf(cols, 16).intValue()));
      
         cols = getParameter("linkcolor");
         if (cols == null) cols = "0x0000AA";
         linkc = new Color(Integer.valueOf(cols, 16).intValue());
         
         setFont(new Font(
               getParameter("font"),
               Font.BOLD,
               Integer.parseInt(getParameter("fontsize"))));
         
         setForeground(linkc);
         setCursor(on);
         addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                  if (label == command) {
                     label = running;
                     setCursor(off);
                     setForeground(basicc);
                     repaint();
                     launch();
                  }
               }
            });
      
      
         URL docbase = getDocumentBase();
       //  System.err.println(docbase);
         
         BufferedReader plar, vdr, qjtr, dpar, strr;
         
         String allfiles = getParameter("data");
         if (allfiles == null) {
            plar = new BufferedReader(new InputStreamReader(
                    new URL(docbase,getParameter("plafile")
                        ).openConnection().getInputStream()));
            vdr  = new BufferedReader(new InputStreamReader(
                    new URL(docbase,getParameter("vdfile")
                        ).openConnection().getInputStream()));
            qjtr = new BufferedReader(new InputStreamReader(
                    new URL(docbase,getParameter("qjtfile")
                    ).openConnection().getInputStream()));
            dpar = new BufferedReader(new InputStreamReader(
                    new URL(docbase,getParameter("dpafile")
                    ).openConnection().getInputStream()));
            strr = new BufferedReader(new InputStreamReader(
                    new URL(docbase,getParameter("strfile")
                    ).openConnection().getInputStream()));
         }   
         else {
            plar = vdr = qjtr = dpar = strr = new CatReader(
                new InputStreamReader( 
                    new URL(docbase, allfiles
                    ).openConnection().getInputStream()));
         } 
         DotGraph dg = new DotGraph(plar);
         ValGraph vg = new ValGraph(vdr);
         
 //      System.err.println("Next: making the inferer! Allfiles = " + allfiles);
         Inferer ifr = new Inferer( new JoinTree(qjtr) );
 //      System.err.println("Inferer was succesful");
         
         Answun answun = new Answun(ifr, dpar, strr);
         
         String birdpar = getParameter("bird");
         Image bird = (birdpar == null) 
                     ? getImage(getCodeBase(), "bird.gif")
                     : getImage(getDocumentBase(), birdpar);
         
         setup(dg, vg, ifr, bird, answun);
      }
      catch(Exception e) {
         e.printStackTrace();
      }
   }
   
    private static class CatReader extends BufferedReader {
        String line;
        public CatReader(InputStreamReader r) {
            super(r);
        }
        public String readLine() throws IOException {
            line = super.readLine();
            return "#$#$#$#END_OF_BLOCK".equals(line) ? null : line;
        }
        public void close() throws IOException {
            while (line != null && !"#$#$#$#END_OF_BLOCK".equals(line)) {
                readLine();
            }
            if (line == null) {
                super.close();
            }
        }
    } 
   
   public void setup(DotGraph dg, ValGraph vg, Inferer ifr, 
            Image bird, Answun answun) {
     
      // create the canvas component
      canv = new BezierCanvas(dg, vg, ifr, answun);
      canv.setBackground(new Color(0xFFF0CC));
      
      // a scroll pane for the canvas
      pane = new DBScrollPane(canv);
      pane.setBackground(new Color(0xDDD0AA));
      canv.pane = pane;
      
      canv.addMouseListener(
         new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
               if (evt.getClickCount() < 2) return;
               double x = evt.getX() /canv.zoom;
               double y = evt.getY() /canv.zoom;
               
               double newZoom = 100;
               zoombox.setZoom(newZoom);
               canv.setZoom((float)newZoom);
               canv.centerAt((int)(x * newZoom), (int)(y*newZoom));
            }
         });    
      
      pane.addComponentListener(
         new ComponentAdapter() {
            public void componentResized(ComponentEvent evt) {
               zoombox.setMinZoom(canv.calcGoodZoom());
               
            }
         });
      
      MediaTracker track = new MediaTracker(this);
      track.addImage(bird, 0);
      try {
         track.waitForAll();
      }
      catch (InterruptedException e) { e.printStackTrace(); }
      
      zoombox = new ZoomBox(bird);
      zoombox.canv = canv;
      zoombox.setSize(bird.getWidth(this), 180);
      zoombox.setBackground(Color.white);
   }
   
   public void launch() {
      // the window
      String title;
        try {
            if ( (title = getParameter("title")) == null ) {
                title = "The Amazing Bayes Browser";
            }
        } catch (Exception e) {
            title = "The Amazing Bayes Browser";
        }
         
      final Frame f = new DBFrame(title);
      f.addWindowListener(new WindowAdapter() {
         public void windowClosing(WindowEvent evt) {
            if (exit) {
                System.exit(0);
            }
            f.dispose();
            label = command;
            setForeground(linkc);
            setCursor(on);
            repaint();
         }});
         
      Container pan = new Container() {
            public void setBounds(int x, int y, int w, int h) {
               super.setBounds(x, y, w, h);
               pane.setBounds(0, 0, w, h);
            }
         };
      pan.setLayout(null);
      pan.add(zoombox);
      pan.add(pane);
         
      f.setLayout(new BorderLayout());
      f.add(pan, "Center");
      
      Dimension ss = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension ns = canv.getPreferredSize();
      
      int w = Math.min( ss.width/3*2, ns.width+10);
      int h = Math.min( ss.height/5*4, ns.height+40);
      
      f.setSize(w, h);
      
      double zom = canv.calcGoodZoom();
      zoombox.setZoom(zom);
      canv.setProbs();
      
      f.setVisible(true);
      
   }
   
   public void paint(Graphics g) {
   
      Dimension d = getSize();
      int w = d.width;
      int h = d.height;
      
      
      FontMetrics fm = Toolkit.getDefaultToolkit().getFontMetrics(getFont()); 
      int fw = fm.stringWidth(label);
      
      g.drawLine( 4, h-7, 4 + fw, h-7 );
      g.drawString(label, 4, h-8);
   }
}
