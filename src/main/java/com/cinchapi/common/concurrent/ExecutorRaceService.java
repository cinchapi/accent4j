/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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

import java.util.Arrays;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.cinchapi.common.base.Array;
import com.google.common.base.Preconditions;

/**
 * An {@link ExecutorRaceService} provides a mechanism for submitting multiple
 * {@link Callable tasks} to an {@link Executor} and waiting for the first task
 * to complete.
 * <p>
 * This is similar to a {@link ExecutorCompletionService} but the semantics are
 * slightly different. Central to this service is the concept of "racing". As
 * such, it is possible to give one task an arbitrary
 * {@link #raceWithHeadStart(long, TimeUnit, Callable, Callable...) head start}
 * so that other tasks do not execute if that task completes first.
 * </p>
 *
 * @author Jeff Nelson
 */
public class ExecutorRaceService<V> {

    /**
     * The {@link Executor} that executes the tasks.
     */
    private final Executor executor;

    /**
     * Construct a new instance.
     * 
     * @param executor
     */
    public ExecutorRaceService(Executor executor) {
        this.executor = executor;
    }

    /**
     * Return the {@link Future} for the first of the {@code task} and
     * {@code tasks} to complete.
     * <p>
     * The {@link Future} is guaranteed to be {@link Future#isDone()} when this
     * method returns.
     * </p>
     * 
     * @param task
     * @param tasks
     * @return the {@link Future} of the completed task
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    public Future<V> race(Callable<V> task, Callable<V>... tasks)
            throws InterruptedException {
        return raceWithHeadStart(0, TimeUnit.MICROSECONDS, task, tasks);
    }

    /**
     * Return the {@link Future} for the first of the {@code task} and
     * {@code tasks} to complete.
     * <p>
     * The {@link Future} is guaranteed to be {@link Future#isDone()} when this
     * method returns.
     * </p>
     * 
     * @param task
     * @param tasks
     * @return the {@link Future} of the completed task
     * @throws InterruptedException
     */
    public Future<V> race(Runnable task, Runnable... tasks)
            throws InterruptedException {
        return raceWithHeadStart(0, TimeUnit.MICROSECONDS, task, tasks);
    }

    /**
     * Return the {@link Future} for the first of the {@code headStartTask} and
     * {@code tasks} to complete; giving the first {@code task}
     * {@code headStartTime} {@code headStartTimeUnit} to complete before
     * allowing the remaining {@code tasks} to race.
     * <p>
     * Even if the {@code headStartTask} does not finish within the head start
     * period, it may still finish before the other {@code tasks} and returns
     * its {@link Future}.
     * </p>
     * <p>
     * The {@link Future} is guaranteed to be {@link Future#isDone()} when this
     * method returns.
     * </p>
     * 
     * @param headStartTime
     * @param headStartTimeUnit
     * @param headStartTask
     * @param tasks
     * @return the {@link Future} of the completed task
     * @throws InterruptedException
     */
    @SuppressWarnings("unchecked")
    public Future<V> raceWithHeadStart(long headStartTime,
            TimeUnit headStartTimeUnit, Callable<V> headStartTask,
            Callable<V>... tasks) throws InterruptedException {
        BlockingQueue<Future<V>> completed = new LinkedBlockingQueue<Future<V>>();
        Preconditions.checkNotNull(headStartTask);
        Preconditions.checkState(tasks.length > 0);
        RunnableFuture<V> headStartFuture = new FutureTask<V>(headStartTask);
        executor.execute(new QueueingFuture(headStartFuture, completed));
        try {
            headStartFuture.get(headStartTime, headStartTimeUnit);
            return headStartFuture;
        }
        catch (ExecutionException | TimeoutException e) {
            for (Callable<V> task : tasks) {
                RunnableFuture<V> future = new FutureTask<V>(task);
                executor.execute(new QueueingFuture(future, completed));
            }
            return completed.take();
        }
    }

    /**
     * Return the {@link Future} for the first of the {@code headStartTask} and
     * {@code tasks} to complete; giving the first {@code task}
     * {@code headStartTime} {@code headStartTimeUnit} to complete before
     * allowing the remaining {@code tasks} to race.
     * <p>
     * Even if the {@code headStartTask} does not finish within the head start
     * period, it may still finish before the other {@code tasks} and returns
     * its {@link Future}.
     * </p>
     * <p>
     * The {@link Future} is guaranteed to be {@link Future#isDone()} when this
     * method returns.
     * </p>
     * 
     * @param headStartTime
     * @param headStartTimeUnit
     * @param headStartTask
     * @param tasks
     * @return the {@link Future} of the completed task
     * @throws InterruptedException
     */
    public Future<V> raceWithHeadStart(long headStartTime,
            TimeUnit headStartTimeUnit, Runnable headStartTask,
            Runnable... tasks) throws InterruptedException {
        return raceWithHeadStart(headStartTime, headStartTimeUnit,
                Executors.callable(headStartTask, null),
                Arrays.stream(tasks).map(Executors::callable)
                        .collect(Collectors.toList())
                        .toArray(Array.containing()));
    }

    /**
     * FutureTask extension to enqueue upon completion
     */
    private class QueueingFuture extends FutureTask<Void> {

        /**
         * The queue of completed tasks where this one should be added when it
         * is {@link #done()}.
         */
        private final BlockingQueue<Future<V>> completed;

        /**
         * The future that tracks the task completion.
         */
        private final Future<V> task;

        /**
         * Construct a new instance.
         * 
         * @param task
         * @param completed
         */
        QueueingFuture(RunnableFuture<V> task,
                BlockingQueue<Future<V>> completed) {
            super(task, null);
            this.task = task;
            this.completed = completed;
        }

        @Override
        protected void done() {
            completed.add(task);
        }
    }

}
