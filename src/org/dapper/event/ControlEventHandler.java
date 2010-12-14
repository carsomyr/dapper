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

package org.dapper.event;

import static org.dapper.Constants.DEFAULT_BUFFER_SIZE;
import static org.dapper.Constants.MAX_CONTROL_MESSAGE_SIZE;
import static org.dapper.event.ControlEvent.ControlEventType.END_OF_STREAM;
import static org.dapper.event.SourceType.CONNECTION;

import org.shared.event.SourceLocal;
import org.shared.net.Connection;
import org.shared.net.handler.XmlHandler;
import org.w3c.dom.Element;

/**
 * A subclass of {@link XmlHandler} specialized for handling {@link ControlEvent}s.
 * 
 * @apiviz.has org.dapper.event.ControlEvent - - - event
 * @apiviz.owns org.dapper.event.SourceType
 * @param <C>
 *            the {@link Connection} type.
 * @author Roy Liu
 */
public class ControlEventHandler<C extends Connection> //
        extends XmlHandler<ControlEventHandler<C>, C, ControlEvent, SourceType> {

    final SourceLocal<ControlEvent> delegate;

    /**
     * Default constructor.
     * 
     * @param delegate
     *            the delegate to which events will be forwarded.
     */
    public ControlEventHandler(String name, SourceLocal<ControlEvent> delegate) {
        super(name, CONNECTION, DEFAULT_BUFFER_SIZE, MAX_CONTROL_MESSAGE_SIZE);

        this.delegate = delegate;
    }

    @Override
    public void onLocal(ControlEvent evt) {
        this.delegate.onLocal(evt);
    }

    @Override
    protected ControlEvent parse(Element rootElement) {
        return BaseControlEvent.parse(rootElement, this);
    }

    @Override
    protected ControlEvent createEos() {
        return new BaseControlEvent(END_OF_STREAM, this);
    }

    @Override
    protected ControlEvent createError() {
        return new ErrorEvent(getConnection().getException(), this);
    }
}
