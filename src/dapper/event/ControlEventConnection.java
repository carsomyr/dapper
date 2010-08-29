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
    protected void onError() {
        onLocal(new ErrorEvent(getError(), this));
    }

    @Override
    protected ControlEvent parse(Element rootElement) {
        return (rootElement == null) ? new ControlEvent(ControlEventType.END_OF_STREAM, this) //
                : ControlEvent.parse(rootElement, this);
    }
}
