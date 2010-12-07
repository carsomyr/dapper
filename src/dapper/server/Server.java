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

import static dapper.Constants.DEFAULT_SERVER_PORT;
import static dapper.event.ControlEvent.ControlEventType.CREATE_FLOW;
import static dapper.event.ControlEvent.ControlEventType.CREATE_USER_QUEUE;
import static dapper.event.ControlEvent.ControlEventType.GET_FLOW_PROXY;
import static dapper.event.ControlEvent.ControlEventType.GET_PENDING_COUNT;
import static dapper.event.ControlEvent.ControlEventType.SET_AUTOCLOSE_IDLE;
import static shared.net.ConnectionManager.InitializationType.REGISTER;
import static shared.net.Constants.DEFAULT_BACKLOG_SIZE;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shared.net.Connection;
import shared.util.IoBase;
import dapper.Constants;
import dapper.DapperBase;
import dapper.event.ControlEventHandler;
import dapper.event.FlowEvent;
import dapper.server.ServerProcessor.FlowBuildRequest;
import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowBuilder;
import dapper.server.flow.FlowNode;

/**
 * The Dapper server class.
 * 
 * @apiviz.composedOf dapper.server.ServerProcessor
 * @author Roy Liu
 */
public class Server extends Thread implements Closeable {

    /**
     * The instance used for logging.
     */
    final protected static Logger log = LoggerFactory.getLogger(Server.class);

    /**
     * Gets the static {@link Logger} instance.
     */
    final public static Logger getLog() {
        return log;
    }

    final DapperBase base;
    final ServerProcessor processor;
    final ServerSocketChannel ssChannel;

    volatile boolean run;

    /**
     * Default constructor.
     * 
     * @param address
     *            the server's {@link InetAddress} as it would appear to clients.
     * @param port
     *            the port to listen on.
     * @throws UnknownHostException
     *             when the server's {@link InetAddress} could not be inferred.
     * @throws IOException
     *             when the underlying {@link ServerSocketChannel} could not be bound.
     */
    public Server(InetAddress address, int port) throws UnknownHostException, IOException {
        super("Server");

        this.ssChannel = ServerSocketChannel.open();
        this.ssChannel.socket().bind(new InetSocketAddress(port), DEFAULT_BACKLOG_SIZE);

        this.base = new DapperBase();

        this.processor = new ServerProcessor((address != null) ? address : DapperBase.inferAddress(), //
                //
                new Runnable() {

                    @Override
                    public void run() {
                        IoBase.close(Server.this);
                    }
                } //
        );
        this.processor.start();

        this.run = true;

        start();
    }

    /**
     * Alternate constructor. Listens on the default port value {@link Constants#DEFAULT_SERVER_PORT}.
     * 
     * @throws UnknownHostException
     *             when the server's {@link InetAddress} could not be inferred.
     * @throws IOException
     *             when the underlying {@link ServerSocketChannel} could not be bound.
     */
    public Server() throws UnknownHostException, IOException {
        this(null, DEFAULT_SERVER_PORT);
    }

    /**
     * A facade for {@link #createFlow(FlowBuilder, ClassLoader, int)}.
     * 
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public FlowProxy createFlow(FlowBuilder builder, ClassLoader cl) //
            throws InterruptedException, ExecutionException {
        return createFlow(builder, cl, FlowEvent.F_NONE);
    }

    /**
     * Creates a {@link Flow}.
     * 
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public FlowProxy createFlow(FlowBuilder builder, ClassLoader cl, int flowFlags) //
            throws InterruptedException, ExecutionException {
        return this.processor.request(CREATE_FLOW, new FlowBuildRequest(builder, cl, flowFlags));
    }

    /**
     * Refreshes all {@link Flow}s.
     * 
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public List<FlowProxy> refresh() throws InterruptedException, ExecutionException {
        return this.processor.request(GET_FLOW_PROXY, null);
    }

    /**
     * Closes all idle clients.
     * 
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public void closeIdleClients() throws InterruptedException, ExecutionException {
        this.processor.request(SET_AUTOCLOSE_IDLE, null);
    }

    /**
     * Sets whether idle clients should be automatically closed.
     * 
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public Server setAutocloseIdle(boolean autocloseIdle) throws InterruptedException, ExecutionException {

        this.processor.request(SET_AUTOCLOSE_IDLE, Boolean.valueOf(autocloseIdle));

        return this;
    }

    /**
     * Gets the number of additional clients required to saturate all pending computations.
     * 
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    public int getPendingCount() throws InterruptedException, ExecutionException {
        return (Integer) this.processor.request(GET_PENDING_COUNT, (Object) null);
    }

    /**
     * Creates a new {@link Queue} for subscribing to {@link FlowEvent}s.
     * 
     * @param <F>
     *            the {@link Flow} attachment type.
     * @param <N>
     *            the {@link FlowNode} attachment type.
     * @throws InterruptedException
     *             when this operation is interrupted.
     * @throws ExecutionException
     *             when something goes awry.
     */
    @SuppressWarnings("unchecked")
    public <F, N> BlockingQueue<FlowEvent<F, N>> createFlowEventQueue() //
            throws InterruptedException, ExecutionException {
        return (BlockingQueue<FlowEvent<F, N>>) this.processor.request(CREATE_USER_QUEUE, (Object) null);
    }

    /**
     * Delegates to the underlying {@link ServerProcessor}.
     */
    @Override
    public String toString() {
        return this.processor.toString();
    }

    /**
     * Shuts down this thread.
     */
    @Override
    public void close() {

        this.run = false;
        interrupt();
    }

    /**
     * Runs the connection creation loop.
     */
    @Override
    public void run() {

        try {

            loop: for (; this.run;) {

                // Attempt to accept a connection.
                ControlEventHandler<Connection> ceh = this.base.createControlHandler(this.processor);
                ceh.setHandler(new ClientState(ceh, this.processor, this.base));

                SocketChannel sChannel = this.ssChannel.accept();

                try {

                    this.base.getManager().init(REGISTER, ceh, sChannel).get();

                } catch (Exception e) {

                    getLog().info("The connection was closed prematurely.", e);

                    continue loop;
                }
            }

        } catch (Throwable t) {

            // The close was deliberate, so ignore.
            if (t instanceof ClosedByInterruptException) {
                return;
            }

            getLog().info("Server accept thread encountered an unexpected exception.", t);

        } finally {

            IoBase.close(this.base);
            IoBase.close(this.processor);
            IoBase.close(this.ssChannel);
        }
    }
}
