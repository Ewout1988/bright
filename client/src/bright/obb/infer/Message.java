package bright.obb.infer;


class Message {
    
    static final int L = 1;
    static final int P = 2;

    public int type;
    public JoinTreeClique from;
    public JoinTreeClique to;
    public double[] m;

    public Message(int type, 
		   JoinTreeClique from, JoinTreeClique to, double[] m){
	this.type = type;
	this.from = from;
	this.to = to;
	this.m = m;
    }

    public String toString(){
	String s =  ((type==L)?"L":"P")+"-message from "
	    +from.id+" to "+to.id+": ";
	for(int i=0; i<m.length;++i){
	    s += m[i]+" ";
	}
	return s;
    }
}
