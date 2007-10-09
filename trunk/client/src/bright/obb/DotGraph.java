package bright.obb;

import java.io.*;
import java.util.*;

public class DotGraph {

   float scale, width, height;

   float marg = 0.25f;

   Vector nodes = new Vector();
   Vector edges = new Vector();

   Hashtable nodelookup = new Hashtable();
   
   // Constructs a DotGraph out of a .pla file given as a Reader object
   public DotGraph(BufferedReader r) throws IOException {
    
      String s, t;          // current line (s) and current token (t)
      StringTokenizer st;   // tokenizer for current line
      
      // Read first line
      s = r.readLine();
      st = new StringTokenizer(s);
      st.nextToken();               // dump the word "graph"
      scale  = readFloat(st);
      width  = readFloat(st) + 2*marg + 0.5f;
      height = readFloat(st) + 2*marg + 0.5f;
      
      // Read nodes
      for ( ;; ) {
         s = r.readLine();
         st = new StringTokenizer(s);
         if (!(t = st.nextToken()).equals("node"))
            break;
         Node node = new Node();
         node.id = st.nextToken();
         node.x = node.origx = readFloat(st) + marg;
         node.y = node.origy = height - (readFloat(st) + marg + 0.5f);
         node.w = readFloat(st);
         node.h = readFloat(st);
         node.label = unquote(st);
         nodes.addElement(node);
         nodelookup.put(node.id, node);
      }
      
      // read edges
      while ( !t.equals("stop") ) {
         Node sourcenode = (Node)nodelookup.get(st.nextToken());
         Node targetnode = (Node)nodelookup.get(st.nextToken());
         
         int n = readInt(st) + 1;
         Edge edge = new Edge();
         edge.source = sourcenode;
         edge.target = targetnode;
         edge.cx = new float[n+1];
         edge.cy = new float[n+1];
         edge.cx[0] = sourcenode.x;
         edge.cy[0] = sourcenode.y;
         for (int i = 1; i < n; ++i) {
            edge.cx[i] = readFloat(st) + marg;
            edge.cy[i] = height - readFloat(st) - marg - 0.5f;
         }
         
         edges.addElement(edge);
         
         sourcenode.fromEdges.addElement(edge);
         targetnode.toEdges.addElement(edge);
         
         s = r.readLine();
         st = new StringTokenizer(s);
         t = st.nextToken();
      }
      r.readLine();
   }
   
    private static String unquote(StringTokenizer st) {
        String s = st.nextToken();
        if (s.startsWith("\"")) {
            while(! s.endsWith("\"")) {
                s += " " + st.nextToken();
            }
            return s.substring(1, s.length()-1); 
        }
        return s;
    }
   
   private static float readFloat(StringTokenizer st) {
      return new Float(st.nextToken()).floatValue();
   }
   
   private static int readInt(StringTokenizer st) {
      return Integer.parseInt(st.nextToken());
   }
   
   public static void main(String[] args) throws IOException {
      BufferedReader r = new BufferedReader(new FileReader(args[0]));
      DotGraph dg = new DotGraph(r);
      System.err.println(dg.nodes);
      System.err.println(dg.edges);
      
   }
   
   public class Node {
      String label;
      String id;
      float x, y, w, h;
      float origx, origy;
      Vector fromEdges = new Vector();
      Vector toEdges = new Vector();
      VDotNode vnode;
      
      boolean moved;
      
      public String toString() {
         return label + "( "+ x+" "+y+" )";
      }
      
      public void translate(float dx, float dy) {
         moved = true;
         int n = fromEdges.size();
         for (int i = 0; i < n; ++i) {
            Edge e = (Edge)fromEdges.elementAt(i);
            e.moved = true;
            e.bezier.updateForm();
         }
         n = toEdges.size();
         for (int i = 0; i < n; ++i) {
            Edge e = (Edge)toEdges.elementAt(i);
            e.moved = true;
            e.bezier.updateForm();
         }
         x += dx;
         y += dy;
      }
      
      public void takeOriginalPosition() {
         x = origx;
         y = origy;
         moved = false;
         int n = fromEdges.size();
         for (int i = 0; i < n; ++i) {
            Edge e = (Edge)fromEdges.elementAt(i);
            e.moved = e.target.moved;
            e.bezier.updateForm();
         }
         n = toEdges.size();
         for (int i = 0; i < n; ++i) {
            Edge e = (Edge)toEdges.elementAt(i);
            e.moved = e.source.moved;
            e.bezier.updateForm();
         }
         
         BezierCanvas canv = (BezierCanvas)vnode.getParent();
      
         vnode.setLocation((int)((x-w/2)*canv.zoom), (int)((y-h/2)*canv.zoom));
         canv.repaint();
      }
   }
    
   public class Edge {
      Node source;
      Node target;
      Bezier bezier;
      float[] cx, cy;
      public String toString() {
         return "edge "+cx.length;
      }
      
      boolean moved;
   }
}
