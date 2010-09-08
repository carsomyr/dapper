/**
 * <p>
 * Copyright (c) 2008 The Regents of the University of California<br>
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

    @Override
    public boolean countDown(T value) {

        if (value != null) {
            Control.checkTrue(this.countDownSet.remove(value), //
                    "Value was not part of countdown");
        }

        return this.countDownSet.isEmpty();
    };

    @Override
    public Set<T> getRemaining() {
        return this.unmodifiableView;
    }
}
