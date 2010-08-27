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

package dapper.server.flow;

import java.util.EnumSet;

import dapper.event.ControlEvent.ControlEventType;
import dapper.event.ExecuteAckEvent;

/**
 * An enumeration of {@link LogicalNode} states.
 * 
 * @author Roy Liu
 */
public enum LogicalNodeStatus {

    /**
     * Indicates that the node is pending execution on unmet dependencies.
     */
    PENDING_DEPENDENCY, //

    /**
     * Indicates that the node is pending execution.
     */
    PENDING_EXECUTE, //

    /**
     * Indicates that the node is awaiting a {@link ControlEventType#RESOURCE_ACK} from clients.
     */
    RESOURCE, //

    /**
     * Indicates that the node is awaiting a {@link ControlEventType#PREPARE_ACK} from clients.
     */
    PREPARE, //

    /**
     * Indicates that the node is awaiting an {@link ExecuteAckEvent} from clients.
     */
    EXECUTE, //

    /**
     * Indicates that the node has completed successfully.
     */
    FINISHED, //

    /**
     * Indicates that the node has failed to complete successfully.
     */
    FAILED;

    /**
     * The {@link EnumSet} of executable states.
     */
    final protected static EnumSet<LogicalNodeStatus> ExecutableSet = EnumSet.of(PENDING_DEPENDENCY, PENDING_EXECUTE);

    /**
     * The {@link EnumSet} of executing states.
     */
    final protected static EnumSet<LogicalNodeStatus> ExecutingSet = EnumSet.of(RESOURCE, PREPARE, EXECUTE);

    /**
     * The {@link EnumSet} of finished states.
     */
    final protected static EnumSet<LogicalNodeStatus> FinishedSet = EnumSet.of(FINISHED);

    /**
     * The {@link EnumSet} of mergeable states used in bookkeeping for subflow embedding.
     */
    final protected static EnumSet<LogicalNodeStatus> MergeableSet = EnumSet.of(PENDING_DEPENDENCY, PENDING_EXECUTE, //
            FINISHED);

    /**
     * Checks for execution eligibility.
     */
    public boolean isExecutable() {
        return ExecutableSet.contains(this);
    }

    /**
     * Checks if the computation is executing.
     */
    public boolean isExecuting() {
        return ExecutingSet.contains(this);
    }

    /**
     * Checks if the computation is finished.
     */
    public boolean isFinished() {
        return FinishedSet.contains(this);
    }

    /**
     * Checks if the computation can be merged with another for the purposes of subflow embedding.
     */
    public boolean isMergeable() {
        return MergeableSet.contains(this);
    }
}
