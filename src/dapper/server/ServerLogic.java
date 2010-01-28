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

package dapper.server;

import static dapper.Constants.CLIENT_TIMEOUT;
import static dapper.event.ControlEvent.ControlEventType.EXECUTE;
import static dapper.event.ControlEvent.ControlEventType.INIT;
import static dapper.event.ControlEvent.ControlEventType.PREPARE;
import static dapper.event.ControlEvent.ControlEventType.REFRESH;
import static shared.util.Control.assertTrue;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.Map.Entry;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import shared.util.Control;
import dapper.DapperException;
import dapper.client.ClientStatus;
import dapper.codelet.Codelet;
import dapper.event.AddressEvent;
import dapper.event.ControlEvent;
import dapper.event.ControlEventConnection;
import dapper.event.DataRequestEvent;
import dapper.event.ErrorEvent;
import dapper.event.ExecuteAckEvent;
import dapper.event.ResetEvent;
import dapper.event.ResourceEvent;
import dapper.event.TimeoutEvent;
import dapper.event.ControlEvent.ControlEventType;
import dapper.server.ServerProcessor.FlowBuildRequest;
import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.ServerProcessor.QueryEvent;
import dapper.server.flow.CountDown;
import dapper.server.flow.EmbeddingCodelet;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowListener;
import dapper.server.flow.FlowNode;
import dapper.server.flow.FlowStatus;
import dapper.server.flow.FlowUtilities;
import dapper.server.flow.HandleEdge;
import dapper.server.flow.LogicalEdge;
import dapper.server.flow.LogicalNode;
import dapper.server.flow.LogicalNodeStatus;
import dapper.util.RequirementsMatcher;

/**
 * A class that houses the Dapper server logic.
 * 
 * @apiviz.owns dapper.server.ClientState
 * @author Roy Liu
 */
public class ServerLogic {

    /**
     * A {@link Comparator} for comparing {@link LogicalNode}s based on the sizes of the {@link FlowNode} equivalence
     * classes they represent.
     */
    final static protected Comparator<LogicalNode> SizeComparator = //
    new Comparator<LogicalNode>() {

        public int compare(LogicalNode l1, LogicalNode l2) {
            return l1.getFlowNodes().size() - l2.getFlowNodes().size();
        }
    };

    /**
     * A {@link Pattern} used to parse data requests.
     */
    final static protected Pattern DataRequestPattern = Pattern.compile("([0-9a-zA-Z_]+):(.*)");

    final InetAddress address;
    final ServerProcessor sp;
    final Set<ClientState> clientWaitSet;
    final Map<Flow, FlowProxy> allFlowsMap;
    final List<LogicalNode> executeList;

    boolean autoClose;

    /**
     * Default constructor.
     */
    public ServerLogic(InetAddress address, ServerProcessor sp) {

        this.address = address;
        this.sp = sp;

        this.clientWaitSet = new HashSet<ClientState>();

        // The map of all flows has weak keys in case the outside world loses all strong references to the
        // flows stored therein.
        this.allFlowsMap = new WeakHashMap<Flow, FlowProxy>();

        // The execute list holds everything currently eligible for execution.
        this.executeList = new ArrayList<LogicalNode>();

        this.autoClose = false;
    }

    // HELPER METHODS

    /**
     * Resets the given {@link LogicalNode} because some one of its equivalence class members failed.
     */
    protected void resetNode(LogicalNode node) {

        for (FlowNode flowNode : node.getFlowNodes()) {

            ClientState csh = flowNode.getClientState();

            if (csh != null) {

                // The client had better not be in the wait set when we add it.
                assertTrue(this.clientWaitSet.add(csh));

                csh.setFlowNode(null);
                flowNode.setClientState(null);

                csh.getConnection().onRemote(new ResetEvent("One client failed in its execution", //
                        new RuntimeException(), null));
                csh.setStatus(ClientStatus.WAIT);
            }
        }
    }

    /**
     * Purges the given {@link Flow}.
     */
    protected void purgeFlow(Flow flow, Throwable t) {

        Server.getLog().info(String.format("The flow '%s' was purged.", flow), t);

        Set<LogicalNode> nodes = flow.getNodes();

        // Deliver a reset to all active clients.
        // Since nodes are reinserted into the execute list, do this BEFORE removing all nodes from it.
        for (LogicalNode node : nodes) {

            resetNode(node);
            node.setStatus(LogicalNodeStatus.FAILED);
        }

        // Remove all traces of the flow.
        this.executeList.removeAll(nodes);

        FlowProxy fp = this.allFlowsMap.get(flow);

        fp.onFlowError(fp.getAttachment(), t);
        fp.setException(t);

        flow.setStatus(FlowStatus.FAILED);
    }

    /**
     * Marks the given client as ready, and transitions all clients belonging to the same equivalence class if they are
     * all ready.
     */
    protected void transitionIfReady(ClientState csh1, //
            ClientStatus assignedClientStatus, ClientStatus nextClientStatus, //
            LogicalNodeStatus currentNodeStatus, LogicalNodeStatus nextNodeStatus, //
            ControlEventType eventType, long timeout) {

        FlowNode fn1 = csh1.getFlowNode();
        LogicalNode node = fn1.getLogicalNode();

        csh1.setStatus(assignedClientStatus);
        csh1.untimeout();

        CountDown<FlowNode> clientCountDown = node.getClientCountDown();

        if (clientCountDown.countDown(fn1)) {

            Server.getLog().debug(String.format("%s received on flow \"%s\". Transitioning to %s.", //
                    assignedClientStatus, node.getFlow(), nextClientStatus));

            for (FlowNode fn2 : node.getFlowNodes()) {

                ClientState csh2 = fn2.getClientState();

                assertTrue(csh2.getStatus() == assignedClientStatus);

                csh2.getConnection().onRemote(new ControlEvent(eventType, null));
                csh2.setStatus(nextClientStatus);
                csh2.timeout(timeout);
            }

            // Start a count down for waiting on all clients to acknowledge.
            assertTrue(node.getStatus() == currentNodeStatus);
            clientCountDown.reset();
            node.setStatus(nextNodeStatus);
        }
    }

    /**
     * Closes all idle clients.
     */
    protected void closeIdleClients() {

        int nflowNodes = getPendingCount();

        for (ClientState csh : new LinkedList<ClientState>(this.clientWaitSet) //
                .subList(0, Math.max(this.clientWaitSet.size() - nflowNodes, 0))) {

            this.clientWaitSet.remove(csh);

            Control.close(csh.getConnection());
            csh.untimeout();
            csh.setStatus(ClientStatus.INVALID);
        }
    }

    /**
     * Gets the pending computation count.
     */
    protected int getPendingCount() {

        int nflowNodes = 0;

        for (LogicalNode node : this.executeList) {
            nflowNodes += node.getFlowNodes().size();
        }

        return nflowNodes;
    }

    /**
     * Gets the pending computation count for the given {@link Flow}.
     */
    protected int getPendingCount(Flow flow) {

        int nflowNodes = 0;

        Set<LogicalNode> nodes = flow.getNodes();

        for (LogicalNode node : this.executeList) {

            if (nodes.contains(node)) {
                nflowNodes += node.getFlowNodes().size();
            }
        }

        return nflowNodes;
    }

    // INTERNAL LOGIC

    /**
     * Refreshes the computation state and sees if any work can be executed.
     */
    protected void handleRefresh() {

        // Greedily pick small equivalence classes by sorting with a size-based comparator.
        Collections.sort(this.executeList, SizeComparator);

        loop: for (Iterator<LogicalNode> itr = this.executeList.iterator(); itr.hasNext();) {

            LogicalNode node = itr.next();

            Set<FlowNode> flowNodes = node.getFlowNodes();

            // Eject from the loop if the equivalence class size exceeds the number of available clients.
            if (flowNodes.size() > this.clientWaitSet.size()) {
                break loop;
            }

            Set<Entry<FlowNode, ClientState>> matchEntries = //
            RequirementsMatcher.match(flowNodes, this.clientWaitSet);

            // Skip the current iteration if not all requirements could be met.
            if (flowNodes.size() != matchEntries.size()) {
                continue loop;
            }

            for (Entry<FlowNode, ClientState> matchEntry : matchEntries) {

                FlowNode flowNode = matchEntry.getKey();
                ClientState csh = matchEntry.getValue();

                assertTrue(csh.getStatus() == ClientStatus.WAIT);

                // The client no longer belongs to the waiting set.
                assertTrue(this.clientWaitSet.remove(csh));

                // Make the node and the client known to each other.
                flowNode.setClientState(csh);
                csh.setFlowNode(flowNode);
            }

            // Only add out-edges for assignment.
            for (FlowNode flowNode : node.getFlowNodes()) {

                for (FlowEdge flowEdge : flowNode.getOut()) {
                    flowEdge.generate();
                }
            }

            // Some edges may query the client states at both ends, so create resources AFTER linking.
            for (Entry<FlowNode, ClientState> matchEntry : matchEntries) {

                FlowNode flowNode = matchEntry.getKey();
                ClientState csh = matchEntry.getValue();

                // Send over resource descriptors.
                csh.getConnection().onRemote(flowNode.createResourceEvent());
                csh.setStatus(ClientStatus.RESOURCE);
                csh.timeout(CLIENT_TIMEOUT);
            }

            // Start a count down for waiting on all clients to acknowledge.
            assertTrue(node.getStatus().isExecutable());
            node.getClientCountDown().reset();
            node.setStatus(LogicalNodeStatus.RESOURCE);

            // Remove the node because it is now executing.
            itr.remove();
        }

        // Automatically close any clients left over.
        if (this.autoClose) {
            closeIdleClients();
        }
    }

    /**
     * Performs initialization of {@link Flow}s from within the processing thread.
     */
    @SuppressWarnings("unchecked")
    protected void handleQueryInit(QueryEvent<FlowBuildRequest, FlowProxy> evt) {

        FlowBuildRequest fbr = evt.getInput();

        FlowBuilder fb = fbr.flowBuilder;
        ClassLoader cl = fbr.classLoader;

        Flow flow = new Flow(fb.toString(), cl);

        try {

            flow.build(fb, null);

        } catch (DapperException e) {

            // Signal failure.
            evt.setException(e);

            this.sp.onLocal(new ControlEvent(REFRESH, this.sp));

            return;
        }

        this.executeList.addAll(FlowUtilities.buildCountDowns(flow));

        // Create and register the flow proxy.
        FlowProxy fp = this.sp.new FlowProxy(flow, //
                (fbr.listener != null) ? (FlowListener<Object, Object>) fbr.listener : FlowUtilities.NullListener);
        fp.onFlowBegin(fp.getAttachment());

        assertTrue(this.allFlowsMap.put(flow, fp) == null);

        evt.setOutput(fp);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Refreshes {@link Flow}s.
     */
    protected void handleQueryRefresh(QueryEvent<Flow, List<FlowProxy>> evt) {

        Flow f = evt.getInput();

        if (f != null) {

            // If the query was for a particular flow.
            FlowProxy fp = this.allFlowsMap.get(f);

            if (fp != null) {

                fp.setFlow(f.clone());

                evt.setOutput(null);

            } else {

                evt.setException(new IllegalArgumentException( //
                        "Flow did not originate from this server"));
            }

        } else {

            List<FlowProxy> res = new ArrayList<FlowProxy>();

            for (Entry<Flow, FlowProxy> entry : this.allFlowsMap.entrySet()) {

                Flow flow = entry.getKey();
                FlowProxy fp = entry.getValue();

                if (flow.getStatus().isExecuting()) {

                    fp.setFlow(flow.clone());

                    res.add(fp);
                }
            }

            evt.setOutput(res);
        }
    }

    /**
     * Purges {@link Flow}s.
     */
    protected void handleQueryPurge(QueryEvent<Flow, Object> evt) {

        Flow flow = evt.getInput();

        if (flow.getStatus().isExecuting()) {
            purgeFlow(flow, new IllegalStateException("The flow was purged"));
        }

        // Notify the invoker of completion.
        evt.setOutput(null);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Sets the idle client auto-close option.
     */
    protected void handleQueryCloseIdle(QueryEvent<Boolean, Object> evt) {

        Boolean value = evt.getInput();

        if (value == null) {

            closeIdleClients();

        } else {

            this.autoClose = value.booleanValue();
        }

        // Notify the invoker of completion.
        evt.setOutput(null);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Gets the number of additional clients required to saturate pending computations.
     */
    protected void handleQueryPendingCount(QueryEvent<Object, Integer> evt) {
        evt.setOutput(getPendingCount());
    }

    /**
     * Gets the number of additional clients required to saturate pending computations on the given {@link Flow}.
     */
    protected void handleQueryFlowPendingCount(QueryEvent<Flow, Integer> evt) {
        evt.setOutput(getPendingCount(evt.getInput()));
    }

    // CLIENT LOGIC

    /**
     * Handles an end-of-stream.
     */
    protected void handleEOS(ControlEventConnection connection) {
        handleError(new ErrorEvent(new IOException("End-of-stream encountered"), connection));
    }

    /**
     * Handles the given {@link TimeoutEvent}.
     */
    protected void handleTimeout(TimeoutEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        switch (csh.getStatus()) {

        case RESOURCE:
        case PREPARE:
            handleError(new ErrorEvent(new TimeoutException("Client timed out"), evt.getSource()));
            break;

        case EXECUTE:

            FlowNode flowNode = csh.getFlowNode();

            purgeFlow(flowNode.getLogicalNode().getFlow(), new IllegalStateException( //
                    String.format("Maximum execution time limit of " //
                            + "%d milliseconds exceeded", //
                            flowNode.getTimeout())));

            this.sp.onLocal(new ControlEvent(REFRESH, this.sp));

            break;

        default:
            throw new AssertionError("Control should never reach here");
        }
    }

    /**
     * Handles an {@link ErrorEvent}.
     */
    protected void handleError(ErrorEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        Throwable error = evt.getError();

        Server.getLog().info(String.format("Received error from %s: %s.", evt.getSource(), error.getMessage()));

        FlowNode flowNode = csh.getFlowNode();

        // In case the client was awaiting instructions.
        this.clientWaitSet.remove(csh);

        if (flowNode != null) {

            FlowProxy fp = this.allFlowsMap.get(flowNode.getLogicalNode().getFlow());
            fp.onFlowNodeError(fp.getAttachment(), flowNode.getAttachment(), error);

            // Unlink the client from its node BEFORE resetting its equivalence class peers.
            csh.setFlowNode(null);
            flowNode.setClientState(null);

            LogicalNode node = flowNode.getLogicalNode();

            // Reset everything in this flow node's equivalence class.
            assertTrue(node.getStatus().isExecuting() && this.executeList.add(node));
            resetNode(node);
            node.setStatus(LogicalNodeStatus.FAILED);
        }

        // Close the connection and invalidate it, since the error could have resulted from a timeout.
        Control.close(csh.getConnection());
        csh.untimeout();
        csh.setStatus(ClientStatus.INVALID);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles the given {@link ResetEvent}.
     */
    protected void handleReset(ResetEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        Throwable error = evt.getException();

        Server.getLog().info(String.format("Received error from client %s.", csh.getAddress()), error);

        FlowNode flowNode = csh.getFlowNode();

        assertTrue(flowNode != null && csh == flowNode.getClientState());

        LogicalNode node = flowNode.getLogicalNode();

        assertTrue(node != null && node.getFlowNodes().contains(flowNode));

        FlowProxy fp = this.allFlowsMap.get(flowNode.getLogicalNode().getFlow());
        fp.onFlowNodeError(fp.getAttachment(), flowNode.getAttachment(), error);

        int maxRetries = flowNode.getRetries();

        if (flowNode.incrementAndGetRetries() <= maxRetries) {

            // Reset everything in this flow node's equivalence class.
            assertTrue(node.getStatus().isExecuting() && this.executeList.add(node));
            resetNode(node);
            node.setStatus(LogicalNodeStatus.FAILED);

        } else {

            // Purge the flow if too many retries.
            purgeFlow(node.getFlow(), new IllegalStateException( //
                    String.format("Maximum failed execution limit of " //
                            + "%d retries exceeded", maxRetries)));
        }

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Transitions from {@link ClientStatus#IDLE} to {@link ClientStatus#WAIT}.
     */
    protected void handleIdleToWait(AddressEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        // The client had better not be in the wait set when we add it.
        assertTrue(this.clientWaitSet.add(csh));

        // Set the address of the client for later reference.
        csh.getConnection().onRemote(new ControlEvent(INIT, null));

        InetSocketAddress address = evt.getAddress();

        // Treat loopback addresses specially.
        if (address.getAddress() == null || !address.getAddress().isLoopbackAddress()) {

            String domain = evt.getDomain();

            csh.setAddress(address);
            csh.setDomain((!domain.equals("") && !domain.equals("local")) ? domain : "remote");

        } else {

            csh.setAddress(new InetSocketAddress(this.address, address.getPort()));
            csh.setDomain("local");
        }

        // The client is now awaiting further instructions.
        csh.setStatus(ClientStatus.WAIT);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles the given {@link DataRequestEvent}.
     */
    protected void handleDataRequest(DataRequestEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        FlowNode flowNode = csh.getFlowNode();

        assertTrue(flowNode != null && csh == flowNode.getClientState());

        String identifier = evt.getPathname();

        Matcher m = DataRequestPattern.matcher(identifier);

        try {

            Control.checkTrue(m.matches(), //
                    "Invalid request syntax");

            String mode = m.group(1);
            String pathname = m.group(2);

            final byte[] data;

            if (mode.equals("cp")) {

                data = Control.getBytes(flowNode.getCodelet().getClass().getClassLoader() //
                        .getResourceAsStream(pathname));

            } else if (mode.equals("id")) {

                data = FlowUtilities.createIdentifier(HandleEdge.class).getBytes();

            } else {

                throw new RuntimeException("Invalid request syntax");
            }

            csh.getConnection().onRemote(new DataRequestEvent(m.group(0), data, null));

        } catch (Exception e) {

            handleReset(new ResetEvent(String.format("Requested data \"%s\" could not be retrieved", identifier), e, //
                    evt.getSource()));
        }
    }

    /**
     * The client has acknowledged receipt of the given {@link ResourceEvent}. Once all clients of the same equivalence
     * class check in, they all transition from {@link ClientStatus#RESOURCE} to {@link ClientStatus#PREPARE}.
     */
    protected void handleResourceToPrepare(ClientState csh) {
        transitionIfReady(csh, //
                ClientStatus.RESOURCE_ACK, ClientStatus.PREPARE, //
                LogicalNodeStatus.RESOURCE, LogicalNodeStatus.PREPARE, //
                PREPARE, CLIENT_TIMEOUT);
    }

    /**
     * The client has requisitioned all necessary resources for execution. Once all clients of the same equivalence
     * class check in, they all transition from {@link ClientStatus#PREPARE} to {@link ClientStatus#EXECUTE}.
     */
    protected void handlePrepareToExecute(ClientState csh) {

        FlowNode flowNode = csh.getFlowNode();

        FlowProxy fp = this.allFlowsMap.get(flowNode.getLogicalNode().getFlow());
        fp.onFlowNodeBegin(fp.getAttachment(), flowNode.getAttachment());

        transitionIfReady(csh, //
                ClientStatus.PREPARE_ACK, ClientStatus.EXECUTE, //
                LogicalNodeStatus.PREPARE, LogicalNodeStatus.EXECUTE, //
                EXECUTE, flowNode.getTimeout());
    }

    /**
     * The client has successfully executed; it transitions from {@link ClientStatus#EXECUTE} to
     * {@link ClientStatus#WAIT}. Once all clients of the same equivalence class check in, the server searches for
     * dependent {@link LogicalNode}s that are newly eligible for execution. Should member {@link FlowNode}s embed
     * subflows, the server executes their {@link FlowBuilder}s and rebuilds the underlying {@link Flow}.
     */
    protected void handleExecuteToWait(ExecuteAckEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        FlowNode fn1 = csh.getFlowNode();
        LogicalNode node = fn1.getLogicalNode();
        Flow flow = node.getFlow();

        Server.getLog().debug(String.format("%s received on flow \"%s\".", evt.getType(), flow));

        FlowProxy fp = this.allFlowsMap.get(flow);
        fp.onFlowNodeEnd(fp.getAttachment(), fn1.getAttachment());

        try {

            FlowUtilities.assignParameters(fn1, evt.getParameters(), evt.getEdgeParameters());

        } catch (DapperException e) {

            handleReset(new ResetEvent("Failed to assign embedding parameters", e, evt.getSource()));

            this.sp.onLocal(new ControlEvent(REFRESH, this.sp));

            return;
        }

        assertTrue(node.getStatus() == LogicalNodeStatus.EXECUTE);

        if (node.getClientCountDown().countDown(fn1)) {

            ArrayList<Object> buildArgs = new ArrayList<Object>();

            for (FlowNode fn2 : node.getFlowNodes()) {

                Codelet c2 = fn2.getCodelet();

                if (c2 instanceof EmbeddingCodelet) {

                    buildArgs.add(c2);
                    buildArgs.add(fn2);
                }
            }

            // Mark the node as having finished BEFORE determining completion or embedding a subflow.
            node.setStatus(LogicalNodeStatus.FINISHED);

            // No subflows require embedding.
            if (buildArgs.isEmpty()) {

                // Scan the out-neighbors to see which ones are ready.
                for (LogicalEdge edge : node.getOut()) {

                    LogicalNode dependent = edge.getV();

                    // This node is ready to execute.
                    if (dependent.getDependencyCountDown().countDown(node)) {
                        this.executeList.add(dependent);
                    }
                }

                // Count down on the node's flow.
                if (flow.getFlowCountDown().countDown(node)) {

                    fp.onFlowEnd(fp.getAttachment());
                    fp.setOutput(null);

                    flow.setStatus(FlowStatus.FINISHED);
                }
            }
            // A subflow requires embedding.
            else {

                Set<LogicalNode> nodes = flow.getNodes();

                // Remove all eligible nodes belonging to this flow.
                this.executeList.removeAll(nodes);

                try {

                    flow.build(buildArgs.toArray());

                } catch (DapperException e) {

                    purgeFlow(flow, e);

                    this.sp.onLocal(new ControlEvent(REFRESH, this.sp));

                    return;
                }

                // Restore all eligible nodes belonging to this flow.
                this.executeList.addAll(FlowUtilities.buildCountDowns(flow));

                // By construction, the modified flow no longer contains the completed node.
                assertTrue(!nodes.contains(node));
            }
        }

        // The client had better not be in the wait set when we add it.
        assertTrue(this.clientWaitSet.add(csh));

        // Unlink the client from its node.
        csh.setFlowNode(null);
        fn1.setClientState(null);

        // The client is now awaiting further instructions.
        csh.setStatus(ClientStatus.WAIT);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }
}
