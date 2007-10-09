package bright.obb.infer;

import java.util.BitSet;


public class JoinTreeClique implements Cloneable{
    int id;
    JoinTreeClique[] children;
    JoinTreeClique parent;
    BitSet assigned;

    BitSet vars;
    BitSet s;
    BitSet r;
    double[] psi;

    int lambdacount;
    double[] p;

    public JoinTreeClique(int id) { 
	this.id = id ;
    }

    final void copy(JoinTreeClique src, JoinTree jt, int[] x){
    
	if(null != src.parent) {
	    parent = jt.clq[src.parent.id];
	}
	children = new JoinTreeClique[src.children.length];
	for(int c=0; c<src.children.length; ++c){
	    children[c] = jt.clq[src.children[c].id];
	}
	assigned = (BitSet) src.assigned.clone();
	vars = (BitSet) src.vars.clone();
	s = (BitSet) src.s.clone();
	r = (BitSet) src.r.clone();
	
	int[] cfg = new int[jt.varcount];
	
	boolean x_intersects = false;
	for(int j=0; j<x.length; ++j){
	    if((x[j] != -1) && (vars.get(j))){
		vars.clear(j);
		s.clear(j);
		r.clear(j);
		cfg[j] = x[j];
		x_intersects = true;
	    } 
	}

	if(! x_intersects) { // Just copy
	    int psilength = src.psi.length;
	    psi = new double[psilength];
	    p = new double[psilength];
	    System.arraycopy(src.psi,0,psi,0,psilength);
	    return; // Uuh, we are done
	}


	// This hurts (there must be a smarter way), but ... psi
	int psilength = 1;
	BitSetEnumerator jtbse = new BitSetEnumerator(vars);
	while(jtbse.hasMoreElements()){
	    psilength *= jt.valcount[jtbse.nextElement()];
	}
	
	psi = new double[psilength];
	p = new double[psilength];
	
	BitSetEnumerator srcbse = new BitSetEnumerator(src.vars);
	for(int psix=0; psix<psilength;++psix){
	    int cfgix = 0; int base = 1;
	    srcbse.reset();
	    while(srcbse.hasMoreElements()){
		int v = srcbse.nextElement();
		cfgix += cfg[v] * base;
		base *= jt.valcount[v];
	    }
	    psi[psix] = src.psi[cfgix];
	    
	    if(psix == psilength-1) break;
	    
	    jtbse.reset();
	    while(jtbse.hasMoreElements()){ 
		int v = jtbse.nextElement();
		if(++cfg[v] < jt.valcount[v]) break;
		cfg[v] = 0;
	    }		    
	}
    }

    public String toString(){
	String res = "Clique "+id+":\n";
	res += "Vars = " +  vars + "\n";
	res += "Parent = " +  ((null==parent)?"none":(""+parent.id)) + "\n";
	res += "Child count = " + children.length + "\n";
	res += "Children :";
	for(int c=0; c<children.length; ++c){
	    res += " "+ children[c].id;
	}
	res += "\n";
	res += "S = " + s + "\n";
	res += "R = " + r + "\n";
	res += "A = " + assigned + "\n";
	res += "Psilength = " + psi.length + "\n";
	res += "Psi = ";
	for(int j=0; j<psi.length;++j){
	    res += " "+psi[j];
	}
	res += "\n";
	if(null==p) {
	    res += "Plength = 0\n";
	} else {
	    res += "Plength = " + p.length + "\n";
	    res += "P =";
	    for(int j=0; j<p.length; ++j){
		res += " "+p[j];
	    }
	    res += "\n";
	}
	res += "lambdacount = " + lambdacount + "\n";
	return res;
    }

}
