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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.base.ArrayBuilder;

/**
 * Unit tests for {@link JoinableExecutorService}.
 *
 * @author Jeff Nelson
 */
public class JoinableExecutorServiceTest extends AbstractExecutorServiceTest {

    @Test
    public void testBasicTaskExecution() throws Exception {
        Callable<Integer> task = () -> 123;
        Future<Integer> future = executor.submit(task);
        Assert.assertEquals(Integer.valueOf(123), future.get());
    }

    @Test
    public void testShutdownBehavior() {
        executor.shutdown();
        Assert.assertTrue(executor.isShutdown());
        try {
            executor.submit(() -> {});
            Assert.fail("Should throw exception after shutdown");
        }
        catch (RejectedExecutionException e) {
            // Expected behavior
        }
    }

    @Test
    public void testIsShutdownAndIsTerminated() throws InterruptedException {
        executor.shutdown();
        Assert.assertTrue(executor.isShutdown());

        // Let any existing tasks finish
        while (!executor.isTerminated()) {
            executor.awaitTermination(100, TimeUnit.MILLISECONDS);
        }
        Assert.assertTrue(executor.isTerminated());
    }

    @Test
    public void testExecuteMultipleTasks() throws InterruptedException {
        JoinableExecutorService executor = (JoinableExecutorService) this.executor;
        ArrayBuilder<Runnable> commands = ArrayBuilder.builder();
        AtomicInteger counter = new AtomicInteger(0);
        for (int i = 0; i < 10; i++) {
            commands.add(() -> counter.incrementAndGet());
        }
        executor.join(commands.build());
        Assert.assertEquals(10, counter.get());
    }

    @Test
    public void testJoinRunnables() throws Exception {
        JoinableExecutorService executor = (JoinableExecutorService) this.executor;
        Runnable[] tasks = new Runnable[100];
        Map<Integer, Thread> map = new ConcurrentHashMap<>();
        Map<Integer, AtomicInteger> count = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; ++i) {
            int j = i;
            tasks[i] = () -> {
                map.put(j, Thread.currentThread());
                count.computeIfAbsent(j, $ -> new AtomicInteger(0))
                        .incrementAndGet();
            };
        }
        executor.join(tasks);
        Set<Thread> workers = map.values().stream().collect(Collectors.toSet());
        if(workers.contains(Thread.currentThread()) && workers.size() > 1) {
            // Ensure that no task was dropped
            Assert.assertEquals(100, map.size());
            Assert.assertEquals(100, count.size());

            // Ensure that no task was dropped
            for (Entry<Integer, AtomicInteger> entry : count.entrySet()) {
                Assert.assertEquals(1, entry.getValue().get());
            }
        }
        else if(workers.contains(Thread.currentThread())) {
            Assert.fail();
            System.err.println(
                    "The main thread did all of the work without the executor. Possible race condition");
        }
        else {
            Assert.fail();
            System.err.println(
                    "The main thread did not help with any of the work. Possible race condition");
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testJoinCallables() throws Exception {
        JoinableExecutorService executor = (JoinableExecutorService) this.executor;
        Callable<Integer>[] tasks = new Callable[100];
        Map<Integer, Thread> map = new ConcurrentHashMap<>();
        Map<Integer, AtomicInteger> count = new ConcurrentHashMap<>();
        for (int i = 0; i < 100; ++i) {
            int j = i;
            tasks[i] = () -> {
                map.put(j, Thread.currentThread());
                count.computeIfAbsent(j, $ -> new AtomicInteger(0))
                        .incrementAndGet();
                return j;
            };
        }
        List<Future<Integer>> futures = executor.join(tasks);
        Set<Thread> workers = map.values().stream().collect(Collectors.toSet());
        if(workers.contains(Thread.currentThread()) && workers.size() > 1) {
            // Ensure that no task was dropped
            Assert.assertEquals(100, map.size());
            Assert.assertEquals(100, count.size());

            // Ensure that no task was dropped
            for (Entry<Integer, AtomicInteger> entry : count.entrySet()) {
                Assert.assertEquals(1, entry.getValue().get());
            }
            for (Future<Integer> future : futures) {
                Assert.assertTrue(future.isDone());
            }
        }
        else if(workers.contains(Thread.currentThread())) {
            Assert.fail();
            System.err.println(
                    "The main thread did all of the work without the executor. Possible race condition");
        }
        else {
            Assert.fail();
            System.err.println(
                    "The main thread did not help with any of the work. Possible race condition");
        }
    }

    @Test
    public void testExceptionHandling() {
        JoinableExecutorService executor = (JoinableExecutorService) this.executor;
        AtomicInteger counter = new AtomicInteger();

        List<Runnable> tasks = new ArrayList<>();
        tasks.add(() -> {
            throw new IllegalArgumentException("Task 1 exception");
        });
        tasks.add(() -> counter.incrementAndGet());
        executor.join((task, error) -> {
            counter.incrementAndGet();
        }, tasks.toArray(new Runnable[0]));
        Assert.assertEquals(2, counter.get());
    }

    @Test
    public void testGuranteedProgressEvenIfAllWorkerThreadsAreBusy()
            throws InterruptedException {
        int numThreads = 4;
        JoinableExecutorService a = (JoinableExecutorService) this.executor;
        ExecutorService b = Executors.newFixedThreadPool(numThreads);
        AtomicInteger aCount = new AtomicInteger(0);
        AtomicInteger bCount = new AtomicInteger(0);
        Thread aThread = new Thread(() -> {
            for (int i = 0; i < numThreads; ++i) {
                a.submit(() -> {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {}
                });
            }
            Runnable[] tasks = new Runnable[numThreads];
            for (int i = 0; i < numThreads; ++i) {
                tasks[i] = () -> aCount.incrementAndGet();
            }
            a.join(tasks);

        });
        Thread bThread = new Thread(() -> {
            for (int i = 0; i < numThreads; ++i) {
                b.submit(() -> {
                    try {
                        Thread.sleep(5000);
                    }
                    catch (InterruptedException e) {}
                });
            }
            for (int i = 0; i < numThreads; ++i) {
                b.submit(() -> bCount.incrementAndGet());
            }
        });
        aThread.start();
        bThread.start();
        Thread.sleep(100);
        Assert.assertTrue(aCount.get() > 0);
        Assert.assertFalse(bCount.get() > 0);
    }

    /**
     * <strong>Goal:</strong> Verify that the work to join {@link Runnable
     * Runnables} grows with the number of tasks, not with its square, when
     * the calling thread runs them because every worker is busy.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 200,000 tasks that each count once.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The join returns within five seconds, after
     * every task has run.
     */
    @Test(timeout = 5000)
    public void testJoinRunnablesWhileWorkersAreBusyIsLinear()
            throws InterruptedException {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            AtomicInteger count = new AtomicInteger();
            Runnable[] tasks = new Runnable[200000];
            for (int i = 0; i < tasks.length; ++i) {
                tasks[i] = count::incrementAndGet;
            }
            joinable.join(tasks);
            Assert.assertEquals(tasks.length, count.get());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that the work to join {@link Callable
     * Callables} grows with the number of tasks, not with its square, when
     * the calling thread runs them because every worker is busy.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 200,000 tasks that each return their index.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The join returns within five seconds, and
     * each {@link Future} holds the index of its task.
     */
    @SuppressWarnings("unchecked")
    @Test(timeout = 5000)
    public void testJoinCallablesWhileWorkersAreBusyIsLinear()
            throws Exception {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            Callable<Integer>[] tasks = new Callable[200000];
            for (int i = 0; i < tasks.length; ++i) {
                int index = i;
                tasks[i] = () -> index;
            }
            List<Future<Integer>> futures = joinable.join(tasks);
            for (int i = 0; i < tasks.length; ++i) {
                Assert.assertEquals(Integer.valueOf(i), futures.get(i).get());
            }
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a join runs each task exactly once
     * while the workers and the calling thread both run tasks.
     * <p>
     * <strong>Start state:</strong> An executor with four idle workers.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 10,000 tasks that each count how many times they run.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each task ran exactly once.
     */
    @Test
    public void testJoinRunsEachTaskOnce() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        AtomicIntegerArray runs = new AtomicIntegerArray(10000);
        Runnable[] tasks = new Runnable[runs.length()];
        for (int i = 0; i < tasks.length; ++i) {
            int index = i;
            tasks[i] = () -> runs.incrementAndGet(index);
        }
        joinable.join(tasks);
        for (int i = 0; i < runs.length(); ++i) {
            Assert.assertEquals(1, runs.get(i));
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code shutdownNow} returns only the
     * tasks that never ran, even after a join whose tasks the calling thread
     * ran while every worker was busy.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 100 tasks.</li>
     * <li>Submit three more tasks, which no thread runs.</li>
     * <li>Call {@code shutdownNow}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code shutdownNow} returns three tasks.
     */
    @Test
    public void testShutdownNowAfterJoinReturnsOnlyTasksThatNeverRan()
            throws InterruptedException {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            Runnable[] tasks = new Runnable[100];
            for (int i = 0; i < tasks.length; ++i) {
                tasks[i] = () -> {};
            }
            joinable.join(tasks);
            for (int i = 0; i < 3; ++i) {
                joinable.execute(() -> {});
            }
            Assert.assertEquals(3, joinable.shutdownNow().size());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * Occupy {@code workers} worker threads of {@code executor} with tasks
     * that block until the returned latch counts down, and return once each
     * of them has started.
     *
     * @param executor the executor whose workers to block
     * @param workers the number of workers that {@code executor} has
     * @return the latch that releases the blocked workers
     * @throws InterruptedException if interrupted while waiting for the
     *             workers to start
     */
    private static CountDownLatch blockWorkers(ExecutorService executor,
            int workers) throws InterruptedException {
        CountDownLatch started = new CountDownLatch(workers);
        CountDownLatch release = new CountDownLatch(1);
        for (int i = 0; i < workers; ++i) {
            executor.execute(() -> {
                started.countDown();
                try {
                    release.await();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }
        started.await();
        return release;
    }

    @Override
    protected ExecutorService $getExecutorService() {
        return JoinableExecutorService.create(4);
    }

}
