/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2010 Roy Liu, The Regents of the University of California <br />
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

package dapper.util;

import java.util.Collection;
import java.util.Map;

/**
 * Defines an algorithm for matching {@link Requirement}s with satisfiers.
 * 
 * @apiviz.has dapper.util.Requirement - - - argument
 * @author Roy Liu
 */
public interface MatchingAlgorithm {

    /**
     * Matches {@link Requirement}s with satisfiers.
     * 
     * @param <R>
     *            the {@link Requirement} type.
     * @param <S>
     *            the satisfier type.
     * @return a mapping of {@link Requirement}s to satisfiers.
     */
    public <R extends Requirement<S>, S> Map<R, S> match(Collection<R> requirements, Collection<S> satisfiers);
}
