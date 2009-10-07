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

import static dapper.event.ControlEvent.ControlEventType.DATA_REQUEST;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.codec.Base64;
import shared.event.Source;

/**
 * A subclass of {@link ControlEvent} for requesting and receiving data.
 * 
 * @author Roy Liu
 */
public class DataRequestEvent extends ControlEvent {

    final String pathname;
    final byte[] data;

    /**
     * Default constructor.
     */
    public DataRequestEvent(String pathname, byte[] data, Source<ControlEvent, SourceType> source) {
        super(DATA_REQUEST, source);

        this.pathname = pathname;
        this.data = data;
    }

    /**
     * Alternate constructor.
     */
    public DataRequestEvent(Node contentNode, Source<ControlEvent, SourceType> source) {
        super(DATA_REQUEST, source);

        NodeList nodeList = contentNode.getChildNodes();

        this.pathname = nodeList.item(0).getTextContent();
        this.data = Base64.base64ToBytes(nodeList.item(1).getTextContent());
    }

    /**
     * Gets the pathname.
     */
    public String getPathname() {
        return this.pathname;
    }

    /**
     * Gets the data.
     */
    public byte[] getData() {
        return this.data;
    }

    @Override
    protected void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.createElement("pathname")) //
                .setTextContent(this.pathname);
        contentNode.appendChild(doc.createElement("data")) //
                .setTextContent(Base64.bytesToBase64(this.data));
    }
}
