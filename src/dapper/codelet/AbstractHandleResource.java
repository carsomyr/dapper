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

package dapper.codelet;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Formatter;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.array.ObjectArray;
import shared.util.Control;

/**
 * A partial implementation of {@link Resource}s as handles.
 * 
 * @param <T>
 *            the parameterization lower bounded by {@link AbstractHandleResource} itself.
 * @author Roy Liu
 */
abstract public class AbstractHandleResource<T extends AbstractHandleResource<T>> implements Resource, Iterable<String> {

    final String name;

    /**
     * An {@link ObjectArray} of handle entries.
     */
    protected ObjectArray<String> handleArray;

    /**
     * Counts the number of handle entries.
     */
    protected int nentries;

    /**
     * Default constructor.
     */
    protected AbstractHandleResource(String name, ObjectArray<String> handleArray) {

        this.name = name;

        this.handleArray = handleArray;
        this.nentries = this.handleArray.size(0);
    }

    /**
     * Alternate constructor.
     */
    protected AbstractHandleResource(Node node) {

        NodeList list = node.getChildNodes();

        Node nameNode = list.item(0);
        Control.checkTrue(nameNode.getNodeName().equals("name"));

        Node handlesNode = list.item(1);
        Control.checkTrue(handlesNode.getNodeName().equals("handles"));

        NodeList handlesChildren = handlesNode.getChildNodes();

        int nchildren = handlesChildren.getLength();

        this.handleArray = new ObjectArray<String>(String.class, nchildren << 1, 2);
        this.nentries = nchildren;

        for (int i = 0; i < nchildren; i++) {

            Node entryNode = handlesChildren.item(i);
            Control.checkTrue(entryNode.getNodeName().equals("entry"));

            NodeList entryChildren = entryNode.getChildNodes();

            Node handleNode = entryChildren.item(0);
            Control.checkTrue(handleNode.getNodeName().equals("handle"));

            this.handleArray.set(handleNode.getTextContent(), i, 0);

            Node stemNode = entryChildren.item(1);
            Control.checkTrue(stemNode.getNodeName().equals("handle_stem"));

            this.handleArray.set(stemNode.getTextContent(), i, 1);
        }

        this.name = nameNode.getTextContent();
    }

    /**
     * Gets the number of handles.
     */
    public int nhandles() {
        return this.nentries;
    }

    /**
     * Gets the handle at the given position.
     */
    public String getHandle(int i) {
        return get(i, 0);
    }

    /**
     * Gets the stem at the given position.
     */
    public String getStem(int i) {
        return get(i, 1);
    }

    /**
     * Gets the handle and the stem at the given position.
     */
    public String[] get(int i) {
        return new String[] { getHandle(i), getStem(i) };
    }

    /**
     * Gets the {@link ObjectArray} of handle entries.
     */
    public ObjectArray<String> get() {
        return this.handleArray.subarray(0, this.nentries, 0, 2);
    }

    /**
     * An internal access method.
     */
    protected String get(int i, int j) {

        Control.checkTrue(i < this.nentries, //
                "Index out of bounds");

        return this.handleArray.get(i, j);
    }

    /**
     * Creates an {@link Iterator} over the available handles.
     */
    public Iterator<String> iterator() {

        return new Iterator<String>() {

            int counter = 0;

            public boolean hasNext() {
                return this.counter < AbstractHandleResource.this.nentries;
            }

            public String next() {

                Control.checkTrue(hasNext(), //
                        "No more elements");

                return getHandle(this.counter++);
            }

            public void remove() {
                throw new UnsupportedOperationException("This iterator does not support removal");
            }
        };
    }

    public String getName() {
        return this.name;
    }

    public T setName(String name) {
        throw new UnsupportedOperationException("Cannot set the name of this resource");
    }

    public void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("name")) //
                .setTextContent(this.name);

        Node handlesNode = contentNode.appendChild(doc.createElement("handles"));

        for (int i = 0, n = this.nentries; i < n; i++) {

            Node entryNode = handlesNode.appendChild(doc.createElement("entry"));

            entryNode.appendChild(doc.createElement("handle")) //
                    .setTextContent(this.handleArray.get(i, 0));
            entryNode.appendChild(doc.createElement("handle_stem")) //
                    .setTextContent(this.handleArray.get(i, 1));
        }
    }

    /**
     * Creates a human-readable summary of this {@link Resource}.
     */
    @Override
    public String toString() {

        String[] rawValues = get().values();

        Formatter f = new Formatter();
        f.format("%s[\"%s\", {", getType(), getName());

        int len = rawValues.length;

        if (len > 0) {

            for (int i = 0, n = len - 2; i < n; i += 2) {
                f.format("\"%s\":\"%s\", ", rawValues[i], rawValues[i + 1]);
            }

            f.format("\"%s\":\"%s\"", rawValues[len - 2], rawValues[len - 1]);
        }

        f.format("}]");

        return f.toString();
    }

    public InputStream getInputStream() {
        throw new UnsupportedOperationException("This resource does not support input streams");
    }

    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException("This resource does not support output streams");
    }
}
