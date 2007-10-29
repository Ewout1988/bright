/*
 * Created on Oct 22, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;

import org.jdom.Element;

public class LearnedNetwork extends Network {
    public static class Properties {
        private String description;
        private Date computed;
        private double score;
        private long evaluations;
        private Learner.Properties learnerProperties;
        
        public String toString() {
            return description;
        }

        public Date getComputed() {
            return computed;
        }

        public void setComputed(Date computed) {
            this.computed = computed;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
        
        public Properties(String description, Date computed, Learner.Result result, Learner.Properties learnerProperties)
        {
            this.description = description;
            this.computed = computed;
            this.score = result.score;
            this.evaluations = result.evaluations;
            this.learnerProperties = learnerProperties.clone();
        }
        
        public Properties()
        { }

        public Learner.Properties getLearnerProperties() {
            return learnerProperties;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public long getEvaluations() {
            return evaluations;
        }

        public void setEvaluations(long evaluations) {
            this.evaluations = evaluations;
        }
    }
    
    private Properties properties;

    public LearnedNetwork(Properties properties, File strFile, File vdFile) throws FileNotFoundException, IOException { 
        this(properties, new FileInputStream(strFile), new FileInputStream(vdFile));
    }

    public LearnedNetwork(Properties properties, InputStream strFile, InputStream vdFile) throws IOException {
        super(strFile, vdFile);

        this.properties = properties;
    }

    public String toString() {
        return properties.toString();
    }
    
    public Properties getProperties() {
        return properties;
    }

    public LearnedNetwork(Element xml, String vdFile) throws FileNotFoundException, IOException, ApplicationException { 
        this(xml, vdFile == null ? null : new FileInputStream(vdFile));
    }

    public LearnedNetwork(Element xml, InputStream vdFile) throws ApplicationException {
        super(xml, vdFile);

        Element propertiesE = xml.getChild("Properties");

        if (propertiesE != null) {
            properties = new Properties();

            properties.description = propertiesE.getChild("Description").getTextTrim();
            Element computedE = propertiesE.getChild("Computed");
            if (computedE != null) {
                try {
                    properties.computed = DateFormat.getDateTimeInstance().parse(computedE.getChild("Date").getTextTrim());
                    properties.learnerProperties = new Learner.Properties(computedE);
                    properties.score = Double.parseDouble(computedE.getChild("Score").getTextTrim());
                    properties.evaluations = Long.parseLong(computedE.getChild("Evaluations").getTextTrim());
                } catch (ParseException e) {
                    System.err.println("Error parsing date: " + computedE.getChild("Date").getTextTrim());
                    properties.computed = null;
                }
            }
        }
    }

    public Element saveToXML() {
        Element e;

        Element result = super.saveToXML();
        result.setName("LearnedNetwork");
        
        Element propertiesE = new Element("Properties"); result.addContent(propertiesE);
        e = new Element("Description"); e.setText(properties.description); propertiesE.addContent(e);
        
        if (properties.computed != null) {
            Element computedE = new Element("Computed"); propertiesE.addContent(computedE);
            e = new Element("Date"); e.setText(DateFormat.getDateTimeInstance().format(properties.computed)); computedE.addContent(e);
            properties.learnerProperties.saveXML(computedE);
            e = new Element("Score"); e.setText(Double.toString(properties.score)); computedE.addContent(e);
            e = new Element("Evaluations"); e.setText(Long.toString(properties.evaluations)); computedE.addContent(e);
        }

        return result;
    }

    public double getEss() {
        return properties.getLearnerProperties().getEss();
    }
    
    public void writeDot(String dotFile, Project project) throws FileNotFoundException, ApplicationException {
        computeWeights(project);
        PrintStream f = new PrintStream(dotFile);
        writeDot(f, "weights", true);
        f.flush();
        f.close();        
    }
    
    public String detailsHtml() {
        return "<html><table>"
            + "<tr><td><b>Created by:</b></td><td>Simulated annealing</td></tr>"
/*            + "<tr><td><b>Date:</b></td><td>" + properties.getComputed() + "</td></tr>" */
            + "<tr><td><b>ESS:</b></td><td>" + getEss() + "</td></tr>"
            + "<tr><td><b>Extra parameter cost:</b></td><td>" + properties.getLearnerProperties().getParameterCost() + "</td></tr>"
            + "<tr><td><b>Iterations:</b></td><td>" + properties.getLearnerProperties().getIterations() + "</td></tr>"
            + "<tr><td><b>Number of coolings:</b></td><td>" + properties.getLearnerProperties().getCoolings() + "</td></tr>"
            + "<tr><td><b>Score:</b></td><td>" + properties.getScore() + "</td></tr>"
            + "<tr><td><b>Evaluations:</b></td><td>" + properties.getEvaluations() + "</td></tr>"
            + "<tr><td><b>Arcs:</b></td><td>" + getArcCount() + "</td></tr>"
            + "</table></html>";
    }
}
