package bright.obb.infer;

import java.io.*;
import java.util.StringTokenizer;
import java.util.BitSet;
import java.util.Vector;


public class JoinTree implements Cloneable {

    public int[] valcount; 
    public int varcount;
    public int clqcount;
    public JoinTreeClique[] leafs;
    JoinTreeClique[] clq;

    // Following attributes are created by copy, not by constructor

    public JoinTree(String filename) 
	    throws FileNotFoundException, IOException {
	
	this( new BufferedReader(new FileReader(filename)) );
    }
    
    public JoinTree(BufferedReader br) throws IOException {
    
	this.valcount = getIntArray(br.readLine());
	this.varcount = valcount.length;
	this.clqcount = getInt(br.readLine());
	this.clq = new JoinTreeClique[clqcount];
	Vector[] childcare = new Vector[clqcount];
	for(int i=0; i<clqcount; ++i){
	    this.clq[i] = new JoinTreeClique(i);
	    childcare[i] = new Vector();
	}
	int[] leaft = getIntArray(br.readLine());
	this.leafs = new JoinTreeClique[leaft.length];
	for(int l=0; l<leaft.length; ++l){
	    this.leafs[l] = this.clq[leaft[l]];
	}
	br.readLine();

	for(int i=0; i<clqcount; ++i){
	    this.clq[i].vars = getBitSet(br.readLine());
	    int parix = getInt(br.readLine());
	    if(parix != -1){
		this.clq[i].parent = this.clq[parix];
		childcare[parix].addElement(this.clq[i]);
	    }
	    this.clq[i].s = getBitSet(br.readLine());
	    this.clq[i].r = getBitSet(br.readLine());
	    this.clq[i].assigned = getBitSet(br.readLine());
	    this.clq[i].psi = getDoubleArray(br.readLine());
	    br.readLine();
	}

	for(int i=0; i<clqcount; ++i){
	    this.clq[i].children = new JoinTreeClique[childcare[i].size()];
	    childcare[i].copyInto(this.clq[i].children);
	}

	br.close();
    }

    static final int getInt(String line) {
	return Integer.parseInt(new StringTokenizer(line).nextToken());
    }

    static final int[] getIntArray(String line) {
	StringTokenizer st = new StringTokenizer(line);
	int[] intarray = new int [Integer.parseInt(st.nextToken())];
	
	for(int i=0; st.hasMoreTokens(); ++i){
	    intarray[i] = Integer.parseInt(st.nextToken());
	}

	return intarray;
    }

    static final double[] getDoubleArray(String line) {
	StringTokenizer st = new StringTokenizer(line);
	double[] dblarray = new double [Integer.parseInt(st.nextToken())];
	for(int i=0; st.hasMoreTokens(); ++i){
	    dblarray[i] = new Double(st.nextToken()).doubleValue();
	}

	return dblarray;
    }

    static final BitSet getBitSet(String line) {
	StringTokenizer st = new StringTokenizer(line);
	BitSet bitset = new BitSet();

	int count = Integer.parseInt(st.nextToken());
	
	for(int i=0; i<count; ++i){
	    bitset.set(Integer.parseInt(st.nextToken()));
	}

	return bitset;
    }

    static JoinTree copy(JoinTree src, int[] x) 
	throws CloneNotSupportedException {
	
	JoinTree jt = (JoinTree) src.clone();

	jt.clq = new JoinTreeClique[jt.clqcount];
	for(int i=0; i<src.clqcount; ++i){
	    jt.clq[i] = new JoinTreeClique(i);
	}
	for(int i=0; i<src.clqcount; ++i){
	    jt.clq[i].copy(src.clq[i], jt, x);
	}	    

	jt.leafs = new JoinTreeClique[src.leafs.length];
	for(int t=0; t<jt.leafs.length; ++t){
	    jt.leafs[t] = jt.clq[src.leafs[t].id];
	}

	return jt;
    }

    public String toString(){
	String res = "";
	for(int i = 0; i<clqcount; ++i){
	    res += clq[i];
	    res += "\n";
	}
	return res;
    }

}
