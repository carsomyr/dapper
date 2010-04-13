/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 The Regents of the University of California <br />
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

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

import dapper.server.flow.FlowBuilder;

/**
 * A subclass of {@link JDialog} for aiding in the creation of command line arguments for {@link FlowBuilder}
 * instantiation.
 * 
 * @author Roy Liu
 */
@SuppressWarnings("serial")
public class FlowBuilderDialog extends JDialog {

    /**
     * Default constructor.
     */
    public FlowBuilderDialog(final CodeletTree tree, final Class<? extends FlowBuilder> clazz, final ClassLoader cl) {

        super((Frame) null, false);

        setTitle("Arguments");

        final String[] argumentNames = clazz.getAnnotation(Program.class).arguments();

        JPanel main = new JPanel();

        main.setLayout(new GridBagLayout());
        main.setBorder(null);
        main.setOpaque(true);
        main.setFocusable(false);
        main.setBackground(Color.white);
        main.setForeground(Color.black);

        GridBagConstraints c = new GridBagConstraints();

        final JTextField[] textFields = new JTextField[argumentNames.length];

        for (int i = 0, n = argumentNames.length; i < n; i++) {

            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 0;
            c.gridy = i;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.5;
            c.weighty = 1.0 / (n + 1);

            main.add(new JLabel(argumentNames[i]), c);

            c.fill = GridBagConstraints.BOTH;
            c.anchor = GridBagConstraints.CENTER;
            c.gridx = 1;
            c.gridy = i;
            c.gridwidth = 1;
            c.gridheight = 1;
            c.weightx = 0.5;
            c.weighty = 1.0 / (n + 1);

            main.add(textFields[i] = new JTextField(), c);
        }

        c.fill = GridBagConstraints.BOTH;
        c.anchor = GridBagConstraints.CENTER;
        c.gridx = 0;
        c.gridy = argumentNames.length;
        c.gridwidth = 2;
        c.gridheight = 1;
        c.weightx = 1.0;
        c.weighty = 1.0 / (argumentNames.length + 1);

        JButton dismissButton = new JButton("ok");

        dismissButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent evt) {

                String[] args = new String[argumentNames.length];

                for (int i = 0, n = argumentNames.length; i < n; i++) {
                    args[i] = textFields[i].getText();
                }

                tree.runFromCommands(clazz, cl, args);

                ((Frame) getParent()).dispose();
            }
        });

        main.add(dismissButton, c);

        //

        setContentPane(main);
        setSize(new Dimension(100 + (argumentNames.length * 100), 200));
        setLocationRelativeTo(null);

        setVisible(true);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    }
}
