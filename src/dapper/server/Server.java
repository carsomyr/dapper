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

import static dapper.Constants.BACKLOG;
import static dapper.Constants.PORT;
import static dapper.event.ControlEvent.ControlEventType.QUERY_CLOSE_IDLE;
import static dapper.event.ControlEvent.ControlEventType.QUERY_INIT;
import static dapper.event.ControlEvent.ControlEventType.QUERY_PENDING_COUNT;
import static dapper.event.ControlEvent.ControlEventType.QUERY_REFRESH;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shared.util.Control;
import shared.util.CoreThread;
import dapper.AsynchronousBase;
import dapper.Constants;
import dapper.event.ControlEventConnection;
import dapper.server.ServerProcessor.FlowBuildRequest;
import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowListener;

/**
 * The Dapper server class.
 * 
 * @apiviz.composedOf dapper.server.ServerProcessor
 * @author Roy Liu
 */
public class Server extends CoreThread implements Closeable {

    /**
     * The instance used for logging.
     */
    final protected static Logger Log = LoggerFactory.getLogger(Server.class);

    /**
     * Gets the static {@link Logger} instance.
     */
    final public static Logger getLog() {
        return Log;
    }

    final AsynchronousBase base;
    final ServerProcessor processor;
    final ServerSocketChannel ssChannel;

    volatile boolean run;

    /**
     * Default constructor.
     * 
     * @param port
     *            the port to listen on.
     * @throws UnknownHostException
     *             when the server's {@link InetAddress} could not be inferred.
     * @throws IOException
     *             when the underlying {@link ServerSocketChannel} could not be bound.
     */
    public Server(int port) throws UnknownHostException, IOException {
        super("Server");

        this.ssChannel = ServerSocketChannel.open();
        this.ssChannel.socket().bind(new InetSocketAddress(port), BACKLOG);

        this.base = new AsynchronousBase();

        this.processor = new ServerProcessor( //
                AsynchronousBase.inferAddress(), //
                //
                new Runnable() {

                    public void run() {
                        Control.close(Server.this);
                    }
                });

        this.run = true;

        start();
    }

    /**
     * Alternate constructor. Listens on the default port value {@link Constants#PORT}.
     * 
     * @throws UnknownHostException
     *             when the server's {@link InetAddress} could not be inferred.
     * @throws IOException
     *             when the underlying {@link ServerSocketChannel} could not be bound.
     */
    public Server() throws UnknownHostException, IOException {
        this(PORT);
    }

    /**
     * A facade for {@link #createFlow(FlowBuilder, ClassLoader, FlowListener)}.
     * 
     * @throws InterruptedException
     *             when this operation was interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public FlowProxy createFlow(FlowBuilder builder, ClassLoader cl) //
            throws InterruptedException, ExecutionException {
        return createFlow(builder, cl, null);
    }

    /**
     * Creates a {@link Flow}.
     * 
     * @throws InterruptedException
     *             when this operation was interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    @SuppressWarnings("unchecked")
    public FlowProxy createFlow(FlowBuilder builder, ClassLoader cl, FlowListener<?, ?> listener) //
            throws InterruptedException, ExecutionException {
        return this.processor.query(QUERY_INIT, new FlowBuildRequest(builder, cl, (FlowListener<Object, ?>) listener));
    }

    /**
     * Refreshes all {@link Flow}s.
     * 
     * @throws InterruptedException
     *             when this operation was interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public List<FlowProxy> refresh() throws InterruptedException, ExecutionException {
        return this.processor.query(QUERY_REFRESH, null);
    }

    /**
     * Closes all idle clients.
     * 
     * @throws InterruptedException
     *             when this operation was interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public void closeIdleClients() throws InterruptedException, ExecutionException {
        this.processor.query(QUERY_CLOSE_IDLE, null);
    }

    /**
     * Sets whether idle clients should be automatically closed.
     * 
     * @throws InterruptedException
     *             when this operation was interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public Server setAutoCloseIdle(boolean value) throws InterruptedException, ExecutionException {

        this.processor.query(QUERY_CLOSE_IDLE, Boolean.valueOf(value));

        return this;
    }

    /**
     * Gets the number of additional clients required to saturate all pending computations.
     * 
     * @throws InterruptedException
     *             when this operation was interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public int getPendingCount() throws InterruptedException, ExecutionException {
        return (Integer) this.processor.query(QUERY_PENDING_COUNT, (Object) null);
    }

    /**
     * Delegates to the underlying {@link ServerProcessor}.
     */
    @Override
    public String toString() {
        return this.processor.toString();
    }

    /**
     * Shuts down this client thread.
     */
    public void close() {

        this.run = false;
        interrupt();
    }

    @Override
    protected void runUnchecked() throws Exception {

        loop: for (; this.run;) {

            // Attempt to accept a connection.
            ControlEventConnection cec = this.base.createControlConnection(this.processor);
            cec.setHandler(new ClientState(cec, this.processor, this.base));

            try {

                cec.register(this.ssChannel.accept()).get();

            } catch (Exception e) {

                getLog().info("The connection was closed prematurely.", e);

                continue loop;
            }
        }
    }

    @Override
    protected void runCatch(Throwable t) {

        // The close was deliberate, so ignore.
        if (t instanceof ClosedByInterruptException) {
            return;
        }

        getLog().info("Server accept thread encountered an unexpected exception.", t);
    }

    @Override
    protected void runFinalizer() {

        Control.close(this.base);
        Control.close(this.processor);
        Control.close(this.ssChannel);
    }
}
