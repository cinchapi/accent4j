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
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.google.common.collect.ImmutableList;

/**
 * A {@link JoinableExecutorService} that only uses the calling thread.
 *
 * @author Jeff Nelson
 */
public class JoinableDirectExecutorService extends AbstractExecutorService
        implements
        JoinableExecutorService {

    /**
     * Shutdown tracker
     */
    private final CountDownLatch shutdown = new CountDownLatch(1);

    @Override
    public void shutdown() {
        shutdown.countDown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return ImmutableList.of();
    }

    @Override
    public boolean isShutdown() {
        return shutdown.getCount() == 0;
    }

    @Override
    public boolean isTerminated() {
        return shutdown.getCount() == 0;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit)
            throws InterruptedException {
        return shutdown.await(timeout, unit);
    }

    @Override
    public void execute(Runnable command) {
        join(command);
    }

    @Override
    public <V> List<Future<V>> join(
            BiConsumer<Callable<V>, Throwable> errorHandler,
            @SuppressWarnings("unchecked") Callable<V>... tasks) {
        if(shutdown.getCount() > 0) {
            List<Future<V>> futures = new ArrayList<>();
            for (Callable<V> task : tasks) {
                try {
                    V result = task.call();
                    futures.add(CompletableFuture.completedFuture(result));
                }
                catch (Exception e) {
                    errorHandler.accept(task, e);
                }
            }
            return futures;
        }
        else {
            throw new RejectedExecutionException();
        }
    }

    @Override
    public void join(BiConsumer<Runnable, Throwable> errorHandler,
            Runnable... tasks) {
        if(shutdown.getCount() > 0) {
            for (Runnable task : tasks) {
                try {
                    task.run();
                }
                catch (Throwable t) {
                    errorHandler.accept(task, t);
                }
            }
        }
        else {
            throw new RejectedExecutionException();
        }
    }

}
