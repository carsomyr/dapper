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

package org.dapper;

import static org.dapper.Constants.DEFAULT_BUFFER_SIZE;
import static org.dapper.Constants.TIMER_PURGE_INTERVAL_MILLIS;
import static org.shared.util.XmlBase.strictErrorHandler;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Collections;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dapper.event.ControlEvent;
import org.dapper.event.ControlEventHandler;
import org.shared.codec.Codecs;
import org.shared.event.Event;
import org.shared.event.SourceLocal;
import org.shared.net.Connection;
import org.shared.net.ConnectionManager;
import org.shared.net.SocketConnection;
import org.shared.net.SocketManager;
import org.shared.net.handler.SynchronousHandler;
import org.shared.net.nio.NioManager;
import org.shared.util.IoBase;
import org.w3c.dom.Document;

/**
 * A utility class for common Dapper operations.
 * 
 * @author Roy Liu
 */
public class DapperBase implements Closeable {

    /**
     * The address of the local host.
     */
    final protected static byte[] localHost = Codecs.hexToBytes("80000001");

    /**
     * A {@link DocumentBuilder} local to the current thread.
     */
    final protected static ThreadLocal<DocumentBuilder> builderLocal = new ThreadLocal<DocumentBuilder>() {

        @Override
        protected DocumentBuilder initialValue() {

            try {

                DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                dbf.setValidating(true);
                dbf.setFeature("http://apache.org/xml/features/validation/dynamic", true);

                DocumentBuilder db = dbf.newDocumentBuilder();
                db.setErrorHandler(strictErrorHandler);

                return db;

            } catch (ParserConfigurationException e) {

                throw new RuntimeException(e);
            }
        }
    };

    final SocketManager<?, ? extends SocketConnection> manager;
    final Timer timer;
    final AtomicLong counter;

    /**
     * Default constructor.
     */
    public DapperBase() {

        this.manager = new NioManager("CM").setBufferSize(DEFAULT_BUFFER_SIZE);
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {

                DapperBase.this.timer.purge();
                System.gc();
            }

        }, 0, TIMER_PURGE_INTERVAL_MILLIS);

        this.counter = new AtomicLong(0);
    }

    /**
     * Shuts down the underlying {@link ConnectionManager} and {@link Timer}.
     */
    @Override
    public void close() {

        IoBase.close(this.manager);
        this.timer.cancel();
    }

    /**
     * Creates a {@link ControlEventHandler} for control messages.
     * 
     * @param <C>
     *            the {@link Connection} type.
     */
    public <C extends Connection> ControlEventHandler<C> createControlHandler(SourceLocal<ControlEvent> delegate) {
        return new ControlEventHandler<C>(String.format("EC_%d", this.counter.getAndIncrement()), delegate);
    }

    /**
     * Creates a {@link SynchronousHandler} for TCP stream transfer.
     * 
     * @param <C>
     *            the {@link Connection} type.
     */
    public <C extends Connection> SynchronousHandler<C> createStreamHandler() {
        return new SynchronousHandler<C>(String.format("SC_%d", this.counter.getAndIncrement()), DEFAULT_BUFFER_SIZE);
    }

    /**
     * Gets the {@link SocketManager}.
     */
    public SocketManager<?, ? extends SocketConnection> getManager() {
        return this.manager;
    }

    /**
     * Schedules an interrupt for the given thread.
     */
    public TimerTask scheduleInterrupt(final Thread thread, long timeout) {

        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                thread.interrupt();
            }
        };

        this.timer.schedule(task, timeout);

        return task;
    }

    /**
     * Schedules an {@link Event} for firing.
     * 
     * @param <T>
     *            the {@link Event} type.
     */
    public <T extends Event<T, ?, ?>> TimerTask scheduleEvent(final T evt, long timeout) {

        TimerTask task = new TimerTask() {

            @Override
            public void run() {
                evt.getSource().onLocal(evt);
            }
        };

        this.timer.schedule(task, timeout);

        return task;
    }

    /**
     * Gets the bound address of the underlying {@link ConnectionManager}.
     */
    public InetSocketAddress getBoundAddress() {
        return this.manager.getBoundAddresses().get(0);
    }

    /**
     * Infers the {@link InetAddress} of this machine, as it is known to the outside world.
     */
    final public static InetAddress inferAddress() {

        try {

            for (NetworkInterface iFace : Collections.list(NetworkInterface.getNetworkInterfaces())) {

                for (InetAddress addr : Collections.list(iFace.getInetAddresses())) {

                    // Match the first address that isn't the local host.
                    if (addr.getAddress().length == 4 && !Arrays.equals(localHost, addr.getAddress())) {
                        return addr;
                    }
                }
            }

            return InetAddress.getLocalHost();

        } catch (IOException e) {

            throw new RuntimeException("Could not infer network address", e);
        }
    }

    /**
     * Creates a new {@link Document}.
     */
    final public static Document newDocument() {
        return builderLocal.get().newDocument();
    }

    /**
     * Parses a {@link Document} from the given {@link InputStream}.
     */
    final public static Document parse(InputStream in) {

        try {

            return builderLocal.get().parse(in);

        } catch (RuntimeException e) {

            throw e;

        } catch (Exception e) {

            throw new RuntimeException(e);
        }
    }

    /**
     * Parses a {@link Document} from the given string.
     */
    final public static Document parse(String s) {
        return parse(new ByteArrayInputStream(s.getBytes()));
    }
}
