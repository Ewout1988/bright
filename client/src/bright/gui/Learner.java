/*
 * Created on Sep 30, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;

import org.jdom.Element;

public class Learner {

    private Project project;
    private ProgressListener progress;
    private Process learner;

    public static class Properties {
        private int iterations;
        private int coolings;
        private double ess;
        private double parameterCost;
        
        public Properties() {
            this.iterations = 10000;
            this.coolings = 1;
            this.ess = 1;
            this.parameterCost = 0;
        }

        public int getCoolings() {
            return coolings;
        }

        public void setCoolings(int coolings) {
            this.coolings = coolings;
        }

        public double getEss() {
            return ess;
        }

        public void setEss(double ess) {
            this.ess = ess;
        }

        public int getIterations() {
            return iterations;
        }

        public void setIterations(int iterations) {
            this.iterations = iterations;
        }

        public double getParameterCost() {
            return parameterCost;
        }

        public void setParameterCost(double parameterCost) {
            this.parameterCost = parameterCost;
        }

        public void saveXML(Element result) {
            Element sa = new Element("SimulatedAnnealing"); result.addContent(sa);
            Element e = new Element("Iterations"); e.setText(Integer.toString(iterations)); sa.addContent(e);
            e = new Element("Coolings"); e.setText(Integer.toString(coolings)); sa.addContent(e);
            Element ppScore = new Element("PosteriorProbability"); result.addContent(ppScore);
            e = new Element("Ess"); e.setText(Double.toString(ess)); ppScore.addContent(e);
            e = new Element("ExtraParameterCost"); e.setText(Double.toString(parameterCost)); ppScore.addContent(e);
        }

        public Properties(Element xml) {
            Element sa = xml.getChild("SimulatedAnnealing");
            iterations = Integer.valueOf(sa.getChild("Iterations").getTextTrim());
            coolings = Integer.valueOf(sa.getChild("Coolings").getTextTrim());
            Element ppScore = xml.getChild("PosteriorProbability");
            ess = Double.valueOf(ppScore.getChild("Ess").getTextTrim());
            parameterCost = Double.valueOf(ppScore.getChild("ExtraParameterCost").getTextTrim());
        }
        
        public Properties clone() {
            Properties result = new Properties();
            
            result.coolings = coolings;
            result.ess = ess;
            result.iterations = iterations;
            result.parameterCost = parameterCost;
            
            return result;
        }
    };
    
    public Learner(Project project, ProgressListener progress) {
        this.project = project;
        this.progress = progress;
    }

    public static class Result {
        double score;
        long   evaluations;
        
        Result(double score, long evaluations) {
            this.score = score;
            this.evaluations = evaluations;
        }
    }
    
    public Result run(final File structFile) throws ApplicationException {
        /*
         * Run bnlearner
         */
        learner = null;
        File projectDir = new File(project.getProjectDir());
        Properties properties = project.getLearnerDefaults();
        
        try {
            File reportFile = File.createTempFile("bnlearn", ".stat", projectDir);

            String cmds[] = { Settings.getSettings().getLearnerDir() + "bnlearner", project.getVdFile(), project.getIdtFile(),
                    String.valueOf(project.getNumInstances()), String.valueOf(properties.getEss()),
                    reportFile.getAbsolutePath(), structFile.getAbsolutePath(), String.valueOf(properties.getIterations()),
                    String.valueOf(properties.getCoolings()), String.valueOf(properties.getParameterCost())};


            ProcessBuilder pb = new ProcessBuilder(cmds);
            pb.directory(projectDir);
            Thread progressThread = new Thread(new Runnable() {

                public void run() {
                    InputStream input = learner.getErrorStream();
                    try {
                        doLogWindow(input);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });

            learner = pb.start();
            progressThread.run();
            int result = learner.waitFor();

            if (result != 0) {
                throw new ApplicationException("bnlearner exited with error: " + result);
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(reportFile)));
            String s = in.readLine();
            String[] parts = s.split(" ");

            reportFile.delete();
            
            return new Result(Double.valueOf(parts[4]), Long.valueOf(parts[0]));
        } catch (InterruptedIOException e) {
            throw new ApplicationException("Interrupted!");
        } catch (IOException e) {
            if (learner != null)
                learner.destroy();
            throw new ApplicationException("Error: I/O Error while invoking bnlearner"
                + e.getMessage());
        } catch (InterruptedException e) {
            learner.destroy();
            throw new ApplicationException("Interrupted!");
        }
    }

    public void stop() {
        learner.destroy();        
    }

    private void doLogWindow(InputStream input) throws IOException {
        BufferedReader in
            = new BufferedReader(new InputStreamReader(input));
        String message = "Starting";
        String s = null;
        do {
            s = in.readLine();
            if (s != null) {
                try {
                    double d = Double.valueOf(s);
                    if (progress != null)
                        progress.updateProgress(message, 100 * d);
                    else
                        System.err.println((d * 100) + "%");
                } catch (NumberFormatException e) {
                    message = s;
                    progress.updateProgress(message, 0);                    
                }
            }
        } while (s != null);
    }

}
