package bright.obb.infer;

import java.util.BitSet;


class Marginalizer {

    public int cfgc;
    public int mcfgc;
    public int mix;
    public int ix;
    
    private int cfgl;
    private int[] maxv;
    private int[] posval;
    private int[] maxposval;
    private int[] cfg;

    public Marginalizer(BitSet clqv, BitSet marked, int[] valcount){
	BitSetEnumerator bse = new BitSetEnumerator(clqv);

	cfgc = 1;
	mcfgc = 1;
	cfgl = bse.getCount();

	maxv = new int[cfgl];
	posval = new int[cfgl];
	maxposval = new int[cfgl];
	cfg = new int[cfgl];

	for(int p=0; bse.hasMoreElements();++p){
	    int v = bse.nextElement();
	    int valcv = valcount[v];
	    cfgc *= valcv;
	    maxv[p] = valcv-1;
	    if(marked.get(v)){
		posval[p] = mcfgc;
		maxposval[p] = mcfgc * maxv[p];
		mcfgc *= valcv;
	    } 
	}
    }

    public final void next(){
	for(int p=0; p<cfgl; ++p){
	    if(cfg[p] == maxv[p]) {// overflow would occur
		cfg[p] = 0;
		mix -= maxposval[p];
	    } else {
		++ cfg[p];
		mix += posval[p];
		break;
	    }
	}
	++ ix;
    }
}
