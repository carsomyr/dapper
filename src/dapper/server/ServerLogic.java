/**
 * <p>
 * Copyright (c) 2008-2010 The Regents of the University of California<br>
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

package dapper.server;

import static dapper.Constants.CLIENT_TIMEOUT_MILLIS;
import static dapper.event.ControlEvent.ControlEventType.EXECUTE;
import static dapper.event.ControlEvent.ControlEventType.INIT;
import static dapper.event.ControlEvent.ControlEventType.PREPARE;
import static dapper.event.ControlEvent.ControlEventType.REFRESH;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import shared.util.Control;
import dapper.DapperException;
import dapper.client.ClientStatus;
import dapper.codelet.Codelet;
import dapper.event.AddressEvent;
import dapper.event.ControlEvent;
import dapper.event.ControlEvent.ControlEventType;
import dapper.event.ControlEventConnection;
import dapper.event.DataEvent;
import dapper.event.ErrorEvent;
import dapper.event.ExecuteAckEvent;
import dapper.event.ResetEvent;
import dapper.event.TimeoutEvent;
import dapper.server.ServerProcessor.FlowBuildRequest;
import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.ServerProcessor.RequestEvent;
import dapper.server.flow.CountDown;
import dapper.server.flow.EmbeddingCodelet;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowEdge;
import dapper.server.flow.FlowNode;
import dapper.server.flow.FlowStatus;
import dapper.server.flow.FlowUtilities;
import dapper.server.flow.HandleEdge;
import dapper.server.flow.LogicalEdge;
import dapper.server.flow.LogicalNode;
import dapper.server.flow.LogicalNodeStatus;
import dapper.util.MatchingAlgorithm;
import dapper.util.MaximumFlowMatching;

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
    final protected static Comparator<LogicalNode> SizeComparator = new Comparator<LogicalNode>() {

        @Override
        public int compare(LogicalNode l1, LogicalNode l2) {
            return l1.getFlowNodes().size() - l2.getFlowNodes().size();
        }
    };

    /**
     * A {@link Pattern} used to parse data requests.
     */
    final protected static Pattern DataRequestPattern = Pattern.compile("^([a-zA-Z_0-9]+):(.*)$");

    final InetAddress address;
    final ServerProcessor sp;
    final Set<ClientState> clientWaitSet;
    final Map<Flow, FlowProxy> allFlowsMap;
    final List<LogicalNode> executeList;
    final MatchingAlgorithm matching;

    boolean autocloseIdle;
    boolean suspended;

    /**
     * Default constructor.
     */
    public ServerLogic(InetAddress address, ServerProcessor sp) {

        this.address = address;
        this.sp = sp;

        this.clientWaitSet = new HashSet<ClientState>();

        // The map of all flows has weak keys in case the outside world loses all strong references to the flows stored
        // therein.
        this.allFlowsMap = new WeakHashMap<Flow, FlowProxy>();

        // The execute list holds everything currently eligible for execution.
        this.executeList = new ArrayList<LogicalNode>();

        this.matching = new MaximumFlowMatching();

        this.autocloseIdle = false;
        this.suspended = false;
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
                Control.assertTrue(this.clientWaitSet.add(csh));

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

        Server.getLog().info(String.format("The flow \"%s\" was purged.", flow), t);

        Set<LogicalNode> nodes = flow.getNodes();

        // Deliver a reset to all active clients.
        for (LogicalNode node : nodes) {

            this.executeList.remove(node);
            resetNode(node);
            node.setStatus(LogicalNodeStatus.FAILED);
        }

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

                Control.assertTrue(csh2.getStatus() == assignedClientStatus);

                csh2.getConnection().onRemote(new ControlEvent(eventType, null));
                csh2.setStatus(nextClientStatus);
                csh2.timeout(timeout);
            }

            // Start a count down for waiting on all clients to acknowledge.
            Control.assertTrue(node.getStatus() == currentNodeStatus);
            clientCountDown.reset();
            node.setStatus(nextNodeStatus);
        }
    }

    /**
     * Closes all idle clients.
     */
    protected void closeIdleClients() {

        for (ClientState csh : new ArrayList<ClientState>(this.clientWaitSet) //
                .subList(0, Math.max(this.clientWaitSet.size() - getPendingCount(), 0))) {

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

        int nFlowNodes = 0;

        for (LogicalNode node : this.executeList) {
            nFlowNodes += node.getFlowNodes().size();
        }

        return nFlowNodes;
    }

    /**
     * Gets the pending computation count for the given {@link Flow}.
     */
    protected int getPendingCount(Flow flow) {

        int nFlowNodes = 0;

        Set<LogicalNode> nodes = flow.getNodes();

        for (LogicalNode node : this.executeList) {

            if (nodes.contains(node)) {
                nFlowNodes += node.getFlowNodes().size();
            }
        }

        return nFlowNodes;
    }

    // INTERNAL LOGIC

    /**
     * Handles a request to refresh the computation state and see if any work can be done.
     */
    protected void handleRefresh() {

        // Check if a suspension is in effect.
        if (this.suspended) {
            return;
        }

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
            this.matching.match(flowNodes, this.clientWaitSet).entrySet();

            // Skip the current iteration if not all requirements could be met.
            if (flowNodes.size() != matchEntries.size()) {
                continue loop;
            }

            for (Entry<FlowNode, ClientState> matchEntry : matchEntries) {

                FlowNode flowNode = matchEntry.getKey();
                ClientState csh = matchEntry.getValue();

                Control.assertTrue(csh.getStatus() == ClientStatus.WAIT);

                // The client no longer belongs to the wait set.
                Control.assertTrue(this.clientWaitSet.remove(csh));

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
                csh.timeout(CLIENT_TIMEOUT_MILLIS);
            }

            // Start a count down for waiting on all clients to acknowledge.
            Control.assertTrue(node.getStatus().isExecutable());
            node.getClientCountDown().reset();
            node.setStatus(LogicalNodeStatus.RESOURCE);

            // Remove the node because it is now executing.
            itr.remove();
        }

        // Automatically close any clients left over.
        if (this.autocloseIdle) {
            closeIdleClients();
        }
    }

    /**
     * Handles a request to create a new {@link Flow}.
     */
    protected void handleCreateFlow(RequestEvent<FlowBuildRequest, FlowProxy> evt) {

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

        for (LogicalNode node : FlowUtilities.buildCountDowns(flow)) {

            this.executeList.add(node);
            node.setStatus(LogicalNodeStatus.PENDING_EXECUTE);
        }

        // Create and register the flow proxy.
        FlowProxy fp = this.sp.new FlowProxy(flow, fbr.flowFlags);
        fp.onFlowBegin(fp.getAttachment());

        Control.assertTrue(this.allFlowsMap.put(flow, fp) == null);

        evt.setOutput(fp);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles a request to purge an active {@link Flow}.
     */
    protected void handlePurgeFlow(RequestEvent<Flow, Object> evt) {

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
     * Handles a request to set the idle client autoclose option.
     */
    protected void handleSetAutocloseIdle(RequestEvent<Boolean, Object> evt) {

        Boolean autocloseIdle = evt.getInput();

        if (autocloseIdle == null) {

            closeIdleClients();

        } else {

            this.autocloseIdle = autocloseIdle.booleanValue();
        }

        // Notify the invoker of completion.
        evt.setOutput(null);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles a request to get the {@link FlowProxy} associated with an individual {@link Flow} or all
     * {@link FlowProxy}s associated with all {@link Flow}s.
     */
    protected void handleGetFlowProxy(RequestEvent<Flow, List<FlowProxy>> evt) {

        Flow f = evt.getInput();

        if (f != null) {

            // If the request was for a particular flow.
            FlowProxy fp = this.allFlowsMap.get(f);

            if (fp != null) {

                fp.setFlow(f.clone());

                evt.setOutput(null);

            } else {

                evt.setException(new IllegalArgumentException("Flow did not originate from this server"));
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
     * Handles a request to get the number of additional clients required to saturate pending computations.
     */
    protected void handleGetPendingCount(RequestEvent<Object, Integer> evt) {
        evt.setOutput(getPendingCount());
    }

    /**
     * Handles a request to get the number of additional clients required to saturate pending computations on the given
     * {@link Flow}.
     */
    protected void handleGetFlowPendingCount(RequestEvent<Flow, Integer> evt) {
        evt.setOutput(getPendingCount(evt.getInput()));
    }

    /**
     * Handles a request to suspend or resume server activities.
     */
    protected void handleSuspendResume(ControlEvent evt) {

        switch (evt.getType()) {

        case SUSPEND:
            this.suspended = true;
            break;

        case RESUME:

            this.suspended = false;
            this.sp.onLocal(new ControlEvent(REFRESH, this.sp));

            break;

        default:
            throw new AssertionError("Control should never reach here");
        }
    }

    // CLIENT LOGIC

    /**
     * Handles a connection end-of-stream notification.
     */
    protected void handleEOS(ControlEventConnection connection) {
        handleError(new ErrorEvent(new IOException("End-of-stream encountered"), connection));
    }

    /**
     * Handles a connection error notification.
     */
    protected void handleError(ErrorEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        Throwable exception = evt.getError();

        Server.getLog().info(String.format("Received error from %s: %s.", evt.getSource(), exception.getMessage()));

        FlowNode flowNode = csh.getFlowNode();

        // In case the client was awaiting instructions.
        this.clientWaitSet.remove(csh);

        if (flowNode != null) {

            FlowProxy fp = this.allFlowsMap.get(flowNode.getLogicalNode().getFlow());
            fp.onFlowNodeError(fp.getAttachment(), flowNode.getAttachment(), exception);

            // Unlink the client from its node BEFORE resetting its equivalence class peers.
            csh.setFlowNode(null);
            flowNode.setClientState(null);

            LogicalNode node = flowNode.getLogicalNode();

            // Reset everything in this flow node's equivalence class.
            Control.assertTrue(node.getStatus().isExecuting() && this.executeList.add(node));
            resetNode(node);
            node.setStatus(LogicalNodeStatus.PENDING_EXECUTE);
        }

        // Close the connection and invalidate it, since the error could have resulted from a timeout.
        Control.close(csh.getConnection());
        csh.untimeout();
        csh.setStatus(ClientStatus.INVALID);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles a timeout notification.
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

            purgeFlow(flowNode.getLogicalNode().getFlow(), new IllegalStateException(String.format("Maximum " //
                    + "execution time limit of %d milliseconds exceeded", flowNode.getTimeout())));

            this.sp.onLocal(new ControlEvent(REFRESH, this.sp));

            break;

        default:
            throw new AssertionError("Control should never reach here");
        }
    }

    /**
     * Handles a message from the client requesting data.
     */
    protected void handleData(DataEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        FlowNode flowNode = csh.getFlowNode();

        Control.assertTrue(flowNode != null && csh == flowNode.getClientState());

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

            csh.getConnection().onRemote(new DataEvent(m.group(0), data, null));

        } catch (Exception e) {

            handleReset(new ResetEvent(String.format("Requested data \"%s\" could not be retrieved", identifier), e, //
                    evt.getSource()));
        }
    }

    /**
     * Handles a message from the client resetting both ends to a common, inactive state.
     */
    protected void handleReset(ResetEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        Throwable exception = evt.getException();

        Server.getLog().info(String.format("Received error from client %s.", csh.getAddress()), exception);

        FlowNode flowNode = csh.getFlowNode();

        Control.assertTrue(flowNode != null && csh == flowNode.getClientState());

        LogicalNode node = flowNode.getLogicalNode();

        Control.assertTrue(node != null && node.getFlowNodes().contains(flowNode));

        FlowProxy fp = this.allFlowsMap.get(flowNode.getLogicalNode().getFlow());
        fp.onFlowNodeError(fp.getAttachment(), flowNode.getAttachment(), exception);

        int maxRetries = flowNode.getRetries();

        if (flowNode.incrementAndGetRetries() <= maxRetries) {

            // Reset everything in this flow node's equivalence class.
            Control.assertTrue(node.getStatus().isExecuting() && this.executeList.add(node));
            resetNode(node);
            node.setStatus(LogicalNodeStatus.PENDING_EXECUTE);

        } else {

            // Purge the flow if too many retries.
            purgeFlow(node.getFlow(), new IllegalStateException(String.format("Maximum failed execution limit of " //
                    + "%d retries exceeded", maxRetries)));
        }

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles a client transition from {@link ClientStatus#IDLE} to {@link ClientStatus#WAIT}.
     */
    protected void handleIdleToWait(AddressEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        // The client had better not be in the wait set when we add it.
        Control.assertTrue(this.clientWaitSet.add(csh));

        // Notify the client of connection establishment.
        csh.getConnection().onRemote(new ControlEvent(INIT, null));

        // Set the address of the client for later reference.
        InetSocketAddress address = evt.getAddress();

        // Treat loopback addresses specially.
        if (address.getAddress() == null || !address.getAddress().isLoopbackAddress()) {

            String domain = evt.getDomain();

            csh.setAddress(address);
            csh.setDomain((!domain.equals("") && !domain.equals("local")) ? domain : "remote");

        } else {

            String domain = evt.getDomain();

            csh.setAddress(new InetSocketAddress(this.address, address.getPort()));
            csh.setDomain((!domain.equals("") && !domain.equals("remote")) ? domain : "local");
        }

        // The client is now awaiting further instructions.
        csh.setStatus(ClientStatus.WAIT);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }

    /**
     * Handles a possible client transition from {@link ClientStatus#RESOURCE} to {@link ClientStatus#PREPARE}, as the
     * client has acknowledged receipt of its resource descriptor. Once all clients of the same equivalence class check
     * in, they all make said transition.
     */
    protected void handleResourceToPrepare(ClientState csh) {
        transitionIfReady(csh, //
                ClientStatus.RESOURCE_ACK, ClientStatus.PREPARE, //
                LogicalNodeStatus.RESOURCE, LogicalNodeStatus.PREPARE, //
                PREPARE, CLIENT_TIMEOUT_MILLIS);
    }

    /**
     * Handles a possible client transition from {@link ClientStatus#PREPARE} to {@link ClientStatus#EXECUTE}, as the
     * client has requisitioned all necessary resources for execution. Once all clients of the same equivalence class
     * check in, they all make said transition.
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
     * Handles a client transition from {@link ClientStatus#EXECUTE} to {@link ClientStatus#WAIT}, as the client has
     * successfully executed. Once all clients of the same equivalence class check in, the server searches for dependent
     * {@link LogicalNode}s that are newly eligible for execution. Should member {@link FlowNode}s embed subflows, the
     * server executes their {@link FlowBuilder}s and rebuilds the underlying {@link Flow}.
     */
    protected void handleExecuteToWait(ExecuteAckEvent evt) {

        ClientState csh = (ClientState) evt.getSource().getHandler();

        FlowNode fn1 = csh.getFlowNode();
        LogicalNode n1 = fn1.getLogicalNode();
        Flow flow = n1.getFlow();

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

        Control.assertTrue(n1.getStatus() == LogicalNodeStatus.EXECUTE);

        if (n1.getClientCountDown().countDown(fn1)) {

            ArrayList<Object> buildArgs = new ArrayList<Object>();

            for (FlowNode fn2 : n1.getFlowNodes()) {

                Codelet c2 = fn2.getCodelet();

                if (c2 instanceof EmbeddingCodelet) {

                    buildArgs.add(c2);
                    buildArgs.add(fn2);
                }
            }

            // Mark the node as having finished BEFORE determining completion or embedding a subflow.
            n1.setStatus(LogicalNodeStatus.FINISHED);

            // No subflows require embedding.
            if (buildArgs.isEmpty()) {

                // Scan the out-neighbors to see which ones are ready.
                for (LogicalEdge edge : n1.getOut()) {

                    LogicalNode n2 = edge.getV();

                    // This node is ready to execute.
                    if (n2.getDependencyCountDown().countDown(n1)) {

                        this.executeList.add(n2);
                        n2.setStatus(LogicalNodeStatus.PENDING_EXECUTE);
                    }
                }

                // Count down on the node's flow.
                if (flow.getFlowCountDown().countDown(n1)) {

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
                for (LogicalNode n2 : FlowUtilities.buildCountDowns(flow)) {

                    this.executeList.add(n2);
                    n2.setStatus(LogicalNodeStatus.PENDING_EXECUTE);
                }

                // By construction, the modified flow no longer contains the completed node.
                Control.assertTrue(!nodes.contains(n1));
            }
        }

        // The client had better not be in the wait set when we add it.
        Control.assertTrue(this.clientWaitSet.add(csh));

        // Unlink the client from its node.
        csh.setFlowNode(null);
        fn1.setClientState(null);

        // The client is now awaiting further instructions.
        csh.setStatus(ClientStatus.WAIT);

        // Interrupt self.
        this.sp.onLocal(new ControlEvent(REFRESH, this.sp));
    }
}
