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

        public void handle(ControlEvent evt) {
            // Do nothing.
        }
    };

    /**
     * Default constructor.
     */
    public TimeoutEvent(final Handle<Object> handle, final Object tag, final Source<ControlEvent, SourceType> source) {
        super(TIMEOUT, new Source<ControlEvent, SourceType>() {

            public SourceType getType() {
                return source.getType();
            }

            public void onLocal(ControlEvent evt) {
                source.onLocal(evt);
            }

            public void onRemote(ControlEvent evt) {
                source.onRemote(evt);
            }

            public Handler<ControlEvent> getHandler() {
                return (handle.get() == tag) ? source.getHandler() : EmptyHandler;
            }

            public void setHandler(Handler<ControlEvent> handler) {
                source.setHandler(handler);
            }

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
