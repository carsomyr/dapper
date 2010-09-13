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

package dapper.server.flow;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.util.Control;
import dapper.DapperException;
import dapper.codelet.Codelet;
import dapper.codelet.OutputHandleResource;
import dapper.server.flow.FlowEdge.FlowEdgeType;

/**
 * A static utility class for {@link Flow} bookkeeping.
 * 
 * @author Roy Liu
 */
abstract public class FlowUtilities {

    /**
     * A mapping of classes to unique identifier counters.
     */
    final protected static Map<Class<?>, AtomicInteger> CounterMap = new WeakHashMap<Class<?>, AtomicInteger>();

    /**
     * Creates a unique identifier for the given class.
     */
    final public static String createIdentifier(Class<?> clazz) {

        synchronized (CounterMap) {

            AtomicInteger counter = CounterMap.get(clazz);

            if (counter == null) {

                counter = new AtomicInteger(0);
                CounterMap.put(clazz, counter);
            }

            return String.format("%08x", counter.getAndIncrement());
        }
    }

    /**
     * Performs depth-first search to extract an equivalence class.
     * 
     * @param <T>
     *            the {@link Set} type.
     */
    final public static <T extends Set<FlowNode>> T equivalenceClassDFS(FlowNode currentNode, T visitedNodes) {

        // Mark the current node as visited.
        visitedNodes.add(currentNode);

        // Use a Map as a container class for two-tuples.
        Map<FlowNode, FlowEdgeType> neighbors = new HashMap<FlowNode, FlowEdgeType>();

        for (FlowEdge edge : currentNode.getIn()) {

            FlowNode u = edge.getU();

            // Consider the neighbor only if does not embed a subflow.
            if (!(u.getCodelet() instanceof EmbeddingCodelet)) {

                neighbors.put(u, edge.getType());

            }
            // If the neighbor does embed a subflow, check that we didn't visit it already.
            else {

                Control.checkTrue(!visitedNodes.contains(u), //
                        "Invalid placement for subflow embedding");
            }
        }

        for (FlowEdge edge : currentNode.getOut()) {

            FlowNode v = edge.getV();

            // Consider the neighbor only if the current node does not embed a subflow.
            if (!(currentNode.getCodelet() instanceof EmbeddingCodelet)) {

                neighbors.put(edge.getV(), edge.getType());
            }
            // If the current node does embed a subflow, check that we didn't visit the neighbor already.
            else {

                Control.checkTrue(edge.getType() == FlowEdgeType.DUMMY, //
                        "Outgoing edges of subflow embeddings must be dummies");

                Control.checkTrue(!visitedNodes.contains(v), //
                        "Invalid placement for subflow embedding");
            }
        }

        for (Entry<FlowNode, FlowEdgeType> entry : neighbors.entrySet()) {

            FlowNode neighbor = entry.getKey();
            FlowEdgeType type = entry.getValue();

            if (!visitedNodes.contains(neighbor) && type == FlowEdgeType.STREAM) {
                equivalenceClassDFS(neighbor, visitedNodes);
            }

            Control.checkTrue(!visitedNodes.contains(neighbor) || type == FlowEdgeType.STREAM, //
                    "Streaming equivalence classes must consist of only streaming edges");
        }

        return visitedNodes;
    }

    /**
     * Rebuilds dependency and {@link Flow} count downs.
     */
    final public static Set<LogicalNode> buildCountDowns(Flow flow) {

        Set<LogicalNode> nodes = flow.getNodes();
        Set<LogicalNode> executeNodes = new HashSet<LogicalNode>();

        flow.getFlowCountDown().reset();

        // Start a dependency count down on all nodes, where we consider a node eligible for execution if it has no
        // remaining dependencies.
        for (LogicalNode node : nodes) {

            CountDown<LogicalNode> countDown = node.getDependencyCountDown();
            countDown.reset();

            boolean countedDown = countDown.countDown(null);

            if (node.getStatus().isExecutable() && countedDown) {
                executeNodes.add(node);
            }
        }

        for (LogicalNode node : nodes) {

            // Simulate node completion.
            if (node.getStatus().isFinished()) {

                // Count down on all out-neighbors.
                for (LogicalEdge edge : node.getOut()) {

                    LogicalNode dependent = edge.getV();

                    boolean countedDown = dependent.getDependencyCountDown().countDown(node);

                    // Add the node if it hasn't yet finished.
                    // Ah yes, the old short-circuit evaluation has come back to haunt me.
                    if (dependent.getStatus().isExecutable() && countedDown) {
                        executeNodes.add(dependent);
                    }
                }

                // Count down on the flow.
                flow.getFlowCountDown().countDown(node);
            }
        }

        return executeNodes;
    }

    /**
     * Assigns {@link FlowEdge} parameters to the given client.
     * 
     * @throws DapperException
     *             when something goes awry.
     */
    final public static void assignParameters(FlowNode flowNode, Node embeddingParameters, Node edgeParameters) //
            throws DapperException {

        Codelet codelet = flowNode.getCodelet();

        List<FlowEdge> outEdges = flowNode.getOut();

        NodeList l1 = edgeParameters.getChildNodes();

        try {

            if (codelet instanceof EmbeddingCodelet) {
                ((EmbeddingCodelet) codelet).setEmbeddingParameters(embeddingParameters);
            }

            for (int i = 0, n = l1.getLength(); i < n; i++) {

                FlowEdge outEdge = outEdges.get(i);

                Node edgeParameter = l1.item(i);

                switch (outEdge.getType()) {

                case HANDLE:
                    ((HandleEdge) outEdge).setHandleInformation( //
                            new OutputHandleResource(edgeParameter).get());
                    break;
                }
            }

        } catch (Throwable t) {

            throw new DapperException(t);
        }
    }

    // Dummy constructor.
    FlowUtilities() {
    }
}
