/**
 * <p>
 * Copyright (c) 2010 The Regents of the University of California<br>
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

import static dapper.event.ControlEvent.ControlEventType.RESUME;
import static dapper.event.ControlEvent.ControlEventType.SUSPEND;

import java.io.Closeable;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import shared.event.Source;
import shared.util.ReferenceReaper;
import shared.util.ReferenceReaper.ReferenceType;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowNode;

/**
 * A mechanism for broadcasting {@link FlowEvent}s from the Dapper server to multiple subscribers in a thread-safe way.
 * 
 * @apiviz.has dapper.event.FlowEvent - - - event
 * @author Roy Liu
 */
public class FlowEventBroadcaster implements BlockingQueue<FlowEvent<?, ?>>, Closeable {

    final int backlog;
    final Source<ControlEvent, SourceType> processor;
    final ReferenceReaper<Queue<FlowEvent<?, ?>>> rr;
    final Set<Queue<FlowEvent<?, ?>>> queues;

    int eventCount;

    boolean closed;

    /**
     * Default constructor.
     */
    public FlowEventBroadcaster(int threshold, Source<ControlEvent, SourceType> processor) {

        this.backlog = threshold;
        this.processor = processor;

        this.rr = new ReferenceReaper<Queue<FlowEvent<?, ?>>>();
        // Use an IdentityHashMap to prevent comparison of contents based on list equality.
        this.queues = Collections.newSetFromMap(new IdentityHashMap<Queue<FlowEvent<?, ?>>, Boolean>());

        this.eventCount = 0;

        this.closed = false;
    }

    /**
     * Causes all waiting threads in {@link BlockingQueue#take()} to immediately wake up and not block from thereon
     * after.
     */
    @Override
    public void close() {

        synchronized (this) {

            this.closed = true;

            notifyAll();
        }
    }

    /**
     * Increments the event count.
     */
    public void incrEventCount(int amount) {

        int queueCount = this.queues.size();

        this.eventCount += amount;

        if (this.eventCount > queueCount * this.backlog //
                && this.eventCount - amount <= queueCount * this.backlog) {
            this.processor.onLocal(new BaseControlEvent(SUSPEND, this.processor));
        }
    }

    /**
     * Decrements the event count.
     */
    public void decrEventCount(int amount) {

        int queueCount = this.queues.size();

        this.eventCount -= amount;

        if (this.eventCount + amount > queueCount * this.backlog //
                && this.eventCount <= queueCount * this.backlog) {
            this.processor.onLocal(new BaseControlEvent(RESUME, this.processor));
        }
    }

    /**
     * Creates a user-facing, read-only {@link Queue}.
     * 
     * @param <F>
     *            the {@link Flow} attachment type.
     * @param <N>
     *            the {@link FlowNode} attachment type.
     */
    @SuppressWarnings("unchecked")
    public <F, N> BlockingQueue<FlowEvent<F, N>> createUserQueue() {

        final FlowEventBroadcaster feb = this;
        final Queue<FlowEvent<F, N>> backing = new LinkedList<FlowEvent<F, N>>();

        BlockingQueue<FlowEvent<F, N>> queue = new BlockingQueue<FlowEvent<F, N>>() {

            @Override
            public FlowEvent<F, N> peek() {

                synchronized (feb) {
                    return backing.peek();
                }
            }

            @Override
            public FlowEvent<F, N> element() {

                synchronized (feb) {
                    return backing.element();
                }
            }

            @Override
            public FlowEvent<F, N> poll() {

                synchronized (feb) {

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.poll();
                }
            }

            @Override
            public FlowEvent<F, N> poll(long timeout, TimeUnit unit) throws InterruptedException {

                long timeoutMillis = unit.toMillis(timeout);

                synchronized (feb) {

                    for (long remaining = timeoutMillis, end = System.currentTimeMillis() + timeoutMillis; //
                    remaining > 0 && backing.isEmpty() && !feb.closed; //
                    remaining = end - System.currentTimeMillis()) {
                        feb.wait(remaining);
                    }

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.poll();
                }
            }

            @Override
            public FlowEvent<F, N> remove() {

                synchronized (feb) {

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.remove();
                }
            }

            @Override
            public FlowEvent<F, N> take() throws InterruptedException {

                synchronized (feb) {

                    for (; backing.isEmpty() && !feb.closed;) {
                        feb.wait();
                    }

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.poll();
                }
            }

            @Override
            public void clear() {

                synchronized (feb) {

                    decrEventCount(backing.size());

                    backing.clear();
                }
            }

            @Override
            public int size() {

                synchronized (feb) {
                    return backing.size();
                }
            }

            @Override
            public boolean isEmpty() {

                synchronized (feb) {
                    return backing.isEmpty();
                }
            }

            @Override
            public int remainingCapacity() {
                return Integer.MAX_VALUE;
            }

            @Override
            public String toString() {

                synchronized (feb) {
                    return backing.toString();
                }
            }

            //

            @Override
            public boolean add(FlowEvent<F, N> evt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean addAll(Collection<? extends FlowEvent<F, N>> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean contains(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int drainTo(Collection<? super FlowEvent<F, N>> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public int drainTo(Collection<? super FlowEvent<F, N>> c, int maxElements) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<FlowEvent<F, N>> iterator() {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean offer(FlowEvent<F, N> evt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean offer(FlowEvent<F, N> evt, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void put(FlowEvent<F, N> evt) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            @Override
            public <T> T[] toArray(T[] a) {
                throw new UnsupportedOperationException();
            }
        };

        this.rr.wrap(ReferenceType.WEAK, (Queue<FlowEvent<?, ?>>) ((Queue<?>) queue), new Runnable() {

            @Override
            public void run() {

                synchronized (feb) {

                    feb.queues.remove(backing);

                    decrEventCount(backing.size());
                }
            }
        });

        synchronized (this) {
            this.queues.add((Queue<FlowEvent<?, ?>>) ((Queue<?>) backing));
        }

        return queue;
    }

    @Override
    public boolean add(FlowEvent<?, ?> evt) {

        synchronized (this) {

            for (Queue<FlowEvent<?, ?>> queue : this.queues) {
                queue.add(evt);
            }

            incrEventCount(this.queues.size());

            notifyAll();
        }

        return true;
    }

    @Override
    public boolean offer(FlowEvent<?, ?> evt) {
        return add(evt);
    }

    @Override
    public boolean offer(FlowEvent<?, ?> evt, long timeout, TimeUnit unit) {
        return add(evt);
    }

    @Override
    public void put(FlowEvent<?, ?> evt) {
        add(evt);
    }

    @Override
    public int size() {

        synchronized (this) {
            return this.eventCount;
        }
    }

    @Override
    public boolean isEmpty() {

        synchronized (this) {
            return this.eventCount == 0;
        }
    }

    @Override
    public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /**
     * Creates a human-readable representation of this {@link Queue}.
     */
    @Override
    public String toString() {

        synchronized (this) {
            return this.queues.toString();
        }
    }

    //

    @Override
    public boolean addAll(Collection<? extends FlowEvent<?, ?>> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super FlowEvent<?, ?>> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int drainTo(Collection<? super FlowEvent<?, ?>> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowEvent<?, ?> element() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Iterator<FlowEvent<?, ?>> iterator() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowEvent<?, ?> peek() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowEvent<?, ?> poll() {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowEvent<?, ?> poll(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowEvent<?, ?> remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FlowEvent<?, ?> take() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
}
