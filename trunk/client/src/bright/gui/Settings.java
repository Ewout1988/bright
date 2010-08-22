/*
 * Created on Oct 5, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;
import org.jdom.output.XMLOutputter;

public class Settings {
    private static final String VERSION = "0.1";

    private String learnerDir;
    private String perlPath;
    private String dotPath;
    private String wtsProxyHost;
    private String wtsProxyPort;
    private String wtsPrepObbServiceName;
    private String wtsUrl;
    
    static private Settings singleton = null;
    
    static public Settings getSettings() 
    {
        if (singleton == null) {
            singleton = readSettings();
        }
        
        return singleton;
    }

    private Settings(File home) {
        learnerDir = "bin" + File.separator;
        dotPath = "Graphviz" + File.separator + "bin" + File.separator + "dot";
        perlPath = "";

        wtsProxyHost = "";
        wtsProxyPort = "";
        wtsPrepObbServiceName = "bright-prepobb";
        wtsUrl = "http://regadb.med.kuleuven.be/wts/services/";

        if (home != null) {
            learnerDir = home.getAbsolutePath() + File.separator + learnerDir;
            dotPath = home.getAbsolutePath() + File.separator + dotPath;
        }
    }

    private static Settings readSettings() {
        File home = homeLocation();

        Settings result = new Settings(home);

        if (home != null) {
            File settingsFile = new File(home.getAbsolutePath() + File.separator + "settings.xml");
            
            if (settingsFile.canRead()) {
                SAXBuilder builder = new SAXBuilder();
                try {
                    Document document = builder.build(settingsFile);
                    Element root = document.getRootElement();
                    result.learnerDir = root.getChild("LearnerDir").getTextTrim();
                    result.perlPath = root.getChild("PerlPath").getTextTrim();
                    result.dotPath = root.getChild("DotPath").getTextTrim();

                    Element wts = root.getChild("Wts");
                    result.wtsProxyHost = wts.getChild("ProxyHost").getTextTrim();
                    result.wtsProxyPort = wts.getChild("ProxyPort").getTextTrim();
                    result.wtsUrl = wts.getChild("Url").getTextTrim();
                    result.wtsPrepObbServiceName = wts.getChild("PrepObbServiceName").getTextTrim();
                } catch (JDOMException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        if (result.wtsProxyHost.length() > 0) {
            System.setProperty("http.proxyHost", result.wtsProxyHost);
            System.setProperty("http.proxyPort", result.wtsProxyPort);
        }
        
        return result;
    }

    public void save() {
        Element root = new Element("BrightSettings");
        Element e;
        e = new Element("Version"); e.setText(VERSION); root.addContent(e);
        e = new Element("LearnerDir"); e.setText(learnerDir); root.addContent(e);
        e = new Element("PerlPath"); e.setText(perlPath); root.addContent(e);
        e = new Element("DotPath"); e.setText(dotPath); root.addContent(e);
        
        Element wts = new Element("Wts"); root.addContent(wts);
        e = new Element("ProxyHost"); e.setText(wtsProxyHost); wts.addContent(e);
        e = new Element("ProxyPort"); e.setText(wtsProxyPort); wts.addContent(e);
        e = new Element("Url"); e.setText(wtsUrl); wts.addContent(e);
        e = new Element("PrepObbServiceName"); e.setText(wtsPrepObbServiceName); wts.addContent(e);
        
        Document document = new Document();
        document.setRootElement(root);

        if (wtsProxyHost.length() > 0) {
            System.setProperty("http.proxyHost", wtsProxyHost);
            System.setProperty("http.proxyPort", wtsProxyPort);
        } else {
            System.clearProperty("http.proxyHost");
            System.clearProperty("http.proxyPort");
        }

        try {
            OutputStream fileStream = new FileOutputStream(settingsLocation());        
            XMLOutputter xml = new XMLOutputter("    ", true);
            xml.output(document, fileStream);
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }
    
    private static File homeLocation() {
       try {
            File jarFile = new File(Settings.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI());

            File dir;
            
            if (jarFile.isDirectory()) {
                if (jarFile.getName().equalsIgnoreCase("bin")) {
                    dir = jarFile.getParentFile();
                } else
                    dir = jarFile;
            } else
                dir = jarFile.getParentFile();
            
            return dir;
        } catch (URISyntaxException e) {
            System.err.println(e.getMessage());
        }
        
        return null;
    }

    private static File settingsLocation() {
        return new File(homeLocation().getAbsolutePath() + File.separator + "settings.xml");
    }

    public String getDotPath() {
        return dotPath;
    }

    public void setDotPath(String dotPath) {
        this.dotPath = dotPath;
    }

    public String getLearnerDir() {
        return learnerDir;
    }

    public void setLearnerDir(String learnerDir) {
        this.learnerDir = learnerDir;
    }

    public String getPerlPath() {
        return perlPath;
    }

    public void setPerlPath(String perlPath) {
        this.perlPath = perlPath;
    }

    public String getWtsPrepObbServiceName() {
        return wtsPrepObbServiceName;
    }

    public void setWtsPrepObbServiceName(String wtsPrepObbServiceName) {
        this.wtsPrepObbServiceName = wtsPrepObbServiceName;
    }

    public boolean runPrepObbLocally() {
        return (perlPath != null) && (perlPath.length() > 0);
    }
    
    public String getWtsProxyHost() {
        return wtsProxyHost;
    }

    public void setWtsProxyHost(String wtsProxyHost) {
        this.wtsProxyHost = wtsProxyHost;
    }

    public String getWtsProxyPort() {
        return wtsProxyPort;
    }

    public void setWtsProxyPort(String wtsProxyPort) {
        this.wtsProxyPort = wtsProxyPort;
    }

    public String getWtsUrl() {
        return wtsUrl;
    }

    public void setWtsUrl(String wtsUrl) {
        this.wtsUrl = wtsUrl;
    }
    
}
