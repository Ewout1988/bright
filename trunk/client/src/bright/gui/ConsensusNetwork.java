/*
 * Created on Oct 22, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class ConsensusNetwork extends Network {
    public static class Properties {
        private boolean directed;
        private boolean ignoredArcDirection;
        private double minimumSupport;
        private String description;

        public String toString() {
            return description;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
        
        public boolean isDirected() {
            return directed;
        }

        public void setDirected(boolean directed) {
            this.directed = directed;
        }

        public Properties(String description, boolean directed, boolean ignoredArcDirection, double minimumSupport)
        {
            this.description = description;
            this.directed = directed;
            this.ignoredArcDirection = ignoredArcDirection;
            this.minimumSupport = minimumSupport;
        }

        public boolean ignoredArcDirection() {
            return ignoredArcDirection;
        }

        public void setIgnoredArcDirection(boolean ignoredArcDirection) {
            this.ignoredArcDirection = ignoredArcDirection;
        }

        public double getMinimumSupport() {
            return minimumSupport;
        }

        public void setMinimumSupport(double minimumSupport) {
            this.minimumSupport = minimumSupport;
        }
    }
    
    private Properties properties;

    public ConsensusNetwork(double minimumSupport, boolean directed, boolean ignoreArcDirections, Network template) { 
        super(template);

        this.properties = new Properties("", directed, ignoreArcDirections, minimumSupport);
    }

    public String toString() {
        return properties.toString();
    }
    
    public Properties getProperties() {
        return properties;
    }

    public void addArc(int head, int tail, boolean directed, double support) {
        if (!directed && tail < head) {
           int sw =  head;
           head = tail;
           tail = sw;
        }
        
        if (addArc(head, tail, directed)) {
            variables.get(tail).setSupport(head, support);
        }
    }

    @Override
    public void writeDot(String dotFile, Project project) throws FileNotFoundException, ApplicationException {
        PrintStream f = new PrintStream(dotFile);
        writeDot(f, "support", properties.isDirected());
        f.flush();
        f.close();        
    }
    
    public String detailsHtml() {
        return "<html><table>"
            + "<tr><td><b>Created by:</b></td><td>Consensus</td></tr>"
            + "<tr><td><b>Directed:</b></td><td>" + (properties.isDirected() ? "Yes" : "No") + "</td></tr>"
            + (properties.isDirected()
                    ? "<tr><td><b>Bidirectional support:</b></td><td>" + (properties.ignoredArcDirection() ? "Yes" : "No") + "</td></tr>"
                    : "")
            + "<tr><td><b>Minimum support:</b></td><td>" + (properties.getMinimumSupport()) + "</td></tr>"
            + "<tr><td><b>Arcs:</b></td><td>" + getArcCount() + "</td></tr>"
            + "</table></html>";
    }

}
