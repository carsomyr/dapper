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

package dapper.event;

import static dapper.event.ControlEvent.ControlEventType.TIMEOUT;
import shared.event.Handler;
import shared.event.Source;
import shared.parallel.Handle;

/**
 * A subclass of {@link ControlEvent} for delivering timeouts.
 * 
 * @author Roy Liu
 */
public class TimeoutEvent extends ControlEvent {

    /**
     * A {@link Handler} that does nothing.
     */
    final protected static Handler<ControlEvent> EmptyHandler = new Handler<ControlEvent>() {

        @Override
        public void handle(ControlEvent evt) {
            // Do nothing.
        }
    };

    /**
     * Default constructor.
     */
    public TimeoutEvent(final Handle<Object> handle, final Object tag, final Source<ControlEvent, SourceType> source) {
        super(TIMEOUT, new Source<ControlEvent, SourceType>() {

            @Override
            public SourceType getType() {
                return source.getType();
            }

            @Override
            public void onLocal(ControlEvent evt) {
                source.onLocal(evt);
            }

            @Override
            public void onRemote(ControlEvent evt) {
                source.onRemote(evt);
            }

            @Override
            public Handler<ControlEvent> getHandler() {
                return (handle.get() == tag) ? source.getHandler() : EmptyHandler;
            }

            @Override
            public void setHandler(Handler<ControlEvent> handler) {
                source.setHandler(handler);
            }

            @Override
            public void close() {
                source.close();
            }

            @Override
            public String toString() {
                return source.toString();
            }
        });
    }
}
