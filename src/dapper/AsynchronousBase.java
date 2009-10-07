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

            for (Enumeration<NetworkInterface> e1 = NetworkInterface.getNetworkInterfaces(); //
            e1.hasMoreElements();) {

                for (Enumeration<InetAddress> e2 = e1.nextElement().getInetAddresses(); //
                e2.hasMoreElements();) {

                    InetAddress addr = e2.nextElement();

                    // Match the first address that isn't the local host.
                    if (addr.getAddress().length == 4 //
                            && !Arrays.equals(LocalHost, addr.getAddress())) {
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
