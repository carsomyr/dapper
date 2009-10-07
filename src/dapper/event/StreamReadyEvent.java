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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.STREAM_READY;
import shared.event.Source;
import shared.net.SynchronousManagedConnection;
import dapper.codelet.Identified;

/**
 * A subclass of {@link ControlEvent} for carrying ready input/output streams.
 * 
 * @author Roy Liu
 */
public class StreamReadyEvent extends ControlEvent implements Identified {

    final String identifier;
    final SynchronousManagedConnection connection;

    /**
     * Default constructor.
     */
    public StreamReadyEvent(String identifier, SynchronousManagedConnection connection,
            Source<ControlEvent, SourceType> source) {
        super(STREAM_READY, source);

        this.identifier = identifier;
        this.connection = connection;
    }

    public String getIdentifier() {
        return this.identifier;
    }

    /**
     * Gets the newly accepted/connected {@link SynchronousManagedConnection}.
     */
    public SynchronousManagedConnection getConnection() {
        return this.connection;
    }
}
