/*
 * Copyright (c) 2015 Cinchapi Inc.
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
package com.cinchapi.common.profile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

/**
 * An abstraction for measuring the amount of time it takes to perform an
 * action.
 * <p>
 * When creating a {@link Benchmark} you simply define an {@link #action()
 * action} that is {@link #run() run once} or {@link #run(int) multiple times}.
 * As the action runs, the elapsed time is measured and returned in the
 * {@link TimeUnit} of choice.
 * </p>
 * 
 * @author Jeff Nelson
 */
public abstract class Benchmark {

    /*
     * TODO:
     * - add additional measure methods (e.g., longest, shortest, etc)
     * - add, to builder, ability to fork the benchmark and run it in a separate
     * JVM
     */

    /**
     * Start building a benchmark by providing the action to measure.
     * 
     * @param action the action to benchmark
     * @return a builder to continue configuring the benchmark
     */
    public static ActionStage measure(Runnable action) {
        return new BenchmarkBuilder(action);
    }

    /**
     * Start building a benchmark by providing the action to measure.
     * 
     * @param action the action to benchmark
     * @return a builder to continue configuring the benchmark
     */
    public static ActionStage of(Runnable action) {
        return measure(action);
    }

    /**
     * The {@link TimeUnit} in which the elapsed time should be expressed when
     * returned to the caller.
     */
    private final TimeUnit unit;

    /**
     * Construct a new instance.
     * 
     * @param unit the desired {@link #unit time unit}
     */
    public Benchmark(TimeUnit unit) {
        this.unit = unit;
    }

    public abstract void action();

    /**
     * Return the average run time of the {@link #action()} over the specified
     * number of run {@code times}.
     * 
     * @param times
     * @return the average run time
     */
    public final double average(int times) {
        return (double) run(times) / times;
    }

    /**
     * Run the {@link #action() action} once and return the elapsed time,
     * expressed in the {@link TimeUnit} that was passed into the constructor.
     * 
     * @return the elapsed time
     */
    public final long run() {
        long start = System.nanoTime();
        action();
        long end = System.nanoTime();
        return unit.convert(end - start, TimeUnit.NANOSECONDS);
    }

    /**
     * Run the {@link #action action} the specified number of {@times} and
     * return the elapsed time, expressed in the {@link TimeUnit} that was
     * passed into the constructor.
     * 
     * @param rounds
     * @return the elapsed time
     */
    public final long run(int times) {
        Preconditions.checkArgument(times > 0,
                "The number of run times must be greater than 0");
        long start = System.nanoTime();
        for (int i = 0; i < times; ++i) {
            action();
        }
        long end = System.nanoTime();
        return unit.convert(end - start, TimeUnit.NANOSECONDS);
    }

    /**
     * Interface for the first stage of the builder where an action is provided.
     */
    public interface ActionStage {
        /**
         * Specify the time unit for benchmark results.
         * 
         * @param unit the time unit to use for results
         * @return the next stage of the builder
         */
        TimeUnitStage in(TimeUnit unit);
    }

    /**
     * Interface for the configuration stage of the builder.
     */
    public interface ConfigStage {
        /**
         * Configure the benchmark to run asynchronously.
         * 
         * @return the next stage of the builder
         */
        ConfigStage async();

        /**
         * Configure the benchmark to run asynchronously with a custom executor.
         * 
         * @param executor the executor to use for async execution
         * @return the next stage of the builder
         */
        ConfigStage async(Executor executor);

        /**
         * Run the benchmark action multiple times and return the average time.
         * 
         * @param times the number of times to run the action
         * @return a future that will complete with the average elapsed time
         */
        CompletableFuture<Double> average(int times);

        /**
         * Run the benchmark action once.
         * 
         * @return a future that will complete with the elapsed time
         */
        CompletableFuture<Long> run();

        /**
         * Run the benchmark action multiple times.
         * 
         * @param times the number of times to run the action
         * @return a future that will complete with the total elapsed time
         */
        CompletableFuture<Long> run(int times);

        /**
         * Configure the benchmark to perform warmup runs before measurement.
         * 
         * @param count the number of warmup runs to perform
         * @return the next stage of the builder
         */
        ConfigStage warmups(int count);
    }

    /**
     * Interface for the stage of the builder where a time unit is provided.
     */
    public interface TimeUnitStage {
        /**
         * Configure the benchmark to run asynchronously.
         * 
         * @return the next stage of the builder
         */
        ConfigStage async();

        /**
         * Configure the benchmark to run asynchronously with a custom executor.
         * 
         * @param executor the executor to use for async execution
         * @return the next stage of the builder
         */
        ConfigStage async(Executor executor);

        /**
         * Run the benchmark action multiple times and return the average time.
         * 
         * @param times the number of times to run the action
         * @return a future that will complete with the average elapsed time
         */
        CompletableFuture<Double> average(int times);

        /**
         * Run the benchmark action once.
         * 
         * @return a future that will complete with the elapsed time
         */
        CompletableFuture<Long> run();

        /**
         * Run the benchmark action multiple times.
         * 
         * @param times the number of times to run the action
         * @return a future that will complete with the total elapsed time
         */
        CompletableFuture<Long> run(int times);

        /**
         * Configure the benchmark to perform warmup runs before measurement.
         * 
         * @param count the number of warmup runs to perform
         * @return the next stage of the builder
         */
        ConfigStage warmups(int count);
    }

    /**
     * Implementation of the benchmark builder.
     */
    private static class BenchmarkBuilder implements
            ActionStage,
            TimeUnitStage,
            ConfigStage {
        /**
         * The action to benchmark.
         */
        private final Runnable action;

        /**
         * The time unit in which to express benchmark results.
         */
        private TimeUnit unit;

        /**
         * The number of warmup runs to perform before measurement.
         */
        private int warmupCount = 0;

        /**
         * The executor to use for asynchronous execution, or null for
         * synchronous execution.
         */
        private Executor executor = null;

        private BenchmarkBuilder(Runnable action) {
            this.action = action;
        }

        @Override
        public ConfigStage async() {
            this.executor = ForkJoinPool.commonPool();
            return this;
        }

        @Override
        public ConfigStage async(Executor executor) {
            this.executor = executor;
            return this;
        }

        @Override
        public CompletableFuture<Double> average(int times) {
            return executeTask(() -> {
                performWarmups();
                return createBenchmark().average(times);
            });
        }

        @Override
        public TimeUnitStage in(TimeUnit unit) {
            this.unit = unit;
            return this;
        }

        @Override
        public CompletableFuture<Long> run() {
            return executeTask(() -> {
                performWarmups();
                return createBenchmark().run();
            });
        }

        @Override
        public CompletableFuture<Long> run(int times) {
            return executeTask(() -> {
                performWarmups();
                return createBenchmark().run(times);
            });
        }

        @Override
        public ConfigStage warmups(int count) {
            this.warmupCount = count;
            return this;
        }

        /**
         * Create a Benchmark instance that uses the configured action.
         */
        private Benchmark createBenchmark() {
            return new Benchmark(unit) {
                @Override
                public void action() {
                    action.run();
                }
            };
        }

        /**
         * Execute a task either synchronously or asynchronously based on
         * configuration.
         */
        private <T> CompletableFuture<T> executeTask(Supplier<T> task) {
            if(executor != null) {
                return CompletableFuture.supplyAsync(task, executor);
            }
            else {
                return CompletableFuture.completedFuture(task.get());
            }
        }

        /**
         * Perform warmup runs if configured.
         */
        private void performWarmups() {
            Preconditions.checkArgument(warmupCount >= 0,
                    "The warmups parameter must be a positive number.");
            for (int i = 0; i < warmupCount; i++) {
                action.run();
            }
        }
    }
}
