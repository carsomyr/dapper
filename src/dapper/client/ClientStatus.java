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
