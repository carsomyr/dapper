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

import org.dapper.server.ServerProcessor.FlowProxy;
import org.dapper.server.flow.Flow;
import org.shared.event.Source;
import org.shared.event.XmlEvent;
import org.w3c.dom.Node;

/**
 * Defines a Dapper control message.
 * 
 * @apiviz.owns org.dapper.event.ControlEvent.ControlEventType
 * @author Roy Liu
 */
public interface ControlEvent extends XmlEvent<ControlEvent, ControlEvent.ControlEventType, SourceType> {

    /**
     * An enumeration of {@link ControlEvent} types.
     */
    public enum ControlEventType {

        /**
         * Indicates the first message from the server.
         */
        INIT {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new BaseControlEvent(this, source);
            }
        }, //

        /**
         * Indicates a message from the client conveying an externally facing local IP address.
         */
        ADDRESS {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new AddressEvent(contentNode, source);
            }
        }, //

        /**
         * Indicates a message from the server conveying a resource descriptor.
         */
        RESOURCE {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new ResourceEvent(contentNode, source);
            }
        }, //

        /**
         * Indicates a message from the client on receipt of a resource descriptor.
         */
        RESOURCE_ACK {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new BaseControlEvent(this, source);
            }
        }, //

        /**
         * Indicates a message from the server instructing the client to begin requisitioning resources for execution.
         */
        PREPARE {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new BaseControlEvent(this, source);
            }
        }, //

        /**
         * Indicates a message from the client on load of all necessary resources.
         */
        PREPARE_ACK {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new BaseControlEvent(this, source);
            }
        }, //

        /**
         * Indicates a message from the server instructing the client to begin executing.
         */
        EXECUTE {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new BaseControlEvent(this, source);
            }
        }, //

        /**
         * Indicates a message from the client on successful execution.
         */
        EXECUTE_ACK {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new ExecuteAckEvent(contentNode, source);
            }
        }, //

        /**
         * Indicates a message from the client requesting data or a message from the server conveying data.
         */
        DATA {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new DataEvent(contentNode, source);
            }
        }, //

        /**
         * Indicates a message from the server/client resetting both ends to a common, inactive state.
         */
        RESET {

            @Override
            protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
                return new ResetEvent(contentNode, source);
            }
        }, //

        /**
         * Indicates a request to refresh the computation state and see if any work can be done.
         */
        REFRESH, //

        /**
         * Indicates a connection end-of-stream notification.
         */
        END_OF_STREAM, //

        /**
         * Indicates a connection error notification.
         */
        ERROR, //

        /**
         * Indicates a stream readiness notification.
         */
        STREAM_READY, //

        /**
         * Indicates a timeout notification.
         */
        TIMEOUT, //

        /**
         * Indicates a request to create a new {@link Flow}.
         */
        CREATE_FLOW, //

        /**
         * Indicates a request to purge an active {@link Flow}.
         */
        PURGE_FLOW, //

        /**
         * Indicates a request to set the idle client autoclose option.
         */
        SET_AUTOCLOSE_IDLE, //

        /**
         * Indicates a request to get the {@link FlowProxy} associated with an individual {@link Flow} or all
         * {@link FlowProxy}s associated with all {@link Flow}s.
         */
        GET_FLOW_PROXY, //

        /**
         * Indicates a request to get the number of additional clients required to saturate pending computations.
         */
        GET_PENDING_COUNT, //

        /**
         * Indicates a request to get the number of additional clients required to saturate pending computations on the
         * given {@link Flow}.
         */
        GET_FLOW_PENDING_COUNT, //

        /**
         * Indicates a request to create a new user-facing {@link FlowEvent} queue.
         */
        CREATE_USER_QUEUE, //

        /**
         * Indicates a request to suspend server activities.
         */
        SUSPEND, //

        /**
         * Indicates a request to resume server activities.
         */
        RESUME;

        /**
         * Parses a {@link ControlEvent} from the given DOM {@link Node}.
         */
        protected ControlEvent parse(Node contentNode, Source<ControlEvent, SourceType> source) {
            throw new UnsupportedOperationException("Parse method not defined");
        }
    }
}
