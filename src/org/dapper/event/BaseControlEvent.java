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

package org.dapper.event;

import org.dapper.DapperBase;
import org.shared.event.Source;
import org.shared.event.XmlEvent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * An implementation of {@link ControlEvent} that can be subclassed into user-defined events.
 * 
 * @apiviz.owns org.dapper.event.ControlEvent.ControlEventType
 * @author Roy Liu
 */
public class BaseControlEvent implements ControlEvent {

    final ControlEventType type;
    final Source<ControlEvent, SourceType> source;

    /**
     * Default constructor.
     */
    public BaseControlEvent(ControlEventType type, Source<ControlEvent, SourceType> source) {

        this.type = type;
        this.source = source;
    }

    /**
     * Transfers the contents of this event into the given DOM {@link Node}.
     */
    protected void getContents(Node contentNode) {
    }

    @Override
    public Element toDom() {

        Document doc = DapperBase.newDocument();

        Element rootElement = doc.createElement(XmlEvent.class.getName());

        rootElement.appendChild(doc.createElement("type")) //
                .setTextContent(getType().toString());

        getContents(rootElement.appendChild(doc.createElement("content")));

        return rootElement;
    }

    @Override
    public Source<ControlEvent, SourceType> getSource() {
        return this.source;
    }

    @Override
    public ControlEventType getType() {
        return this.type;
    }

    /**
     * Parses a {@link ControlEvent} from the given root DOM {@link Element}.
     */
    public static ControlEvent parse(Node rootElement, Source<ControlEvent, SourceType> source) {

        NodeList children = rootElement.getChildNodes();
        return ControlEventType.valueOf(children.item(0).getTextContent()).parse(children.item(1), source);
    }
}
