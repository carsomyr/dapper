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

import static dapper.Constants.BLACK;
import static dapper.Constants.DARK_GREEN;
import static dapper.Constants.DARK_ORANGE;
import static dapper.Constants.DARK_RED;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Formatter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import shared.array.ObjectArray;
import shared.event.EnumStatus;
import shared.parallel.Edge;
import shared.parallel.LimitedMemoryPolicy;
import shared.parallel.Traversable;
import shared.util.Control;
import dapper.DapperException;
import dapper.codelet.Codelet;
import dapper.codelet.Taggable;
import dapper.server.ClientState;
import dapper.server.flow.FlowEdge.FlowEdgeType;

/**
 * A class for storing the flow computation state.
 * 
 * @apiviz.composedOf dapper.server.flow.AbstractCountDown
 * @apiviz.composedOf dapper.server.flow.FlowEdge
 * @apiviz.composedOf dapper.server.flow.FlowNode
 * @apiviz.composedOf dapper.server.flow.LogicalEdge
 * @apiviz.composedOf dapper.server.flow.LogicalNode
 * @apiviz.owns dapper.server.flow.FlowStatus
 * @apiviz.uses dapper.server.flow.FlowUtilities
 * @author Roy Liu
 */
public class Flow implements Cloneable, Renderable, EnumStatus<FlowStatus>, Taggable<Object> {

    /**
     * A temporary mapping of {@link FlowNode} proxies to actuals local to the current thread.
     */
    final protected static ThreadLocal<Map<FlowNode, FlowNode>> nodeMapLocal = //
    new ThreadLocal<Map<FlowNode, FlowNode>>();

    /**
     * An empty {@link FlowEdge} list.
     */
    final protected static List<FlowEdge> emptyList = Collections.emptyList();

    //

    final String name;
    final ClassLoader cl;

    Object attachment;

    FlowStatus status;

    //

    Set<LogicalNode> nodes;
    Set<FlowNode> flowNodes;

    /**
     * The {@link CountDown} on {@link LogicalNode}s.
     */
    protected CountDown<LogicalNode> flowCountDown;

    /**
     * Default constructor.
     */
    public Flow(String name, ClassLoader cl) {

        this.name = name;
        this.cl = cl;

        this.attachment = null;
        this.status = FlowStatus.EXECUTE;

        //

        this.nodes = new HashSet<LogicalNode>();
        this.flowNodes = new HashSet<FlowNode>();

        this.flowCountDown = createFlowCountDown(new HashSet<LogicalNode>());
    }

    /**
     * Adds a {@link FlowNode} along with {@link FlowEdge} dependencies.
     */
    public FlowNode add(FlowNode node, FlowEdge... edgeDeps) {

        Map<FlowNode, FlowNode> nodeMap = nodeMapLocal.get();

        Control.checkTrue(nodeMap != null, //
                "Node additions must be made from within build methods");

        // Check everything before inserts.
        Control.checkTrue(!nodeMap.containsKey(node), //
                "Node already exists");

        for (FlowEdge edgeDep : edgeDeps) {

            // Check that the dependency exists.
            Control.checkTrue(nodeMap.containsKey(edgeDep.getU()), //
                    "Dependency does not exist");

            // Check that the edge dependency ends at the given node.
            Control.checkTrue(edgeDep.getV().equals(node), //
                    "Edge must end with the node being added");
        }

        FlowNode newNode = node.clone();
        nodeMap.put(node, newNode);

        for (FlowEdge edgeDep : edgeDeps) {

            FlowEdge newEdge = edgeDep.clone();

            newEdge.setU(nodeMap.get(edgeDep.getU()));
            newEdge.setV(nodeMap.get(edgeDep.getV()));

            newEdge.getU().getOut().add(newEdge);
            newEdge.getV().getIn().add(newEdge);
        }

        return node;
    }

    /**
     * Adds a {@link FlowEdge} and checks for cycles that may arise.
     */
    public <T extends FlowEdge> T add(T edge) {

        Map<FlowNode, FlowNode> nodeMap = nodeMapLocal.get();

        Control.checkTrue(nodeMap != null, //
                "Edge additions must be made from within build methods");

        FlowNode u = nodeMap.get(edge.getU());
        FlowNode v = nodeMap.get(edge.getV());

        // Check existence of endpoints.
        Control.checkTrue(u != null && v != null, //
                "Start/end node does not exist");

        FlowEdge newEdge = edge.clone();

        newEdge.setU(u);
        newEdge.setV(v);

        u.getOut().add(newEdge);
        v.getIn().add(newEdge);

        return edge;
    }

    /**
     * Creates the flow {@link CountDown}.
     */
    protected CountDown<LogicalNode> createFlowCountDown(final Set<LogicalNode> cds) {

        return new AbstractCountDown<LogicalNode>(cds) {

            @Override
            public void reset() {

                cds.clear();
                cds.addAll(Flow.this.nodes);
            }
        };
    }

    /**
     * Copies this flow.
     */
    @Override
    public Flow clone() {

        final Flow res;

        try {

            res = (Flow) super.clone();

        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }

        res.nodes = new HashSet<LogicalNode>();
        res.flowNodes = new HashSet<FlowNode>();

        Map<LogicalNode, LogicalNode> logicalNodeMap = new HashMap<LogicalNode, LogicalNode>();
        Map<LogicalEdge, LogicalEdge> logicalEdgeMap = new HashMap<LogicalEdge, LogicalEdge>();

        Map<FlowNode, FlowNode> flowNodeMap = new HashMap<FlowNode, FlowNode>();
        Map<FlowEdge, FlowEdge> flowEdgeMap = new HashMap<FlowEdge, FlowEdge>();

        for (LogicalNode logicalNode : this.nodes) {

            LogicalNode newLogicalNode = logicalNode.clone();
            newLogicalNode.setFlow(res);

            res.nodes.add(newLogicalNode);

            Control.checkTrue(logicalNodeMap.put(logicalNode, newLogicalNode) == null, //
                    "Value already assigned");

            for (LogicalEdge logicalEdge : logicalNode.getOut()) {
                Control.checkTrue(logicalEdgeMap.put(logicalEdge, logicalEdge.clone()) == null, //
                        "Value already assigned");
            }
        }

        assignNodeNeighbors(logicalNodeMap, logicalEdgeMap);

        for (FlowNode flowNode : this.flowNodes) {

            FlowNode newFlowNode = flowNode.clone();
            newFlowNode.setLogicalNode(logicalNodeMap.get(flowNode.getLogicalNode()));

            res.flowNodes.add(newFlowNode);

            Control.checkTrue(flowNodeMap.put(flowNode, newFlowNode) == null, //
                    "Value already assigned");

            for (FlowEdge flowEdge : flowNode.getOut()) {
                Control.checkTrue(flowEdgeMap.put(flowEdge, flowEdge.clone()) == null, //
                        "Value already assigned");
            }
        }

        assignNodeNeighbors(flowNodeMap, flowEdgeMap);

        Set<LogicalNode> newNodeSet = new HashSet<LogicalNode>();

        for (Entry<LogicalNode, LogicalNode> entry : logicalNodeMap.entrySet()) {

            LogicalNode oldLogicalNode = entry.getKey();
            LogicalNode newLogicalNode = entry.getValue();

            Set<LogicalNode> newDependencySet = new HashSet<LogicalNode>();
            Set<FlowNode> newClientSet = new HashSet<FlowNode>();

            for (LogicalNode logicalNode : oldLogicalNode.getDependencyCountDown().getRemaining()) {
                newDependencySet.add(logicalNodeMap.get(logicalNode));
            }

            for (FlowNode flowNode : oldLogicalNode.getClientCountDown().getRemaining()) {
                newClientSet.add(flowNodeMap.get(flowNode));
            }

            newLogicalNode.dependencyCountDown = newLogicalNode.createDependencyCountDown(newDependencySet);
            newLogicalNode.clientCountDown = newLogicalNode.createClientCountDown(newClientSet);

            Set<FlowNode> newFlowNodes = newLogicalNode.getFlowNodes();

            for (FlowNode flowNode : oldLogicalNode.getFlowNodes()) {
                newFlowNodes.add(flowNodeMap.get(flowNode));
            }

            newNodeSet.add(newLogicalNode);
        }

        res.flowCountDown = res.createFlowCountDown(newNodeSet);

        return res;
    }

    /**
     * Rebuilds edge incidence relationships on a flow copy.
     */
    @SuppressWarnings("unchecked")
    final protected static <V extends Traversable<V, E>, E extends Edge<V>> void assignNodeNeighbors( //
            Map<V, V> nodeMap, Map<E, E> edgeMap) {

        for (Entry<E, E> entry : edgeMap.entrySet()) {

            E oldEdge = entry.getKey();
            E newEdge = entry.getValue();

            newEdge.setU(nodeMap.get(oldEdge.getU()));
            newEdge.setV(nodeMap.get(oldEdge.getV()));
        }

        for (Entry<V, V> entry : nodeMap.entrySet()) {

            V oldVertex = entry.getKey();
            V newVertex = entry.getValue();

            List<E> newIn = (List<E>) newVertex.getIn();
            List<E> newOut = (List<E>) newVertex.getOut();

            for (E oldEdge : oldVertex.getIn()) {
                newIn.add(edgeMap.get(oldEdge));
            }

            for (E oldEdge : oldVertex.getOut()) {
                newOut.add(edgeMap.get(oldEdge));
            }
        }
    }

    /**
     * Gets the name of this flow.
     */
    @Override
    public String toString() {
        return this.name;
    }

    @Override
    public FlowStatus getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(FlowStatus status) {
        this.status = status;
    }

    @Override
    public Object getAttachment() {
        return this.attachment;
    }

    @Override
    public Flow setAttachment(Object attachment) {

        this.attachment = attachment;

        return this;
    }

    /**
     * Gets the {@link LogicalNode}s.
     */
    public Set<LogicalNode> getNodes() {
        return this.nodes;
    }

    /**
     * Gets the flow {@link CountDown}.
     */
    public CountDown<LogicalNode> getFlowCountDown() {
        return this.flowCountDown;
    }

    /**
     * Builds this flow using the given {@link FlowBuilder}.
     * 
     * @throws DapperException
     *             when something goes awry.
     */
    public void build(Object... args) throws DapperException {

        Control.checkTrue(args.length % 2 == 0, //
                "Number of arguments must be even");

        final Flow backup = clone();

        try {

            embedSubflows(args);

        } catch (Throwable t) {

            this.nodes = backup.nodes;
            this.flowNodes = backup.flowNodes;
            this.flowCountDown = backup.flowCountDown;

            for (LogicalNode node : this.nodes) {
                node.setFlow(this);
            }

            // Remember to redirect all connections.
            for (FlowNode flowNode : this.flowNodes) {

                ClientState csh = flowNode.getClientState();

                if (csh != null) {
                    csh.getConnection().setHandler(csh);
                }
            }

            throw new DapperException(t);
        }
    }

    /**
     * Embeds subflows.
     */
    protected void embedSubflows(Object[] args) {

        for (int i = 0, n = args.length; i < n; i += 2) {

            FlowBuilder builder = (FlowBuilder) args[i];
            FlowNode subflowNode = (FlowNode) args[i + 1];

            final List<FlowEdge> in, out;
            final List<FlowEdge> newIn = new ArrayList<FlowEdge>();
            final List<FlowNode> newOut = new ArrayList<FlowNode>();

            if (subflowNode != null) {

                in = subflowNode.getIn();
                out = subflowNode.getOut();

            } else {

                in = emptyList;
                out = emptyList;
            }

            // Create edge and node proxies for safety.

            final Map<FlowNode, FlowNode> inMap = new HashMap<FlowNode, FlowNode>();
            final Map<FlowEdge, FlowEdge> proxyEdgeMap = new HashMap<FlowEdge, FlowEdge>();

            final Set<FlowNode> outSet = new HashSet<FlowNode>();
            final Map<FlowNode, FlowNode> proxyNodeMap = new HashMap<FlowNode, FlowNode>();

            for (int j = 0, m = in.size(); j < m; j++) {

                FlowEdge fe1 = in.get(j);

                FlowNode originalNode = fe1.getU();
                inMap.put(originalNode, originalNode.clone());

                if (fe1.getType() == FlowEdgeType.HANDLE && ((HandleEdge) fe1).isExpandOnEmbed()) {

                    Control.assertTrue(in.remove(j) != null);

                    List<FlowEdge> neighborOut = originalNode.getOut();
                    int outIndex = neighborOut.indexOf(fe1);

                    Control.assertTrue(neighborOut.remove(outIndex) != null);

                    HandleEdge he = (HandleEdge) fe1;

                    ObjectArray<String> handleArray = he.getHandleInformation();

                    int nEntries = handleArray.size(0);

                    for (int k = 0; k < nEntries; k++) {

                        HandleEdge newHe = he.clone().setHandleInformation( //
                                handleArray.subarray(k, k + 1, 0, 2));
                        newHe.setU(originalNode);
                        newHe.setV(null);

                        in.add(j + k, newHe);
                        neighborOut.add(outIndex + k, newHe);
                    }

                    j += nEntries - 1;
                    m += nEntries - 1;
                }
            }

            for (FlowEdge fe1 : in) {

                FlowEdge newEdge = fe1.clone();
                newEdge.setU(inMap.get(fe1.getU()));

                proxyEdgeMap.put(newEdge, fe1);
                newIn.add(newEdge);
            }

            for (FlowEdge fe1 : out) {

                FlowNode originalNode = fe1.getV();
                outSet.add(originalNode);

                if (subflowNode != null) {
                    Control.assertTrue(originalNode.getIn().remove(fe1));
                }
            }

            for (FlowNode fn1 : outSet) {

                FlowNode newNode = fn1.clone();

                proxyNodeMap.put(newNode, fn1);
                newOut.add(newNode);
            }

            // Execute the embedding.

            Map<FlowNode, FlowNode> tmpMap = new HashMap<FlowNode, FlowNode>();

            nodeMapLocal.set(tmpMap);

            for (FlowNode fn1 : newOut) {
                add(fn1);
            }

            Thread th = Thread.currentThread();
            ClassLoader prevCl = th.getContextClassLoader();
            th.setContextClassLoader(this.cl);

            try {

                final Flow thisFlow = this;

                builder.build(new Flow(builder.toString(), null) {

                    @Override
                    public FlowNode add(FlowNode node, FlowEdge... edgeDeps) {
                        return thisFlow.add(node, edgeDeps);
                    }

                    @Override
                    public <T extends FlowEdge> T add(T edge) {
                        return thisFlow.add(edge);
                    }

                    @Override
                    public Object getAttachment() {
                        return thisFlow.getAttachment();
                    }

                    @Override
                    public Flow setAttachment(Object attachment) {

                        thisFlow.setAttachment(attachment);

                        return this;
                    }

                }, newIn, newOut);

            } finally {

                th.setContextClassLoader(prevCl);
            }

            nodeMapLocal.set(null);

            Map<FlowNode, FlowNode> tmpMapCopy = new HashMap<FlowNode, FlowNode>(tmpMap);

            for (FlowNode fn1 : newOut) {
                tmpMapCopy.remove(fn1);
            }

            this.flowNodes.addAll(tmpMapCopy.values());

            // Map edge and node proxies to originals.

            for (FlowEdge fe1 : newIn) {

                FlowNode inNode = tmpMap.get(fe1.getV());

                Control.checkTrue(inNode != null, //
                        "An in-edge was not bound or a node was not explicitly added");

                FlowEdge originalEdge = proxyEdgeMap.get(fe1);

                originalEdge.setV(inNode);
                inNode.getIn().add(originalEdge);
            }

            for (FlowNode fn1 : newOut) {

                FlowNode outNode = tmpMap.get(fn1);
                FlowNode originalNode = proxyNodeMap.get(fn1);

                for (FlowEdge fe1 : outNode.getIn()) {

                    fe1.setV(originalNode);
                    originalNode.getIn().add(fe1);
                }

                for (FlowEdge fe1 : outNode.getOut()) {

                    fe1.setU(originalNode);
                    originalNode.getOut().add(fe1);
                }
            }
        }

        // Remove all embedding flow nodes and unlink them from their parents.

        Set<LogicalNode> removals = new HashSet<LogicalNode>();

        for (int i = 0, n = args.length; i < n; i += 2) {

            FlowNode subflowNode = (FlowNode) args[i + 1];

            if (subflowNode != null) {

                Control.assertTrue(this.flowNodes.remove(subflowNode));
                removals.add(subflowNode.getLogicalNode());
            }
        }

        for (LogicalNode node : removals) {

            Control.assertTrue(this.nodes.remove(node));

            for (FlowNode fn1 : node.getFlowNodes()) {
                fn1.setLogicalNode(null);
            }
        }

        FlowNode stopFlowNode = new FlowNode((Codelet) null);

        for (FlowNode fn1 : this.flowNodes) {
            stopFlowNode.getIn().add(new DummyEdge(fn1, stopFlowNode));
        }

        // Assign priority order according to the traversal policy and assert that the number of nodes traversed is
        // equal to set of nodes.
        Control.assertTrue(new LimitedMemoryPolicy<FlowNode, FlowEdge>() //
                .assign(stopFlowNode) == this.flowNodes.size() + 1);

        //

        Set<FlowNode> allFlowNodes = new HashSet<FlowNode>(this.flowNodes);

        for (Set<FlowNode> equivalenceClass; !allFlowNodes.isEmpty(); allFlowNodes.removeAll(equivalenceClass)) {

            equivalenceClass = FlowUtilities.equivalenceClassDfs(allFlowNodes.iterator().next(), //
                    new HashSet<FlowNode>());

            Set<FlowNode> unattachedFlowNodes = new HashSet<FlowNode>();
            Set<LogicalNode> touchedNodes = new HashSet<LogicalNode>();

            for (FlowNode fn1 : equivalenceClass) {

                LogicalNode n1 = fn1.getLogicalNode();

                if (n1 != null) {

                    touchedNodes.add(n1);

                } else {

                    unattachedFlowNodes.add(fn1);
                }
            }

            Control.assertTrue(!unattachedFlowNodes.isEmpty() || touchedNodes.size() <= 1);

            final LogicalNode canonicalNode;

            // If there are unattached flow nodes, merge them, along with attached flow nodes, into one equivalence
            // class.
            if (!unattachedFlowNodes.isEmpty()) {

                canonicalNode = new LogicalNode(this);

                Set<FlowNode> canonicalFlowNodes = canonicalNode.getFlowNodes();

                canonicalFlowNodes.addAll(equivalenceClass);

                // The construction ensures that only pending and finished nodes can be joined.
                for (LogicalNode n1 : touchedNodes) {
                    Control.assertTrue(n1.getStatus().isMergeable() //
                            && canonicalFlowNodes.containsAll(n1.getFlowNodes()) //
                            && this.nodes.remove(n1));
                }

                for (FlowNode fn1 : canonicalFlowNodes) {
                    fn1.setLogicalNode(canonicalNode);
                }

                this.nodes.add(canonicalNode);
            }
            // Otherwise, everything belongs to a single equivalence class.
            else {

                canonicalNode = touchedNodes.iterator().next();

                Set<FlowNode> canonicalFlowNodes = canonicalNode.getFlowNodes();

                Control.assertTrue(touchedNodes.size() == 1 //
                        && equivalenceClass.containsAll(canonicalFlowNodes) //
                        && canonicalFlowNodes.containsAll(equivalenceClass));
            }
        }

        // Associate logical nodes to physical nodes, and determine connections among physical nodes.

        Map<FlowNode, LogicalNode> reverseMap = new HashMap<FlowNode, LogicalNode>();

        for (LogicalNode n1 : this.nodes) {

            for (FlowNode flowNode : n1.getFlowNodes()) {
                reverseMap.put(flowNode, n1);
            }
        }

        for (LogicalNode n1 : this.nodes) {

            // Note: Logical edges fulfill hashCode and equals contracts.
            Set<LogicalEdge> inSet = new HashSet<LogicalEdge>();
            Set<LogicalEdge> outSet = new HashSet<LogicalEdge>();

            for (FlowNode flowNode : n1.getFlowNodes()) {

                for (FlowEdge edge : flowNode.getIn()) {
                    inSet.add(new LogicalEdge(reverseMap.get(edge.getU()), n1));
                }

                for (FlowEdge edge : flowNode.getOut()) {
                    outSet.add(new LogicalEdge(n1, reverseMap.get(edge.getV())));
                }
            }

            // Remove self-loops.
            inSet.remove(new LogicalEdge(n1, n1));
            outSet.remove(new LogicalEdge(n1, n1));

            n1.getIn().clear();
            n1.getIn().addAll(inSet);

            n1.getOut().clear();
            n1.getOut().addAll(outSet);
        }

        // Prepare and execute a topological sort.

        LogicalNode stopNode = new LogicalNode(this);

        for (LogicalNode n1 : this.nodes) {
            stopNode.getIn().add(new LogicalEdge(n1, stopNode));
        }

        Control.assertTrue(new LimitedMemoryPolicy<LogicalNode, LogicalEdge>() //
                .assign(stopNode) == this.nodes.size() + 1);
    }

    @Override
    public void render(Formatter f) {

        final String color;

        switch (getStatus()) {

        case EXECUTE:
            color = DARK_ORANGE;
            break;

        case FINISHED:
            color = DARK_GREEN;
            break;

        case FAILED:
            color = DARK_RED;
            break;

        default:
            color = BLACK;
            break;
        }

        f.format("digraph {%n%n");
        f.format("size = \"%f, %f\";%n", 8.5, 11.0);
        f.format("ratio = auto;%n");
        f.format("margin = 0;%n");
        f.format("rankdir = LR;%n");
        f.format("node [shape = box];%n");

        f.format("%n/* Begin Node Specification */%n%n");

        f.format("subgraph cluster_flow {%n%n");
        f.format("    color = \"#%s\";%n", color);

        for (LogicalNode node : new TreeSet<LogicalNode>(getNodes())) {
            node.render(f);
        }

        f.format("}%n}%n");
    }
}
