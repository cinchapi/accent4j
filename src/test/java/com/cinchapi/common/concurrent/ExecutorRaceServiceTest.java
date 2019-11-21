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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 *
 * @author jeff
 */
public class ExecutorRaceServiceTest {

    @SuppressWarnings("unchecked")
    @Test
    public void testRaceReturnsFastest() throws InterruptedException, ExecutionException {
        Callable<Integer> a = () -> {
            Thread.sleep(1000);
            return 1;
        };
        Callable<Integer> b = () -> {
            Thread.sleep(500);
            return 2;
        };
        Callable<Integer> c = () -> {
            Thread.sleep(1500);
            return 3;
        };
        ExecutorRaceService<Integer> ers = new ExecutorRaceService<>(Executors.newCachedThreadPool());
        Future<Integer> future = ers.race(a, b,c);
        Assert.assertTrue(future.isDone());
        int expected = 2;
        int actual = future.get();
        Assert.assertEquals(expected, actual);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRaceWithHeadStart() throws InterruptedException, ExecutionException {
        Callable<Integer> a = () -> {
            Thread.sleep(1000);
            return 1;
        };
        Callable<Integer> b = () -> {
            Thread.sleep(500);
            return 2;
        };
        Callable<Integer> c = () -> {
            Thread.sleep(1500);
            return 3;
        };
        ExecutorRaceService<Integer> ers = new ExecutorRaceService<>(Executors.newCachedThreadPool());
        Future<Integer> future = ers.raceWithHeadStart(1, TimeUnit.SECONDS, a, b,c);
        Assert.assertTrue(future.isDone());
        int expected = 1;
        int actual = future.get();
        Assert.assertEquals(expected, actual);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRaceWithHeadStartAndStillLose() throws InterruptedException, ExecutionException {
        Callable<Integer> a = () -> {
            Thread.sleep(1000);
            return 1;
        };
        Callable<Integer> b = () -> {
            Thread.sleep(500);
            return 2;
        };
        Callable<Integer> c = () -> {
            Thread.sleep(1500);
            return 3;
        };
        ExecutorRaceService<Integer> ers = new ExecutorRaceService<>(Executors.newCachedThreadPool());
        Future<Integer> future = ers.raceWithHeadStart(500, TimeUnit.MILLISECONDS, c,b,a);
        Assert.assertTrue(future.isDone());
        int expected = 2;
        int actual = future.get();
        Assert.assertEquals(expected, actual);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testRaceDoesNotRunCallableIfHeadStartPrevails() throws InterruptedException, ExecutionException {
        AtomicBoolean run = new AtomicBoolean(false);
        Callable<Integer> a = () -> {
            Thread.sleep(1000);
            run.set(true);
            return 1;
        };
        Callable<Integer> b = () -> {
            Thread.sleep(500);
            return 2;
        };
        Callable<Integer> c = () -> {
            Thread.sleep(1500);
            run.set(true);
            return 3;
        };
        ExecutorRaceService<Integer> ers = new ExecutorRaceService<>(Executors.newCachedThreadPool());
        Future<Integer> future = ers.raceWithHeadStart(1000, TimeUnit.MILLISECONDS, b,a,c);
        Assert.assertTrue(future.isDone());
        int expected = 2;
        int actual = future.get();
        Assert.assertEquals(expected, actual);
        Assert.assertFalse(run.get());
        Thread.sleep(1500);
        Assert.assertFalse(run.get());
    }

}
