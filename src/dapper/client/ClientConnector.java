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

package dapper.client;

import static dapper.Constants.REQUEST_TIMEOUT_MILLIS;
import static dapper.client.Client.HEADER_LENGTH;
import static shared.net.ConnectionManager.InitializationType.CONNECT;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Future;

import shared.event.Source;
import shared.net.SocketConnection;
import shared.net.handler.SynchronousHandler;
import shared.util.Control;
import shared.util.CoreThread;
import dapper.DapperBase;
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
    final Map<String, SynchronousHandler<SocketConnection>> handlerMap;

    final DapperBase base;
    final Source<ControlEvent, SourceType> callback;

    /**
     * Default constructor.
     */
    public ClientConnector(Set<StreamResource<?>> connectResources, //
            DapperBase base, Source<ControlEvent, SourceType> callback) {
        super("Connector Thread");

        this.base = base;

        this.connectResources = connectResources;
        this.handlerMap = new HashMap<String, SynchronousHandler<SocketConnection>>();

        this.callback = callback;
    }

    /**
     * Invalidates this connector thread.
     */
    @Override
    public void close() {
        interrupt();
    }

    @Override
    protected void doRun() throws Exception {

        // Before proceeding, schedule an interrupt.
        this.base.scheduleInterrupt(this, REQUEST_TIMEOUT_MILLIS);

        Map<String, Future<? extends SocketConnection>> futureMap = //
        new HashMap<String, Future<? extends SocketConnection>>();

        for (StreamResource<?> connectResource : this.connectResources) {

            String identifier = connectResource.getIdentifier();
            SynchronousHandler<SocketConnection> sh = this.base.createStreamHandler();

            futureMap.put(identifier, this.base.getManager().init(CONNECT, sh, connectResource.getAddress()));

            Client.getLog().debug(String.format("Connecting: %s.", connectResource.getAddress()));

            this.handlerMap.put(identifier, sh);
        }

        for (Entry<String, SynchronousHandler<SocketConnection>> entry : this.handlerMap.entrySet()) {

            String identifier = entry.getKey();
            SynchronousHandler<SocketConnection> sh = entry.getValue();

            Control.checkTrue(identifier.length() == HEADER_LENGTH);

            SocketConnection conn = futureMap.get(identifier).get();

            sh.getOutputStream().write(identifier.getBytes());

            Client.getLog().debug(String.format("Connected: %s.", conn.getRemoteAddress()));

            this.callback.onLocal(new StreamReadyEvent<SocketConnection>(identifier, sh, this.callback));
        }
    }

    @Override
    protected void doCatch(Throwable t) {

        // Close all connections.
        for (SynchronousHandler<?> handler : this.handlerMap.values()) {
            Control.close(handler);
        }

        Client.getLog().info("Connect failure.", t);

        ResetEvent resetEvent = new ResetEvent("Connect failure", t, this.callback);
        resetEvent.set(this);

        this.callback.onLocal(resetEvent);
    }
}
