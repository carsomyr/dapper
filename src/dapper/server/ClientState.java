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

package dapper.server;

import java.net.InetSocketAddress;
import java.util.TimerTask;

import shared.event.EnumStatus;
import shared.event.Handler;
import shared.parallel.Handle;
import dapper.AsynchronousBase;
import dapper.client.ClientStatus;
import dapper.codelet.Locatable;
import dapper.event.ControlEvent;
import dapper.event.ControlEventConnection;
import dapper.event.TimeoutEvent;
import dapper.server.flow.FlowNode;

/**
 * The Dapper server-side client state that stores information on a per-client basis, but forwards all events on to a
 * {@link ServerProcessor} instance.
 * 
 * @author Roy Liu
 */
public class ClientState //
        implements Handler<ControlEvent>, Locatable, Cloneable, EnumStatus<ClientStatus>, Handle<Object> {

    final ControlEventConnection connection;
    final Handler<ControlEvent> serverHandler;
    final AsynchronousBase base;

    InetSocketAddress address;

    Object timeoutToken;
    TimerTask timeoutTask;

    ClientStatus status;

    String domain;

    //

    FlowNode flowNode;

    /**
     * Default constructor.
     */
    public ClientState(ControlEventConnection connection, Handler<ControlEvent> serverHandler, AsynchronousBase base) {

        this.connection = connection;
        this.serverHandler = serverHandler;
        this.base = base;

        this.address = null;
        this.flowNode = null;
        this.timeoutToken = null;
        this.timeoutTask = null;
        this.domain = null;

        this.status = ClientStatus.IDLE;
    }

    /**
     * Creates a {@link ClientState} with this client's settings.
     */
    @Override
    public ClientState clone() {

        final ClientState res;

        try {

            res = (ClientState) super.clone();

        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }

        res.flowNode = null;

        return res;
    }

    @Override
    public void handle(ControlEvent evt) {
        this.serverHandler.handle(evt);
    }

    @Override
    public Object get() {
        return this.timeoutToken;
    }

    @Override
    public void set(Object output) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InetSocketAddress getAddress() {
        return this.address;
    }

    @Override
    public ClientState setAddress(InetSocketAddress address) {

        this.address = address;

        return this;
    }

    @Override
    public ClientStatus getStatus() {
        return this.status;
    }

    @Override
    public void setStatus(ClientStatus status) {
        this.status = status;
    }

    /**
     * Gets the domain.
     */
    public String getDomain() {
        return this.domain;
    }

    /**
     * Sets the domain.
     */
    public void setDomain(String domain) {
        this.domain = domain;
    }

    /**
     * Gets the {@link FlowNode}.
     */
    public FlowNode getFlowNode() {
        return this.flowNode;
    }

    /**
     * Sets the {@link FlowNode}.
     */
    public void setFlowNode(FlowNode flowNode) {
        this.flowNode = flowNode;
    }

    /**
     * Schedules a timeout.
     */
    public void timeout(long timeout) {

        untimeout();

        this.timeoutToken = new Object();
        this.timeoutTask = this.base.scheduleEvent( //
                (ControlEvent) new TimeoutEvent(this, this.timeoutToken, this.connection), timeout);
    }

    /**
     * Cancels a timeout.
     */
    public void untimeout() {

        // Cancel any existing timeout.
        if (this.timeoutToken != null) {

            this.timeoutToken = null;
            this.timeoutTask.cancel();
        }
    }

    /**
     * Gets the {@link ControlEventConnection}.
     */
    public ControlEventConnection getConnection() {
        return this.connection;
    }
}
