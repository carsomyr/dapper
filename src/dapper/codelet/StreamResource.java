/**
 * <p>
 * Copyright (C) 2008 The Regents of the University of California<br />
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
