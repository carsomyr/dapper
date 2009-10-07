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

import java.util.EnumSet;

import dapper.event.ExecuteAckEvent;
import dapper.event.ControlEvent.ControlEventType;

/**
 * An enumeration of {@link LogicalNode} states.
 * 
 * @author Roy Liu
 */
public enum LogicalNodeStatus {

    /**
     * Indicates that the node is pending execution.
     */
    PENDING, //

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
    final protected static EnumSet<LogicalNodeStatus> ExecutableSet = EnumSet.of(PENDING, FAILED);

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
    final protected static EnumSet<LogicalNodeStatus> MergeableSet = EnumSet.of(PENDING, FINISHED);

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
