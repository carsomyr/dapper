/**
 * <p>
 * Copyright (C) 2008 The Regents of the University of California<br />
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

package dapper.client;

import static dapper.client.ClientStatus.IDLE;
import static dapper.event.SourceType.PROCESSOR;

import java.net.InetSocketAddress;

import shared.event.EnumStatus;
import shared.event.Handler;
import shared.event.StateProcessor;
import shared.event.StateTable;
import shared.event.Transitions;
import shared.event.Transitions.Transition;
import shared.util.Control;
import dapper.AsynchronousBase;
import dapper.event.ControlEvent;
import dapper.event.ControlEvent.ControlEventType;
import dapper.event.ControlEventConnection;
import dapper.event.DataRequestEvent;
import dapper.event.ErrorEvent;
import dapper.event.ExecuteAckEvent;
import dapper.event.ResetEvent;
import dapper.event.ResourceEvent;
import dapper.event.SourceType;
import dapper.event.StreamReadyEvent;

/**
 * The Dapper client processor.
 * 
 * @apiviz.composedOf dapper.client.ClientLogic
 * @apiviz.owns dapper.client.ClientStatus
 * @author Roy Liu
 */
public class ClientProcessor extends StateProcessor<ControlEvent, ControlEventType, SourceType> implements
        EnumStatus<ClientStatus> {

    final ClientLogic logic;

    final StateTable<ClientStatus, ControlEventType, ControlEvent> fsmInternal;
    final StateTable<ClientStatus, ControlEventType, ControlEvent> fsmExternal;

    ClientStatus status;

    /**
     * Default constructor.
     */
    public ClientProcessor(AsynchronousBase base, //
            InetSocketAddress localAddress, //
            InetSocketAddress remoteAddress, //
            String domain, //
            Runnable finalizer) {
        super("CEP");

        setFinalizer(finalizer);

        this.logic = new ClientLogic(base, localAddress, remoteAddress, domain, this);

        this.fsmInternal = new StateTable<ClientStatus, ControlEventType, ControlEvent>(this, //
                ClientStatus.class, ControlEventType.class, //
                "internal");
        this.fsmExternal = new StateTable<ClientStatus, ControlEventType, ControlEvent>(this, //
                ClientStatus.class, ControlEventType.class, //
                "external");

        this.status = IDLE;
    }

    public SourceType getType() {
        return PROCESSOR;
    }

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
            throw new AssertionError("Control should never reach here");
        }
    }

    public ClientStatus getStatus() {
        return this.status;
    }

    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    // INTERNAL LOGIC

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
    final Handler<ControlEvent> reset = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleReset((ResetEvent) evt);
        }
    };

    // Self initialization.
    @Transition(currentState = "IDLE", eventType = "INIT", group = "internal")
    final Handler<ControlEvent> idleToConnect = new Handler<ControlEvent>() {

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
    final Handler<ControlEvent> streamReady = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleStreamReady((StreamReadyEvent) evt);
        }
    };

    // Determination of execution eligibility.
    @Transition(currentState = "PREPARE", eventType = "REFRESH", group = "internal")
    final Handler<ControlEvent> refresh = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleRefresh();
        }
    };

    // Execution success.
    @Transition(currentState = "EXECUTE", eventType = "EXECUTE_ACK", group = "internal")
    final Handler<ControlEvent> executeSuccess = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleExecuteSuccess((ExecuteAckEvent) evt);
        }
    };

    // Client process shutdown.
    @Transition(currentState = "SHUTDOWN", eventType = "SHUTDOWN", group = "internal")
    final Handler<ControlEvent> shutdown = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            Control.close(ClientProcessor.this);
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
    final Handler<ControlEvent> error = new Handler<ControlEvent>() {

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
    final Handler<ControlEvent> eos = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleEOS((ControlEventConnection) evt.getSource());
        }
    };

    // Server connection initialized.
    @Transition(currentState = "CONNECT", eventType = "INIT", group = "external")
    final Handler<ControlEvent> connectToWait = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleConnectToWait((ControlEventConnection) evt.getSource());
        }
    };

    // Resource descriptors.
    @Transition(currentState = "WAIT", eventType = "RESOURCE", group = "external")
    final Handler<ControlEvent> waitToResource = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleWaitToResource((ResourceEvent) evt);
        }
    };

    // Resource preparation.
    @Transition(currentState = "RESOURCE", eventType = "PREPARE", group = "external")
    final Handler<ControlEvent> resourceToPrepare = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleResourceToPrepare();
        }
    };

    // Codelet execution.
    @Transition(currentState = "PREPARE", eventType = "EXECUTE", group = "external")
    final Handler<ControlEvent> prepareToExecute = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handlePrepareToExecute();
        }
    };

    @Transitions(transitions = {
            //
            // Request for class data from the server.
            @Transition(currentState = "EXECUTE", eventType = "DATA_REQUEST", group = "internal"), //
            @Transition(currentState = "EXECUTE", eventType = "DATA_REQUEST", group = "external") //
    })
    final Handler<ControlEvent> dataRequest = new Handler<ControlEvent>() {

        public void handle(ControlEvent evt) {
            ClientProcessor.this.logic.handleDataRequest((DataRequestEvent) evt);
        }
    };
}
