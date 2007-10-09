package bright.obb.infer;

import java.io.*;
import java.util.StringTokenizer;
import java.util.BitSet;



public class Inferer{
    
    public double[][] p; // Result will be here 

    JoinTree pjt; // Permanent join tree 
    JoinTree jt;  // Working copy of join tree
    MessageQueue msgq; // Message queue
    int[] inst;

    public Inferer(JoinTree jt){
	this.pjt = jt;
	msgq = new MessageQueue();
	inst = new int[jt.varcount];
	p = new double[jt.varcount][];
	for(int v=0; v < jt.varcount; ++v){
	    p[v] = new double[jt.valcount[v]];
	}
	unsetAllInsts();
    }

    public double[] getProbs(int i) {
        return p[i];
    }

    public final void setInst(int v, int val){
	inst[v] = val;
        
   //     System.err.println(v +" set to "+val);
    }
    
    public boolean isObserved(int i) {
       return inst[i] != -1;
    }
   
    public void unsetAllInsts(){
	for(int v=0; v < inst.length; ++v){
	    setInst(v,-1);
	}
    }

    private void lambdaPropagate(JoinTreeClique i){

	double[] psi = i.psi;

	// Build lambda message

	Marginalizer mgzr 
	    = new Marginalizer(i.vars, i.s, jt.valcount);

	double[] lambda = new double[mgzr.mcfgc];


	int[] ixi_for_mix = new int[mgzr.mcfgc];
	int[][] ixs_for_mix = new int[mgzr.mcfgc][];
	for(int mix=0; mix<mgzr.mcfgc; ++mix) {
	    ixs_for_mix[mix] = new int[mgzr.cfgc/mgzr.mcfgc];
	}

	for(; mgzr.ix<mgzr.cfgc; mgzr.next()){
	    lambda[mgzr.mix] += psi[mgzr.ix];
	    ixs_for_mix[mgzr.mix][ixi_for_mix[mgzr.mix]++] = mgzr.ix;
	}

	// Normalize psi

	for(int mix=0; mix<mgzr.mcfgc; ++mix) {
	    for(int ixi=0; ixi<ixs_for_mix[mix].length; ++ixi) {
		psi[ixs_for_mix[mix][ixi]] /= lambda[mix];
	    }
	}
	
	// Send lambda message;
	if(null != i.parent) {
	    msgq.put(new Message(Message.L, i, i.parent, lambda));
	}
    }


    private void handleLambdaMessage(Message msg){
	// Update psi

	JoinTreeClique i = msg.from;
	JoinTreeClique j = msg.to;
	double[] psi = j.psi;

	Marginalizer mgzr = new Marginalizer(j.vars, i.s, jt.valcount);
	for(; mgzr.ix<mgzr.cfgc; mgzr.next()){
	    psi[mgzr.ix] *= msg.m[mgzr.mix];
	}

	++ j.lambdacount;

	// More action if all children have sent lambdas
	
	if(j.children.length == j.lambdacount) {
	    lambdaPropagate(j);
	    if(null == j.parent){
		System.arraycopy(psi,0,j.p,0,psi.length);
		piPropagate(j);
	    }
	}
    }

    private void piPropagate(JoinTreeClique j){
	double[] p = j.p;

	// Build pi messages for all children and send them

	for(int ii=0; ii<j.children.length;++ii){
	    JoinTreeClique i = j.children[ii];
	    Marginalizer mgzr = new Marginalizer(j.vars, i.s, jt.valcount);
	    double[] pi = new double[mgzr.mcfgc];
	    for(; mgzr.ix<mgzr.cfgc; mgzr.next()){
		pi[mgzr.mix] += p[mgzr.ix];
	    }

	    msgq.put(new Message(Message.P, j, i, pi));
	}
    }

    private void handlePiMessage(Message msg){

	JoinTreeClique i = msg.to;
	double[] psi = i.psi;
	double[] p = i.p;

	Marginalizer mgzr  = new Marginalizer(i.vars, i.s, jt.valcount);
	for(; mgzr.ix<mgzr.cfgc; mgzr.next()){
	    p[mgzr.ix] = msg.m[mgzr.mix] * psi[mgzr.ix];
	}

	if(i.children.length != 0){
	    piPropagate(i);
	}
    }

    private void marginalize(){
	p = new double[jt.varcount][];
	for(int ii=0; ii<jt.clqcount; ++ii){
	    JoinTreeClique clq = jt.clq[ii];
	    BitSetEnumerator bse = new BitSetEnumerator(clq.assigned);
	    while(bse.hasMoreElements()){
		int v = bse.nextElement();
		p[v] = new double[jt.valcount[v]];

		if(-1 != inst[v]) {
		    p[v][inst[v]] = 1;
		    continue;
		}
		
		BitSet vset = new BitSet(); vset.set(v);
		Marginalizer mgzr  
		    = new Marginalizer(clq.vars, vset, jt.valcount);

		for(; mgzr.ix<mgzr.cfgc; mgzr.next()){
		    p[v][mgzr.mix] += clq.p[mgzr.ix];
		}
	    }
	}
    }

    public void infer() {

      try {
	// System.out.println(pjt);
	

      // A.	
	
	jt = JoinTree.copy(pjt,inst);
	
		
	// System.out.println(jt);
	/* Leafs start sending messages */
	
      // B.	
	
	for(int l=0; l<jt.leafs.length; ++l){
	    lambdaPropagate(jt.leafs[l]);
	}

	/* Message loop */
	while(msgq.count()>0){
	    Message msg = msgq.get();
	    // System.out.println(msg);
	    if(msg.type == Message.L) {
		handleLambdaMessage(msg);
	    } else {
		handlePiMessage(msg);
	    }
	}

	/* and marginalize within clique - results are in p */

	marginalize();
      }
      catch (CloneNotSupportedException e) {
         e.printStackTrace();
         System.exit(666);
      }
    }

    public void readInst(String filename)
	throws FileNotFoundException, IOException {
	BufferedReader br 
	    = new BufferedReader(new FileReader(filename));
	String line;
	while(null != (line=br.readLine())){
	    StringTokenizer st = new StringTokenizer(line);
	    setInst(Integer.parseInt(st.nextToken()),
		    Integer.parseInt(st.nextToken()));
	}

	br.close();
    }

    public static void main(String[] argv){
	try {
	    JoinTree jt = new JoinTree(argv[0]);
	    Inferer ifr = new Inferer(jt);
	    if(argv.length>1) {
		ifr.readInst(argv[1]);
	    }

	   // for(int t=0; t<1000; ++t){
      		ifr.infer();
	    //}

	    for(int v=0; v<jt.varcount; ++v){
		for(int l=0; l<jt.valcount[v]; ++l){
		    System.out.print(ifr.p[v][l]+" ");
		}
		System.out.println();;
	    }

	} catch (Exception e){
	    System.err.println(e.getMessage());
	    e.printStackTrace(System.err);
	}
    }
}
