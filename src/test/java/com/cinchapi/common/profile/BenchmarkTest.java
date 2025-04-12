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
package com.cinchapi.common.profile;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CompletableFuture;

public class BenchmarkTest {

    private Benchmark benchmark;

    @Before
    public void setUp() {
        benchmark = new Benchmark(TimeUnit.MILLISECONDS) {
            @Override
            public void action() {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
    }

    @Test
    public void testRun() {
        long elapsed = benchmark.run();
        Assert.assertTrue(elapsed >= 10);
        Assert.assertTrue(elapsed < 20);
    }

    @Test
    public void testRunMultipleTimes() {
        long elapsed = benchmark.run(5);
        Assert.assertTrue(elapsed >= 50);
        Assert.assertTrue(elapsed < 100);
    }

    @Test
    public void testRunWithWarmups() {
        long elapsed = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).warmups(2).run(3).join();

        Assert.assertTrue(elapsed >= 30);
        Assert.assertTrue(elapsed < 60);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunWithNegativeTimes() {
        benchmark.run(-1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRunWithNegativeWarmupsAndTimes() {
        Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).in(TimeUnit.MILLISECONDS).warmups(-1).run(-1);
    }

    @Test
    public void testAverage() {
        double avg = benchmark.average(5);
        Assert.assertTrue(avg >= 10);
        Assert.assertTrue(avg < 20);
    }

    @Test
    public void testAverageWithWarmups() {
        double avg = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).warmups(2).average(3).join();

        Assert.assertTrue(avg >= 10);
        Assert.assertTrue(avg < 20);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAverageWithZeroTimes() {
        Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).in(TimeUnit.MILLISECONDS).average(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testAverageWithNegativeWarmupsAndZeroTimes() {
        Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).in(TimeUnit.MILLISECONDS).warmups(-1).average(0);
    }

    @Test
    public void testLongRunningBenchmark() {
        Benchmark longBenchmark = new Benchmark(TimeUnit.SECONDS) {
            @Override
            public void action() {
                try {
                    Thread.sleep(1000);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        };
        long elapsed = longBenchmark.run(5);
        Assert.assertTrue(elapsed >= 5);
        Assert.assertTrue(elapsed < 6);
    }

    @Test
    public void testBenchmarkBuilderBasic() {
        long elapsed = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).run().join();

        Assert.assertTrue(elapsed >= 10);
        Assert.assertTrue(elapsed < 20);
    }

    @Test
    public void testBenchmarkBuilderMultipleRuns() {
        long elapsed = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).run(5).join();

        Assert.assertTrue(elapsed >= 50);
        Assert.assertTrue(elapsed < 100);
    }

    @Test
    public void testBenchmarkBuilderAverage() {
        double avg = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).average(5).join();

        Assert.assertTrue(avg >= 10);
        Assert.assertTrue(avg < 20);
    }

    @Test
    public void testBenchmarkBuilderAsync() {
        long elapsed = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).async().run(3).join();

        Assert.assertTrue(elapsed >= 30);
        Assert.assertTrue(elapsed < 60);
    }

    @Test
    public void testBenchmarkBuilderAsyncWithCustomExecutor() {
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors
                .newSingleThreadExecutor();
        try {
            long elapsed = Benchmark.measure(() -> {
                try {
                    Thread.sleep(10);
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).in(TimeUnit.MILLISECONDS).async(executor).run(3).join();

            Assert.assertTrue(elapsed >= 30);
            Assert.assertTrue(elapsed < 60);
        }
        finally {
            executor.shutdown();
        }
    }

    @Test
    public void testBenchmarkBuilderAsyncWithWarmups() {
        CompletableFuture<Long> future = Benchmark.measure(() -> {
            try {
                Thread.sleep(10);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.MILLISECONDS).async().warmups(2).run(3);

        long elapsed = future.join();
        Assert.assertTrue(elapsed >= 30);
        Assert.assertTrue(elapsed < 60);
    }

    @Test
    public void testBenchmarkBuilderWithDifferentTimeUnit() {
        long elapsed = Benchmark.measure(() -> {
            try {
                Thread.sleep(1000);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).in(TimeUnit.SECONDS).run().join();

        Assert.assertTrue(elapsed >= 1);
        Assert.assertTrue(elapsed < 2);
    }
}
