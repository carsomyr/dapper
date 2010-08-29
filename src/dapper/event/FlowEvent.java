/**
 * <p>
 * Copyright (C) 2010 The Regents of the University of California<br />
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

import shared.event.Event;
import shared.event.Source;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowNode;

/**
 * An event class for notifying subscribers of changes on {@link Flow}s and {@link FlowNode}s.
 * 
 * @param <F>
 *            the {@link Flow} attachment type.
 * @param <N>
 *            the {@link FlowNode} attachment type.
 * @apiviz.owns dapper.event.FlowEvent.FlowEventType
 * @author Roy Liu
 */
public class FlowEvent<F, N> implements Event<FlowEvent<F, N>, FlowEvent.FlowEventType, SourceType> {

    /**
     * A flag indicating interest in no events.
     */
    final public static int F_NONE = 0x00000000;

    /**
     * A flag indicating interest in {@link Flow} events.
     */
    final public static int F_FLOW = 0x00000001;

    /**
     * A flag indicating interest in {@link FlowNode} events.
     */
    final public static int F_FLOW_NODE = 0x00000002;

    /**
     * A flag indicating interest in all events.
     */
    final public static int F_ALL = 0xFFFFFFFF;

    /**
     * An enumeration of {@link FlowEvent} types.
     */
    public enum FlowEventType {

        /**
         * Indicates the start of a {@link Flow}.
         */
        FLOW_BEGIN, //

        /**
         * Indicates the end of a {@link Flow}.
         */
        FLOW_END, //

        /**
         * Indicates an error on a {@link Flow}.
         */
        FLOW_ERROR, //

        /**
         * Indicates the start of a {@link FlowNode} computation.
         */
        FLOW_NODE_BEGIN, //

        /**
         * Indicates the end of a {@link FlowNode} computation.
         */
        FLOW_NODE_END, //

        /**
         * Indicates an error on a {@link FlowNode} computation.
         */
        FLOW_NODE_ERROR;
    }

    final FlowEventType type;
    final F flowAttachment;
    final N flowNodeAttachment;
    final Throwable error;

    /**
     * Default constructor.
     */
    public FlowEvent(FlowEventType type, F flowAttachment, N flowNodeAttachment, Throwable error) {

        this.type = type;
        this.flowAttachment = flowAttachment;
        this.flowNodeAttachment = flowNodeAttachment;
        this.error = error;
    }

    @Override
    public FlowEventType getType() {
        return this.type;
    }

    @Override
    public Source<FlowEvent<F, N>, SourceType> getSource() {
        return null;
    }

    /**
     * Creates a human-readable representation of this event.
     */
    @Override
    public String toString() {
        return String.format("%s[%s, %s, %s, %s]", //
                FlowEvent.class.getSimpleName(), this.type, this.flowAttachment, this.flowNodeAttachment, //
                (this.error != null) ? this.error.getClass().getSimpleName() : null);
    }

    /**
     * Gets the {@link Flow} attachment.
     */
    public F getFlowAttachment() {
        return this.flowAttachment;
    }

    /**
     * Gets the {@link FlowNode} attachment.
     */
    public N getFlowNodeAttachment() {
        return this.flowNodeAttachment;
    }

    /**
     * Gets the error that just occurred.
     */
    public Throwable getError() {
        return this.error;
    }
}
