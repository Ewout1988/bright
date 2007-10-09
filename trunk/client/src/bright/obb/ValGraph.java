package bright.obb;

import java.io.*;
import java.util.*;

public class ValGraph {

   Hashtable vals = new Hashtable();
   Vector nodes = new Vector();

   public ValGraph(BufferedReader r) throws IOException {
    
      String s;
      Vector help = new Vector();
      
      while ( (s = r.readLine()) != null) {
      	 String[] vv = s.split("\t");
         //StringTokenizer st = new StringTokenizer(s, "\t");
         //String attname = st.nextToken();
         String attname = vv[0];
       
       	 for (int i = 1; i < vv.length; ++i)
       	    help.addElement(vv[i]);
         //while (st.hasMoreTokens()) {
         //   help.addElement(st.nextToken());
         //}
         int n = help.size();
         String[] vs = new String[n];
         for (int i = 0; i < n; ++i) {
            vs[i] = (String)help.elementAt(i);
         }
         vals.put(attname, vs);
         nodes.addElement(attname);
         help.removeAllElements();
      }
   }
   
    public static void main(String[] args) throws Exception {
       BufferedReader vr = new BufferedReader(new FileReader(args[0]+".vd"));
       ValGraph vg = new ValGraph(vr);
    }
       
}
