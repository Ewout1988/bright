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

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;


public class SettingsDialog extends JDialog {

    private static final long serialVersionUID = -4618759882137585409L;

    private JFormattedTextField learnerDirField;
    private JFormattedTextField perlPathField;
    private JFormattedTextField dotPathField;
    private JFormattedTextField wtsProxyHostField;
    private JFormattedTextField wtsProxyPortField;
    private JFormattedTextField wtsPrepObbServiceNameField;
    private JFormattedTextField wtsUrlField;

    private JPanel labelPane;
    private JPanel fieldPane;

    public SettingsDialog(final Frame frame) {
        super(frame);

        final Settings settings = Settings.getSettings();
        
        setTitle("B-Right: settings");

        JPanel configPane = new JPanel();
        configPane.setLayout(new BorderLayout());

        labelPane = new JPanel();
        labelPane.setLayout(new GridLayout(0, 1));
        fieldPane = new JPanel();
        fieldPane.setLayout(new GridLayout(0, 1));

        learnerDirField = addField("Directory with learner binaries: ", settings.getLearnerDir());
        perlPathField = addField("(Optional) perl binary: ", settings.getPerlPath());
        dotPathField = addField("Path to graphviz dot binary: ", settings.getDotPath());

        wtsProxyHostField = addField("HTTP Proxy host, for remote perl utilities: ", settings.getWtsProxyHost());
        wtsProxyPortField = addField("HTTP Proxy port, for remote perl utilities: ", settings.getWtsProxyPort());
        wtsUrlField = addField("Remote perl utilities URL: ", settings.getWtsUrl());
        wtsPrepObbServiceNameField = addField("Remote perl utilities service: ", settings.getWtsPrepObbServiceName());

        JPanel settingsPane = new JPanel();
        settingsPane.setBorder(BorderFactory.createEmptyBorder(5, 10,
                                                               5, 10));
        settingsPane.setLayout(new BorderLayout());
        settingsPane.add(labelPane, BorderLayout.CENTER);
        settingsPane.add(fieldPane, BorderLayout.EAST);

        JPanel settingsSpacerPane = new JPanel(new BorderLayout());
        settingsSpacerPane.add(settingsPane, BorderLayout.CENTER);

        JPanel midPane = new JPanel();
        midPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        midPane.setLayout(new BorderLayout());
        midPane.add(settingsSpacerPane, BorderLayout.WEST);
        configPane.add(midPane, BorderLayout.CENTER);

        JButton okButton = new JButton("Ok and save");        
        JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                settings.setDotPath(dotPathField.getText());
                settings.setLearnerDir(makeProperDir(learnerDirField.getText()));
                settings.setPerlPath(makeProperDir(perlPathField.getText()));
                settings.setWtsProxyHost(wtsProxyHostField.getText());
                settings.setWtsProxyPort(wtsProxyPortField.getText());
                settings.setWtsPrepObbServiceName(wtsPrepObbServiceNameField.getText());
                settings.setWtsUrl(wtsUrlField.getText());
                
                settings.save();
                
                setVisible(false);
            }

            private String makeProperDir(String text) {
                String t = text.trim();
                if (t.length() == 0)
                    return t;
                else
                    if (!t.endsWith(File.separator)) {
                        if (t.endsWith("/") || t.endsWith("\\"))
                            t = t.substring(0, t.length() - 2);
                        t = t + File.separator;
                    }

                return t;
            }});
        
        cancelButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent arg0) {
                setVisible(false);
            }
        });
        
        JPanel buttonPane = new JPanel();
        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
        
        buttonPane.add(okButton);
        buttonPane.add(cancelButton);
                
        JPanel lowPane = new JPanel();
        lowPane.add(buttonPane);
        configPane.add(lowPane, BorderLayout.SOUTH);

        setContentPane(configPane);        
        
        pack();
    }

    /**
     * @param labelText
     * @param value
     */
    private JFormattedTextField addField(String labelText, String value) {
        JLabel label = new JLabel(labelText);
        JFormattedTextField field = new JFormattedTextField(value);
        field.setColumns(40);
        label.setLabelFor(field);
        labelPane.add(label);
        fieldPane.add(field);
        
        return field;
    }
}

