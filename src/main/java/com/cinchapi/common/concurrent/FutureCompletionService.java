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

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.common.collect.Lists;

/**
 *
 *
 * @author jeff
 */
public class FutureCompletionService<V> {

    private final BlockingQueue<Future<V>> completed;
    private List<QueueingFuture> futures;

    @SafeVarargs
    public FutureCompletionService(Future<V>... futures) {
        this.completed = new LinkedBlockingQueue<Future<V>>();
        this.futures = Lists.newArrayList();
        for(Future<V> future : futures) {
            this.futures.add(new QueueingFuture(future));
        }
    }

    public V get() throws InterruptedException, ExecutionException {
        return completed.take().get();
    }

    public V get(Future<V> preferred, long timeout, TimeUnit unit)
            throws InterruptedException, ExecutionException {
        try {
            return preferred.get(timeout, unit);
        }
        catch (TimeoutException e) {
            return completed.take().get();
        }
    }
    
    /**
     * FutureTask extension to enqueue upon completion
     */
    private class QueueingFuture extends FutureTask<Void> {
        QueueingFuture(Future<V> task) {
            super(new FutureTask<>(() -> task.get()), null);
            this.task = task;
            if(task.isDone()) {
                completed.add(task);
            }
        }
        protected void done() { completed.add(task); }
        private final Future<V> task;
    }

}
