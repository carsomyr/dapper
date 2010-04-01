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
            this.processor.onLocal(new ControlEvent(SUSPEND, this.processor));
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
            this.processor.onLocal(new ControlEvent(RESUME, this.processor));
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

            public FlowEvent<F, N> peek() {

                synchronized (feb) {
                    return backing.peek();
                }
            }

            public FlowEvent<F, N> element() {

                synchronized (feb) {
                    return backing.element();
                }
            }

            public FlowEvent<F, N> poll() {

                synchronized (feb) {

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.poll();
                }
            }

            public FlowEvent<F, N> poll(long timeout, TimeUnit unit) throws InterruptedException {

                synchronized (feb) {

                    for (long remaining, end = System.currentTimeMillis() + unit.toMillis(timeout); backing.size() == 0 //
                            && (remaining = end - System.currentTimeMillis()) > 0 //
                            && !feb.closed;) {
                        feb.wait(remaining);
                    }

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.poll();
                }
            }

            public FlowEvent<F, N> remove() {

                synchronized (feb) {

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.remove();
                }
            }

            public FlowEvent<F, N> take() throws InterruptedException {

                synchronized (feb) {

                    for (; backing.size() == 0 && !feb.closed;) {
                        feb.wait();
                    }

                    decrEventCount(Math.min(backing.size(), 1));

                    return backing.poll();
                }
            }

            public void clear() {

                synchronized (feb) {

                    decrEventCount(backing.size());

                    backing.clear();
                }
            }

            public int size() {

                synchronized (feb) {
                    return backing.size();
                }
            }

            public boolean isEmpty() {

                synchronized (feb) {
                    return backing.isEmpty();
                }
            }

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

            public boolean add(FlowEvent<F, N> evt) {
                throw new UnsupportedOperationException();
            }

            public boolean addAll(Collection<? extends FlowEvent<F, N>> c) {
                throw new UnsupportedOperationException();
            }

            public boolean contains(Object o) {
                throw new UnsupportedOperationException();
            }

            public boolean containsAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            public int drainTo(Collection<? super FlowEvent<F, N>> c) {
                throw new UnsupportedOperationException();
            }

            public int drainTo(Collection<? super FlowEvent<F, N>> c, int maxElements) {
                throw new UnsupportedOperationException();
            }

            public Iterator<FlowEvent<F, N>> iterator() {
                throw new UnsupportedOperationException();
            }

            public boolean offer(FlowEvent<F, N> evt) {
                throw new UnsupportedOperationException();
            }

            public boolean offer(FlowEvent<F, N> evt, long timeout, TimeUnit unit) {
                throw new UnsupportedOperationException();
            }

            public void put(FlowEvent<F, N> evt) throws InterruptedException {
                throw new UnsupportedOperationException();
            }

            public boolean remove(Object o) {
                throw new UnsupportedOperationException();
            }

            public boolean removeAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            public boolean retainAll(Collection<?> c) {
                throw new UnsupportedOperationException();
            }

            public Object[] toArray() {
                throw new UnsupportedOperationException();
            }

            public <T> T[] toArray(T[] a) {
                throw new UnsupportedOperationException();
            }
        };

        this.rr.wrap(ReferenceType.WEAK, (Queue<FlowEvent<?, ?>>) ((Queue<?>) queue), new Runnable() {

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

    public boolean add(FlowEvent<?, ?> evt) {

        synchronized (this) {

            incrEventCount(this.queues.size());

            for (Queue<FlowEvent<?, ?>> queue : this.queues) {
                queue.add(evt);
            }

            notifyAll();
        }

        return true;
    }

    public boolean offer(FlowEvent<?, ?> evt) {
        return add(evt);
    }

    public boolean offer(FlowEvent<?, ?> evt, long timeout, TimeUnit unit) {
        return add(evt);
    }

    public void put(FlowEvent<?, ?> evt) {
        add(evt);
    }

    public int size() {

        synchronized (this) {
            return this.eventCount;
        }
    }

    public boolean isEmpty() {

        synchronized (this) {
            return (this.eventCount == 0);
        }
    }

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

    public boolean addAll(Collection<? extends FlowEvent<?, ?>> c) {
        throw new UnsupportedOperationException();
    }

    public void clear() {
        throw new UnsupportedOperationException();
    }

    public boolean contains(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean containsAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public int drainTo(Collection<? super FlowEvent<?, ?>> c) {
        throw new UnsupportedOperationException();
    }

    public int drainTo(Collection<? super FlowEvent<?, ?>> c, int maxElements) {
        throw new UnsupportedOperationException();
    }

    public FlowEvent<?, ?> element() {
        throw new UnsupportedOperationException();
    }

    public Iterator<FlowEvent<?, ?>> iterator() {
        throw new UnsupportedOperationException();
    }

    public FlowEvent<?, ?> peek() {
        throw new UnsupportedOperationException();
    }

    public FlowEvent<?, ?> poll() {
        throw new UnsupportedOperationException();
    }

    public FlowEvent<?, ?> poll(long timeout, TimeUnit unit) {
        throw new UnsupportedOperationException();
    }

    public FlowEvent<?, ?> remove() {
        throw new UnsupportedOperationException();
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    public FlowEvent<?, ?> take() {
        throw new UnsupportedOperationException();
    }

    public Object[] toArray() {
        throw new UnsupportedOperationException();
    }

    public <T> T[] toArray(T[] a) {
        throw new UnsupportedOperationException();
    }
}
