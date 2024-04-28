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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.cinchapi.common.base.CheckedExceptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/**
 * A {@link JoinableExecutorService} provides asynchronous execution of task
 * groups with an option for the calling thread to {@link #join(Runnable...)
 * join} and participate in executing its group's tasks while awaiting their
 * completion.
 * <p>
 * This {@link ExecutorService} acts as a shared resource for multiple
 * processes, enabling each to execute a group of subtasks. It uniquely allows
 * the calling threads of these processes to contribute actively in executing
 * the tasks they submitted, facilitating faster completion and efficient use of
 * resources.
 * </p>
 * <p>
 * When a group of tasks is submitted, they are <strong>initiated</strong> in
 * iteration order. However, this {@link ExecutorService} strives to balance the
 * distribution of progress across all groups to maintain consistency in overall
 * system performance. So, internal worker threads tend to execute tasks across
 * groups in a breadth-first manner.
 * </p>
 *
 * @author Jeff Nelson
 */
public class JoinableExecutorService extends AbstractExecutorService {
    
    /**
     * The underlying {@link ExecutorService} that provides and manages each
     * worker thread.
     */
    private final ExecutorService workers;

    /**
     * The task groups that have been {@link #submit(BlockingQueue) submitted}.
     */
    private final BlockingQueue<BlockingQueue<? extends Runnable>> groups;

    /**
     * The state of the executor.
     */
    private AtomicReference<State> state;

    /**
     * Construct a new instance.
     * 
     * @param numWorkerThreads
     */
    public JoinableExecutorService(int numWorkerThreads) {
        this(numWorkerThreads, Executors.defaultThreadFactory());
    }

    /**
     * Construct a new instance.
     * 
     * @param numWorkerThreads
     * @param threadFactory
     */
    public JoinableExecutorService(int numWorkerThreads,
            ThreadFactory threadFactory) {
        this.groups = new LinkedBlockingQueue<>();
        this.workers = Executors.newFixedThreadPool(numWorkerThreads,
                threadFactory);
        this.state = new AtomicReference<>(State.RUNNING);
        for (int i = 0; i < numWorkerThreads; ++i) {
            workers.execute(() -> {
                for (;;) {
                    if(state.compareAndSet(State.TERMINATED,
                            State.TERMINATED)) {
                        break;
                    }
                    else {
                        BlockingQueue<? extends Runnable> group;
                        if(state.compareAndSet(State.SHUTDOWN,
                                State.SHUTDOWN)) {
                            group = groups.poll();
                            if(group == null) {
                                break;
                            }
                        }
                        else {
                            try {
                                group = groups.take();
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
                        }
                        Runnable task = group.poll();
                        if(task != null) {
                            run(task);
                            if(state.compareAndSet(State.SHUTDOWN,
                                    State.SHUTDOWN)) {
                                // Since we are shutting down, just complete all
                                // the tasks in the group and be done
                                while ((task = group.poll()) != null) {
                                    run(task);
                                }
                            }
                            else {
                                groups.offer(group);
                            }
                        }
                        else {
                            // Since part of the contract is that tasks will not
                            // be added after a queue has been submitted, assume
                            // that all that work for this group has been done.
                            continue;
                        }
                    }
                }
            });
        }

    }

    /**
     * Blocks until all tasks have completed execution after a shutdown request
     * or the current thread is interrupted, whichever
     * happens first.
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
        return workers.awaitTermination(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        if(state.compareAndSet(State.RUNNING, State.RUNNING)) {
            BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
            queue.add(command);
            groups.offer(queue);
        }
        else {
            throw new RejectedExecutionException();
        }
    }

    @Override
    public boolean isShutdown() {
        return workers.isShutdown() && state.get() != State.RUNNING;
    }

    @Override
    public boolean isTerminated() {
        return workers.isTerminated() && state.get() == State.TERMINATED
                && groups.isEmpty();
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
        if(state.compareAndSet(State.RUNNING, State.RUNNING)) {
            BlockingQueue<FutureTask<V>> queue = new LinkedBlockingQueue<>();
            List<Future<V>> futures = new ArrayList<>();
            for (Callable<V> task : tasks) {
                FutureTask<V> future = new FutureTask<>(task);
                queue.offer(future);
                futures.add(future);
            }
            groups.offer(queue);
            FutureTask<V> task;
            while ((task = queue.poll()) != null) {
                // Use the calling thread to steal work before waiting for all
                // the tasks to complete
                run(task);
            }
            for (int i = 0; i < futures.size(); ++i) {
                Future<V> future = futures.get(i);
                try {
                    future.get();
                }
                catch (ExecutionException e) {
                    Callable<V> t = tasks[i];
                    errorHandler.accept(t, e);
                    throw CheckedExceptions.wrapAsRuntimeException(e);
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
        if(state.compareAndSet(State.RUNNING, State.RUNNING)) {
            BlockingQueue<FutureTask<Void>> queue = new LinkedBlockingQueue<>();
            List<Future<Void>> futures = new ArrayList<>();
            for (Runnable task : tasks) {
                FutureTask<Void> future = new FutureTask<>(task, null);
                queue.offer(future);
                futures.add(future);
            }
            groups.offer(queue);
            Runnable task;
            while ((task = queue.poll()) != null) {
                // Use the calling thread to steal work before waiting for all
                // the tasks to complete
                run(task);
            }
            for (int i = 0; i < futures.size(); ++i) {
                Future<Void> future = futures.get(i);
                try {
                    future.get();
                }
                catch (ExecutionException e) {
                    Runnable t = tasks[i];
                    errorHandler.accept(t, e);
                    throw CheckedExceptions.wrapAsRuntimeException(e);
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
        if(state.compareAndSet(State.RUNNING, State.SHUTDOWN)) {
            workers.shutdownNow();
            while (!workers.isTerminated()) {
                continue;
            }
            state.compareAndSet(State.SHUTDOWN, State.TERMINATED);
        }
    }

    @Override
    public List<Runnable> shutdownNow() {
        if(state.compareAndSet(State.RUNNING, State.TERMINATED)
                || state.compareAndSet(State.SHUTDOWN, State.TERMINATED)) {
            workers.shutdownNow();
            List<Runnable> unfinished = new ArrayList<>();
            groups.forEach(group -> group.drainTo(unfinished));
            groups.clear();
            return unfinished;
        }
        else {
            return ImmutableList.of();
        }
    }

    /**
     * Execute the {@code task}.
     * 
     * @param task
     */
    private void run(Runnable task) {
        if(task instanceof RunnableFuture
                && !((RunnableFuture<?>) task).isDone()) {
            // NOTE: Exception handling is automatically
            // handled by the internals of FutureTask
            task.run();
        }
        else if(task != null) {
            try {
                task.run();
            }
            catch (Throwable e) {}
        }
    }

    /**
     * Tracks the state of the executor.
     *
     * @author Jeff Nelson
     */
    private enum State {
        RUNNING, SHUTDOWN, TERMINATED
    }

}
