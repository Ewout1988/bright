package bright.obb.infer;

import java.io.*;
import java.util.StringTokenizer;
import java.util.BitSet;


public class Ansu{
    
    static double[][][] dipa; 
    static double[][] dipas; 
    static double[][][] alpha; 
    static double[][][] beta; 
    static double[][][] gamma; 
    static double[][][] delta; 
    static BitSet[] parents;

    static Inferer ifr;

    private static double theta(int v, int u, int l){
	return dipa[v][u][l]/dipas[v][u];
    }

    private static void marginalize(double[][][] al, double[][][] be, 
				    double m){
	for(int ii=0; ii<ifr.jt.clqcount; ++ii){
	    JoinTreeClique clq = ifr.jt.clq[ii];
	    // System.out.println(clq);
	    BitSetEnumerator bse = new BitSetEnumerator(clq.assigned);
	    while(bse.hasMoreElements()){
		int v = bse.nextElement();
		double[] p = clq.p;   ////// SHOULD be psi ?
		double[][] alphat = al[v];
		double[][] betat = be[v];
		double[] sum = new double[alphat.length];
		double sumv = 0;
		// System.out.println(v+" "+parents[v]+" " +clq.vars );
		AnsuMarginalizer mgzr 
		    = new AnsuMarginalizer(clq.vars, parents[v], 
					   ifr.jt.valcount, ifr.inst);
		// System.out.println(mgzr);
		if(-1 == ifr.inst[v]) {
		    for(mgzr.first(); mgzr.ix<mgzr.cfgc; mgzr.next()){
			// System.out.println(mgzr.pix+" "+mgzr.ix);
			alphat[mgzr.pix][mgzr.cfg(v)] += p[mgzr.ix]*m;
		    }
		} else {
		    int instv = ifr.inst[v];
		    for(mgzr.first(); mgzr.ix<mgzr.cfgc; mgzr.next()){
			alphat[mgzr.pix][instv] += p[mgzr.ix]*m;
		    }		    
		}

		for(int u=0; u<alphat.length; ++u){
		    for(int i=0; i<alphat[u].length; ++i){
			sum[u] += alphat[u][i];
		    }
		    sumv += sum[u];
		}

		for(int u=0; u<alphat.length; ++u){
		    double beterm2 = sumv - sum[u];
		    for(int i=0; i<alphat[u].length; ++i){
			betat[u][i] = (sum[u]-alphat[u][i]) / (1-theta(v,u,i));
 			alphat[u][i] = alphat[u][i]/theta(v,u,i) - betat[u][i];
			betat[u][i] += beterm2;
		    }
		}
	    }
	}
    }

    public static void readStruct(String filename)
	throws FileNotFoundException, IOException { 
	BufferedReader br 
	    = new BufferedReader(new FileReader(filename));
	String line = br.readLine();
	parents = new BitSet[Integer.parseInt(line)];
 	for(int v=0; null != (line=br.readLine()); ++v){
	    StringTokenizer st = new StringTokenizer(line);
	    st.nextToken(); st.nextToken();
	    parents[v] = new BitSet();
	    while(st.hasMoreTokens()){
		parents[v].set(Integer.parseInt(st.nextToken()));
	    }
	}
	br.close();
    }

    public static void readDipa(String filename, int[] valcount)
	throws FileNotFoundException, IOException { 
	BufferedReader br 
	    = new BufferedReader(new FileReader(filename));
	dipa = new double[valcount.length][][];
	dipas = new double[valcount.length][];
	alpha = new double[valcount.length][][];
	beta = new double[valcount.length][][];
	gamma = new double[valcount.length][][];
	delta = new double[valcount.length][][];
 	for(int v=0; v<valcount.length; ++v){
	    int pcc = Integer.parseInt(br.readLine());
	    dipa[v] = new double[pcc][valcount[v]];
	    dipas[v] = new double[dipa[v].length];
	    alpha[v] = new double[pcc][valcount[v]];
	    beta[v] = new double[pcc][valcount[v]];
	    gamma[v] = new double[pcc][valcount[v]];
	    delta[v] = new double[pcc][valcount[v]];
	    
	    for(int u=0; u<pcc; ++u){
		StringTokenizer st = new StringTokenizer(br.readLine());
		for(int l=0; l<valcount[v]; ++l){
		    dipa[v][u][l] = new Double(st.nextToken()).doubleValue();
		    dipas[v][u] += dipa[v][u][l];
		}
	    }
	}

	br.close();
    }

    private static double pard(int v, int u, int l){
	double a = alpha[v][u][l];
	double b = beta[v][u][l];
	double g = gamma[v][u][l];
	double d = delta[v][u][l];
	double x = theta(v,u,l);

	return (a*d - b*g) / (g*g*x*x + 2*g*d +d*d); 
    }

    private static void printmrgnls(double m){
	for(int ii=0; ii<ifr.jt.clqcount; ++ii){
	    double sum = 0;
	    JoinTreeClique clq = ifr.jt.clq[ii];
	    double[] p = clq.p;   ////// SHOULD be psi ?
	    Marginalizer mgzr 
		= new Marginalizer(clq.vars, clq.vars, ifr.jt.valcount);
	    for(; mgzr.ix<mgzr.cfgc; mgzr.next()){
		sum += p[mgzr.ix]*m;
	    }
	    System.out.println("Clq "+ii+" sum is "+sum);
	}
    }

    public static void main(String[] argv){
	try {
	    JoinTree jt = new JoinTree(argv[0]);
	    ifr = new Inferer(jt);
	    
	    readStruct(argv[1]);
	    readDipa(argv[2], jt.valcount);

	    if(argv.length>5) {
		ifr.readInst(argv[5]);
	    }

	    int A = Integer.parseInt(argv[3]);
	    int a = Integer.parseInt(argv[4]);

	    int[] e = new int[ifr.inst.length];
	    System.arraycopy(ifr.inst,0,e,0,ifr.inst.length);
	    ifr.unsetAllInsts();
	    double pe = 1;
	    for(int i=0; i<e.length; ++i){
		if(-1 != e[i]) {
		    ifr.infer();
		    pe *= ifr.p[i][e[i]];
		    ifr.setInst(i,e[i]);
		}
	    }

	    ifr.infer();
	    double ans = ifr.p[A][a];
	    double pea = pe * ans;

	    //	    printmrgnls(pe);
	    marginalize(gamma,delta,pe);
	    ifr.setInst(A,a);
	    ifr.infer();
	    marginalize(alpha,beta,pea);
	    
	    double var = 0;
	    for(int v=0; v<jt.varcount; ++v){
		// if(-1 != ifr.inst[v]) continue; why 
		
		int vcv = jt.valcount[v];
		double[][] vacom = new double[vcv][vcv];
		double[] prd = new double[vcv];
		
		for(int u=0; u<dipa[v].length; ++u){
		    double dps = dipas[v][u]; 
		    double dpdenom = dps*dps*(dps+1);
		    // System.out.println(v+" "+u);
		    for(int dp1=0; dp1<vcv; ++dp1){
			for(int dp2=0; dp2<vcv; ++dp2){
			    double dpnom = (dp1==dp2) 
				? (dipa[v][u][dp1]*(dps-dipa[v][u][dp1])) 
				: (-dipa[v][u][dp1]*dipa[v][u][dp2]);
			    vacom[dp1][dp2] = dpnom/dpdenom;
			    // System.out.print(vacom[dp1][dp2]+" ");
			}
			// System.out.println();
		    }
		    
		    System.out.print(v+" "+u+" ");
		    for(int l1=0; l1<vcv; ++l1) {
			prd[l1] = pard(v,u,l1);
			System.out.print(" "+prd[l1]);
		    }
		    System.out.println();

		    for(int l1=0; l1<vcv; ++l1){
			double vt = 0;
			for(int l2=0; l2<vcv; ++l2){
			    vt += prd[l2] * vacom[l2][l1];
			}
			var += prd[l1] * vt;
			// System.out.println(u+" "+v+" "+var);
		    }

		    /*
		    System.out.print(v+" "+u+" ");
		    for(int l=0; l<alpha[v][u].length; ++l)
			System.out.print(alpha[v][u][l]+" ");
		    System.out.println();
		    System.out.print(v+" "+u+" ");
		    for(int l=0; l<beta[v][u].length; ++l)
			System.out.print(beta[v][u][l]+" ");
		    System.out.println();
		    System.out.print(v+" "+u+" ");
		    for(int l=0; l<gamma[v][u].length; ++l)
			System.out.print(gamma[v][u][l]+" ");
		    System.out.println();
		    System.out.print(v+" "+u+" ");
		    for(int l=0; l<delta[v][u].length; ++l)
			System.out.print(delta[v][u][l]+" ");
		    System.out.println();
		    */
		}
	    }

	    System.out.println(ans+" "+var);

	} catch (Exception e){
	    System.err.println(e.getMessage());
	    e.printStackTrace(System.err);
	}
    }
}




