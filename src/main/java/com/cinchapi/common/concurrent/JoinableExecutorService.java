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

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;

import com.cinchapi.common.base.CheckedExceptions;

/**
 * A {@link JoinableExecutorService} is a specialized {@link ExecutorService}
 * designed to accelerate the execution of a batch of tasks by having the
 * submitting thread actively participate in the work while it waits for them
 * to complete.
 *
 * <h2>Core Concept: Active Waiting</h2>
 * <p>
 * The primary feature of this interface is the set of
 * {@link #join(Callable...) join} methods.
 * When a thread calls {@link #join(Callable...) join}, it blocks until all
 * provided tasks are finished. Unlike a traditional {@code Future.get()} call
 * where the thread would wait idly (<strong>passive waiting</strong>), a thread
 * calling {@link #join(Callable...) join} becomes a temporary worker for the
 * tasks it just submitted (<strong>active waiting</strong>).
 * </p>
 * <p>
 * This cooperative approach transforms potential idle time into productive
 * work, maximizing CPU utilization and reducing the total time required to
 * complete the entire batch of tasks.
 * </p>
 *
 * <h2>How It Works: The Caller Works, Idle Workers Help</h2>
 * <p>
 * A join hands out its work one item at a time, and the calling thread takes
 * items until none are left. A worker that is idle when the join begins takes
 * items too, so the work spreads across every thread that is free. A worker
 * that is busy does not wait for, or hold up, the join. So when every worker is
 * busy, the calling thread does all of the work itself, in about the time it
 * would take to do it without this executor.
 * </p>
 *
 * <h2>Ideal Use Case: Parallel Independent Tasks</h2>
 * <p>
 * This executor is best suited for scenarios where a thread needs to dispatch
 * a collection of <strong>independent, non-divisible tasks</strong> and then
 * block until all are complete. This is common when a process needs to
 * aggregate the results of several parallel operations.
 * </p>
 * Good examples include:
 * <ul>
 * <li>Making several API calls to different microservices.</li>
 * <li>Running a handful of unrelated, long-running database queries.</li>
 * <li>Processing a set of files from a directory where each file can be
 * handled separately.</li>
 * </ul>
 *
 * <h2>Comparison to Other Java Executors</h2>
 * <p>
 * <strong>vs. {@link java.util.concurrent.ThreadPoolExecutor}:</strong> A
 * standard {@code ThreadPoolExecutor} would leave the calling thread idly
 * blocked while waiting on {@code Future.get()}. This interface makes that
 * waiting period productive.
 * </p>
 * <p>
 * <strong>vs. {@link java.util.concurrent.ForkJoinPool}:</strong> While both
 * executors involve caller participation, they are designed for fundamentally
 * different problems and have different architectures.
 * <ul>
 * <li><strong>Task Type:</strong> This service is for a batch of monolithic
 * {@code Runnable}/{@code Callable} tasks. A {@code ForkJoinPool} is for
 * <strong>divisible, "divide-and-conquer"</strong> style
 * {@code ForkJoinTask}s that can be recursively broken down.</li>
 * <li><strong>Work-Sharing Model:</strong> In this service, the threads of a
 * join take its items one at a time from a shared position, and only idle
 * workers join in. A {@code ForkJoinPool} uses a
 * <strong>work-stealing</strong> algorithm, where any idle thread can steal
 * tasks from any other busy thread, which balances recursive problems
 * well.</li>
 * </ul>
 * </p>
 *
 * @author Jeff Nelson
 */
public interface JoinableExecutorService extends ExecutorService {

    /**
     * Create a new {@link JoinableExecutorService} with a fixed number of
     * worker threads.
     *
     * @param numWorkerThreads the number of worker threads to create
     * @return a new thread pool executor service
     */
    public static JoinableExecutorService create(int numWorkerThreads) {
        return new JoinableThreadPoolExecutor(numWorkerThreads);
    }

    /**
     * Create a new {@link JoinableExecutorService} with a fixed number of
     * worker threads using the provided thread factory.
     *
     * @param numWorkerThreads the number of worker threads to create
     * @param threadFactory the factory to use for creating threads
     * @return a new thread pool executor service
     */
    public static JoinableExecutorService create(int numWorkerThreads,
            ThreadFactory threadFactory) {
        return new JoinableThreadPoolExecutor(numWorkerThreads, threadFactory);
    }

    /**
     * Create a new {@link JoinableExecutorService} that only uses the calling
     * thread.
     *
     * @return a new direct executor service
     */
    public static JoinableExecutorService direct() {
        return new JoinableDirectExecutorService();
    }

    /**
     * Create a new pooled {@link JoinableExecutorService} with a fixed number
     * of worker threads.
     *
     * @param numWorkerThreads the number of worker threads to create
     * @return a new thread pool executor service
     */
    public static JoinableExecutorService pooled(int numWorkerThreads) {
        return create(numWorkerThreads);
    }

    /**
     * Create a new pooled {@link JoinableExecutorService} with a fixed number
     * of worker threads using the provided thread factory.
     *
     * @param numWorkerThreads the number of worker threads to create
     * @param threadFactory the factory to use for creating threads
     * @return a new thread pool executor service
     */
    public static JoinableExecutorService pooled(int numWorkerThreads,
            ThreadFactory threadFactory) {
        return create(numWorkerThreads, threadFactory);
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
     * Each task may execute in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * effect is that the group of {@code tasks} completes in no more time
     * than it would if the calling thread executed them serially.
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
            Callable<V>... tasks);

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
     * Each task may execute in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * effect is that the group of {@code tasks} completes in no more time
     * than it would if the calling thread executed them serially.
     * </p>
     *
     * @param errorHandler a {@link BiConsumer} that runs whenever an error
     *            occurs while executing one of the {@code tasks}.
     * @param tasks
     */
    public void join(BiConsumer<Runnable, Throwable> errorHandler,
            Runnable... tasks);

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
     * Each task may execute in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * effect is that the group of {@code tasks} completes in no more time
     * than it would if the calling thread executed them serially.
     * </p>
     *
     * @param tasks
     * @return a list of {@link Future} objects that correspond to each of the
     *         submitted {@code tasks}; since this method awaits completion of
     *         all tasks, each {@link Future} will be {@link Future#isDone()
     *         done} and the result can be {@link Future#get() retrieved}
     *         immediately
     */
    @SuppressWarnings("unchecked")
    public default <V> List<Future<V>> join(Callable<V>... tasks) {
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
     * Each task may execute in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * effect is that the group of {@code tasks} completes in no more time
     * than it would if the calling thread executed them serially.
     * </p>
     *
     * @param tasks
     */
    public default void join(Runnable... tasks) {
        join((task, error) -> {
            throw CheckedExceptions.wrapAsRuntimeException(error);
        }, tasks);
    }

    /**
     * Assuming an ordered collection of {@code count} tasks, such as an array
     * or a {@link List}, run the provided {@code action} once for each task,
     * and return once every run is done.
     * <p>
     * The {@code action} receives the index of a task, from {@code 0} to
     * {@code count - 1}, and must get that task from the collection by its
     * index and run it. Unlike {@link #join(Runnable...)} and
     * {@link #join(Callable...)}, this form does not take the tasks themselves.
     * It takes only their number, so the caller creates no object for each
     * task.
     * </p>
     * <p>
     * The calling thread runs tasks until none are left, and any worker that is
     * idle when the join begins runs tasks too. So the join finishes even when
     * every worker is busy, and a busy pool leaves every task to the calling
     * thread. The tasks run in no set order, and no task runs twice.
     * </p>
     * <p>
     * Each run that throws is reported to the provided {@code errorHandler}, on
     * the calling thread, with its index. If the calling thread is interrupted
     * while it waits for a worker to finish a run, the join returns at once,
     * with the interrupt status set.
     * </p>
     *
     * @param errorHandler receives the index and the error of each run that
     *            throws
     * @param count the number of tasks in the collection, which must not be
     *            negative; {@code 0} runs nothing
     * @param action receives the index of one task, and gets and runs that
     *            task
     */
    public void join(BiConsumer<Integer, Throwable> errorHandler, int count,
            IntConsumer action);

    /**
     * Assuming an ordered collection of {@code count} tasks, such as an array
     * or a {@link List}, run the provided {@code action} once for each task,
     * and return once every run is done.
     * <p>
     * The {@code action} receives the index of a task, from {@code 0} to
     * {@code count - 1}, and must get that task from the collection by its
     * index and run it. Unlike {@link #join(Runnable...)} and
     * {@link #join(Callable...)}, this form does not take the tasks themselves.
     * It takes only their number, so the caller creates no object for each
     * task.
     * </p>
     * <p>
     * The calling thread runs tasks until none are left, and any worker that is
     * idle when the join begins runs tasks too. So the join finishes even when
     * every worker is busy, and a busy pool leaves every task to the calling
     * thread. The tasks run in no set order, and no task runs twice.
     * </p>
     * <p>
     * If a run throws, the join throws that error, wrapped if it is checked,
     * once every run is done.
     * </p>
     *
     * @param count the number of tasks in the collection, which must not be
     *            negative; {@code 0} runs nothing
     * @param action receives the index of one task, and gets and runs that
     *            task
     */
    public default void join(int count, IntConsumer action) {
        join((index, error) -> {
            throw CheckedExceptions.wrapAsRuntimeException(error);
        }, count, action);
    }

}
