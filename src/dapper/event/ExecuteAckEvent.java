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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.EXECUTE_ACK;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.event.Source;
import shared.parallel.Handle;
import shared.util.Control;
import dapper.codelet.ParameterMetadata;

/**
 * A subclass of {@link ControlEvent} for conveying an acknowledgement of successful execution.
 * 
 * @author Roy Liu
 */
public class ExecuteAckEvent extends ControlEvent implements ParameterMetadata, Handle<Object> {

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

    public Object get() {
        return this.tag;
    }

    public void set(Object tag) {
        this.tag = tag;
    }
}
