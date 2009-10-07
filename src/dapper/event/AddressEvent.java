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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.ADDRESS;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.codec.Hex;
import shared.event.Source;

/**
 * A subclass of {@link ControlEvent} for conveying an {@link InetSocketAddress} by which a client may be contacted.
 * 
 * @author Roy Liu
 */
public class AddressEvent extends ControlEvent {

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
                    InetAddress.getByAddress(Hex.hexToBytes(nodeList.item(0).getTextContent())), //
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
                .setTextContent(Hex.bytesToHex(this.address.getAddress().getAddress()));
        contentNode.appendChild(doc.createElement("port")) //
                .setTextContent(Integer.toString(this.address.getPort()));
        contentNode.appendChild(doc.createElement("domain")) //
                .setTextContent(this.domain);
    }
}
