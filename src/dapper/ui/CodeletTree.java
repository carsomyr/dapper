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

import java.awt.Color;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.List;
import java.util.Observable;
import java.util.Scanner;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import shared.metaclass.JarRegistry;
import shared.metaclass.RegistryClassLoader;
import shared.util.Control;
import dapper.codelet.Codelet;
import dapper.server.ServerProcessor.FlowBuildRequest;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;

/**
 * A visual hierarchy of Dapper execution archives and their {@link Codelet}s.
 * 
 * @apiviz.composedOf dapper.ui.CodeletTree.RunnableNode
 * @author Roy Liu
 */
@SuppressWarnings("serial")
public class CodeletTree extends JTree implements DropTargetListener {

    /**
     * A {@link Pattern} for detecting binary class names.
     */
    final public static Pattern BinaryNamePattern = Pattern.compile("(" //
            // Capture names of base classes.
            + "(:?[a-zA-Z_][0-9a-zA-Z_]*/)*[a-zA-Z_][0-9a-zA-Z_]*"
            // Capture names of inner classes.
            + "(:?\\$[0-9]+|\\$[a-zA-Z_][0-9a-zA-Z_]*)*" //
            + ")\\.class");

    final MutableTreeNode rootNode;
    final DefaultTreeModel treeModel;

    final Observable submitFlowObservable;

    /**
     * Default constructor.
     */
    public CodeletTree() {
        super(new DefaultTreeModel(new DefaultMutableTreeNode("Archives")));

        setBackground(Color.white);
        setForeground(Color.black);

        // Allow discontiguous selection.
        getSelectionModel().setSelectionMode(TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION);

        new DropTarget(this, this);

        this.submitFlowObservable = new Observable() {

            @Override
            public void notifyObservers() {

                setChanged();
                super.notifyObservers();
            }

            @Override
            public void notifyObservers(Object arg) {

                setChanged();
                super.notifyObservers(arg);
            }
        };

        this.treeModel = (DefaultTreeModel) getModel();
        this.rootNode = (DefaultMutableTreeNode) this.treeModel.getRoot();
    }

    /**
     * Processes a {@link DropTargetDropEvent} over this component.
     */
    @SuppressWarnings("unchecked")
    public void drop(DropTargetDropEvent dtde) {

        Transferable tr = dtde.getTransferable();

        try {

            List<File> files = null;

            for (DataFlavor df : tr.getTransferDataFlavors()) {

                if (df.isFlavorJavaFileListType()) {

                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                    files = (List<File>) tr.getTransferData(df);
                    dtde.dropComplete(true);

                    break;

                } else if (df.isFlavorTextType()) {

                    dtde.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);

                    files = new ArrayList<File>();

                    for (Scanner scanner = new Scanner((Reader) tr.getTransferData(df)); //
                    scanner.hasNextLine();) {
                        files.add(new File(new URL(scanner.nextLine()).toURI()));
                    }

                    dtde.dropComplete(true);

                    break;
                }
            }

            Control.checkTrue(files != null, //
                    "No suitable data flavor found");

            for (File file : files) {
                registerJar(file, null, null);
            }

        } catch (Throwable t) {

            FlowManager.getLog().info("Failed to import archive.", t);
        }
    }

    /**
     * Registers a Jar with the user interface.
     * 
     * @throws ClassNotFoundException
     *             when some class(es) could not be found.
     * @throws IOException
     *             when something goes awry.
     */
    protected void registerJar(File file, String targetName, final String[] args) throws IOException,
            ClassNotFoundException {

        List<String> classNames = new ArrayList<String>();

        JarRegistry jr = new JarRegistry(new JarFile(file));

        final RegistryClassLoader rcl = new RegistryClassLoader();
        rcl.addRegistry(jr);

        for (String pathname : jr.getDataMap().keySet()) {

            Matcher m = BinaryNamePattern.matcher(pathname);

            if (m.matches()) {
                classNames.add(m.group(1).replace("/", "."));
            }
        }

        List<Class<? extends FlowBuilder>> builderClasses = getBuilders(rcl, classNames);
        List<RunnableNode> children = new ArrayList<RunnableNode>();

        for (final Class<? extends FlowBuilder> clazz : builderClasses) {

            children.add(new RunnableNode(clazz.getName()) {

                @Override
                public void run() {
                    runFromJar(clazz, rcl);
                }
            });
        }

        addChildren(file.getName(), children, true);

        if (targetName == null) {
            return;
        }

        Class<? extends FlowBuilder> tmp = null;

        loop: for (final Class<? extends FlowBuilder> clazz : builderClasses) {

            if (targetName.equals(clazz.getName())) {

                tmp = clazz;

                break loop;
            }
        }

        final Class<? extends FlowBuilder> targetClass = tmp;

        Control.checkTrue(targetClass != null, //
                "Target class not found");

        String[] argNames = targetClass.getAnnotation(Program.class).arguments();

        if (argNames.length != args.length) {

            Formatter f = new Formatter();
            f.format("Expecting arguments");

            for (String argName : argNames) {
                f.format(" [%s]", argName);
            }

            throw new IllegalArgumentException(f.toString());
        }

        RunnableNode childNode = new RunnableNode(targetClass.getName()) {

            @Override
            public void run() {
                runFromCommands(targetClass, rcl, args);
            }
        };

        addChildren("<initial>", Collections.singletonList(childNode), false);

        // Simulate a user "run" button press.
        childNode.run();
    }

    /**
     * Derives a {@link RunnableNode} from the contents of a Jar file.
     */
    protected void runFromJar( //
            Class<? extends FlowBuilder> clazz, //
            ClassLoader cl) {

        String[] args = clazz.getAnnotation(Program.class).arguments();

        // Inspect annotations and decide if a dialog box is necessary.
        if (args.length > 0) {

            new FlowBuilderDialog(this, clazz, cl);

        } else {

            runFromCommands(clazz, cl, new String[] {});
        }
    }

    /**
     * Derives a {@link RunnableNode} from the given arguments.
     */
    protected void runFromCommands(Class<? extends FlowBuilder> clazz, ClassLoader cl, String[] args) {

        Observable sfo = CodeletTree.this.submitFlowObservable;

        try {

            sfo.notifyObservers(new FlowBuildRequest(clazz.getConstructor(String[].class) //
                    .newInstance(new Object[] { args }), cl, null));

        } catch (Exception e) {

            FlowManager.getLog().info("Failed to start flow.", e);
        }
    }

    /**
     * Loads {@link FlowBuilder}s from the given {@link ClassLoader}.
     * 
     * @throws ClassNotFoundException
     *             when some class(es) could not be found.
     */
    @SuppressWarnings("unchecked")
    final protected static List<Class<? extends FlowBuilder>> getBuilders( //
            ClassLoader cl, List<String> classNames) throws ClassNotFoundException {

        List<Class<? extends FlowBuilder>> builders = new ArrayList<Class<? extends FlowBuilder>>();

        for (String className : classNames) {

            Class<?> clazz = cl.loadClass(className);

            Program programAnnotation = clazz.getAnnotation(Program.class);

            if (programAnnotation != null) {

                Control.checkTrue(FlowBuilder.class.isAssignableFrom(clazz), //
                        "Class must be able to build flows");

                builders.add((Class<? extends FlowBuilder>) clazz);
            }
        }

        return builders;
    }

    /**
     * Registers an execution archive node along with its {@link RunnableNode} children with this tree.
     */
    public void addChildren(String name, List<RunnableNode> children, boolean append) {

        DefaultMutableTreeNode jarNode = new DefaultMutableTreeNode(name);
        TreePath jarPath = new TreePath(this.rootNode).pathByAddingChild(jarNode);

        this.treeModel.insertNodeInto(jarNode, this.rootNode, //
                append ? this.treeModel.getChildCount(this.rootNode) : 0);

        for (int i = 0, n = children.size(); i < n; i++) {

            this.treeModel.insertNodeInto(children.get(i), jarNode, i);

            TreePath childPath = jarPath.pathByAddingChild(children.get(i));

            makeVisible(childPath);
            scrollRectToVisible(getPathBounds(childPath));
        }
    }

    /**
     * Gets the {@link Flow} submission {@link Observable}.
     */
    public Observable getSubmitFlowObservable() {
        return this.submitFlowObservable;
    }

    /**
     * Runs all selected {@link RunnableNode}s.
     */
    public void runSelected() {

        TreePath[] currentSelections = getSelectionPaths();

        if (currentSelections == null) {
            return;
        }

        for (TreePath currentSelection : currentSelections) {

            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentSelection.getLastPathComponent();

            if (currentNode instanceof RunnableNode) {
                ((Runnable) currentNode).run();
            }
        }
    }

    /**
     * Removes all selected execution archive nodes.
     */
    public void removeSelected() {

        TreePath[] currentSelections = getSelectionPaths();

        if (currentSelections == null) {
            return;
        }

        for (TreePath currentSelection : currentSelections) {

            DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode) currentSelection.getLastPathComponent();
            MutableTreeNode parent = (MutableTreeNode) currentNode.getParent();

            if (parent == this.rootNode) {
                this.treeModel.removeNodeFromParent(currentNode);
            }
        }
    }

    /**
     * A subclass of {@link DefaultMutableTreeNode} that contains runnable code.
     */
    abstract protected static class RunnableNode extends DefaultMutableTreeNode implements Runnable {

        /**
         * Default constructor.
         */
        protected RunnableNode(String name) {
            super(name);
        }

        /**
         * Runs code associated with this node.
         */
        abstract public void run();
    }

    //

    /**
     * Does nothing.
     */
    public void dragEnter(DropTargetDragEvent dtde) {
        // Do nothing.
    }

    /**
     * Does nothing.
     */
    public void dragExit(DropTargetEvent dte) {
        // Do nothing.
    }

    /**
     * Does nothing.
     */
    public void dragOver(DropTargetDragEvent dtde) {
        // Do nothing.
    }

    /**
     * Does nothing.
     */
    public void dropActionChanged(DropTargetDragEvent dtde) {
        // Do nothing.
    }
}
