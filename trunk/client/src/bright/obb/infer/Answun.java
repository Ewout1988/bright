package bright.obb.infer;

import java.io.*;
import java.util.*;



public class Answun {

   Inferer ifr;

   double[][][] dipa; 
   double[][]   dipas; 
   double[][][] alpha; 
   double[][][] beta; 
   double[][][] gamma; 
   double[][][] delta;
   
   BitSet[] parents;
   
   public double lastmean;

   public Answun(Inferer ifr, BufferedReader dpar, BufferedReader strr) 
         throws IOException {
 
      this.ifr = ifr;
      
      readDipa(dpar, ifr.pjt.valcount);
      readStruct(strr);
   }
   
   public double calculateVariance(int varIndex, int valIndex) {
      
      int n = ifr.inst.length;
      int[] copy = new int[n];
      System.arraycopy(ifr.inst, 0, copy, 0, n);
      ifr.unsetAllInsts();
      ifr.infer();
      double pe = 1;
      for (int i=0; i < n; ++i) {
         int value = copy[i];
         if(value != -1) {
            pe *= ifr.p[i][value];
            ifr.setInst(i, value);
            ifr.infer();
         }
      }

      double ans = ifr.p[varIndex][valIndex];
      double pea = pe * ans;
      
      marginalize(gamma, delta, pe);
      ifr.setInst(varIndex, valIndex);
      ifr.infer();
      marginalize(alpha, beta, pea);

      double var = 0;
      JoinTree jt = ifr.jt;
      int varc = jt.varcount;
      int[] valc = jt.valcount;
      for (int v = 0; v < varc; ++v) {
         
         int vcv = valc[v];
         double[][] vacom = new double[vcv][vcv];
         double[] prd = new double[vcv];
         
         for (int u = 0; u < dipa[v].length; ++u){
            double dps = dipas[v][u]; 
            double dpdenom = dps*dps*(dps+1);
            for (int dp1 = 0; dp1 < vcv; ++dp1) {
               for (int dp2 = 0; dp2 < vcv; ++dp2) {
                  double dpnom = (dp1==dp2) 
			       ? (dipa[v][u][dp1]*(dps-dipa[v][u][dp1])) 
			       : (-dipa[v][u][dp1]*dipa[v][u][dp2]);
		  vacom[dp1][dp2] = dpnom/dpdenom;
               }
            }
            for (int l1 = 0; l1 < vcv; ++l1) {
               prd[l1] = pard(v,u,l1);
	    }
            for (int l1 = 0; l1 < vcv; ++l1){
               double vt = 0;
               for (int l2 = 0; l2 < vcv; ++l2){
                  vt += prd[l2] * vacom[l2][l1];
               }
               var += prd[l1] * vt;
       //        System.err.println("* var("+u+"): "+var);
       //        System.err.println("  -prd["+l1+"]: "+prd[l1] +" ; "+vt);
               
	    }
         }
      }
      
      for (int i=0; i < n; ++i) {
         ifr.setInst(i, copy[i]);
      }
    //  ifr.infer();
      this.lastmean = ans;
      return var;
   }
   
   private double pard(int v, int u, int l){
      if(ifr.jt.valcount[v] == 1) 
         return 0; 
       
      double a = alpha[v][u][l];
      double b = beta[v][u][l];
      double g = gamma[v][u][l];
      double d = delta[v][u][l];
      double x = theta(v,u,l);

      return (a*d - b*g) / (g*g*x*x + 2*g*d +d*d); 
   }
   
   private double theta(int v, int u, int l){
	return dipa[v][u][l]/dipas[v][u];
    }
   
   private void marginalize(double[][][] al, double[][][] be, 
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
                
                for (int i = 0; i < alphat.length; ++i) {
                  double[] dd = alphat[i];
                  for (int j = 0; j < dd.length; ++j) {
                     dd[j] = 0;
                  }
                }
                for (int i = 0; i < betat.length; ++i) {
                  double[] dd = betat[i];
                  for (int j = 0; j < dd.length; ++j) {
                     dd[j] = 0;
                  }
               }
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
		   
   private void readDipa(BufferedReader br, int[] valcount) throws IOException { 
	int vcl = valcount.length;
        
        dipa  = new double[vcl][][];
	dipas = new double[vcl][];
	alpha = new double[vcl][][];
	beta  = new double[vcl][][];
	gamma = new double[vcl][][];
	delta = new double[vcl][][];
 	
        for (int v=0; v < vcl; ++v) {
	    int pcc = Integer.parseInt(br.readLine());
	    int vc = valcount[v];
            dipa[v]  = new double[pcc][vc];
	    dipas[v] = new double[pcc];
	    alpha[v] = new double[pcc][vc];
	    beta[v]  = new double[pcc][vc];
	    gamma[v] = new double[pcc][vc];
	    delta[v] = new double[pcc][vc];
	    
	    for (int u = 0; u < pcc; ++u){
		StringTokenizer st = new StringTokenizer(br.readLine());
		for(int l = 0; l < vc; ++l){
                    double val =  new Double(st.nextToken()).doubleValue();
		    dipa[v][u][l] = val;
		    dipas[v][u]  += val;
		}
	    }
	}

	br.close();
    }
    
    private void readStruct(BufferedReader br) throws IOException { 
	
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
}
  
