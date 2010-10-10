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

import static dapper.event.ControlEvent.ControlEventType.DATA;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.codec.Codecs;
import shared.event.Source;

/**
 * A subclass of {@link ControlEvent} for requesting and receiving data.
 * 
 * @author Roy Liu
 */
public class DataEvent extends ControlEvent {

    final String pathname;
    final byte[] data;

    /**
     * Default constructor.
     */
    public DataEvent(String pathname, byte[] data, Source<ControlEvent, SourceType> source) {
        super(DATA, source);

        this.pathname = pathname;
        this.data = data;
    }

    /**
     * Alternate constructor.
     */
    public DataEvent(Node contentNode, Source<ControlEvent, SourceType> source) {
        super(DATA, source);

        NodeList nodeList = contentNode.getChildNodes();

        this.pathname = nodeList.item(0).getTextContent();
        this.data = Codecs.base64ToBytes(nodeList.item(1).getTextContent());
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
                .setTextContent(Codecs.bytesToBase64(this.data));
    }
}
