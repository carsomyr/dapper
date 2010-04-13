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

import java.util.Collections;
import java.util.Set;

import shared.util.Control;

/**
 * An implementation of {@link CountDown} that leaves the {@link CountDown#reset()} method abstract.
 * 
 * @param <T>
 *            the type to count down on.
 * @author Roy Liu
 */
abstract public class AbstractCountDown<T> implements CountDown<T> {

    final Set<T> countDownSet, unmodifiableView;

    /**
     * Default constructor.
     */
    public AbstractCountDown(Set<T> countDownSet) {

        this.countDownSet = countDownSet;
        this.unmodifiableView = Collections.unmodifiableSet(this.countDownSet);
    }

    /**
     * Resets the count down.
     */
    abstract public void reset();

    /**
     * Counts down on the given value.
     */
    public boolean countDown(T value) {

        if (value != null) {
            Control.checkTrue(this.countDownSet.remove(value), //
                    "Value was not part of countdown");
        }

        return this.countDownSet.isEmpty();
    };

    /**
     * Gets the remaining items.
     */
    public Set<T> getRemaining() {
        return this.unmodifiableView;
    }
}
