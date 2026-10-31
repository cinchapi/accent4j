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

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;

import com.google.common.base.Preconditions;

/**
 * A {@link JoinableExecutorService} that uses a pool of worker threads.
 *
 * @author Jeff Nelson
 */
class JoinableThreadPoolExecutor extends AbstractExecutorService implements
        JoinableExecutorService {

    /**
     * Indicates that the executor is in a {@link #state} that allows it to
     * accept new tasks.
     */
    private static final int RUNNING = 1;

    /**
     * Indicates that the executor is in a {@link #state} that prevents the
     * submission of new tasks, but allows for the execution of any existing
     * tasks in service of a graceful shutdown.
     */
    private static final int SHUTDOWN = 2;

    /**
     * Indicates that the executor is in a {@link #state} that no further task
     * execution can happen.
     */
    private static final int TERMINATED = 3;

    /**
     * Indicates that the executor is in a {@link #state} that prevents the
     * submission of new tasks and leaves the tasks that it has not started to
     * the {@link #shutdownNow()} call that asked it to halt, which returns
     * them.
     */
    private static final int STOP = 4;

    /**
     * The state of the executor.
     */
    private AtomicInteger state;

    /**
     * Signals that all worker threads have terminated.
     */
    private CountDownLatch termination;

    /**
     * The queue of tasks that the worker threads pull from.
     */
    private final BlockingQueue<FutureTask<?>> tasks;

    /**
     * The worker threads.
     */
    private final Thread[] workers;

    /**
     * The number of {@link #workers} that are waiting for a task or helping a
     * join.
     */
    private final AtomicInteger idle = new AtomicInteger();

    /**
     * The work of each join that is in progress. A worker runs each of them
     * before it waits for a task, so a worker that frees up during a join takes
     * part in it.
     */
    private final Queue<Runnable> joins = new ConcurrentLinkedQueue<>();

    /**
     * Construct a new instance.
     * 
     * @param numWorkerThreads
     */
    JoinableThreadPoolExecutor(int numWorkerThreads) {
        this(numWorkerThreads, Executors.defaultThreadFactory());
    }

    /**
     * Construct a new instance.
     * 
     * @param numWorkerThreads
     * @param threadFactory
     */
    JoinableThreadPoolExecutor(int numWorkerThreads,
            ThreadFactory threadFactory) {
        this.tasks = new LinkedBlockingQueue<>();
        this.state = new AtomicInteger(RUNNING);
        this.workers = new Thread[numWorkerThreads];
        for (int i = 0; i < numWorkerThreads; ++i) {
            Thread worker = threadFactory.newThread(this::executeTaskLoop);
            workers[i] = worker;
            worker.start();
        }
        this.termination = new CountDownLatch(numWorkerThreads);
    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request
     * or the current thread is interrupted, whichever happens first.
     * 
     * @return {@code true} if the executor is terminated
     * @throws InterruptedException
     */
    public boolean awaitTermination() throws InterruptedException {
        return awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return termination.await(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        if(state.compareAndSet(RUNNING, RUNNING)) {
            FutureTask<Void> task = new FutureTask<>(command, null);
            tasks.offer(task);
            if(state.get() != RUNNING && tasks.remove(task)) {
                // A shutdown that began after the check above may have taken
                // the queue or let every worker exit before the offer, so no
                // thread would ever run the task
                throw new RejectedExecutionException();
            }
        }
        else {
            throw new RejectedExecutionException();
        }
    }

    @Override
    public boolean isShutdown() {
        return state.get() != RUNNING;
    }

    @Override
    public boolean isTerminated() {
        return state.get() == TERMINATED;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> List<Future<V>> join(
            BiConsumer<Callable<V>, Throwable> errorHandler,
            Callable<V>... tasks) {
        return joinAll(tasks, FutureTask::new, errorHandler);
    }

    @Override
    public void join(BiConsumer<Integer, Throwable> errorHandler, int count,
            IntConsumer action) {
        Preconditions.checkNotNull(errorHandler);
        Preconditions.checkNotNull(action);
        Preconditions.checkArgument(count >= 0);
        if(state.compareAndSet(RUNNING, RUNNING)) {
            // Each thread that drains reads one position past the last index,
            // which would wrap an int when count is near Integer.MAX_VALUE
            AtomicLong next = new AtomicLong();
            CountDownLatch done = new CountDownLatch(count);
            Queue<Entry<Integer, Throwable>> errors;
            errors = new ConcurrentLinkedQueue<>();
            Runnable drain = () -> {
                long position;
                while ((position = next.getAndIncrement()) < count) {
                    int index = (int) position;
                    try {
                        action.accept(index);
                    }
                    catch (Throwable error) {
                        errors.add(new SimpleImmutableEntry<>(index, error));
                    }
                    finally {
                        done.countDown();
                    }
                }
            };
            joins.add(drain);
            // A worker that is busy now finds the join in #joins when it frees
            // up, so only the workers that are idle now need a wake-up
            int helpers = Math.min(idle.get(), count - 1);
            for (int i = 0; i < helpers; ++i) {
                tasks.offer(new Helper(drain));
            }
            drain.run();
            try {
                done.await();
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            joins.remove(drain);
            errors.stream()
                    .sorted(Comparator.comparingInt(error -> error.getKey()))
                    .forEach(error -> errorHandler.accept(error.getKey(),
                            error.getValue()));
        }
        else {
            throw new RejectedExecutionException();
        }
    }

    @Override
    public void join(BiConsumer<Runnable, Throwable> errorHandler,
            Runnable... tasks) {
        joinAll(tasks, task -> new FutureTask<Void>(task, null), errorHandler);
    }

    @Override
    public void shutdown() {
        if(state.compareAndSet(RUNNING, SHUTDOWN)) {
            for (int i = 0; i < workers.length; ++i) {
                Thread worker = workers[i];
                worker.interrupt();
            }
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        List<Runnable> unfinished = new ArrayList<>();
        if(state.compareAndSet(RUNNING, STOP)
                || state.compareAndSet(SHUTDOWN, STOP)) {
            for (Thread worker : workers) {
                worker.interrupt();
            }
            tasks.drainTo(unfinished);
            // A helper holds no work of its own: the joining thread runs every
            // index that no helper takes
            unfinished.removeIf(task -> task instanceof Helper);
        }
        return unfinished;
    }

    /**
     * Run tasks on a worker thread until the executor terminates. While the
     * executor runs, the worker runs a queued task if there is one; otherwise
     * it helps each join in progress, then waits for a task. After
     * {@link #shutdown()}, the worker runs the tasks that remain in the queue
     * and exits. The last worker to exit marks the executor terminated.
     */
    private void executeTaskLoop() {
        try {
            for (;;) {
                if(state.compareAndSet(RUNNING, RUNNING)) {
                    FutureTask<?> task;
                    idle.incrementAndGet();
                    try {
                        // Count idle before reading #joins, as a join registers
                        // before it reads #idle, so a join that begins as this
                        // worker goes idle is seen here or by the Helper it
                        // queues. A queued task comes before any join, and the
                        // worker blocks only right after it has read #joins
                        task = tasks.poll();
                        if(task == null) {
                            for (Runnable join : joins) {
                                join.run();
                            }
                            task = tasks.take();
                        }
                    }
                    catch (InterruptedException e) {
                        // Interrupt signals a request to shutdown or
                        // shutdownNow. In either case, re-loop and
                        // check the #state. If an immediate halt is
                        // required, the internal task queue will be
                        // emptied and we don't have to worry here
                        Thread.currentThread().interrupt();
                        continue;
                    }
                    finally {
                        idle.decrementAndGet();
                    }
                    task.run();
                }
                else if(state.compareAndSet(SHUTDOWN, SHUTDOWN)) {
                    FutureTask<?> task = tasks.poll();
                    if(task == null) {
                        // No tasks remain, so break out and terminate this
                        // thread.
                        break;
                    }
                    else {
                        task.run();
                    }
                }
                else if(state.compareAndSet(STOP, STOP)) {
                    // The tasks that the queue still holds belong to the
                    // shutdownNow call, which returns them, so this worker
                    // takes none of them.
                    break;
                }
                else if(state.compareAndSet(TERMINATED, TERMINATED)) {
                    break;
                }
                else {
                    // The state has changed, so re-loop and check again
                    continue;
                }
            }
        }
        finally {
            termination.countDown();
            if(termination.getCount() == 0) {
                // The executor reached one of these states to get here, so at
                // most one of these moves it on.
                state.compareAndSet(SHUTDOWN, TERMINATED);
                state.compareAndSet(STOP, TERMINATED);
            }
        }
    }

    /**
     * Run each of the provided {@code tasks} in a
     * {@link #join(BiConsumer, int, IntConsumer) join} of its
     * {@link FutureTask}, report each task that fails to the
     * {@code errorHandler}, and return the {@link Future Futures} in the order
     * of the {@code tasks}.
     *
     * @param tasks the tasks to run, which must not be empty
     * @param futureOf makes the {@link FutureTask} that runs a task
     * @param errorHandler receives each task that fails, with its error
     * @return the {@link Future} of each task, in the order of {@code tasks}
     */
    private <T, V> List<Future<V>> joinAll(T[] tasks,
            Function<T, FutureTask<V>> futureOf,
            BiConsumer<T, Throwable> errorHandler) {
        Preconditions.checkNotNull(tasks);
        Preconditions.checkArgument(tasks.length > 0);
        List<Future<V>> futures = new ArrayList<>(tasks.length);
        for (T task : tasks) {
            futures.add(futureOf.apply(task));
        }
        // Each FutureTask keeps the error of its task, so no run of the join
        // throws
        join(futures.size(),
                index -> ((FutureTask<V>) futures.get(index)).run());
        for (int i = 0; i < futures.size(); ++i) {
            try {
                futures.get(i).get();
            }
            catch (ExecutionException e) {
                errorHandler.accept(tasks[i], e);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return futures;
    }

    /**
     * A task that a {@link #join(BiConsumer, int, IntConsumer) join} queues so
     * that an idle worker runs some of its indices.
     */
    private static final class Helper extends FutureTask<Void> {

        /**
         * Construct a new instance.
         *
         * @param drain the work that runs indices of the join until none are
         *            left
         */
        Helper(Runnable drain) {
            super(drain, null);
        }

    }

}
