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

import static dapper.Constants.BACKLOG;
import static dapper.Constants.MAX_PENDING_ACCEPTS;
import static dapper.Constants.PORT;
import static dapper.Constants.TIMEOUT;
import static dapper.event.ControlEvent.ControlEventType.INIT;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import shared.cli.CLI;
import shared.cli.CLIOptions;
import shared.cli.CLIOptions.CLIOption;
import shared.net.Connection.InitializationType;
import shared.net.SynchronousManagedConnection;
import shared.util.Control;
import shared.util.CoreThread;
import dapper.AsynchronousBase;
import dapper.event.ControlEvent;
import dapper.event.StreamReadyEvent;
import dapper.server.flow.FlowUtilities;
import dapper.server.flow.StreamEdge;

/**
 * The Dapper client main class.
 * 
 * @apiviz.composedOf dapper.client.ClientProcessor
 * @author Roy Liu
 */
@CLIOptions(options = {
//
        @CLIOption(opt = "h", longOpt = "host", numArgs = 1, description = "the server address"), //
        @CLIOption(opt = "d", longOpt = "domain", numArgs = 1, description = "the execution domain") //
})
public class Client extends CoreThread implements Closeable {

    /**
     * The instance used for logging.
     */
    final protected static Logger Log = LoggerFactory.getLogger(Client.class);

    /**
     * The expected header length of an incoming stream.
     */
    final protected static int HEADER_LENGTH = FlowUtilities.createIdentifier(StreamEdge.class).length();

    /**
     * A guard against the creation of too many accept threads.
     */
    final protected static Semaphore Guard = new Semaphore(MAX_PENDING_ACCEPTS);

    /**
     * Gets the static {@link Logger} instance.
     */
    final public static Logger getLog() {
        return Log;
    }

    final AsynchronousBase base;
    final ClientProcessor processor;
    final InetSocketAddress localAddress;
    final ServerSocketChannel ssChannel;

    volatile boolean run;

    /**
     * Default constructor.
     */
    public Client(InetSocketAddress address, String domain) {
        super("Client");

        this.base = new AsynchronousBase();

        try {

            this.ssChannel = ServerSocketChannel.open();
            this.ssChannel.socket().bind(new InetSocketAddress(0), BACKLOG);

        } catch (IOException e) {

            throw new RuntimeException(e);
        }

        this.localAddress = new InetSocketAddress(this.ssChannel.socket().getLocalPort());

        this.processor = new ClientProcessor(this.base, this.localAddress, address, domain, //
                //
                new Runnable() {

                    @Override
                    public void run() {
                        Control.close(Client.this);
                    }
                } //
        );

        // Prime the processor with an artificially generated event.
        this.processor.onLocal(new ControlEvent(INIT, this.processor));

        this.run = true;

        start();
    }

    /**
     * Delegates to the underlying {@link ClientProcessor}.
     */
    @Override
    public String toString() {
        return this.processor.toString();
    }

    /**
     * Shuts down this client thread.
     */
    @Override
    public void close() {

        this.run = false;
        interrupt();
    }

    @Override
    protected void runUnchecked() throws Exception {

        loop: for (; this.run;) {

            // Attempt to accept a connection.
            final SynchronousManagedConnection smc = this.base.createStreamConnection();

            SocketChannel sChannel = this.ssChannel.accept();

            try {

                smc.init(InitializationType.REGISTER, sChannel).get();

            } catch (Exception e) {

                getLog().info("The connection was closed prematurely.", e);

                continue loop;
            }

            new CoreThread("Accept Sync") {

                @Override
                protected void runUnchecked() throws IOException {

                    // Before proceeding, schedule an interrupt.
                    Client.this.base.scheduleInterrupt(this, TIMEOUT);

                    getLog().debug(String.format("Accepting: %s.", //
                            smc.getRemoteAddress()));

                    // Attempt to read the header.

                    byte[] header = new byte[HEADER_LENGTH];

                    InputStream in = smc.getInputStream();

                    for (int size, offset = 0, length = header.length; //
                    length > 0; offset += size, length -= size) {

                        size = in.read(header, offset, length);

                        // If a complete header could not be read.
                        Control.checkTrue(size != -1, //
                                "Could not read a complete header");
                    }

                    // Success! Notify the server processor.

                    Client.this.processor.onLocal(new StreamReadyEvent(new String(header), smc, Client.this.processor));

                    getLog().debug(String.format("Accepted: %s.", //
                            smc.getRemoteAddress()));
                }

                @Override
                protected void runCatch(Throwable t) {

                    Control.close(smc);

                    getLog().info("Accept failure.", t);
                }

                @Override
                protected void runFinalizer() {
                    Guard.release(1);
                }

            }.run();

            Guard.acquireUninterruptibly(1);
        }
    }

    @Override
    protected void runCatch(Throwable t) {

        // The close was deliberate, so ignore.
        if (t instanceof ClosedByInterruptException) {
            return;
        }

        getLog().info("Client accept thread encountered an unexpected exception.", t);
    }

    @Override
    protected void runFinalizer() {

        Control.close(this.base);
        Control.close(this.processor);
        Control.close(this.ssChannel);
    }

    /**
     * Creates a client.
     * 
     * @throws ParseException
     *             when the command-line arguments couldn't be parsed.
     */
    public static void createClient(String[] args) throws ParseException {

        String host, domain;

        try {

            CommandLine cmdLine = CLI.createCommandLine(Client.class, args);

            host = cmdLine.getOptionValue("h");

            if (host == null) {
                host = "localhost";
            }

            domain = cmdLine.getOptionValue("d");

            if (domain == null) {
                domain = "";
            }

        } catch (ParseException e) {

            getLog().info(CLI.createHelp(Client.class));

            throw e;
        }

        new Client(inferAddress(host), domain);
    }

    /**
     * Tries to infer an {@link InetSocketAddress} from the given "hostname:port" description.
     */
    final protected static InetSocketAddress inferAddress(String description) {

        String[] split = description.split(":", -1);

        switch (split.length) {

        case 1:
            return new InetSocketAddress(split[0], PORT);

        case 2:
            return new InetSocketAddress(split[0], Integer.parseInt(split[1]));

        default:
            throw new IllegalArgumentException("Invalid \"hostname:port\" description");
        }
    }
}
