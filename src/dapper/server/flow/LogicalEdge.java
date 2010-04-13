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

package dapper.server.flow;

import shared.parallel.Edge;

/**
 * An edge class that represents the relationship between two {@link LogicalNode}s.
 * 
 * @author Roy Liu
 */
public class LogicalEdge implements Edge<LogicalNode>, Cloneable {

    LogicalNode u, v;

    /**
     * Default constructor.
     */
    public LogicalEdge(LogicalNode u, LogicalNode v) {

        this.u = u;
        this.v = v;
    }

    /**
     * Copies this edge.
     */
    @Override
    public LogicalEdge clone() {

        final LogicalEdge res;

        try {

            res = (LogicalEdge) super.clone();

        } catch (CloneNotSupportedException e) {

            throw new RuntimeException(e);
        }

        res.setU(null);
        res.setV(null);

        return res;
    }

    public LogicalNode getU() {
        return this.u;
    }

    public void setU(LogicalNode u) {
        this.u = u;
    }

    public LogicalNode getV() {
        return this.v;
    }

    public void setV(LogicalNode v) {
        this.v = v;
    }

    /**
     * Fulfills the {@link Object#equals(Object)} contract.
     */
    @Override
    public boolean equals(Object o) {

        LogicalEdge e = (LogicalEdge) o;
        return e.u.equals(this.u) && e.v.equals(this.v);
    }

    /**
     * Fulfills the {@link Object#hashCode()} contract.
     */
    @Override
    public int hashCode() {
        return this.u.hashCode() ^ this.v.hashCode();
    }
}
