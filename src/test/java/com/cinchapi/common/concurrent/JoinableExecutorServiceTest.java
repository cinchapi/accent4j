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
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.base.ArrayBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.Uninterruptibles;

/**
 * Unit tests for {@link JoinableExecutorService}.
 *
 * @author Jeff Nelson
 */
public class JoinableExecutorServiceTest extends
        AbstractJoinableExecutorServiceTest {

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

    /**
     * <strong>Goal:</strong> Verify that a join of callables runs each task
     * exactly once and answers a done {@link Future} for each of them, in the
     * order the tasks were given.
     * <p>
     * <strong>Start state:</strong> A new executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 100 callables, each of which counts its own runs and answers
     * its own index.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each task ran once, and the join answers one
     * done {@link Future} per task, holding the indices in order.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testJoinCallablesRunsEachTaskOnceAndAnswersItsFuture()
            throws Exception {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        Callable<Integer>[] tasks = new Callable[100];
        AtomicIntegerArray runs = new AtomicIntegerArray(tasks.length);
        for (int i = 0; i < tasks.length; ++i) {
            int index = i;
            tasks[i] = () -> {
                runs.incrementAndGet(index);
                return index;
            };
        }

        List<Future<Integer>> futures = joinable.join(tasks);

        for (int i = 0; i < runs.length(); ++i) {
            Assert.assertEquals(1, runs.get(i));
        }
        Assert.assertEquals(tasks.length, futures.size());
        for (int i = 0; i < futures.size(); ++i) {
            Assert.assertTrue(futures.get(i).isDone());
            Assert.assertEquals(Integer.valueOf(i), futures.get(i).get());
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
     * <strong>Goal:</strong> Verify that a graceful {@code shutdown} runs the
     * tasks that the queue holds before its workers exit.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Queue three tasks, which no worker is free to run.</li>
     * <li>Call {@code shutdown} and wait for the executor to terminate.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> All three tasks ran.
     */
    @Test
    public void testShutdownRunsTheTasksTheQueueHolds()
            throws InterruptedException {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            AtomicInteger ran = new AtomicInteger();
            for (int i = 0; i < 3; ++i) {
                joinable.execute(ran::incrementAndGet);
            }

            joinable.shutdown();

            Assert.assertTrue(joinable.awaitTermination(10, TimeUnit.SECONDS));
            Assert.assertEquals(3, ran.get());
        }
        finally {
            release.countDown();
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
     * <strong>Expected:</strong> {@code shutdownNow} returns three tasks, and
     * none of them runs.
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
            AtomicInteger ran = new AtomicInteger();
            for (int i = 0; i < 3; ++i) {
                joinable.execute(ran::incrementAndGet);
            }

            List<Runnable> unfinished = joinable.shutdownNow();

            Assert.assertEquals(3, unfinished.size());
            // Read the count once every worker has exited, so that a worker
            // that would run one of these tasks has already run it.
            release.countDown();
            Assert.assertTrue(joinable.awaitTermination(10, TimeUnit.SECONDS));
            Assert.assertEquals(0, ran.get());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code shutdownNow} returns the tasks
     * that the queue holds, and none of them runs, when each worker that the
     * call frees reaches the queue before the call takes the tasks.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task, and whose threads return from an interrupt that
     * another thread sends only once they exit.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Queue three tasks, which no worker is free to run.</li>
     * <li>Call {@code shutdownNow}.</li>
     * <li>Release the workers and wait for the executor to terminate.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code shutdownNow} returns three tasks, and
     * none of them runs.
     *
     * @see <a href="https://github.com/cinchapi/accent4j/issues/63">GH-63</a>
     */
    @Test
    public void testGH63ShutdownNowReturnsTheTasksThatAFreedWorkerReaches()
            throws InterruptedException {
        ThreadFactory factory = runnable -> new Thread(runnable) {

            @Override
            public void interrupt() {
                super.interrupt();
                // A worker interrupts itself to restore its flag, and must not
                // wait for its own exit
                if(Thread.currentThread() != this) {
                    Uninterruptibles.joinUninterruptibly(this, 10,
                            TimeUnit.SECONDS);
                }
            }

        };
        JoinableExecutorService joinable = JoinableExecutorService.create(4,
                factory);
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            AtomicInteger ran = new AtomicInteger();
            for (int i = 0; i < 3; ++i) {
                joinable.execute(ran::incrementAndGet);
            }

            List<Runnable> unfinished = joinable.shutdownNow();

            Assert.assertEquals(3, unfinished.size());
            release.countDown();
            Assert.assertTrue(joinable.awaitTermination(10, TimeUnit.SECONDS));
            Assert.assertEquals(0, ran.get());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code shutdownNow} omits the helper
     * that a join queues for a worker that counts as idle but is busy in
     * another join.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>On another thread, join five indices that each block until the end
     * of the test, and wait until that thread runs index 0.</li>
     * <li>Release the workers, and wait until they run indices 1 to 4 of that
     * join, which leaves each of them counted as idle.</li>
     * <li>Join two indices, which queues one helper that no worker can
     * take.</li>
     * <li>Call {@code shutdownNow}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code shutdownNow} returns no tasks.
     */
    @Test
    public void testShutdownNowOmitsTheWorkOfAJoin()
            throws InterruptedException {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        CountDownLatch first = new CountDownLatch(1);
        CountDownLatch held = new CountDownLatch(5);
        CountDownLatch hold = new CountDownLatch(1);
        Thread other = new Thread(() -> joinable.join(5, index -> {
            first.countDown();
            held.countDown();
            // shutdownNow interrupts the workers before it drains the queue, so
            // a worker that woke up could take the helper first
            Uninterruptibles.awaitUninterruptibly(hold);
        }));
        other.start();
        try {
            first.await();
            release.countDown();
            held.await();
            joinable.join(2, index -> {});
            Assert.assertEquals(0, joinable.shutdownNow().size());
        }
        finally {
            release.countDown();
            hold.countDown();
            other.join();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a join of indices leaves every index
     * to the calling thread when every worker is busy.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 1,000 indices, each of which records the thread that runs
     * it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The calling thread ran every index.
     */
    @Test
    public void testJoinIndicesRunsOnCallingThreadWhenWorkersAreBusy()
            throws InterruptedException {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            Thread caller = Thread.currentThread();
            AtomicInteger elsewhere = new AtomicInteger();
            joinable.join(1000, index -> {
                if(Thread.currentThread() != caller) {
                    elsewhere.incrementAndGet();
                }
            });
            Assert.assertEquals(0, elsewhere.get());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a worker that was idle when a join
     * began helps run its indices.
     * <p>
     * <strong>Start state:</strong> A new executor with four workers, whose
     * threads the test records as the executor creates them.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Wait until each worker is in the {@code WAITING} state.</li>
     * <li>Join two indices, each of which waits until both of them are
     * running.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both indices run at the same time, so the join
     * returns without either wait timing out.
     */
    @Test
    public void testJoinIndicesUsesIdleWorkers() {
        List<Thread> workers = new CopyOnWriteArrayList<>();
        JoinableExecutorService joinable = JoinableExecutorService.create(4,
                task -> {
                    Thread worker = new Thread(task);
                    worker.setDaemon(true);
                    workers.add(worker);
                    return worker;
                });
        // A worker enters the WAITING state only inside take, after it has
        // looked for joins, so only a queued helper can bring it to this join
        for (Thread worker : workers) {
            while (worker.getState() != Thread.State.WAITING) {
                Thread.yield();
            }
        }
        CyclicBarrier together = new CyclicBarrier(2);
        AtomicInteger timeouts = new AtomicInteger();
        joinable.join(2, index -> {
            try {
                together.await(5, TimeUnit.SECONDS);
            }
            catch (Exception e) {
                timeouts.incrementAndGet();
            }
        });
        Assert.assertEquals(0, timeouts.get());
        joinable.shutdownNow();
    }

    /**
     * <strong>Goal:</strong> Verify that a worker that frees up after a join of
     * indices begins helps run its indices.
     * <p>
     * <strong>Start state:</strong> An executor whose four workers are each
     * blocked on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join two indices. Each index releases the blocked workers and then
     * waits until both indices are running.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> A freed worker runs the second index while the
     * calling thread waits in the first, so the join returns without either
     * wait timing out.
     */
    @Test
    public void testJoinIndicesUsesWorkersThatFreeUpDuringTheJoin()
            throws InterruptedException {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        CountDownLatch release = blockWorkers(joinable, 4);
        try {
            CyclicBarrier together = new CyclicBarrier(2);
            AtomicInteger timeouts = new AtomicInteger();
            joinable.join(2, index -> {
                release.countDown();
                try {
                    together.await(5, TimeUnit.SECONDS);
                }
                catch (Exception e) {
                    timeouts.incrementAndGet();
                }
            });
            Assert.assertEquals(0, timeouts.get());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a join of indices reports each run
     * that throws to the error handler, with its index, on the calling thread
     * and after every other index runs.
     * <p>
     * <strong>Start state:</strong> An executor with four idle workers.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join 1,000 indices, of which indices 3 and 700 throw.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The error handler receives indices 3 and 700
     * in that order, on the calling thread, and every other index ran.
     */
    @Test
    public void testJoinIndicesReportsEachErrorWithItsIndex() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        Thread caller = Thread.currentThread();
        AtomicIntegerArray runs = new AtomicIntegerArray(1000);
        List<Integer> failed = new ArrayList<>();
        AtomicInteger elsewhere = new AtomicInteger();
        joinable.join((index, error) -> {
            if(Thread.currentThread() != caller) {
                elsewhere.incrementAndGet();
            }
            failed.add(index);
        }, runs.length(), index -> {
            if(index == 3 || index == 700) {
                throw new IllegalStateException();
            }
            else {
                runs.incrementAndGet(index);
            }
        });
        Assert.assertEquals(ImmutableList.of(3, 700), failed);
        Assert.assertEquals(0, elsewhere.get());
        for (int i = 0; i < runs.length(); ++i) {
            Assert.assertEquals(i == 3 || i == 700 ? 0 : 1, runs.get(i));
        }
    }

    /**
     * <strong>Goal:</strong> Verify that an interrupted join of indices returns
     * at once with the interrupt status set, while a worker still runs one of
     * its indices.
     * <p>
     * <strong>Start state:</strong> An executor with four idle workers.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join two indices. On a worker, an index blocks until the end of the
     * test. On the calling thread, an index waits until both indices run and
     * then interrupts the calling thread.</li>
     * <li>Check and clear the interrupt status.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The join returns before the timeout, and the
     * interrupt status is set.
     */
    @Test(timeout = 5000)
    public void testJoinIndicesReturnsAtOnceWhenInterrupted() {
        JoinableExecutorService joinable = (JoinableExecutorService) executor;
        Thread caller = Thread.currentThread();
        CountDownLatch started = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        try {
            joinable.join(2, index -> {
                started.countDown();
                if(Thread.currentThread() == caller) {
                    Uninterruptibles.awaitUninterruptibly(started);
                    caller.interrupt();
                }
                else {
                    Uninterruptibles.awaitUninterruptibly(release);
                }
            });
            Assert.assertTrue(Thread.interrupted());
        }
        finally {
            release.countDown();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a join of {@link Integer#MAX_VALUE}
     * indices never passes the action a negative index when a worker helps.
     * <p>
     * <strong>Start state:</strong> A new executor whose one worker is blocked
     * on a task.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join {@link Integer#MAX_VALUE} indices. When the calling thread
     * reaches the last 100 million, it releases the worker, which then helps
     * with the indices that remain.</li>
     * <li>Submit a task and wait for it, so the worker has left the join.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The action never receives a negative index.
     */
    @Test(timeout = 300000)
    public void testJoinIndicesNeverPassesANegativeIndexWhenCountIsMaxValue()
            throws Exception {
        JoinableExecutorService joinable = JoinableExecutorService.create(1);
        try {
            CountDownLatch release = blockWorkers(joinable, 1);
            int handoff = Integer.MAX_VALUE - 100_000_000;
            AtomicBoolean negative = new AtomicBoolean();
            joinable.join(Integer.MAX_VALUE, index -> {
                if(index == handoff) {
                    release.countDown();
                }
                else if(index < 0) {
                    negative.set(true);
                }
            });
            joinable.submit(() -> {}).get();
            Assert.assertFalse(negative.get());
        }
        finally {
            joinable.shutdownNow();
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
