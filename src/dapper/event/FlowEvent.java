/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2010 The Regents of the University of California <br />
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

    public FlowEventType getType() {
        return this.type;
    }

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
