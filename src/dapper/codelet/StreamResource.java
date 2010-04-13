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

package dapper.codelet;

import static dapper.codelet.Resource.ResourceType.INPUT_STREAM;
import static dapper.codelet.Resource.ResourceType.OUTPUT_STREAM;

import java.io.Closeable;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.codec.Hex;
import shared.parallel.Handle;
import shared.util.Control;

/**
 * An implementation of {@link Resource}s as TCP streams.
 * 
 * @param <T>
 *            the stream type.
 * @author Roy Liu
 */
public class StreamResource<T extends Closeable> implements Resource, Locatable, Handle<T>, Identified {

    final ResourceType type;
    final String identifier, name;

    InetSocketAddress address;

    T stream;

    /**
     * Default constructor.
     */
    public StreamResource(ResourceType type, String identifier, String name, InetSocketAddress address) {

        switch (type) {

        case INPUT_STREAM:
        case OUTPUT_STREAM:
            break;

        default:
            throw new IllegalArgumentException("Invalid stream type");
        }

        this.type = type;
        this.identifier = identifier;
        this.name = name;
        this.address = address;

        this.stream = null;
    }

    /**
     * Alternate constructor.
     */
    public StreamResource(ResourceType type, Node node) {

        this.type = type;

        NodeList list = node.getChildNodes();

        Node addressNode = list.item(0);
        Control.checkTrue(addressNode.getNodeName().equals("address"));

        Node portNode = list.item(1);
        Control.checkTrue(portNode.getNodeName().equals("port"));

        Node identifierNode = list.item(2);
        Control.checkTrue(identifierNode.getNodeName().equals("identifier"));

        Node nameNode = list.item(3);
        Control.checkTrue(nameNode.getNodeName().equals("name"));

        String addressContent = addressNode.getTextContent();

        try {

            this.address = !addressContent.equals("") ? new InetSocketAddress( //
                    InetAddress.getByAddress(Hex.hexToBytes(addressContent)), //
                    Integer.parseInt(portNode.getTextContent())) : null;

        } catch (UnknownHostException e) {

            throw new RuntimeException(e);
        }

        this.identifier = identifierNode.getTextContent();
        this.name = nameNode.getTextContent();
    }

    public InputStream getInputStream() {

        Control.checkTrue(this.type == INPUT_STREAM, //
                "Invalid stream type");

        return (InputStream) this.stream;
    }

    public OutputStream getOutputStream() {

        Control.checkTrue(this.type == OUTPUT_STREAM, //
                "Invalid stream type");

        return (OutputStream) this.stream;
    }

    public ResourceType getType() {
        return this.type;
    }

    public T get() {
        return this.stream;
    }

    public void set(T stream) {
        this.stream = stream;
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public StreamResource<T> setAddress(InetSocketAddress address) {

        this.address = address;

        return this;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    public String getName() {
        return this.name;
    }

    public StreamResource<?> setName(String name) {
        throw new UnsupportedOperationException("Cannot set the name of this resource");
    }

    public void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("address")).setTextContent( //
                (this.address != null) ? Hex.bytesToHex(this.address.getAddress().getAddress()) : "");

        contentNode.appendChild(doc.createElement("port")).setTextContent( //
                (this.address != null) ? Integer.toString(this.address.getPort()) : "");

        contentNode.appendChild(doc.createElement("identifier")).setTextContent(this.identifier);

        contentNode.appendChild(doc.createElement("name")).setTextContent(this.name);
    }

    /**
     * Creates a human-readable summary of this {@link Resource}.
     */
    @Override
    public String toString() {
        return String.format("%s[%s, %s]", //
                this.type, this.identifier, (this.address != null) ? this.address : "");
    }
}
