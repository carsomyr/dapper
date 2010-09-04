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

import static dapper.Constants.LOCAL;

import java.util.List;

import org.w3c.dom.Node;

import shared.util.Arithmetic;
import dapper.codelet.Codelet;
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

    @Override
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

    @Override
    public void run(List<Resource> inResources, List<Resource> outResources, Node parameters) {

        Arithmetic.randomize();

        setEmbeddingParameters(CodeletUtilities.createElement(Integer.toString(Arithmetic.nextInt(3) + 1)));
    }

    @Override
    public Node getEmbeddingParameters() {
        return this.embeddingParameters;
    }

    @Override
    public void setEmbeddingParameters(Node embeddingParameters) {
        this.embeddingParameters = embeddingParameters;
    }

    /**
     * Creates a human-readable description of this {@link Codelet}.
     */
    @Override
    public String toString() {
        return "Fork Bomb";
    }
}
