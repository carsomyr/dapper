/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 The Regents of the University of California <br />
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

import static dapper.Constants.BUFFER_SIZE;
import static dapper.Constants.MAXIMUM_CONTROL_MESSAGE_SIZE;
import static dapper.event.SourceType.CONNECTION;

import org.w3c.dom.Element;

import shared.event.SourceLocal;
import shared.net.ConnectionManager;
import shared.net.XMLConnection;
import dapper.event.ControlEvent.ControlEventType;

/**
 * A subclass of {@link XMLConnection} specialized for handling {@link ControlEvent}s.
 * 
 * @apiviz.has dapper.event.ControlEvent - - - event
 * @apiviz.owns dapper.event.SourceType
 * @author Roy Liu
 */
public class ControlEventConnection extends XMLConnection<ControlEventConnection, ControlEvent, SourceType> {

    final SourceLocal<ControlEvent> delegate;

    /**
     * Default constructor.
     * 
     * @param delegate
     *            the delegate to which events will be forwarded.
     * @see XMLConnection#XMLConnection(String, Enum, int, ConnectionManager)
     */
    public ControlEventConnection(String name, ConnectionManager manager, SourceLocal<ControlEvent> delegate) {
        super(name, CONNECTION, BUFFER_SIZE, MAXIMUM_CONTROL_MESSAGE_SIZE, manager);

        this.delegate = delegate;
    }

    public void onLocal(ControlEvent evt) {
        this.delegate.onLocal(evt);
    }

    @Override
    protected void onError(Throwable error) {
        onLocal(new ErrorEvent(error, this));
    }

    @Override
    protected ControlEvent parse(Element rootElement) {
        return (rootElement == null) ? new ControlEvent(ControlEventType.END_OF_STREAM, this) //
                : ControlEvent.parse(rootElement, this);
    }
}
