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
 * <h2>How It Works: A "Take-Back" Strategy</h2>
 * <p>
 * Implementations of this interface typically use a simple but effective
 * architecture to achieve this behavior:
 * </p>
 * <ol>
 * <li><strong>Single Shared Queue:</strong> All worker threads in the pool
 * pull tasks from one central {@link java.util.concurrent.BlockingQueue}.</li>
 * <li><strong>Submission and "Take-Back":</strong> When {@code join} is
 * called, all tasks are added to this shared queue. The calling thread
 * then immediately attempts to <strong>take back</strong> those same
 * tasks from the queue before a dedicated worker can.</li>
 * <li><strong>Completion:</strong> If the calling thread successfully
 * retrieves a task, it executes it directly. The {@code join} method
 * only returns after all tasks in the batch have been completed, whether
 * they were run by the calling thread or the pool's workers.</li>
 * </ol>
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
 * <li><strong>Architecture:</strong> This service relies on a
 * <strong>single shared queue</strong>, which can be a source of
 * contention. A {@code ForkJoinPool} uses <strong>per-thread
 * deques</strong> (double-ended queues) to dramatically reduce
 * contention.</li>
 * <li><strong>Work-Sharing Model:</strong> This service uses a simple
 * "take-back" strategy. A {@code ForkJoinPool} uses a more sophisticated
 * <strong>work-stealing</strong> algorithm, where any idle thread can
 * steal tasks from any other busy thread, providing superior load
 * balancing for recursive problems.</li>
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
     * Each task may execute in a in a pooled thread, or in the calling thread.
     * Because the calling thread participates in execution while waiting, the
     * affect is that the group of {@code tasks} in completed in at least as
     * much time as they would be if the calling thread executed them serially
     * </p>
     *
     * @param tasks
     */
    public default void join(Runnable... tasks) {
        join((task, error) -> {
            throw CheckedExceptions.wrapAsRuntimeException(error);
        }, tasks);
    }

}
