/*
 * Created on Sep 29, 2007
 *
 * To change the template for this generated file go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
package bright.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Dimension2D;
import java.awt.geom.NoninvertibleTransformException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.apache.batik.bridge.ViewBox;
import org.apache.batik.gvt.CanvasGraphicsNode;
import org.apache.batik.gvt.CompositeGraphicsNode;
import org.apache.batik.gvt.GraphicsNode;
import org.apache.batik.swing.JSVGCanvas;
import org.apache.batik.swing.JSVGScrollPane;
import org.apache.batik.swing.svg.GVTTreeBuilderAdapter;
import org.apache.batik.swing.svg.GVTTreeBuilderEvent;
import org.apache.batik.swing.svg.SVGDocumentLoaderAdapter;
import org.apache.batik.swing.svg.SVGDocumentLoaderEvent;
import org.apache.batik.util.SVGConstants;
import org.apache.batik.util.gui.resource.JToolbarButton;
import org.w3c.dom.svg.SVGDocument;
import org.w3c.dom.svg.SVGSVGElement;

public class Bright extends JPanel {
    private static final long serialVersionUID = 2973533647984331299L;

    private static final FileFilter projectFileFilter = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().endsWith("xml")
                || f.getName().endsWith("XML")
                || f.isDirectory();
        }
        public String getDescription() {
            return "Bright project (bright.xml) files";
        } };

    private static final FileFilter strFileFilter = new FileFilter() {
            public boolean accept(File f) {
                return f.getName().endsWith("str")
                    || f.getName().endsWith("STR")
                    || f.isDirectory();
            }
            public String getDescription() {
                return "B-Right raw structure files (.str)";
            } };

            private static final FileFilter strsFileFilter = new FileFilter() {
                public boolean accept(File f) {
                    return f.getName().endsWith("strs")
                        || f.getName().endsWith("STRS")
                        || f.isDirectory();
                }
                public String getDescription() {
                    return "Bright MCMC newtork structure files (.strs)";
                } };

    private static final String ABOUT_MESSAGE = "B-Right 0.1 (c) 2007 Koen Deforche, Tomi Silander\n"
          + "\n"
          + "Contact:\n"
          + "    koen.deforche@gmail.com";

    private Project project;

    private JFileChooser fc;
    
    private ArrayList<JMenuItem> haveProjectItems;
    private ArrayList<JMenuItem> haveDataItems;
    private ArrayList<JMenuItem> haveNetworkItems;
    private JFrame topframe;
    private JList networkList;
    private DefaultListModel networkListModel;
    private JSVGCanvas svgNetwork;

    private JLabel status;

    private JLabel networkDetails;

    protected boolean networkChanged;

    public Bright(JFrame frame) {
        project = null;
        this.topframe = frame;
        
        createGUI(frame);
    }

    private void createGUI(final JFrame frame) {        
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        add(createProjectPane());
        
        fc = new JFileChooser();        

        haveProjectItems = new ArrayList<JMenuItem>();
        haveDataItems = new ArrayList<JMenuItem>();
        haveNetworkItems = new ArrayList<JMenuItem>();
        
        final JMenuBar menuBar = new JMenuBar();
        frame.setJMenuBar(menuBar);

        JMenu menu = new JMenu("Project");
        menuBar.add(menu);

        JMenuItem menuItem = new JMenuItem("New project...");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                
                if (dirtyCheckOk()) {
                    fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);        
                    fc.setFileFilter(null);

                    int returnVal = fc.showDialog(Bright.this, "Choose project directory");
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        Project p = new Project();
                        p.setProjectDir(fc.getSelectedFile().getAbsolutePath());

                        File vdFile = new File(p.getVdFile());
                        File idtFile = new File(p.getIdtFile());
                        
                        if (vdFile.exists() || idtFile.exists()) {
                            JOptionPane.showMessageDialog(Bright.this,
                                    "Warning: this project directory already contains data files, possibly from another bright project.\n" +
                                    "After loading data, these will be overwritten, destroying this other project!",
                                    "Warning", JOptionPane.WARNING_MESSAGE);
                        }

                        setProject(p);
                    }
                }
            }
        });
        
        menuItem = new JMenuItem("Open project...");
        menu.add(menuItem);

        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                
                if (dirtyCheckOk()) {
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    fc.setFileFilter(projectFileFilter);

                    int returnVal = fc.showOpenDialog(Bright.this);
                    if (returnVal == JFileChooser.APPROVE_OPTION) {
                        File file = fc.getSelectedFile();
                        try {
                            setProject(new Project(file));
                        } catch (IOException e) {
                            e.printStackTrace();
                            JOptionPane.showMessageDialog(Bright.this,
                                    "I/O error reading file: '" + file.getAbsolutePath() + "': "+ e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        } catch (ApplicationException e) {
                            JOptionPane.showMessageDialog(Bright.this,
                                    "Error reading project file: '" + file.getAbsolutePath() + "': "+ e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        haveProjectItems.add(menuItem = new JMenuItem("Save project...", createImageIcon("Save16.gif")));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                saveProject();
            }
        });

        menu.addSeparator();

        haveProjectItems.add(menuItem = new JMenuItem("Import data...", createImageIcon("Open16.gif")));
        menu.add(menuItem);
        menuItem.addActionListener(openDataFile());
        
        menu.addSeparator();
        
        menuItem = new JMenuItem("Quit");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {                
                if (dirtyCheckOk())
                    System.exit(0);
            }
        });

        menu = new JMenu("Network");
        menuBar.add(menu);

        haveDataItems.add(menuItem = new JMenuItem("Learn from data"));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JDialog d = new LearnerDialog(project, Bright.this, topframe);
                d.setVisible(true);
            }
        });

        menu.addSeparator();
        
        haveDataItems.add(menuItem = new JMenuItem("Import..."));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter(strFileFilter);

                int returnVal = fc.showOpenDialog(Bright.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File strFile = fc.getSelectedFile();
                    try {
                        Network n = new Network(strFile, new File(project.getVdFile()));
                        addNetwork(n);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                        JOptionPane.showMessageDialog(Bright.this,
                                "I/O error reading file: '" + strFile.getAbsolutePath() + "': "+ e1.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }                
            }
        });

        haveNetworkItems.add(menuItem = new JMenuItem("Export..."));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                
            }
        });

        haveNetworkItems.add(menuItem = new JMenuItem("Inference Playground"));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    getSelectedNetwork().startInferencePlayground(project);
                } catch (ApplicationException e1) {
                    JOptionPane.showMessageDialog(Bright.this,
                            e1.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        menu = new JMenu("MCMC");
        menuBar.add(menu);

        haveDataItems.add(menuItem = new JMenuItem("Start MCMC run..."));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JOptionPane.showMessageDialog(Bright.this,
                        "MCMC sampling is not yet integrated in the GUI.",
                        "Not yet implemented", JOptionPane.INFORMATION_MESSAGE);
            }
        });

        haveDataItems.add(menuItem = new JMenuItem("Create consensus..."));
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter(strsFileFilter);

                int returnVal = fc.showOpenDialog(Bright.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File file = fc.getSelectedFile();
                    try {
                        final NetworkSet set = new NetworkSet(file, new File(project.getVdFile()));
                        
                        final JDialog d = new JDialog(topframe, true);
                        d.setTitle("Create consensus network");

                        final JLabel text = new JLabel(file.getName() + " contains " + set.getNetworks().size() + " networks");
                        
                        final JFormattedTextField descriptionField = new JFormattedTextField();
                        descriptionField.setValue("Consensus of " + file.getName());
                        descriptionField.setColumns(20);

                        final JFormattedTextField minimumSupportField = new JFormattedTextField();
                        minimumSupportField.setValue(new Double(0.7));
                        minimumSupportField.setColumns(4);
                        
                        final JCheckBox directed = new JCheckBox("Directed graph");
                        directed.setSelected(false);

                        final JCheckBox ignoreDirection = new JCheckBox("Ignore arc direction");
                        ignoreDirection.setSelected(true);
                        ignoreDirection.setEnabled(false);
                        
                        directed.addChangeListener(new ChangeListener() {
                            public void stateChanged(ChangeEvent arg0) {
                                if (directed.isSelected())
                                    ignoreDirection.setEnabled(true);
                                else {
                                    ignoreDirection.setEnabled(false);
                                    ignoreDirection.setSelected(true);
                                }
                            } });

                        JPanel checkPane = new JPanel();
                        checkPane.setLayout(new GridLayout(0, 1));
                        checkPane.add(directed);
                        checkPane.add(ignoreDirection);

                        JPanel labelPane = new JPanel();
                        labelPane.setLayout(new GridLayout(0, 1));

                        labelPane.add(new JLabel("Description:"));
                        labelPane.add(new JLabel("Minimum support (0.0 - 1.0):"));
                        
                        JPanel fieldPane = new JPanel();
                        fieldPane.setLayout(new GridLayout(0, 1));
                        fieldPane.add(descriptionField);
                        fieldPane.add(minimumSupportField);

                        JPanel settingsPane = new JPanel();
                        settingsPane.setBorder(BorderFactory.createEmptyBorder(5, 10,
                                                                               5, 10));
                        settingsPane.setLayout(new BorderLayout());
                        settingsPane.add(checkPane, BorderLayout.SOUTH);
                        settingsPane.add(labelPane, BorderLayout.CENTER);
                        settingsPane.add(fieldPane, BorderLayout.EAST);
                        
                        JPanel northPane = new JPanel();
                        northPane.add(text);
                        northPane.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
                        
                        JButton okButton = new JButton("Ok");
                        okButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                ConsensusNetwork n = set.computeConsensus(
                                        (Double)minimumSupportField.getValue(), directed.isSelected(), ignoreDirection.isSelected());
                                n.getProperties().setDescription((String)descriptionField.getValue());
                                addNetwork(n);
                                d.setVisible(false);
                            } });
                        
                        JButton cancelButton = new JButton("Cancel");
                        cancelButton.addActionListener(new ActionListener() {
                            public void actionPerformed(ActionEvent e) {
                                d.setVisible(false);
                            } });                       
                        
                        JPanel buttonPane = new JPanel();
                        buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0));
                        
                        buttonPane.add(okButton);
                        buttonPane.add(cancelButton);

                        JPanel topPane = new JPanel();
                        topPane.setLayout(new BorderLayout());
                        topPane.add(settingsPane, BorderLayout.CENTER);
                        topPane.add(northPane, BorderLayout.NORTH);
                        topPane.add(buttonPane, BorderLayout.SOUTH);

                        d.setContentPane(topPane);        
                        d.pack();
                        d.setLocationRelativeTo(topframe);

                        d.setVisible(true);
                    } catch (IOException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(Bright.this,
                                "I/O error reading file: '" + file.getAbsolutePath() + "': "+ e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });


        menu = new JMenu("Settings");
        menuBar.add(menu);
        menuItem = new JMenuItem("Edit...");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JDialog d = new SettingsDialog(topframe);
                d.setVisible(true);
            }

        });

        
        menuBar.add(Box.createHorizontalGlue());
        
        menu = new JMenu("Help");
        menuBar.add(menu);
        
        menuItem = new JMenuItem("About B-Right");
        menu.add(menuItem);
        menuItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(Bright.this,
                                              ABOUT_MESSAGE,
                                              "About", JOptionPane.PLAIN_MESSAGE);
            }

        });

        setEnabled(haveProjectItems, false);
        setEnabled(haveDataItems, false);
        setEnabled(haveNetworkItems, false);
    }

    private void setEnabled(ArrayList<JMenuItem> items, boolean how) {
        for (JMenuItem i:items) {
            i.setEnabled(how);
        }
    }

    /**
     * @return
     */
    private ActionListener openDataFile() {
        return new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (project.getNetworks().size() != 0) {
                    if (JOptionPane.showConfirmDialog(Bright.this,
                            "Warning: your project contains networks, these will be deleted when loading new data.\n" +
                            "Do you want to proceed?",
                            "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
                        return;
                    }
                }               

                fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                fc.setFileFilter(new FileFilter() {
                    public boolean accept(File f) {
                        return f.getName().endsWith("csv")
                            || f.getName().endsWith("CSV")
                            || f.isDirectory();
                    }
                    public String getDescription() {
                        return "Comma-Separated-Value (.csv) files";
                    } });

                int returnVal = fc.showOpenDialog(Bright.this);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    File dataFile = fc.getSelectedFile();
                    Table data;
                    try {
                        data = new Table(new BufferedInputStream(new FileInputStream(dataFile)), false);
                        
                        // TODO: sanity check data
                        
                        project.setNumInstances(data.numRows() - 1);
                        try {
                            File vdFile = new File(project.getVdFile());
                            File idtFile = new File(project.getIdtFile());
                            
                            BufferedOutputStream outputVd = new BufferedOutputStream(new FileOutputStream(vdFile));
                            BufferedOutputStream outputIdt = new BufferedOutputStream(new FileOutputStream(idtFile));
                            data.exportAsVdFiles(outputVd, outputIdt);
                            outputVd.flush();
                            outputIdt.flush();

                            setEnabled(haveDataItems, true);
                            
                            deleteNetworks();
                        } catch (FileNotFoundException e1) {
                            JOptionPane.showMessageDialog(Bright.this,
                                    "I/O error writing: " + e1.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        } catch (IOException e) {
                            JOptionPane.showMessageDialog(Bright.this,
                                    "I/O error writing: " + e.getMessage(),
                                    "Error", JOptionPane.ERROR_MESSAGE);
                        }
                   } catch (FileNotFoundException e1) {
                        JOptionPane.showMessageDialog(Bright.this,
                                "Could not read: " + dataFile.getAbsolutePath(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    } catch (RuntimeException e) {
                        JOptionPane.showMessageDialog(Bright.this,
                                "Error reading " + dataFile.getAbsolutePath() + " " + e.getMessage(),
                                "Error", JOptionPane.ERROR_MESSAGE);
                    }
                } else {
                }
            }
        };
    }

    private Component createProjectPane() {
        final JPanel panel = new JPanel(new BorderLayout());

        status = new JLabel();

        JPanel leftPanel = new JPanel(new BorderLayout());

        JLabel l = new JLabel("Networks:");
        JPanel networksLabelPane = new JPanel();
        networksLabelPane.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        networksLabelPane.add(l);
        networksLabelPane.setLayout(new FlowLayout(FlowLayout.LEFT));
        
        networkListModel = new DefaultListModel();
        networkList = new JList(networkListModel);
        networkList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        networkList.setLayoutOrientation(JList.VERTICAL);        
        networkList.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent arg0) {
                showNetwork(getSelectedNetwork());
            } });

        JScrollPane listScroller = new JScrollPane(networkList);
        listScroller.setPreferredSize(new Dimension(250, 80));

        networkDetails = new JLabel("");
        
        leftPanel.add("North", networksLabelPane);
        leftPanel.add("Center", listScroller);
        leftPanel.add("South", networkDetails);

        panel.add("West", leftPanel);

        svgNetwork = new JSVGCanvas();
        svgNetwork.setPreferredSize(new Dimension(500, 500));
        svgNetwork.setDoubleBufferedRendering(true);

        svgNetwork.addSVGDocumentLoaderListener(new SVGDocumentLoaderAdapter() {
            public void documentLoadingStarted(SVGDocumentLoaderEvent arg0) {
                status.setText("Network loading...");
                networkChanged = true;
            }
            public void documentLoadingCompleted(SVGDocumentLoaderEvent arg0) {
                status.setText("Network loaded.");
            }
        });
        svgNetwork.addGVTTreeBuilderListener(new GVTTreeBuilderAdapter() {
            public void gvtBuildCompleted(GVTTreeBuilderEvent arg0) {
                if (networkChanged) {
                    networkChanged = false;

                    SVGDocument svgDocument = svgNetwork.getSVGDocument();
                    if (svgDocument != null) {
                        SVGSVGElement elt = svgDocument.getRootElement();
                        Dimension dim = svgNetwork.getSize();

                        String viewBox = elt.getAttributeNS
                            (null, SVGConstants.SVG_VIEW_BOX_ATTRIBUTE);

                        AffineTransform Tx;
                        if (viewBox.length() != 0) {
                            String aspectRatio = elt.getAttributeNS
                                (null, SVGConstants.SVG_PRESERVE_ASPECT_RATIO_ATTRIBUTE);
                            Tx = ViewBox.getPreserveAspectRatioTransform
                                (elt, viewBox, aspectRatio, dim.width, dim.height, null);
                        } else {
                            // no viewBox has been specified, create a scale transform
                            Dimension2D docSize = svgNetwork.getSVGDocumentSize();
                            double sx = dim.width / docSize.getWidth();
                            double sy = dim.height / docSize.getHeight();
                            double s = Math.min(sx, sy);
                            Tx = AffineTransform.getScaleInstance(s, s);
                        }

                        GraphicsNode gn = svgNetwork.getGraphicsNode();
                        CanvasGraphicsNode cgn = getCanvasGraphicsNode(gn);
                        if (cgn != null) {
                            AffineTransform vTx = cgn.getViewingTransform();
                            if ((vTx != null) && !vTx.isIdentity()) {
                                try {
                                    AffineTransform invVTx = vTx.createInverse();
                                    Tx.concatenate(invVTx);
                                } catch (NoninvertibleTransformException nite) {
                                    /* nothing */
                                }
                            }
                        }

                        svgNetwork.setRenderingTransform(Tx);
                    }           
                }
                status.setText("");
            }
            public void gvtBuildStarted(GVTTreeBuilderEvent arg0) {
                status.setText("Rendering...");
            } });

        JSVGScrollPane scroller = new JSVGScrollPane(svgNetwork);

        final JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        Action a = svgNetwork.new ZoomAction(0.5);
        JToolbarButton b = new JToolbarButton();
        b.setAction(a);
        b.setIcon(createImageIcon("zoom-out.png"));
        toolBar.add(b);

        a = svgNetwork.new ZoomAction(2);
        b = new JToolbarButton();
        b.setAction(a);
        b.setIcon(createImageIcon("zoom-in.png"));
        toolBar.add(b);

        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
        p.add(toolBar);
        p.add(new JSeparator());
        p.add(status);

        JPanel pcenter = new JPanel(new BorderLayout());
        pcenter.add("North", p);
        pcenter.add("Center", scroller);        

        panel.add("Center", pcenter);        

        return panel;
    }

    protected CanvasGraphicsNode getCanvasGraphicsNode(GraphicsNode gn) {
        if (!(gn instanceof CompositeGraphicsNode))
            return null;
        CompositeGraphicsNode cgn = (CompositeGraphicsNode)gn;
        List children = cgn.getChildren();
        if (children.size() == 0)
            return null;
        gn = (GraphicsNode)cgn.getChildren().get(0);
        if (!(gn instanceof CanvasGraphicsNode))
            return null;
        return (CanvasGraphicsNode)gn;
    }

    
    private void setProject(Project p) {
        this.project = p;

        this.topframe.setTitle("B-Right: " + project.getProjectDir());

        setEnabled(haveProjectItems, true);
        setEnabled(haveDataItems, project.getNumInstances() != 0);
        setEnabled(haveNetworkItems, project.getNetworks().size() != 0);

        networkListModel.clear();
        for (Network n : project.getNetworks()) {
            networkListModel.addElement(n);
        }

        if (networkListModel.size() > 0) {
            networkList.setSelectedIndex(0);
            showNetwork(project.getNetworks().get(0));
        } else
            showNetwork(null);
    }

    private void showNetwork(Network network) {
        networkDetails.setText("No network.");
        
        if (network != null) {
            try {
                File svgFile = File.createTempFile("bright", ".svg");
                network.renderToSvg(project, svgFile);
                svgNetwork.setURI(svgFile.toURL().toString());
                svgFile.deleteOnExit();
            } catch (MalformedURLException e) {
                JOptionPane.showMessageDialog(Bright.this,
                        "Internal error: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(Bright.this,
                        "Error while writing to temporary file: " + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            } catch (ApplicationException e) {
                JOptionPane.showMessageDialog(Bright.this, e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }

            networkDetails.setText(network.detailsHtml());
        } else {
            svgNetwork.setDocument(null);
        }
    }

    private Network getSelectedNetwork() {
        int selectedIndex = networkList.getSelectedIndex();
        if (selectedIndex >= 0)
            return project.getNetworks().get(selectedIndex);
        else
            return null;
    }

    public void addLearnedNetwork(File structFile, String description, Learner.Result result) {
        try {
            LearnedNetwork n = new LearnedNetwork(
                    new LearnedNetwork.Properties(description, new Date(), result, project.getLearnerDefaults()),
                    structFile, new File(project.getVdFile()));

            addNetwork(n);
        } catch (FileNotFoundException e) {
            JOptionPane.showMessageDialog(Bright.this,
                    "Error while accessing project files: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(Bright.this,
                    "Error while reading project files: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * @param n
     */
    private void addNetwork(Network n) {
        project.addNetwork(n);
        networkListModel.addElement(n);
        networkList.setSelectedIndex(project.getNetworks().size() - 1);
        showNetwork(n);

        setEnabled(haveNetworkItems, project.getNetworks().size() != 0);
    }

    protected void deleteNetworks() {
        project.getNetworks().clear();
        networkListModel.clear();
        setEnabled(haveNetworkItems, false);
    }

    /**
     * 
     */
    private boolean saveProject() {
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fc.setFileFilter(projectFileFilter);
        fc.setSelectedFile(new File(project.getProjectDir() + File.separator + "bright.xml"));
      
        int returnVal = fc.showSaveDialog(Bright.this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            try {
                project.save(file);
                
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(Bright.this,
                        "I/O error writing file: '" + file.getAbsolutePath() + "': "+ e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);                        
            }
        }
        
        return false;
    }

    /**
     * @return
     */
    private boolean dirtyCheckOk() {
        boolean proceed = true;

        if (project != null && project.isDirty()) {
            int result = JOptionPane.showConfirmDialog(Bright.this,
                    "Warning: your project contains unsaved changes.\n" +
                    "Do you want to save them first ?",
                    "Warning", JOptionPane.YES_NO_CANCEL_OPTION);
   
            if (result == JOptionPane.YES_OPTION) {
                if (!saveProject())
                    proceed = false;
            } else if (result == JOptionPane.CANCEL_OPTION || result == JOptionPane.CLOSED_OPTION) {
                proceed = false;
            }
        }

        return proceed;
    }

    /** Returns an ImageIcon, or null if the path was invalid. */
    private static ImageIcon createImageIcon(String path) {
        path = "images" + File.separator + path;
        java.net.URL imgURL = Bright.class.getResource("/" + path);
        if (imgURL != null) {
            return new ImageIcon(imgURL);
        } else {
            return new ImageIcon(path);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            String lookAndFeel = UIManager.getSystemLookAndFeelClassName();
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) { 
            e.printStackTrace();
        }

        //Create the top-level container and add contents to it.
        JFrame frame = new JFrame("B-Right");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setContentPane(new Bright(frame));

        //Display the window.
        frame.pack();
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = frame.getPreferredSize();
        frame.setLocation(screenSize.width/2 - (frameSize.width/2), screenSize.height/2 - (frameSize.height/2)); 

        frame.setVisible(true);
    }
}
