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
 * A {@link JoinableExecutorService} provides asynchronous execution of tasks
 * with an option for the calling thread to {@link #join(Runnable...)
 * join} and participate in executing its submitted tasks while awaiting their
 * completion.
 * <p>
 * This {@link ExecutorService} acts as a shared resource for multiple
 * processes. Under high contention, the calling thread's participation in
 * completing the tasks it submitted guarantees that the performance will be no
 * worse than if the caller executed each task serially, without using an
 * {@link ExecutorService}.
 * </p>
 * <p>
 * In most cases, the calling thread will work alongside the shared threads in
 * this {@link ExecutorService} to complete all of the submitted tasks faster
 * and more efficiently than using a standard {@link ExecutorService}.
 * </p>
 * <p>
 * Key features of {@link JoinableExecutorService}:
 * <ul>
 * <li>Allows the calling thread to join and participate in executing its
 * submitted tasks.</li>
 * <li>Ensures performance is no worse than serial execution under high
 * contention.</li>
 * <li>Enables faster and more efficient task completion compared to a standard
 * {@link ExecutorService}.</li>
 * </ul>
 * </p>
 * <p>
 * <p>
 * The {@link #join(Runnable...) join} methods block until all submitted tasks
 * are completed.
 * </p>
 *
 * @author Jeff Nelson
 */
public interface JoinableExecutorService extends ExecutorService {

    /**
     * Return a new {@link JoinableExecutorService} with a fixed number of
     * worker threads.
     * 
     * @param numWorkerThreads
     * @return the {@link JoinableExecutorService}
     */
    public static JoinableExecutorService create(int numWorkerThreads) {
        return new JoinableThreadPoolExecutor(numWorkerThreads);
    }

    /**
     * Return a new {@link JoinableExecutorService} with a fixed number of
     * worker threads; each of which is created using the provided
     * {@code threadFactory}.
     * 
     * @param numWorkerThreads
     * @param threadFactory
     * @return the {@link JoinableExecutorService}
     */
    public static JoinableExecutorService create(int numWorkerThreads,
            ThreadFactory threadFactory) {
        return new JoinableThreadPoolExecutor(numWorkerThreads, threadFactory);
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
