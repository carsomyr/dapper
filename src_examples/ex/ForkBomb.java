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

package ex;

import static dapper.Constants.LOCAL;

import java.util.List;

import org.w3c.dom.Node;

import shared.util.Arithmetic;
import dapper.codelet.CodeletUtilities;
import dapper.codelet.Resource;
import dapper.server.flow.EmbeddingCodelet;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.HandleEdge;
import dapper.server.flow.StreamEdge;

/**
 * An {@link EmbeddingCodelet} that repeatedly embeds random numbers of copies of itself up to a fixed depth.
 * 
 * @author Roy Liu
 */
public class ForkBomb implements EmbeddingCodelet {

    /**
     * The maximum recursion depth.
     */
    final protected static int MAX_DEPTH = 4;

    Node embeddingParameters;

    /**
     * Default constructor.
     */
    public ForkBomb() {
        this.embeddingParameters = null;
    }

    public void build(Flow flow, //
            List<FlowEdge> inEdges, //
            List<FlowNode> outNodes) {

        FlowNode inNode = new FlowNode("ex.Error") //
                .setDomainPattern(LOCAL);

        flow.add(inNode);

        int depth = 0;

        for (FlowEdge v : inEdges) {

            v.setV(inNode);

            depth = Math.max(Integer.parseInt(v.getU().getParameters().getTextContent()), depth) + 1;
        }

        if (depth > MAX_DEPTH) {
            return;
        }

        FlowNode singletonNode = new FlowNode("ex.Error") //
                .setDomainPattern(LOCAL) //
                .setParameters(Integer.toString(depth));

        flow.add(singletonNode, new HandleEdge(inNode, singletonNode));

        for (int i = 0, n = Integer.parseInt(getEmbeddingParameters().getTextContent()); i < n; i++) {

            FlowNode forkBombNode = new FlowNode("ex.ForkBomb") //
                    .setDomainPattern(LOCAL);

            flow.add(forkBombNode, new StreamEdge(singletonNode, forkBombNode));
        }
    }

    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) {

        Arithmetic.randomize();

        setEmbeddingParameters(CodeletUtilities.createElement(Integer.toString(Arithmetic.nextInt(3) + 1)));
    }

    public Node getEmbeddingParameters() {
        return this.embeddingParameters;
    }

    public void setEmbeddingParameters(Node embeddingParameters) {
        this.embeddingParameters = embeddingParameters;
    }

    /**
     * Gets a human-readable description.
     */
    @Override
    public String toString() {
        return "Fork Bomb";
    }
}
