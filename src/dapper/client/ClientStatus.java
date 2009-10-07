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

import dapper.codelet.Codelet;
import dapper.event.ResourceEvent;

/**
 * An enumeration of {@link Client} states.
 * 
 * @author Roy Liu
 */
public enum ClientStatus {

    /**
     * Indicates that the client is idle.
     */
    IDLE, //

    /**
     * Indicates that the client is trying to connect to the server. Used by the client only.
     */
    CONNECT, //

    /**
     * Indicates that the client is awaiting further instructions from the server.
     */
    WAIT, //

    /**
     * Indicates that the client has received a {@link ResourceEvent} and is awaiting further instructions from the
     * server.
     */
    RESOURCE, //

    /**
     * Indicates an acknowledgement of the server's {@link ResourceEvent}. Used by the server only.
     */
    RESOURCE_ACK, //

    /**
     * Indicates that the client has received an order to begin resource acquisition and is awaiting further
     * instructions from the server.
     */
    PREPARE, //

    /**
     * Indicates an acknowledgement of the server's order to begin resource acquisition. Used by the server only.
     */
    PREPARE_ACK, //

    /**
     * Indicates that the client is executing a {@link Codelet}.
     */
    EXECUTE, //

    /**
     * Indicates that the client is preparing for shutdown.
     */
    SHUTDOWN, //

    /**
     * Indicates an invalid state. All events from this connection will be ignored. Used by the server only.
     */
    INVALID;
}
