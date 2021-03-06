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

import static org.dapper.event.ControlEvent.ControlEventType.EXECUTE_ACK;

import org.dapper.codelet.ParameterMetadata;
import org.shared.event.Source;
import org.shared.parallel.Handle;
import org.shared.util.Control;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A subclass of {@link ControlEvent} for conveying an acknowledgement of successful execution.
 * 
 * @author Roy Liu
 */
public class ExecuteAckEvent extends BaseControlEvent implements ParameterMetadata, Handle<Object> {

    final Node embeddingParameters, edgeParameters;

    Object tag;

    /**
     * Default constructor.
     */
    public ExecuteAckEvent(Node embeddingParameters, Node edgeParameters, Source<ControlEvent, SourceType> source) {
        super(EXECUTE_ACK, source);

        this.embeddingParameters = embeddingParameters;
        Control.checkTrue(this.embeddingParameters.getNodeName().equals("parameters"));

        this.edgeParameters = edgeParameters;
        Control.checkTrue(this.edgeParameters.getNodeName().equals("edge_parameters"));

        this.tag = null;
    }

    /**
     * Alternate constructor.
     */
    public ExecuteAckEvent(Node contentNode, Source<ControlEvent, SourceType> source) {
        super(EXECUTE_ACK, source);

        NodeList l1 = contentNode.getChildNodes();

        this.embeddingParameters = l1.item(0);
        Control.checkTrue(this.embeddingParameters.getNodeName().equals("parameters"));

        this.edgeParameters = l1.item(1);
        Control.checkTrue(this.edgeParameters.getNodeName().equals("edge_parameters"));

        this.tag = null;
    }

    @Override
    public Node getParameters() {
        return this.embeddingParameters;
    }

    /**
     * Gets the edge parameters.
     */
    public Node getEdgeParameters() {
        return this.edgeParameters;
    }

    @Override
    protected void getContents(Node contentNode) {

        Document doc = contentNode.getOwnerDocument();

        contentNode.appendChild(doc.importNode(this.embeddingParameters, true));
        contentNode.appendChild(doc.importNode(this.edgeParameters, true));
    }

    @Override
    public Object get() {
        return this.tag;
    }

    @Override
    public void set(Object tag) {
        this.tag = tag;
    }
}
