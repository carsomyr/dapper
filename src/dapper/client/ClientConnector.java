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

package dapper.client;

import static dapper.Constants.TIMEOUT;
import static dapper.client.Client.HEADER_LENGTH;
import static shared.util.Control.checkTrue;

import java.io.Closeable;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import shared.event.Source;
import shared.net.SynchronousManagedConnection;
import shared.util.Control;
import shared.util.CoreThread;
import dapper.AsynchronousBase;
import dapper.codelet.StreamResource;
import dapper.event.ControlEvent;
import dapper.event.ResetEvent;
import dapper.event.SourceType;
import dapper.event.StreamReadyEvent;

/**
 * A client connector thread for establishing downstream TCP connections.
 * 
 * @author Roy Liu
 */
public class ClientConnector extends CoreThread implements Closeable {

    final Set<StreamResource<?>> connectResources;
    final Map<String, SynchronousManagedConnection> connectionMap;

    final AsynchronousBase base;
    final Source<ControlEvent, SourceType> callback;

    /**
     * Default constructor.
     */
    public ClientConnector(Set<StreamResource<?>> connectResources, //
            AsynchronousBase base, Source<ControlEvent, SourceType> callback) {
        super("Connector Thread");

        this.base = base;

        this.connectResources = connectResources;
        this.connectionMap = new HashMap<String, SynchronousManagedConnection>();

        this.callback = callback;
    }

    /**
     * Invalidates this connector thread.
     */
    public void close() {
        interrupt();
    }

    @Override
    protected void runUnchecked() throws Exception {

        // Before proceeding, schedule an interrupt.
        this.base.scheduleInterrupt(this, TIMEOUT);

        Map<String, Future<InetSocketAddress>> futureMap = new HashMap<String, Future<InetSocketAddress>>();

        for (StreamResource<?> connectResource : this.connectResources) {

            SynchronousManagedConnection smc = this.base.createStreamConnection();
            String identifier = connectResource.getIdentifier();

            futureMap.put(identifier, smc.connect(connectResource.getAddress()));

            Client.getLog().debug(String.format("Connecting: %s.", connectResource.getAddress()));

            this.connectionMap.put(identifier, smc);
        }

        for (String identifier : this.connectionMap.keySet()) {

            checkTrue(identifier.length() == HEADER_LENGTH);

            SynchronousManagedConnection smc = this.connectionMap.get(identifier);
            futureMap.get(identifier).get();

            OutputStream out = smc.getOutputStream();
            out.write(identifier.getBytes());

            Client.getLog().debug(String.format("Connected: %s.", smc.getRemoteAddress()));

            this.callback.onLocal(new StreamReadyEvent(identifier, smc, this.callback));
        }
    }

    @Override
    protected void runCatch(Throwable t) {

        // Close all connections.
        for (String identifier : this.connectionMap.keySet()) {
            Control.close(this.connectionMap.get(identifier));
        }

        Client.getLog().info("Connect failure.", t);

        ResetEvent resetEvent = new ResetEvent("Connect failure", t.getStackTrace(), this.callback);
        resetEvent.set(this);

        this.callback.onLocal(resetEvent);
    }
}
