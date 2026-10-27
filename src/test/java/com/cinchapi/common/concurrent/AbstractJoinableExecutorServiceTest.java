/*
 * Copyright (c) 2013-2024 Cinchapi Inc.
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

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the parts of the {@link JoinableExecutorService} contract that
 * every implementation must honor.
 *
 * @author Jeff Nelson
 */
public abstract class AbstractJoinableExecutorServiceTest extends
        AbstractExecutorServiceTest {

    /**
     * <strong>Goal:</strong> Verify that a join of indices runs each index
     * exactly once.
     * <p>
     * <strong>Start state:</strong> A new executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 10,000 indices, each of which counts how many times it
     * runs.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each index ran exactly once.
     */
    @Test
    public void testJoinIndicesRunsEachIndexOnce() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        AtomicIntegerArray runs = new AtomicIntegerArray(10000);
        joinable.join(runs.length(), runs::incrementAndGet);
        for (int i = 0; i < runs.length(); ++i) {
            Assert.assertEquals(1, runs.get(i));
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a join of indices without an error
     * handler throws the error of a failed run only after every other index
     * runs.
     * <p>
     * <strong>Start state:</strong> A new executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 1,000 indices, of which index 0 throws.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The join throws that error, and every other
     * index ran.
     */
    @Test
    public void testJoinIndicesThrowsAfterEveryIndexRuns() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        AtomicInteger runs = new AtomicInteger();
        IllegalStateException failure = new IllegalStateException();
        try {
            joinable.join(1000, index -> {
                if(index == 0) {
                    throw failure;
                }
                else {
                    runs.incrementAndGet();
                }
            });
            Assert.fail("Expected the join to throw");
        }
        catch (IllegalStateException e) {
            Assert.assertSame(failure, e);
        }
        Assert.assertEquals(999, runs.get());
    }

    /**
     * <strong>Goal:</strong> Verify that a join of indices without an error
     * handler throws an {@link Error} from a failed run as it is, without a
     * wrapper.
     * <p>
     * <strong>Start state:</strong> A new executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 10 indices, of which index 5 throws an {@link Error}.</li>
     * <li>Catch whatever the join throws.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The join throws the same {@link Error} that
     * index 5 threw.
     */
    @Test
    public void testJoinIndicesThrowsAnErrorUnwrapped() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        Error failure = new Error();
        Throwable thrown = null;
        try {
            joinable.join(10, index -> {
                if(index == 5) {
                    throw failure;
                }
            });
        }
        catch (Throwable e) {
            thrown = e;
        }
        Assert.assertSame(failure, thrown);
    }

    /**
     * <strong>Goal:</strong> Verify that a join of zero indices runs nothing.
     * <p>
     * <strong>Start state:</strong> A new executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join zero indices with an action that counts its runs.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The action never runs.
     */
    @Test
    public void testJoinOfZeroIndicesRunsNothing() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        AtomicInteger runs = new AtomicInteger();
        joinable.join(0, index -> runs.incrementAndGet());
        Assert.assertEquals(0, runs.get());
    }

}
