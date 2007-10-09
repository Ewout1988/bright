package bright.obb.infer;

import java.util.BitSet;

public class BitSetEnumerator {

    private BitSet bs;
    private int bitcount;
    private int bitsnow;
    private int nextpos;

    public BitSetEnumerator(BitSet bs){
	this.bs = bs;
	for(int i=0; i < bs.size(); ++i) {
	    if(bs.get(i)) {
		++bitcount;
	    }
	}
    }

    public final boolean hasMoreElements(){
	return bitsnow < bitcount;
    }

    public final int nextElement(){
	while(!bs.get(nextpos++));
	++bitsnow;
	return nextpos - 1;
    }

    public final int getCount(){
	return bitcount;
    }

    public final void reset(){
	bitsnow = nextpos = 0;
    }

    public String toString(){
	return "("+bitcount+" "+bitsnow+" "+nextpos+")\n"+bs.toString();
    }
}

