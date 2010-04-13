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

package ex;

import static dapper.Constants.LOCAL;

import java.util.List;

import org.w3c.dom.Node;

import dapper.codelet.Resource;
import dapper.server.flow.EmbeddingCodelet;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.HandleEdge;

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

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) {
    }

    public Node getEmbeddingParameters() {
        return null;
    }

    public void setEmbeddingParameters(Node parameters) {
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Fan";
    }
}
