/*
 * Created on Oct 4, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class LearnerDialog extends JDialog implements ProgressListener {

    private static final long serialVersionUID = -4618759882137585409L;

    private JFormattedTextField iterationsField;
    private JFormattedTextField coolingsField;
    private JFormattedTextField essField;
    private JFormattedTextField parameterCostField;
    private JButton runButton;
    private JButton cancelButton;

    private Learner learner;

    private JProgressBar progress;

    private final Bright bright;

    private int interpretIntValue(Object object) {
        if (object instanceof Long)
            return ((Long) object).intValue();
        else
            return ((Integer) object).intValue();
    }

    private double interpretDoubleValue(Object object) {
        return ((Double) object).doubleValue();
    }

    public LearnerDialog(final Project project, Bright bright, final Frame frame) {
        super(frame, true);
        this.bright = bright;
        
        setTitle("Learn network");

        JPanel configPane = new JPanel();
        configPane.setLayout(new BorderLayout());

        JLabel iterationsLabel = new JLabel("Iterations: ");
        iterationsField = new JFormattedTextField();
        iterationsField.setValue(new Integer(project.getLearnerDefaults().getIterations()));
        iterationsField.setColumns(10);
        iterationsLabel.setLabelFor(iterationsField);
        
        JLabel coolingsLabel = new JLabel("Coolings: ");        
        coolingsField = new JFormattedTextField();
        coolingsField.setValue(new Integer(project.getLearnerDefaults().getCoolings()));
        coolingsField.setColumns(2);
        coolingsLabel.setLabelFor(coolingsField);

        JLabel essLabel = new JLabel("ESS: ");        
        essField = new JFormattedTextField();
        essField.setValue(new Double(project.getLearnerDefaults().getEss()));
        essField.setColumns(5);
        essLabel.setLabelFor(essField);

        JLabel parameterCostLabel = new JLabel("Extra parameter cost: ");        
        parameterCostField = new JFormattedTextField();
        parameterCostField.setValue(new Double(project.getLearnerDefaults().getParameterCost()));
        parameterCostField.setColumns(5);
        parameterCostLabel.setLabelFor(parameterCostField);
        
        JPanel labelPane = new JPanel();
        labelPane.setLayout(new GridLayout(0, 1));
        labelPane.add(iterationsLabel);
        labelPane.add(coolingsLabel);
        labelPane.add(essLabel);
        labelPane.add(parameterCostLabel);
        
        JPanel fieldPane = new JPanel();
        fieldPane.setLayout(new GridLayout(0, 1));
        fieldPane.add(iterationsField);
        fieldPane.add(coolingsField);
        fieldPane.add(essField);
        fieldPane.add(parameterCostField);

        JPanel settingsPane = new JPanel();
        settingsPane.setBorder(BorderFactory.createEmptyBorder(5, 10,
                                                               5, 10));
        settingsPane.setLayout(new BorderLayout());
        settingsPane.add(labelPane, BorderLayout.CENTER);
        settingsPane.add(fieldPane, BorderLayout.EAST);

        JPanel settingsSpacerPane = new JPanel(new BorderLayout());
        settingsSpacerPane.add(settingsPane, BorderLayout.NORTH);

        JPanel midPane = new JPanel();
        midPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        midPane.setLayout(new BorderLayout());
        midPane.add(settingsSpacerPane, BorderLayout.WEST);
        configPane.add(midPane, BorderLayout.CENTER);

        runButton = new JButton("Run");
        runButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {                
                final String description = (String)JOptionPane.showInputDialog(
                        frame,
                        "Description for learned network:",
                        "Learn Bayesian Network: description",
                        JOptionPane.PLAIN_MESSAGE, null, null,
                        "Learned Network");
                
                if (description != null) {
                    project.getLearnerDefaults().setCoolings(interpretIntValue(coolingsField.getValue()));
                    project.getLearnerDefaults().setEss(interpretDoubleValue(essField.getValue()));
                    project.getLearnerDefaults().setParameterCost(interpretDoubleValue(parameterCostField.getValue()));
                    project.getLearnerDefaults().setIterations(interpretIntValue(iterationsField.getValue()));
                    
                    runButton.setEnabled(false);
                    cancelButton.setEnabled(true);
                    progress.setValue(0);
                    updateProgress(0);

                    Runnable doRun;
                    learner = new Learner(project, LearnerDialog.this);

                    /*
                     * Spawn thread that will do the actual run
                     */
                    doRun = new Runnable() {
                        public void run() {
                            
                            try {
                                final File structFile = File.createTempFile("bright", ".str");
                                Learner.Result result = learner.run(structFile);
                                LearnerDialog.this.bright.addLearnedNetwork(structFile, description, result);

                                runDone(true, "Done");
                            } catch (ApplicationException e) {
                                try {
                                    showMessage(e.getMessage());
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } catch (InvocationTargetException e1) {
                                    e1.printStackTrace();
                                }
                                runDone(false, "Error");
                            } catch (IOException e) {
                                try {
                                    showMessage("Could not create output file: " + e.getMessage());
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                } catch (InvocationTargetException e1) {
                                    e1.printStackTrace();
                                }
                                runDone(false, "Error");
                            }
                            
                            learner = null;
                        }
                    };
         
                    Thread runThread = new Thread(doRun);
                    runThread.start();
                }
            }
        });
        
        cancelButton = new JButton("Cancel");

        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                learner.stop();
            }
        });
        cancelButton.setEnabled(false);
        
        
        JPanel buttonPane = new JPanel();
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        buttonPane.add(runButton);
        buttonPane.add(cancelButton);
        
        progress = new JProgressBar();
        progress.setStringPainted(true);
        progress.setString("Ready");
        
        JPanel lowPane = new JPanel();
        lowPane.add(buttonPane);
        lowPane.setLayout(new BoxLayout(lowPane, BoxLayout.Y_AXIS));
        lowPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JPanel progressPane = new JPanel();
        progressPane.setLayout(new BoxLayout(progressPane, BoxLayout.X_AXIS));
        progressPane.setBorder(BorderFactory.createEmptyBorder(10, 5, 10, 30));
        progressPane.add(new JLabel("Progress: "));
        progressPane.add(progress);
        lowPane.add(progressPane);

        configPane.add(lowPane, BorderLayout.SOUTH);
        setContentPane(configPane);        
        pack();
    }

    private void showMessage(final String message) throws InterruptedException, InvocationTargetException {

        Runnable updateAComponent = new Runnable() {
            public void run() {
                JOptionPane.showMessageDialog(LearnerDialog.this, message,
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        };

        SwingUtilities.invokeAndWait(updateAComponent);
    }

    public void runDone(final boolean success, final String message) {
        Runnable updateAComponent = new Runnable() {
            public void run() {
                if (!success) {
                    runButton.setEnabled(true);
                    cancelButton.setEnabled(false);
                    progress.setString(message);
                } else
                    setVisible(false);
            }
        };

        SwingUtilities.invokeLater(updateAComponent);
    }
    

    public void updateProgress(double percentage) {
        progress.setValue(Math.max(progress.getValue(), (int)percentage));
        progress.setString(String.valueOf("Running: " + progress.getValue() + "%"));
    }
}

