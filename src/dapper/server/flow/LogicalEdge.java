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

    @Override
    public LogicalNode getU() {
        return this.u;
    }

    @Override
    public void setU(LogicalNode u) {
        this.u = u;
    }

    @Override
    public LogicalNode getV() {
        return this.v;
    }

    @Override
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
