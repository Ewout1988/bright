/*
 * Created on Jan 17, 2005
 */
package bright.obb;

import java.io.*;
import java.util.*;

import bright.obb.infer.Inferer;
import bright.obb.infer.JoinTree;


/**
 * @author kdforc0
 */
public class InfererCLI {

	public static void main(String[] args) throws IOException {
		// Read .pla file
		BufferedReader r = new BufferedReader(new FileReader(args[0]+".pla"));
		DotGraph dg = new DotGraph(r);
      
		// read .vd file
		BufferedReader vr = new BufferedReader(new FileReader(args[0]+".vd"));
		ValGraph vg = new ValGraph(vr);
   
		// read .qjt file
		BufferedReader jr = new BufferedReader(new FileReader(args[0]+".qjt"));
		Inferer ifr = new Inferer(new JoinTree(jr));

		// Usage: obb.InfererCLI data < commands > result
		// command :
		//   SET varName value
		//   UNSET varName
		//   INFER varName value: prints a double to stdout
		
		BufferedReader cmdr = new BufferedReader(new InputStreamReader(System.in));

		boolean dirty = true;

		for (String cmd = cmdr.readLine(); cmd != null; cmd = cmdr.readLine()) {
			String cmda[] = cmd.split(" ");
			
			if (cmda[0].equals("SET")) {
				String varName = cmda[1];
				String value = cmda[2];
				
				int varI = vg.nodes.indexOf(varName);
				if ((varI) < 0)
					System.err.println("No such node: " + varName);
				List vals = Arrays.asList((Object[]) vg.vals.get(varName));
				int valI = vals.indexOf(value);
				
				ifr.setInst(varI, valI);
				dirty = true;
			} else if (cmda[0].equals("UNSET")) {
				String varName = cmda[1];
				
				int varI = vg.nodes.indexOf(varName);
				if ((varI) < 0)
					System.err.println("No such node: " + varName);
				
				ifr.setInst(varI, -1);
				dirty = true;
			} else if (cmda[0].equals("INFER")) {
				String varName = cmda[1];
				String value = cmda[2];				

				int varI = vg.nodes.indexOf(varName);
				if ((varI) < 0)
					System.err.println("No such node: " + varName);
				List vals = Arrays.asList((Object[]) vg.vals.get(varName));
				int valI = vals.indexOf(value);

				if (dirty) {
					ifr.infer();
					dirty = false;
				}
				
				double result = ifr.getProbs(varI)[valI];
				
				System.out.println(result);
			}
		}
	}
}
