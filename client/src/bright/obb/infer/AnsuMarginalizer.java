package bright.obb.infer;

import java.util.BitSet;


class AnsuMarginalizer {

    public int cfgc;
    public int pcfgc;
    public int pix;
    public int ix;
    
    private int cfgl;
    private int[] maxv;
    private int[] posval;
    private int[] maxposval;
    private int[] cfg;
    private int[] v2p;
    private BitSet fixpos;

    public AnsuMarginalizer(BitSet clqv, BitSet marked, 
			    int[] valcount, int[] inst){
	BitSet U = (BitSet) clqv.clone();
	U.or(marked);
	BitSetEnumerator bse = new BitSetEnumerator(U);

	cfgc = 1;
	pcfgc = 1;
	fixpos = new BitSet();
	cfgl = bse.getCount();

	maxv = new int[cfgl];
	posval = new int[cfgl];
	maxposval = new int[cfgl];
	cfg = new int[cfgl];
	v2p = new int[valcount.length];	
	for(int i=0; i<v2p.length;++i) v2p[i] = -1;

	for(int p=0; bse.hasMoreElements(); ++p){
	    int v = bse.nextElement();
	    int valcv = valcount[v];
	    v2p[v] = p;
	    if(clqv.get(v)) {
		cfgc *= valcv;
		maxv[p] = valcv-1;
		if(marked.get(v)){
		    posval[p] = pcfgc;
		    maxposval[p] = pcfgc * maxv[p];
		    pcfgc *= valcv;
		} 
	    } else {
		posval[p] = inst[v] * pcfgc;
 		cfg[p] = inst[v];
		fixpos.set(p);
		pcfgc *= valcv;
	    }
	}
    }

    public final void first(){
	for(int p=0; p<cfgl; ++p){
	    if(fixpos.get(p)){
		pix += posval[p];
		continue;
	    }
	}
    }

    public final void next(){
	for(int p=0; p<cfgl; ++p){
	    if(fixpos.get(p)){
		// pix += posval[p];
		continue;
	    }
	    if(cfg[p] == maxv[p]) {// overflow would occur
		cfg[p] = 0;
		pix -= maxposval[p];
	    } else {
		++ cfg[p];
		pix += posval[p];
		break;
	    }
	}
	++ ix;
    }

    public final int cfg(int v){ return this.cfg[v2p[v]];} 

    public String toString() {
	String s = "";
	s += "cfgc="+cfgc+"\n";
	s += "pcfgc="+pcfgc+"\n";
	s += "pix="+pix+"\n";
	s += "ix="+ix+"\n";
	s += "cfgl="+cfgl+"\n";
	s += "maxv=";
	for(int i=0;i<maxv.length;++i) s+=" "+maxv[i];
	s += "\n";
	s += "posval=";
	for(int i=0;i<posval.length;++i) s+=" "+posval[i];
	s += "\n";
	s += "maxposval=";
	for(int i=0;i<maxposval.length;++i) s+=" "+maxposval[i];
	s += "\n";
	s += "cfg=";
	for(int i=0;i<cfg.length;++i) s+=" "+cfg[i];
	s += "\n";
	s += "v2p=";
	for(int i=0;i<v2p.length;++i) s+=" "+v2p[i];
	s += "\n";
	s += fixpos+"\n";
	return s;
    }
}


