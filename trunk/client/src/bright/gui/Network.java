/*
 * Created on May 18, 2005
 */
package bright.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import net.sf.wts.client.WtsClient;

import org.jdom.Element;

import bright.obb.IApplet;

/**
 * @author kdforc0
 */
public class Network {
	class Variable {
		String name;
		String[] values;
		ArrayList<Integer> parents;
        ArrayList<Double> weights;
        ArrayList<Double> supports;
        
		public Variable(String name) {
			this.name = name;
			this.values = null;
			this.parents = new ArrayList<Integer>();
            this.weights = null;
            this.supports = null;
		}

		public void setParents(int[] parents) {
			this.parents = new ArrayList<Integer>();
			for (int i = 0; i < parents.length; ++i)
				this.parents.add(parents[i]);
		}

		public boolean addParent(int i) {
			if (!parents.contains(i)) {
				parents.add(i);
				return true;
			} else
				return false;
		}

		public void removeParent(int i) {
			parents.remove(i);
		}

        public void setWeight(int from, double weight) {
            if (weights == null) {
                weights = new ArrayList<Double>(parents.size());
                for (int i = 0; i < parents.size(); ++i)
                    weights.add(0.);
            }

            for (int i = 0; i < parents.size(); ++i)
                if (parents.get(i) == from) {
                    if (weights.size() == i)
                        weights.add(weight);
                    else
                        weights.set(i, weight);
                    return;
                }

            throw new RuntimeException("setWeight called for non-existing arc from parent " + from);
        }

        public void setSupport(int from, double support) {
            if (supports == null) {
                supports = new ArrayList<Double>(parents.size());
                for (int i = 0; i < parents.size(); ++i)
                    supports.add(0.);
            }
            
            for (int i = 0; i < parents.size(); ++i)
                if (parents.get(i) == from) {
                    if (supports.size() == i)
                        supports.add(support);
                    else
                        supports.set(i, support);
                    return;
                }

            throw new RuntimeException("setSupport called for non-existing arc from parent " + from);
        }

        public boolean hasParent(int p) {
            for (int i = 0; i < parents.size(); ++i)
                if (parents.get(i) == p)
                    return true;

            return false;
        }
	}

	protected ArrayList<Variable> variables;

    public Network(File strFile, File vdFile) throws FileNotFoundException, IOException { 
        this(new FileInputStream(strFile), new FileInputStream(vdFile));
    }

	public Network(InputStream strFile, InputStream vdFile) throws IOException {
        readVariables(vdFile);
		readStructure(strFile);
	}

    public Network(Network template) {
        this(template.variables);
    }

    public String toString() {
        return "Imported network";
    }    

    public void computeWeights(Project project) throws ApplicationException {
        String idtFile = project.getIdtFile();
        String vdFile = project.getVdFile();
        File projectDir = new File(project.getProjectDir());

        File strFile;
        try {
            strFile = File.createTempFile("bright", ".str", projectDir);
            save(strFile);
        } catch (FileNotFoundException e1) {
            throw new ApplicationException("Could not create file in: " + project.getProjectDir());
        } catch (IOException e1) {
            throw new ApplicationException("Could not create file in: " + project.getProjectDir());
        }

        Runtime runtime = Runtime.getRuntime();
        Process arcweights = null;

        try {
            String cmds[] = { Settings.getSettings().getLearnerDir() + "arcweights", vdFile, idtFile,
                    String.valueOf(project.getNumInstances()), strFile.getAbsolutePath(),
                    String.valueOf(getEss()) };

            arcweights = runtime.exec(cmds, null, projectDir);

            InputStream input = arcweights.getInputStream();
            readWeights(input);
            int result = arcweights.waitFor();

            if (result != 0) {
                throw new ApplicationException("Arcweights exited with error: " + result);
            }
        } catch (InterruptedIOException e) {
            arcweights.destroy();
        } catch (IOException e) {
            if (arcweights != null)
                arcweights.destroy();
            throw new ApplicationException("Error: I/O Error while invoking arcweights: "
                + e.getMessage());
        } catch (InterruptedException e) {
            arcweights.destroy();
        } finally {
            strFile.delete();
        }
    }

    public Network(InputStream vdFile) throws IOException {
        readVariables(vdFile);
    }

    public Network(ArrayList<Variable> variables) {
		this.variables = new ArrayList<Variable>();
		for (Variable v:variables) {
			this.variables.add(new Variable(v.name));
		}
	}

    private void readStructure(InputStream strFile) throws NumberFormatException, IOException {
		LineNumberReader r = new LineNumberReader(new InputStreamReader(strFile));

		readStructure(r);
	}

    public void readStructure(LineNumberReader reader) throws IOException {
        String line = null;
		reader.readLine(); // read line with number of variables.
		int v = 0;
		while ((line = reader.readLine()) != null) {
			String[] l = line.split(" ");

			int numParents = Integer.parseInt(l[1]);
			
			int[] parents = new int[numParents];
			for (int i = 0; i < numParents; ++i) {
				parents[i] = Integer.parseInt(l[2+i]);
			}
			
			variables.get(v).setParents(parents);
			++v;
            
            if (v >= variables.size())
                break;
		}
    }

	private void readVariables(InputStream vdFile) throws IOException {
		variables = new ArrayList<Variable>();
		LineNumberReader r = new LineNumberReader(new InputStreamReader(vdFile));
		
		String line = null;
		while ((line = r.readLine()) != null) {
			String[] l = line.split("\\t");
			
			Variable v = new Variable(l[0]);
            v.values = new String[l.length - 1];
            for (int k = 1; k < l.length; ++k)
                v.values[k-1] = l[k];

            variables.add(v);
		}
	}

    private void readWeights(InputStream weights) throws IOException, ApplicationException {
        LineNumberReader r = new LineNumberReader(new InputStreamReader(weights));
       
        String line = null;
        r.readLine(); // read line with number of variables.
        while ((line = r.readLine()) != null) {
            String[] l = line.split(" ");

            if (l.length != 3)
                throw new ApplicationException("Illegal weights line: '" + line + "'");
            
            int from = Integer.parseInt(l[0]);
            int to = Integer.parseInt(l[1]);
            double weight = 0;
            try {
                weight = Double.parseDouble(l[2]);
            } catch (NumberFormatException e) {
                if (l[2].equals("inf"))
                    weight = Double.POSITIVE_INFINITY;
                else
                    System.err.println("Strange weight: " + l[2]);
            }

            variables.get(to).setWeight(from, weight);
        }
    }

	public boolean addArc(int i, int j, boolean directed) {
       if (directed && findPath(j, i)) {
            return false;
        } else {
            if (!directed) { /* see if there is already an arc from j to i */
                Variable v = variables.get(i);
                if (v.hasParent(j))
                    return false;
            }
            Variable v = variables.get(j);
            if (v.addParent(i)) {
                return true;
            } else
                return false;
        }
	}

	private boolean findPath(int j, int i) {
		Variable v = variables.get(i);
		for (int k = 0; k < v.parents.size(); ++k) {
			int p = v.parents.get(k);
			if (p == j)
				return true;
			if (findPath(j, p))
				return true;
		}
		return false;
	}

	public void save(PrintStream stream) {
		stream.println(variables.size());
		for (int i = 0; i < variables.size(); ++i) {
			Variable v = variables.get(i);
			int c = numChildren(i);
			stream.print("" + c + " " + v.parents.size());
			for (int j = 0; j < v.parents.size(); ++j)
				stream.print(" " + v.parents.get(j));
			stream.println();
		}
	}

    private void save(File strFile) throws FileNotFoundException {
        PrintStream f = new PrintStream(strFile);
        save(f);
        f.flush();
        f.close();
    }

    public void writeDot(PrintStream o, String title, boolean directed) {
        String edgeop;

        if (directed) {
            o.println("digraph \"" + title + "\" {");
            edgeop = "->";
        } else {
            o.println("graph \"" + title + "\" {");
            edgeop = "--";
        }

        o.println("  margin=\"0.1,0.1\"");
        o.println("  clusterrand=none;");
        o.println("  ratio=auto;");
        o.println("  edge [color=\"black\"]\n");
        
        for (int j = 0; j < variables.size(); ++j) {
            Variable v = variables.get(j);
            
            o.println("    " + j + " [label=\"" + v.name + "\"];");
        }

        for (int j = 0; j < variables.size(); ++j) {
            Variable v = variables.get(j);
            
            for (int i = 0; i < v.parents.size(); ++i) {
                o.print("    " + v.parents.get(i) + edgeop + j);
                if (v.weights != null) {
                    o.print(" [color=\"" + dotWeightColor(v.weights.get(i)) + "\"]");
                }
                if (v.supports != null) {
                    o.print(" [color=\"" + dotSupportColor(v.supports.get(i))
                            + "\" label=\"" + String.format("%.2f", v.supports.get(i)) + "\"]");
                }
                o.println(";");
            }
        }
        
        o.println("}");
    }
    
    public void writeDot(String dotFile, Project project) throws FileNotFoundException, ApplicationException
    {
        computeWeights(project);
        PrintStream f = new PrintStream(dotFile);
        writeDot(f, "weights", true);
        f.flush();
        f.close();
    }

    private static String dotSupportColor(double support) {
        return "0,0," + (1-support);
    }

    private static String dotWeightColor(double w) {
        if (w > 1000000000)
            return "black";
        else if (w > 1000000)
            return "0.78,0.83,0.50";
        else if (w > 1000)
            return "0.61,0.77,0.91";
        else
            return "0.51,0.69,0.92";
    }

    public void startInferencePlayground(Project project) throws ApplicationException {        
        WtsClient wtsClient = new WtsClient(Settings.getSettings().getWtsUrl());
        String serviceName = Settings.getSettings().getWtsPrepObbServiceName();
        File strFile = null;
        File optionsFile = null;

        try {
            strFile = File.createTempFile("bright", ".str");
            save(strFile);
            optionsFile = File.createTempFile("bright", ".options");
            PrintStream s = new PrintStream(optionsFile);
            s.println("ESS=" + getEss());
            s.flush();
            s.close();

            String challenge = wtsClient.getChallenge("public");        
            String sessionTicket = wtsClient.login("public", challenge, "public", serviceName);
            
            File vdFile = new File(project.getVdFile());
            
            wtsClient.upload(sessionTicket, serviceName, "vd", vdFile);
            wtsClient.upload(sessionTicket, serviceName, "idt", new File(project.getIdtFile()));
            wtsClient.upload(sessionTicket, serviceName, "str", strFile);
            wtsClient.upload(sessionTicket, serviceName, "options", optionsFile);
            wtsClient.start(sessionTicket, serviceName);
            
            optionsFile.delete();
            
            for (;;) {
                String status = wtsClient.monitorStatus(sessionTicket, serviceName);
                
                if (status.startsWith("ENDED")) {
                    File plaFile = File.createTempFile("bright", ".pla");
                    File qjtFile = File.createTempFile("bright", ".qjt");
                    File dpaFile = File.createTempFile("bright", ".dpa");
                    
                    wtsClient.download(sessionTicket, serviceName, "pla", plaFile);
                    wtsClient.download(sessionTicket, serviceName, "qjt", qjtFile);
                    wtsClient.download(sessionTicket, serviceName, "dpa", dpaFile);

                    IApplet.run(vdFile, strFile, plaFile, qjtFile, dpaFile);

                    strFile.delete();
                    plaFile.delete();
                    qjtFile.delete();
                    dpaFile.delete();

                    break;
                }
                
                Thread.sleep(1000);
            }

            wtsClient.closeSession(sessionTicket, serviceName);
        } catch (RemoteException e) {
            e.printStackTrace();            
            throw new ApplicationException("Error while invoking wts service" + e.getMessage());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            throw new ApplicationException("Error while invoking wts service" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApplicationException("Error while invoking wts service" + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new ApplicationException("Error while invoking wts service" + e.getMessage());
        } finally {
            if (strFile != null)
                strFile.delete();
        }
    }
    
    private int numChildren(int i) {
		int result = 0;
		for (int j = 0; j < variables.size(); ++j) {
			Variable v = variables.get(j);
			if (v.parents.contains(new Integer(i)))
				++result;
		}
		return result;
	}

	public String varName(int i) {
		return variables.get(i).name;
	}

    public void renderToSvg(Project project, File svgFile) throws ApplicationException {
        try {
            File dotFile = File.createTempFile("bright", ".dot");        
            File svgTempFile = File.createTempFile("bright", ".svg");        
            writeDot(dotFile.getAbsolutePath(), project);

            String cmds[] = { Settings.getSettings().getDotPath(), "-Tsvg", "-o",
                    svgTempFile.getAbsolutePath(), dotFile.getAbsolutePath() };

            Process a = Runtime.getRuntime().exec(cmds);
            a.waitFor();

            LineNumberReader reader = new LineNumberReader(new FileReader(svgTempFile));
            PrintStream o = new PrintStream(svgFile);
            for (String s = reader.readLine(); s != null; s = reader.readLine()) {
                s = s.replaceAll("font-family:Nimbus Roman No9 L;font-weight:regular;font-size:11.34pt;", "font-family:serif;font-size:14.00;");
                o.println(s);
            }
            o.flush();
            o.close();

            svgTempFile.delete();
            dotFile.delete();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new ApplicationException("Error while invoking 'dot':" + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApplicationException("Error while invoking 'dot':" + e.getMessage());
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new ApplicationException("Error while invoking 'dot':" + e.getMessage());
        }
    }

    public Network(Element xml, String vdFile) throws FileNotFoundException, IOException, ApplicationException { 
        this(xml, vdFile == null ? null : new FileInputStream(vdFile));
    }

    public Network(Element xml, InputStream vdFile) throws ApplicationException {
        if (vdFile != null)
            try {
                readVariables(vdFile);
            } catch (IOException e1) {
                e1.printStackTrace();
                throw new ApplicationException("Error reading vd-file:" + e1.getMessage());
            }
        
        Element variablesE = xml.getChild("Variables");

        if (variablesE == null) {
            if (variables == null)
                throw new ApplicationException("Expecting <Variables> element.");
        } else {
            variables = new ArrayList<Variable>();
            for (Object vo:variablesE.getChildren("Variable")) {
                Element vE = (Element) vo;

                Variable v = new Variable(vE.getChildTextTrim("Name"));

                Element valuesE = vE.getChild("Values");
                ArrayList<String> values = new ArrayList<String>();
                for (Object vvo:valuesE.getChildren("Value")) {
                    Element vvE = (Element) vvo;
                    values.add(vvE.getTextTrim());
                }
                
                v.values = values.toArray(new String[values.size()]);
                
                variables.add(v);
            }
        }

        Element structureE = xml.getChild("Structure");
        if (structureE == null)
            throw new ApplicationException("Expecting <Structure> element.");
        
        for (Object ao:structureE.getChildren("Arc")) {
            Element arcE = (Element) ao;
            
            String parent = arcE.getChildTextTrim("Parent");
            String child = arcE.getChildTextTrim("Child");
            
            Double weight = null;
            Double support = null;
            
            if (arcE.getChild("Weight") != null)
                weight = Double.valueOf(arcE.getChildTextTrim("Weight"));
            if (arcE.getChild("Support") != null)
                support = Double.valueOf(arcE.getChildTextTrim("Support"));
            
            addArc(parent, child, weight, support);
        }
    }

    /**
     * @param parent
     * @param child
     * @throws ApplicationException
     */
    private void addArc(String parent, String child, Double weight, Double support) throws ApplicationException {
        Variable childVar = variables.get(variableIndexOf(child));

        childVar.addParent(variableIndexOf(parent));
        if (weight != null)
            childVar.setWeight(variableIndexOf(parent), weight);
        if (support != null)
            childVar.setSupport(variableIndexOf(parent), support);
    }

    private int variableIndexOf(String name) throws ApplicationException {
        for (int k = 0; k < variables.size(); ++k) {
            Variable v = variables.get(k);
            if (v.name.equals(name))
                return k;
        }

        throw new ApplicationException("Could not resolve variable: '" + name + "'");
    }

    public Element saveToXML() {
        Element e;

        Element result = new Element("Network");
        
        Element structureE = new Element("Structure"); result.addContent(structureE);

        for (Variable v:variables) {
            for (int k = 0; k < v.parents.size(); ++k) {
                Element arcE = new Element("Arc"); structureE.addContent(arcE);
                e = new Element("Parent"); e.setText(variables.get(v.parents.get(k)).name); arcE.addContent(e);
                e = new Element("Child"); e.setText(v.name); arcE.addContent(e);
                if (v.weights != null) {
                    e = new Element("Weight"); e.setText(Double.toString(v.weights.get(k))); arcE.addContent(e);
                }
                if (v.supports != null) {
                    e = new Element("Support"); e.setText(Double.toString(v.supports.get(k))); arcE.addContent(e);
                }
            }
        }

        return result;
    }

    public int getArcCount() {
        int result = 0;

        for (Variable v:variables) {
            result += v.parents.size();
        }
        return result;
    }
    
    public double getEss() {
        return 1.;
    }

    public ArrayList<Variable> getVariables() {
        return variables;
    }
    
    public String detailsHtml() {
        return "<html><table>"
            + "<tr><td><b>Arcs:</b></td><td>" + getArcCount() + "</td></tr>"
            + "</table></html>";
    }
}
