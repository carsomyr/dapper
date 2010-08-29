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

package dapper;

import static dapper.Constants.BACKLOG;
import static dapper.Constants.BUFFER_SIZE;
import static dapper.Constants.TIMER_PURGE_INTERVAL;

import java.io.Closeable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicLong;

import shared.codec.Hex;
import shared.event.Event;
import shared.event.SourceLocal;
import shared.net.ConnectionManager;
import shared.net.SynchronousManagedConnection;
import shared.util.Control;
import dapper.event.ControlEvent;
import dapper.event.ControlEventConnection;

/**
 * A class for housing shared, asynchronous services like {@link ConnectionManager} and {@link Timer}.
 * 
 * @author Roy Liu
 */
public class AsynchronousBase implements Closeable {

    /**
     * The address of the local host.
     */
    final protected static byte[] LocalHost = Hex.hexToBytes("80000001");

    final ConnectionManager manager;
    final Timer timer;
    final AtomicLong counter;

    /**
     * Default constructor.
     */
    public AsynchronousBase() {

        this.manager = new ConnectionManager("CM", BACKLOG);
        this.timer = new Timer();
        this.timer.schedule(new TimerTask() {

            @Override
            public void run() {

                AsynchronousBase.this.timer.purge();
                System.gc();
            }

        }, 0, TIMER_PURGE_INTERVAL);

        this.counter = new AtomicLong(0);
    }

    /**
     * Shuts down the underlying {@link ConnectionManager} and {@link Timer}.
     */
    @Override
    public void close() {

        Control.close(this.manager);
        this.timer.cancel();
    }

    /**
     * Creates a {@link ControlEventConnection} for control messages.
     */
    public ControlEventConnection createControlConnection(SourceLocal<ControlEvent> delegate) {
        return new ControlEventConnection(String.format("EC_%d", this.counter.getAndIncrement()), //
                this.manager, delegate);
    }

    /**
     * Creates a {@link SynchronousManagedConnection} for TCP stream transfer.
     */
    public SynchronousManagedConnection createStreamConnection() {
        return new SynchronousManagedConnection(String.format("SC_%d", this.counter.getAndIncrement()), //
                this.manager) //
                .setBufferSize(BUFFER_SIZE);
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

            for (Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces(); e1.hasMoreElements();) {

                for (Enumeration<InetAddress> e2 = e1.nextElement().getInetAddresses(); e2.hasMoreElements();) {

                    InetAddress addr = e2.nextElement();

                    // Match the first address that isn't the local host.
                    if (addr.getAddress().length == 4 && !Arrays.equals(LocalHost, addr.getAddress())) {
                        return addr;
                    }
                }
            }

            return InetAddress.getLocalHost();

        } catch (Exception e) {

            throw new RuntimeException("Error while inferring network address", e);
        }
    }
}
