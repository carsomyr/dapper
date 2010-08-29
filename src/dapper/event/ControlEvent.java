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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import shared.event.Source;
import shared.event.XMLEvent;
import shared.util.Control;
import dapper.server.flow.Flow;

/**
 * A base class for all Dapper control messages.
 * 
 * @apiviz.owns dapper.event.ControlEvent.ControlEventType
 * @author Roy Liu
 */
public class ControlEvent extends XMLEvent<ControlEvent, ControlEvent.ControlEventType, SourceType> {

    /**
     * An enumeration of {@link ControlEvent} types.
     */
    public enum ControlEventType {

        /**
         * Indicates an initialization catalyst for the server/client.
         */
        INIT {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ControlEvent(this, source);
            }
        }, //

        /**
         * Indicates the start of a client's relationship with the server.
         */
        ADDRESS {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new AddressEvent(rootNode, source);
            }
        }, //

        /**
         * Indicates an interrupt that causes the server/client to reassess the computation state.
         */
        REFRESH {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                throw new UnsupportedOperationException();
            }
        }, //

        /**
         * Indicates resource descriptions sent from server to client.
         */
        RESOURCE {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ResourceEvent(rootNode, source);
            }
        }, //

        /**
         * Indicates a request for data.
         */
        DATA_REQUEST {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new DataRequestEvent(rootNode, source);
            }
        }, //

        /**
         * Indicates acknowledgement upon client receipt of a {@link ResourceEvent}.
         */
        RESOURCE_ACK {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ControlEvent(this, source);
            }
        }, //

        /**
         * Indicates an instruction from the server to clients to begin requisitioning resources for execution.
         */
        PREPARE {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ControlEvent(this, source);
            }
        }, //

        /**
         * Indicates acknowledgement upon client load of all necessary resources.
         */
        PREPARE_ACK {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ControlEvent(this, source);
            }
        }, //

        /**
         * Indicates an instruction from the server to execute.
         */
        EXECUTE {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ControlEvent(this, source);
            }
        }, //

        /**
         * Indicates an acknowledgement of successful execution.
         */
        EXECUTE_ACK {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ExecuteAckEvent(rootNode, source);
            }
        }, //

        /**
         * Indicates a reset of the server/client to an inactive state.
         */
        RESET {

            @Override
            protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
                return new ResetEvent(rootNode, source);
            }
        }, //

        /**
         * Indicates that an error has occurred.
         */
        ERROR, //

        /**
         * Indicates that an end-of-stream has been reached.
         */
        END_OF_STREAM, //

        /**
         * Indicates that a stream is ready.
         */
        STREAM_READY, //

        /**
         * Indicates a timeout while waiting for a transition.
         */
        TIMEOUT, //

        /**
         * Indicates shutdown of the client process.
         */
        SHUTDOWN, //

        /**
         * Indicates a query for initializing {@link Flow}s.
         */
        QUERY_INIT, //

        /**
         * Indicates a query for refreshing the state of {@link Flow}s in the server.
         */
        QUERY_REFRESH, //

        /**
         * Indicates a query for purging {@link Flow}s from the server.
         */
        QUERY_PURGE, //

        /**
         * Indicates a query for closing all idle clients <i>or</i> setting the idle client auto-close option, depending
         * on the argument.
         */
        QUERY_CLOSE_IDLE, //

        /**
         * Indicates a query for the number of additional clients required to saturate all pending computations.
         */
        QUERY_PENDING_COUNT, //

        /**
         * Indicates a query for the number of additional clients required to saturate all pending computations on
         * individual {@link Flow}s.
         */
        QUERY_FLOW_PENDING_COUNT, //

        /**
         * Indicates a query for creating a new user-facing {@link FlowEvent} queue.
         */
        QUERY_CREATE_USER_QUEUE, //

        /**
         * Indicates a request for the server to suspend operations by ceasing to assign clients to pending
         * computations.
         */
        SUSPEND, //

        /**
         * Indicates a request for the server to resume operations.
         */
        RESUME;

        /**
         * Parses a {@link ControlEvent} from the given root DOM {@link Node}.
         */
        protected ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {
            throw new UnsupportedOperationException("Parse method not defined");
        }
    }

    final Source<ControlEvent, SourceType> source;

    /**
     * Default constructor.
     */
    public ControlEvent(ControlEventType type, Source<ControlEvent, SourceType> source) {
        super(type);

        this.source = source;
    }

    /**
     * Transfers the contents of this event into the given DOM {@link Node}.
     */
    protected void getContents(Node contentNode) {
    }

    @Override
    public Element toDOM() {

        Document doc = Control.createDocument();

        Element rootElement = doc.createElement(XMLEvent.class.getName());

        rootElement.appendChild(doc.createElement("type")) //
                .setTextContent(getType().toString());

        getContents(rootElement.appendChild(doc.createElement("content")));

        return rootElement;
    }

    @Override
    public Source<ControlEvent, SourceType> getSource() {
        return this.source;
    }

    /**
     * Parses a {@link ControlEvent} from the given root DOM {@link Node}.
     */
    public static ControlEvent parse(Node rootNode, Source<ControlEvent, SourceType> source) {

        NodeList children = rootNode.getChildNodes();

        return ControlEventType.valueOf(children.item(0).getTextContent()).parse(children.item(1), source);
    }
}
