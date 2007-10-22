/*
 * Created on Oct 22, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class NetworkSet {

    private ArrayList<Network> networks;
    private HashMap<String, ArcFeature> features;

    public NetworkSet(File strsFile, File vdFile) throws FileNotFoundException, IOException {
        this(new FileInputStream(strsFile), new FileInputStream(vdFile));
    }

    public ArrayList<Network> getNetworks() {
        return networks;
    }

    public NetworkSet(InputStream strsFile, InputStream vdFile) throws IOException {
        networks = new ArrayList<Network>();
        
        LineNumberReader r = new LineNumberReader(new InputStreamReader(strsFile));
        Network template = new Network(vdFile);
        
        for (;;) {
            String header = r.readLine();
        
            if (header == null)
                break;

            Network n = new Network(template);
            n.readStructure(r);
            
            networks.add(n);
        }
    }
    
    private static class ArcFeature implements Comparable {
        int support;
        int head, tail;

        public int compareTo(Object o) {
            ArcFeature other = (ArcFeature) o;
            return other.support - support;
        }

        public ArcFeature(int head, int tail, int support) {
            this.head = head;
            this.tail = tail;
            this.support = support;
        }
    }
    
    @SuppressWarnings("unchecked")
    public ConsensusNetwork computeConsensus(double minimumSupport, boolean directed, boolean bidirSupport) {
        createFeatureMap();
        ArrayList<ArcFeature> f = new ArrayList<ArcFeature>(features.values());
        Collections.sort(f);

        if (!directed)
            bidirSupport = true;

        ConsensusNetwork result = new ConsensusNetwork(minimumSupport, directed, bidirSupport, networks.get(0));

        for (int i = 0; i < f.size(); ++i) {
            ArcFeature a = (ArcFeature) f.get(i);

            double support = (double)a.support / networks.size();
            if (bidirSupport)
                support += computeSupport(a.tail, a.head);

            if (support >= minimumSupport)
                result.addArc(a.head, a.tail, directed, support);
        }

        return result;
    }

    public double computeSupport(int head, int tail) {
        createFeatureMap();
        
        ArcFeature f = features.get(createFeatureKey(head, tail));

        if (f == null)
            return 0;
        else {
            return (double)f.support / networks.size();
        }
    }
    
    private void createFeatureMap() {
        if (features != null)
            return;

        features = new HashMap<String,ArcFeature>();
        
        for (Network n:networks) {          
            int j = 0;

            for (Network.Variable v:n.getVariables()) {
                for (int k = 0; k < v.parents.size(); ++k) {
                    int p = v.parents.get(k);
                    String s = createFeatureKey(p, j);

                    ArcFeature f;
                    if (features.containsKey(s)) {
                        f = (ArcFeature) features.get(s);
                    } else {
                        f = new ArcFeature(p, j, 0);
                        features.put(s, f);
                    }
                    
                    ++f.support;
                }
                
                ++j;
            }
        }
    }

    /**
     * @param tail
     * @param head
     * @return
     */
    private String createFeatureKey(int head, int tail) {
        return "" + head + " " + tail;
    }
}
