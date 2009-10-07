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

package dapper.server.flow;

/**
 * Defines a listener for {@link Flow} life cycle events.
 * 
 * @param <F>
 *            the {@link Flow} attachment type.
 * @param <N>
 *            the {@link FlowNode} attachment type.
 * @author Roy Liu
 */
public interface FlowListener<F, N> {

    /**
     * On {@link Flow} execution begin.
     * 
     * @param flowAttachment
     *            the {@link Flow} attachment.
     */
    public void onFlowBegin(F flowAttachment);

    /**
     * On {@link Flow} execution end.
     * 
     * @param flowAttachment
     *            the {@link Flow} attachment.
     */
    public void onFlowEnd(F flowAttachment);

    /**
     * On {@link Flow} error.
     * 
     * @param flowAttachment
     *            the {@link Flow} attachment.
     * @param error
     *            the error that occurred.
     */
    public void onFlowError(F flowAttachment, Throwable error);

    /**
     * On {@link FlowNode} execution begin.
     * 
     * @param flowAttachment
     *            the {@link Flow} attachment.
     * @param flowNodeAttachment
     *            the {@link FlowNode} attachment.
     */
    public void onFlowNodeBegin(F flowAttachment, N flowNodeAttachment);

    /**
     * On {@link FlowNode} execution end.
     * 
     * @param flowAttachment
     *            the {@link Flow} attachment.
     * @param flowNodeAttachment
     *            the {@link FlowNode} attachment.
     */
    public void onFlowNodeEnd(F flowAttachment, N flowNodeAttachment);

    /**
     * On {@link FlowNode} error.
     * 
     * @param flowAttachment
     *            the {@link Flow} attachment.
     * @param flowNodeAttachment
     *            the {@link FlowNode} attachment.
     * @param error
     *            the error that occurred.
     */
    public void onFlowNodeError(F flowAttachment, N flowNodeAttachment, Throwable error);
}
