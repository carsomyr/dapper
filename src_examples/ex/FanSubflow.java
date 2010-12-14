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

package ex;

import static org.dapper.Constants.LOCAL;

import java.util.List;

import org.dapper.codelet.Codelet;
import org.dapper.codelet.Resource;
import org.dapper.server.flow.EmbeddingCodelet;
import org.dapper.server.flow.Flow;
import org.dapper.server.flow.FlowEdge;
import org.dapper.server.flow.FlowNode;
import org.dapper.server.flow.HandleEdge;
import org.w3c.dom.Node;

/**
 * An {@link EmbeddingCodelet} that embeds a fan-out followed by fan-in of size equal to the number of incoming
 * {@link HandleEdge}s.
 * 
 * @author Roy Liu
 */
public class FanSubflow implements EmbeddingCodelet {

    /**
     * Default constructor.
     */
    public FanSubflow() {
    }

    @Override
    public void build(Flow flow, //
            List<FlowEdge> inEdges, //
            List<FlowNode> outNodes) {

        FlowNode dn = new FlowNode("ex.Debug") //
                .setDomainPattern(LOCAL);

        FlowNode outNode = outNodes.get(0);

        for (FlowEdge inEdge : inEdges) {

            switch (inEdge.getType()) {

            case STREAM:
            case DUMMY:
                inEdge.setV(outNode);
                break;

            case HANDLE:

                FlowNode newNode = dn.clone();
                flow.add(newNode);
                flow.add(new HandleEdge(newNode, outNode));

                inEdge.setV(newNode);

                break;
            }
        }
    }

    @Override
    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) {
    }

    @Override
    public Node getEmbeddingParameters() {
        return null;
    }

    @Override
    public void setEmbeddingParameters(Node parameters) {
    }

    /**
     * Creates a human-readable description of this {@link Codelet}.
     */
    @Override
    public String toString() {
        return "Fan";
    }
}
