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
public class ClientState implements Handler<ControlEvent>, Locatable, Cloneable, EnumStatus<ClientStatus>,
        Handle<Object> {

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

    public void handle(ControlEvent evt) {
        this.serverHandler.handle(evt);
    }

    public Object get() {
        return this.timeoutToken;
    }

    public void set(Object output) {
        throw new UnsupportedOperationException();
    }

    public InetSocketAddress getAddress() {
        return this.address;
    }

    public ClientState setAddress(InetSocketAddress address) {

        this.address = address;

        return this;
    }

    public ClientStatus getStatus() {
        return this.status;
    }

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
