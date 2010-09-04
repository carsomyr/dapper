/**
 * <p>
 * Copyright (c) 2008 The Regents of the University of California<br>
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * </p>
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.</li>
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
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
    protected int nEntries;

    /**
     * Default constructor.
     */
    protected AbstractHandleResource(String name, ObjectArray<String> handleArray) {

        this.name = name;

        this.handleArray = handleArray;
        this.nEntries = this.handleArray.size(0);
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

        int nChildren = handlesChildren.getLength();

        this.handleArray = new ObjectArray<String>(String.class, nChildren << 1, 2);
        this.nEntries = nChildren;

        for (int i = 0; i < nChildren; i++) {

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
    public int nHandles() {
        return this.nEntries;
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
        return this.handleArray.subarray(0, this.nEntries, 0, 2);
    }

    /**
     * An internal access method.
     */
    protected String get(int i, int j) {

        Control.checkTrue(i < this.nEntries, //
                "Index out of bounds");

        return this.handleArray.get(i, j);
    }

    /**
     * Creates an {@link Iterator} over the available handles.
     */
    @Override
    public Iterator<String> iterator() {

        return new Iterator<String>() {

            int counter = 0;

            @Override
            public boolean hasNext() {
                return this.counter < AbstractHandleResource.this.nEntries;
            }

            @Override
            public String next() {

                Control.checkTrue(hasNext(), //
                        "No more elements");

                return getHandle(this.counter++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("This iterator does not support removal");
            }
        };
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public T setName(String name) {
        throw new UnsupportedOperationException("Cannot set the name of this resource");
    }

    @Override
    public void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("name")) //
                .setTextContent(this.name);

        Node handlesNode = contentNode.appendChild(doc.createElement("handles"));

        for (int i = 0, n = this.nEntries; i < n; i++) {

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

    @Override
    public InputStream getInputStream() {
        throw new UnsupportedOperationException("This resource does not support input streams");
    }

    @Override
    public OutputStream getOutputStream() {
        throw new UnsupportedOperationException("This resource does not support output streams");
    }
}
