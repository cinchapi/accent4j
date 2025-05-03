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
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import com.cinchapi.common.base.CheckedExceptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

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
            this.tasks.offer(new FutureTask<Void>(command, null));
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

    /**
     * Cause the calling thread to temporarily join this {@link ExecutorService
     * executor} and assist in executing the provided {@code tasks}.
     * <p>
     * Similar to all {@link ExecutorService} operations, this will
     * asynchronously perform each of the {@code tasks}, at some point in the
     * future. But, in addition, the calling thread will be blocked until all of
     * them have completed.
     * </p>
     * <p>
     * Each task may execute in a in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * affect is that the group of {@code tasks} in completed in at least as
     * much time as they would be if the calling thread executed them serially
     * </p>
     * 
     * @param errorHandler a {@link BiConsumer} that runs whenever an error
     *            occurs while executing one of the {@code tasks}.
     * @param tasks
     * @return a list of {@link Future} objects that correspond to each of the
     *         submitted {@code tasks}; since this method awaits completion of
     *         all tasks, each {@link Future} will be {@link Future#isDone()
     *         done} and the result can be {@link Future#get() retrieved}
     *         immediately
     */
    @SuppressWarnings("unchecked")
    public <V> List<Future<V>> join(
            BiConsumer<Callable<V>, Throwable> errorHandler,
            Callable<V>... tasks) {
        Preconditions.checkNotNull(tasks);
        Preconditions.checkArgument(tasks.length > 0);
        if(state.compareAndSet(RUNNING, RUNNING)) {
            List<Future<V>> futures = new ArrayList<>();
            for (Callable<V> task : tasks) {
                FutureTask<V> future = new FutureTask<>(task);
                this.tasks.offer(future);
                futures.add(future);
            }
            for (int i = futures.size() - 1; i >= 0; --i) {
                FutureTask<V> task = (FutureTask<V>) futures.get(i);
                if(!task.isDone()) {
                    // Use the calling thread to steal work before awaiting all
                    // tasks to complete
                    task.run();
                }
            }
            for (int i = 0; i < futures.size(); ++i) {
                Future<V> future = futures.get(i);
                try {
                    future.get();
                }
                catch (ExecutionException e) {
                    Callable<V> t = tasks[i];
                    errorHandler.accept(t, e);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            return futures;
        }
        else {
            throw new RejectedExecutionException();
        }

    }

    /**
     * Cause the calling thread to temporarily join this {@link ExecutorService
     * executor} and assist in executing the provided {@code tasks}.
     * <p>
     * Similar to all {@link ExecutorService} operations, this will
     * asynchronously perform each of the {@code tasks}, at some point in the
     * future. But, in addition, the calling thread will be blocked until all of
     * them have completed.
     * </p>
     * <p>
     * Each task may execute in a in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * affect is that the group of {@code tasks} in completed in at least as
     * much time as they would be if the calling thread executed them serially
     * </p>
     * 
     * @param errorHandler a {@link BiConsumer} that runs whenever an error
     *            occurs while executing one of the {@code tasks}.
     * @param tasks
     */
    public void join(BiConsumer<Runnable, Throwable> errorHandler,
            Runnable... tasks) {
        Preconditions.checkNotNull(tasks);
        Preconditions.checkArgument(tasks.length > 0);
        if(state.compareAndSet(RUNNING, RUNNING)) {
            List<FutureTask<Void>> futures = new ArrayList<>();
            for (Runnable task : tasks) {
                FutureTask<Void> future = new FutureTask<>(task, null);
                this.tasks.offer(future);
                futures.add(future);
            }
            for (int i = futures.size() - 1; i >= 0; --i) {
                FutureTask<Void> task = futures.get(i);
                if(!task.isDone()) {
                    // Use the calling thread to steal work before awaiting all
                    // tasks to complete
                    task.run();
                }
            }
            for (int i = 0; i < futures.size(); ++i) {
                Future<Void> future = futures.get(i);
                try {
                    future.get();
                }
                catch (ExecutionException e) {
                    Runnable t = tasks[i];
                    errorHandler.accept(t, e);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        else {
            throw new RejectedExecutionException();
        }
    }

    /**
     * Cause the calling thread to temporarily join this {@link ExecutorService
     * executor} and assist in executing the provided {@code tasks}.
     * <p>
     * Similar to all {@link ExecutorService} operations, this will
     * asynchronously perform each of the {@code tasks}, at some point in the
     * future. But, in addition, the calling thread will be blocked until all of
     * them have completed.
     * </p>
     * <p>
     * Each task may execute in a in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * affect is that the group of {@code tasks} in completed in at least as
     * much time as they would be if the calling thread executed them serially
     * </p>
     * 
     * @param tasks
     * @return a list of {@link Future} objects that correspond to each of the
     *         submitted {@code tasks}; since this method awaits completion of
     *         all tasks, each {@link Future} will be {@link Future#isDone()
     *         done} and the result can be {@link Future#get() retrived}
     *         immediately
     */
    @SuppressWarnings("unchecked")
    public <V> List<Future<V>> join(Callable<V>... tasks) {
        return join((task, error) -> {
            throw CheckedExceptions.wrapAsRuntimeException(error);
        }, tasks);
    }

    /**
     * Cause the calling thread to temporarily join this {@link ExecutorService
     * executor} and assist in executing the provided {@code tasks}.
     * <p>
     * Similar to all {@link ExecutorService} operations, this will
     * asynchronously perform each of the {@code tasks}, at some point in the
     * future. But, in addition, the calling thread will be blocked until all of
     * them have completed.
     * </p>
     * <p>
     * Each task may execute in a in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * affect is that the group of {@code tasks} in completed in at least as
     * much time as they would be if the calling thread executed them serially
     * </p>
     * 
     * @param tasks
     */
    public void join(Runnable... tasks) {
        join((task, error) -> {
            throw CheckedExceptions.wrapAsRuntimeException(error);
        }, tasks);
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
        if(state.compareAndSet(RUNNING, SHUTDOWN)) {
            shutdown();
            List<Runnable> unfinished = new ArrayList<>();
            tasks.drainTo(unfinished);
            return unfinished;
        }
        else {
            return ImmutableList.of();
        }
    }

    /**
     * The main loop for worker threads in the executor service. This method
     * runs continuously until the executor service is terminated. It is
     * responsible for executing tasks from the task groups that have been
     * submitted to the service.
     *
     * <p>
     * The worker loop operates as follows:
     * <ol>
     * <li>If the executor service is in the {@link State#TERMINATED} state, the
     * loop
     * terminates and the worker thread exits.</li>
     * <li>If the executor service is in the {@link State#SHUTDOWN} state, the
     * worker attempts to retrieve and process a task group from the queue.</li>
     * <li>If there are no task groups available in the shutdown state, the
     * worker thread exits the loop and terminates.</li>
     * <li>If the executor service is in the {@link State#RUNNING} state, the
     * worker retrieves a task group from the queue and processes it.</li>
     * <li>For each task in the group, the worker calls the
     * {@link #run(Runnable)} method to execute the task.</li>
     * <li>If the executor service transitions to the {@link State#SHUTDOWN}
     * state during task execution, the worker continues executing all remaining
     * tasks in the current group before exiting the loop.</li>
     * <li>If the executor service remains in the {@link State#RUNNING} state,
     * the worker returns the partially processed group back to the queue for
     * another worker to continue execution.</li>
     * </ol>
     * </p>
     * <p>
     * The worker loop is designed to handle interruptions and state transitions
     * gracefully, ensuring that all submitted tasks are eventually executed
     * unless the executor service is terminated forcefully.
     * </p>
     */
    private void executeTaskLoop() {
        for (;;) {
            if(state.compareAndSet(RUNNING, RUNNING)) {
                FutureTask<?> task;
                try {
                    task = tasks.take();
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
                task.run();
            }
            else if(state.compareAndSet(SHUTDOWN, SHUTDOWN)) {
                FutureTask<?> task = tasks.poll();
                if(task == null) {
                    // No tasks remain, so break out and terminate this thread.
                    state.compareAndSet(SHUTDOWN, TERMINATED);
                    break;
                }
            }
            else if(state.compareAndSet(TERMINATED, TERMINATED)) {
                break;
            }
            else {
                // The state has changed, so re-loop and check again
                continue;
            }
        }
        termination.countDown();
    }

}
