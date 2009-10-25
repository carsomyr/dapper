/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 Roy Liu, The Regents of the University of California <br />
 * <br />
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version. <br />
 * <br />
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. <br />
 * <br />
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 */

package dapper.ui;

import static dapper.Constants.PORT;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shared.cli.CLI;
import shared.cli.CLIOptions;
import shared.cli.CLIOptions.CLIOption;
import shared.log.Logging;
import dapper.server.Server;
import dapper.server.flow.Flow;

/**
 * A visual manager for Dapper {@link Flow}s.
 * 
 * @apiviz.composedOf dapper.ui.FlowBuilderDialog
 * @apiviz.composedOf dapper.ui.FlowPane
 * @apiviz.has dapper.ui.Program - - - argument
 * @author Roy Liu
 */
@CLIOptions(options = {
//
        @CLIOption(opt = "a", longOpt = "archive", numArgs = -1, description = "the execution archive to load"), //
        @CLIOption(opt = "p", longOpt = "port", numArgs = 1, description = "the listening port"), //
        @CLIOption(opt = "c", longOpt = "autocloseidle", numArgs = 0, description = "close idle clients automatically") //
})
@SuppressWarnings("serial")
public class FlowManager extends JFrame {

    /**
     * The instance used for logging.
     */
    final protected static Logger Log = LoggerFactory.getLogger(FlowManager.class);

    // Set up logs.
    static {
        Logging.configureLog4J("dapper/ui/log4j.xml");
    }

    /**
     * Gets the static {@link Logger} instance.
     */
    final public static Logger getLog() {
        return Log;
    }

    final CodeletTree codeletTree;
    final FlowPane flowPane;
    final Server server;

    int screenshotCounter;

    /**
     * Creates a {@link FlowManager}.
     * 
     * @throws IOException
     *             when the underlying {@link Server} instance could not be created.
     */
    public FlowManager(int port) throws IOException {
        super("Flow Manager");

        this.server = new Server(port);

        JPanel main = new JPanel();
        main.setLayout(new GridBagLayout());

        GridBagConstraints c = new GridBagConstraints();

        //

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 1;
        c.gridheight = 2;
        c.weightx = 0.875;
        c.weighty = 1.0;

        this.flowPane = new FlowPane(this.server);

        main.add(new JScrollPane(this.flowPane), c);

        //

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1;
        c.gridy = 0;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 0.125;
        c.weighty = 1.0;

        this.codeletTree = new CodeletTree();

        main.add(new JScrollPane(this.codeletTree), c);

        //

        JButton runButton = new JButton("run");
        runButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FlowManager.this.codeletTree.runSelected();
            }
        });
        runButton.setMinimumSize(new Dimension(50, runButton.getMinimumSize().height));
        runButton.setPreferredSize(new Dimension(50, runButton.getPreferredSize().height));

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 1;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.06125;
        c.weighty = 0.0;

        main.add(runButton, c);

        //

        JButton removeButton = new JButton("unload");
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FlowManager.this.codeletTree.removeSelected();
            }
        });
        removeButton.setMinimumSize(new Dimension(50, removeButton.getMinimumSize().height));
        removeButton.setPreferredSize(new Dimension(50, removeButton.getPreferredSize().height));

        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 2;
        c.gridy = 1;
        c.gridwidth = 1;
        c.gridheight = 1;
        c.weightx = 0.06125;
        c.weighty = 0.0;

        main.add(removeButton, c);

        //

        JMenuBar menuBar = new JMenuBar();

        // Create the 'Options' menu.
        JMenu optionsMenu = new JMenu("Options");

        JMenuItem screenshotSVGItem = new JMenuItem("Take Screenshot (SVG)");
        screenshotSVGItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.ALT_MASK));
        screenshotSVGItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FlowManager.this.flowPane.takeScreenshot();
            }
        });
        optionsMenu.add(screenshotSVGItem);

        JMenuItem screenshotPNGItem = new JMenuItem("Take Screenshot (PNG)");
        screenshotPNGItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, InputEvent.ALT_MASK));
        screenshotPNGItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

                try {

                    ImageIO.write(new Robot().createScreenCapture(getBounds()), "png", //
                            new File(String.format("screenshot-%d.png", //
                                    FlowManager.this.screenshotCounter++)));

                } catch (Exception x) {

                    getLog().info("Error while taking screenshot.", x);

                    return;
                }
            }
        });
        optionsMenu.add(screenshotPNGItem);

        JMenuItem purgeItem = new JMenuItem("Purge Current Flow");
        purgeItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, InputEvent.ALT_MASK));
        purgeItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                FlowManager.this.flowPane.purgeCurrent();
            }
        });
        optionsMenu.add(purgeItem);

        JCheckBoxMenuItem removeFinishedItem = new JCheckBoxMenuItem("Remove Finished Flows");
        removeFinishedItem.setSelected(false);
        removeFinishedItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_R, InputEvent.ALT_MASK));
        removeFinishedItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                FlowManager.this.flowPane.setRemoveFinished(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        optionsMenu.add(removeFinishedItem);

        JCheckBoxMenuItem autoCloseIdleItem = new JCheckBoxMenuItem("Close Idle Clients");
        autoCloseIdleItem.setSelected(false);
        autoCloseIdleItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.ALT_MASK));
        autoCloseIdleItem.addItemListener(new ItemListener() {

            public void itemStateChanged(ItemEvent e) {
                FlowManager.this.flowPane.setAutoCloseIdle(e.getStateChange() == ItemEvent.SELECTED);
            }
        });
        optionsMenu.add(autoCloseIdleItem);

        menuBar.add(optionsMenu);
        setJMenuBar(menuBar);

        //

        this.codeletTree.getSubmitFlowObservable().addObserver(this.flowPane);

        addWindowListener(this.flowPane);

        setContentPane(main);
        setSize(new Dimension(800, 600));
        setLocationRelativeTo(null);

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        this.screenshotCounter = 0;
    }

    /**
     * Gets the {@link CodeletTree}.
     */
    protected CodeletTree getCodeletTree() {
        return this.codeletTree;
    }

    /**
     * Gets the {@link FlowPane}.
     */
    protected FlowPane getFlowPane() {
        return this.flowPane;
    }

    /**
     * Creates the user interface.
     * 
     * @throws ParseException
     *             when the command line arguments couldn't be parsed.
     */
    protected static void createUI(String[] args) throws ParseException {

        int port;

        String[] archiveValues;

        boolean autoCloseIdle;

        try {

            CommandLine cmdLine = CLI.createCommandLine(FlowManager.class, args);

            String portValue = cmdLine.getOptionValue("p");
            port = (portValue != null) ? Integer.parseInt(portValue) : PORT;

            archiveValues = cmdLine.getOptionValues("a");

            if (archiveValues != null && archiveValues.length < 2) {
                throw new ParseException("You must at least specify the archive " //
                        + "followed by the fully qualified target class name");
            }

            autoCloseIdle = cmdLine.hasOption("c");

        } catch (ParseException e) {

            getLog().info(CLI.createHelp(FlowManager.class));

            throw e;
        }

        try {

            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

            FlowManager fm = new FlowManager(port);

            if (archiveValues != null) {
                fm.getCodeletTree().registerJar(new File(archiveValues[0]), archiveValues[1], //
                        Arrays.copyOfRange(archiveValues, //
                                2, archiveValues.length, String[].class));
            }

            fm.getFlowPane().setAutoCloseIdle(autoCloseIdle);

        } catch (Exception e) {

            getLog().info("Error encountered while initializing flow manager.", e);
        }
    }
}
