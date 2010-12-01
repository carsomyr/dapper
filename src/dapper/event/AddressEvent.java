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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.ADDRESS;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.codec.Codecs;
import shared.event.Source;

/**
 * A subclass of {@link ControlEvent} for conveying an {@link InetSocketAddress} by which a client may be contacted.
 * 
 * @author Roy Liu
 */
public class AddressEvent extends BaseControlEvent {

    final InetSocketAddress address;
    final String domain;

    /**
     * Default constructor.
     */
    public AddressEvent(InetSocketAddress address, String domain, Source<ControlEvent, SourceType> source) {
        super(ADDRESS, source);

        this.address = address;
        this.domain = domain;
    }

    /**
     * Alternate constructor.
     */
    public AddressEvent(Node contentNode, Source<ControlEvent, SourceType> source) {
        super(ADDRESS, source);

        NodeList nodeList = contentNode.getChildNodes();

        try {

            this.address = new InetSocketAddress( //
                    InetAddress.getByAddress(Codecs.hexToBytes(nodeList.item(0).getTextContent())), //
                    Integer.parseInt(nodeList.item(1).getTextContent()));

        } catch (UnknownHostException e) {

            throw new RuntimeException(e);
        }

        this.domain = nodeList.item(2).getTextContent();
    }

    /**
     * Gets the {@link InetSocketAddress}.
     */
    public InetSocketAddress getAddress() {
        return this.address;
    }

    /**
     * Gets the execution domain.
     */
    public String getDomain() {
        return this.domain;
    }

    @Override
    protected void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("address")) //
                .setTextContent(Codecs.bytesToHex(this.address.getAddress().getAddress()));
        contentNode.appendChild(doc.createElement("port")) //
                .setTextContent(Integer.toString(this.address.getPort()));
        contentNode.appendChild(doc.createElement("domain")) //
                .setTextContent(this.domain);
    }
}
