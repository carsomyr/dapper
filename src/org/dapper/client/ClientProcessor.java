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

package org.dapper.client;

import static org.dapper.client.ClientStatus.IDLE;
import static org.dapper.event.SourceType.PROCESSOR;

import java.net.InetSocketAddress;

import org.dapper.DapperBase;
import org.dapper.event.ControlEvent;
import org.dapper.event.ControlEvent.ControlEventType;
import org.dapper.event.ControlEventHandler;
import org.dapper.event.DataEvent;
import org.dapper.event.ErrorEvent;
import org.dapper.event.ExecuteAckEvent;
import org.dapper.event.ResetEvent;
import org.dapper.event.ResourceEvent;
import org.dapper.event.SourceType;
import org.dapper.event.StreamReadyEvent;
import org.shared.event.EnumStatus;
import org.shared.event.EventProcessor;
import org.shared.event.Handler;
import org.shared.event.StateTable;
import org.shared.event.Transitions;
import org.shared.event.Transitions.Transition;
import org.shared.net.SocketConnection;

/**
 * The Dapper client processor.
 * 
 * @apiviz.composedOf org.dapper.client.ClientLogic
 * @apiviz.owns org.dapper.client.ClientStatus
 * @author Roy Liu
 */
public class ClientProcessor extends EventProcessor<ControlEvent, ControlEventType, SourceType> //
        implements EnumStatus<ClientStatus> {

    final Runnable finalizer;
    final ClientLogic logic;
    final StateTable<ClientStatus, ControlEventType, ControlEvent> fsmInternal;
    final StateTable<ClientStatus, ControlEventType, ControlEvent> fsmExternal;

    ClientStatus status;

    /**
     * Default constructor.
     */
    public ClientProcessor(DapperBase base, //
            InetSocketAddress localAddress, //
            InetSocketAddress remoteAddress, //
            String domain, //
            Runnable finalizer) {
        super("CEP");

        this.finalizer = finalizer;

        this.logic = new ClientLogic(base, localAddress, remoteAddress, domain, this);

        this.fsmInternal = new StateTable<ClientStatus, ControlEventType, ControlEvent>(this, //
                ClientStatus.class, ControlEventType.class, //
                "internal");
        this.fsmExternal = new StateTable<ClientStatus, ControlEventType, ControlEvent>(this, //
                ClientStatus.class, ControlEventType.class, //
                "external");

        this.status = IDLE;
    }

    @Override
    public SourceType getType() {
        return PROCESSOR;
    }

    @Override
    public void handle(ControlEvent evt) {

        checkCurrentThread();

        switch (evt.getSource().getType()) {

        case PROCESSOR:
            this.fsmInternal.lookup(this, evt);
            break;

        case CONNECTION:
            this.fsmExternal.lookup(this, evt);
            break;

        default:
            throw new IllegalArgumentException("Invalid source type");
        }
    }

    @Override
    public ClientStatus getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    @Override
    protected void doFinally() {
        this.finalizer.run();
    }

    // INTERNAL LOGIC

    @Transition(currentState = "PREPARE", eventType = "REFRESH", group = "internal")
    final Handler<ControlEvent> refreshHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleRefresh();
        }
    };

    @Transitions(transitions = {
            //
            // Resource acquisition failure.
            @Transition(currentState = "PREPARE", eventType = "RESET", group = "internal"), //
            // Execution failure.
            @Transition(currentState = "EXECUTE", eventType = "RESET", group = "internal"), //
            // Premature resource acquisition failure by another client.
            @Transition(currentState = "RESOURCE", eventType = "RESET", group = "external"), //
            // Resource acquisition failure by another client.
            @Transition(currentState = "PREPARE", eventType = "RESET", group = "external"), //
            // Execution failure by another client.
            @Transition(currentState = "EXECUTE", eventType = "RESET", group = "external") //
    })
    final Handler<ControlEvent> resetHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleReset((ResetEvent) evt);
        }
    };

    @Transition(currentState = "IDLE", eventType = "INIT", group = "internal")
    final Handler<ControlEvent> idleToConnectHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleIdleToConnect();
        }
    };

    @Transitions(transitions = {
            //
            @Transition(currentState = "IDLE", eventType = "STREAM_READY", group = "internal"), //
            @Transition(currentState = "CONNECT", eventType = "STREAM_READY", group = "internal"), //
            @Transition(currentState = "WAIT", eventType = "STREAM_READY", group = "internal"), //
            // Potentially premature ready stream.
            @Transition(currentState = "RESOURCE", eventType = "STREAM_READY", group = "internal"), //
            @Transition(currentState = "PREPARE", eventType = "STREAM_READY", group = "internal"), //
            @Transition(currentState = "EXECUTE", eventType = "STREAM_READY", group = "internal") //
    })
    final Handler<ControlEvent> streamReadyHandler = new Handler<ControlEvent>() {

        @SuppressWarnings("unchecked")
        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleStreamReady((StreamReadyEvent<SocketConnection>) evt);
        }
    };

    @Transition(currentState = "EXECUTE", eventType = "EXECUTE_ACK", group = "internal")
    final Handler<ControlEvent> successfulExecutionHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleSuccessfulExecution((ExecuteAckEvent) evt);
        }
    };

    // EXTERNAL LOGIC

    @Transitions(transitions = {
            //
            @Transition(currentState = "CONNECT", eventType = "ERROR", group = "external"), //
            @Transition(currentState = "WAIT", eventType = "ERROR", group = "external"), //
            @Transition(currentState = "RESOURCE", eventType = "ERROR", group = "external"), //
            @Transition(currentState = "PREPARE", eventType = "ERROR", group = "external"), //
            @Transition(currentState = "EXECUTE", eventType = "ERROR", group = "external") //
    })
    final Handler<ControlEvent> errorHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleError(((ErrorEvent) evt));
        }
    };

    @Transitions(transitions = {
            //
            @Transition(currentState = "CONNECT", eventType = "END_OF_STREAM", group = "external"), //
            @Transition(currentState = "WAIT", eventType = "END_OF_STREAM", group = "external"), //
            @Transition(currentState = "RESOURCE", eventType = "END_OF_STREAM", group = "external"), //
            @Transition(currentState = "PREPARE", eventType = "END_OF_STREAM", group = "external"), //
            @Transition(currentState = "EXECUTE", eventType = "END_OF_STREAM", group = "external") //
    })
    final Handler<ControlEvent> eosHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleEos((ControlEventHandler<?>) evt.getSource());
        }
    };

    @Transition(currentState = "CONNECT", eventType = "INIT", group = "external")
    final Handler<ControlEvent> connectToWaitHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleConnectToWait((ControlEventHandler<?>) evt.getSource());
        }
    };

    @Transition(currentState = "WAIT", eventType = "RESOURCE", group = "external")
    final Handler<ControlEvent> waitToResourceHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleWaitToResource((ResourceEvent) evt);
        }
    };

    @Transition(currentState = "RESOURCE", eventType = "PREPARE", group = "external")
    final Handler<ControlEvent> resourceToPrepareHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleResourceToPrepare();
        }
    };

    @Transition(currentState = "PREPARE", eventType = "EXECUTE", group = "external")
    final Handler<ControlEvent> prepareToExecuteHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handlePrepareToExecute();
        }
    };

    @Transitions(transitions = {
            //
            // Request for class data from the server.
            @Transition(currentState = "EXECUTE", eventType = "DATA", group = "internal"), //
            @Transition(currentState = "EXECUTE", eventType = "DATA", group = "external") //
    })
    final Handler<ControlEvent> dataHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleData((DataEvent) evt);
        }
    };
}
