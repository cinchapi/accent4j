/*
 * Copyright (c) 2019 Cinchapi Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cinchapi.common.concurrent;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

/**
 * Similar to a {@link java.util.concurrent.CountDownLatch} but increments
 * instead of decrementing from a set number down to 0.
 * <p>
 * A {@link CountUpLatch} can be used for a thread to wait until other threads
 * have reached a condition that is represented by the latch being
 * {@link #countUp incremented} to a certain number.
 * </p>
 * <p>
 * {@link CountUpLatch} assumes that threads may begin counting up before a
 * listening thread knows for how many counts to {@link #await(long)}.
 * Therefore, if a thread calls {@link #await(long)} with a number that is less
 * than the number of times the latch has been incremented, the method returns
 * immediately.
 * </p>
 * <p>
 * So, the semantics of a {@link CountUpLatch} allow a thread to wait, if
 * necessary, until the latch has been incremented at least n times.
 * </p>
 * 
 * @author Jeff Nelson
 */
public class CountUpLatch {

    /**
     * Synchronization control 
     */
    private final Sync sync = new Sync();

    /**
     * Block, if necessary, until this latch has been incremented at least
     * {@code count} times.
     * 
     * @param count
     * @throws InterruptedException
     */
    public void await(int count) throws InterruptedException {
        sync.acquireSharedInterruptibly(count);
    }

    /**
     * Block, if necessary, until this latch has been incremented at least
     * {@code count} times.
     * 
     * @param count
     * @throws InterruptedException
     * @return boolean
     */
    public boolean await(int count, long timeout, TimeUnit unit)
            throws InterruptedException {
        return sync.tryAcquireSharedNanos(count, unit.toNanos(timeout));
    }

    /**
     * Increment the latch.
     */
    public void countUp() {
        sync.releaseShared(1);
    }

    /**
     * Return the number of times that the latch has been incremented.
     * 
     * @return the current count
     */
    public long getCount() {
        return sync.getCount();
    }

    /**
     * Returns a string identifying this latch, as well as its state.
     * The state, in brackets, includes the String {@code "Count ="}
     * followed by the current count.
     *
     * @return a string identifying this latch, as well as its state
     */
    public String toString() {
        return super.toString() + "[Count = " + getCount() + "]";
    }

    /**
     * Synchronization control for {@link CountUpLatch}.
     *
     * @author Jeff Nelson
     */
    private static final class Sync extends AbstractQueuedSynchronizer {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a new instance.
         */
        private Sync() {
            setState(0);
        }

        @Override
        protected int tryAcquireShared(int seeking) {
            // This is called when a thread awaits for #seeking.
            return (getState() >= seeking) ? 1 : -1;
        }

        @Override
        protected boolean tryReleaseShared(int ignore) {
            // This is called when a thread counts up in which case we must
            // always see if any threads can transition
            for (;;) {
                int c = getState();
                if(compareAndSetState(c, c + 1)) {
                    return true;
                }
            }
        }

        /**
         * Return the current count.
         * @return the count
         */
        int getCount() {
            return getState();
        }
    }

}