package bright.gui;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

/*
 * Created on Apr 19, 2003
 */

/**
 * @author kdf
 */
public class Project {
    private static final String VERSION = "0.1";
    
    private String projectDir;
    private int numInstances;
    private Learner.Properties learnerDefaults;
    private boolean dirty;

    private ArrayList<Network> networks;

    public Project() {
        this.projectDir = null;
        this.numInstances = 0;
        this.learnerDefaults = new Learner.Properties();
        this.dirty = false;

        this.networks = new ArrayList<Network>();
    }

    public Project(File fileName) throws IOException, ApplicationException    {
        dirty = false;        
        retrieve(fileName);
    }

    private void retrieve(File fileName) throws IOException, ApplicationException {
        SAXBuilder builder = new SAXBuilder();
        try {
            Document document = builder.build(fileName);
            Element root = document.getRootElement();
            
            projectDir = root.getChild("ProjectDir").getTextTrim();

            if (projectDir.length() == 0)
                projectDir = fileName.getParentFile().getAbsolutePath();
            numInstances = Integer.valueOf(root.getChild("DataCount").getTextTrim());

            learnerDefaults = new Learner.Properties(root.getChild("Defaults"));
            networks = new ArrayList<Network>();

            Element networksE = root.getChild("Networks");
            for (Object neo:networksE.getChildren("LearnedNetwork")) {
                Element ne = (Element) neo;
                networks.add(new LearnedNetwork(ne, getVdFile()));
            }
            for (Object neo:networksE.getChildren("ConsensusNetwork")) {
                Element ne = (Element) neo;
                networks.add(new ConsensusNetwork(ne, getVdFile()));
            }
            for (Object neo:networksE.getChildren("Network")) {
                Element ne = (Element) neo;
                networks.add(new Network(ne, getVdFile()));
            }
         } catch (JDOMException e) {
            throw new ApplicationException(e.getMessage());
        }        
    }

    public void save(File fileName) throws IOException {
        Element root = new Element("BrightProject");

        Element e;

        e = new Element("Version"); e.setText(VERSION); root.addContent(e);
        
        e = new Element("ProjectDir");
        if (fileName.getParentFile().equals(new File(projectDir)))
            e.setText("");
        else
            e.setText(projectDir);
        root.addContent(e);
 
        e = new Element("DataCount"); e.setText(Integer.toString(numInstances)); root.addContent(e);

        Element defaultsE = new Element("Defaults"); root.addContent(defaultsE);
        learnerDefaults.saveXML(defaultsE);

        Element networksE = new Element("Networks"); root.addContent(networksE);
        for (Network n:networks) {
            e = n.saveToXML(); networksE.addContent(e);
        }

        Document document = new Document();
        document.setRootElement(root);
        
        OutputStream fileStream = new FileOutputStream(fileName);        
        XMLOutputter xml = new XMLOutputter("    ", true);
        xml.output(document, fileStream);
        
        dirty = false;
    }

    public String getBaseName() {
        return "data";
    }

    public String getVdFile() {
        return projectDir + File.separatorChar + getBaseName() + ".vd";
    }

    public String getIdtFile() {
        return projectDir + File.separatorChar + getBaseName() + ".idt";
    }

    public int getNumInstances() {
        return numInstances;
    }

    public void setNumInstances(int numInstances) {
        this.numInstances = numInstances;
        this.dirty = true;
    }

    public String getProjectDir() {
        return projectDir;
    }

    public void setProjectDir(String projectDir) {
        this.projectDir = projectDir;
    }

    public ArrayList<Network> getNetworks() {
        return networks;
    }

    public void addNetwork(Network n) {
        networks.add(n); 
        this.dirty = true;
    }

    public Learner.Properties getLearnerDefaults() {
        return learnerDefaults;
    }

    public boolean isDirty() {
        return dirty;
    }
}
