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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for any {@link AbstractExecutorService}.
 *
 * @author Jeff Nelson
 */
public abstract class AbstractExecutorServiceTest {

    protected ExecutorService executor;

    @Before
    public void setUp() {
        executor = $getExecutorService();
    }

    @After
    public void tearDown() {
        executor.shutdownNow();
    }

    /**
     * Return the {@link AbstractExecutorService} to use in these tests.
     * 
     * @return the {@link AbstractExecutorService}
     */
    protected abstract ExecutorService $getExecutorService();

    @Test
    public void testExecuteRunnable() throws Exception {
        AtomicBoolean done = new AtomicBoolean(false);
        Future<?> future = executor.submit(() -> done.set(true));
        Assert.assertNull(future.get());
        Assert.assertNull(future.get(0, TimeUnit.MILLISECONDS));
        Assert.assertTrue(done.get());
        Assert.assertTrue(future.isDone());
        Assert.assertFalse(future.isCancelled());
    }

    @Test
    public void testSubmitCallable() throws Exception {
        String expected = "foo";
        Future<String> future = executor.submit(() -> expected);
        String result = future.get();
        Assert.assertSame(expected, result);
    }

    @Test
    public void testSubmitRunnable() throws Exception {
        Future<?> future = executor.submit(() -> {});
        future.get();
        Assert.assertTrue(future.isDone());
    }

    @Test
    public void testSubmitRunnableWithFixedResult() throws Exception {
        Object expected = new Object();
        Future<?> future = executor.submit(() -> {}, expected);
        Object result = future.get();
        Assert.assertTrue(future.isDone());
        Assert.assertSame(expected, result);
    }

    @Test(expected = RejectedExecutionException.class)
    public void testExecuteAfterShutdown() {
        executor.shutdown();
        Assert.assertTrue("Executor should be marked as shut down",
                executor.isShutdown());
        executor.execute(() -> System.out.println("This should not run"));
    }

    @Test
    public void testShutdownTwice() {
        executor.shutdown();
        Assert.assertTrue("Executor should be marked as shut down",
                executor.isShutdown());
        executor.shutdown(); // Calling shutdown twice to check for idempotence.
        Assert.assertTrue("Executor should still be marked as shut down",
                executor.isShutdown());
    }

    @Test
    public void testIsTerminated() throws InterruptedException {
        executor.shutdown();
        executor.awaitTermination(50, TimeUnit.MILLISECONDS);
        Assert.assertTrue("Executor should be terminated",
                executor.isTerminated());
    }

    @Test
    public void testIsTerminatedWithoutShutdown() {
        Assert.assertFalse("Executor should not be terminated before shutdown",
                executor.isTerminated());
    }

    @Test
    public void testAwaitTerminationWithoutShutdown()
            throws InterruptedException {
        boolean terminated = executor.awaitTermination(10,
                TimeUnit.MILLISECONDS);
        Assert.assertFalse(
                "Executor service should not terminate since it has not been shut down",
                terminated);
    }

}
