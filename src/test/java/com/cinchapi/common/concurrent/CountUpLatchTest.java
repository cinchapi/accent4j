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
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;

/**
 * Unit tests for {@link CountUpLatch}
 *
 * @author Jeff Nelson
 */
public class CountUpLatchTest {
    
    @Test
    public void testCountUp() {
        CountUpLatch latch = new CountUpLatch();
        for(int i = 0; i < Math.abs(new Random().nextInt()); ++i) {
            latch.countUp();
            Assert.assertEquals(i+1, latch.getCount());
        }
    }
    
    @Test
    public void testCountUpAndAwait() throws InterruptedException {
        CountUpLatch latch = new CountUpLatch();
        AtomicBoolean run = new AtomicBoolean(true);
        List<String> list = Lists.newArrayList();
        Thread counter = new Thread(() ->  {
            while(run.get()) {
                list.add("a");
                latch.countUp();
            }
        });
        
        Thread awaiter = new Thread(() -> {
            try {
                latch.await(100);
                list.add("b");
                run.set(false);
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        awaiter.start();
        Thread.sleep(100);
        counter.start();
        latch.await(100);
        Assert.assertEquals("a", list.get(0));
        Assert.assertTrue(list.contains("a"));
    }
    
    @Test
    public void testAwaitAfterTheFact() throws InterruptedException {
        AtomicBoolean run = new AtomicBoolean(true);
        CountUpLatch latch = new CountUpLatch();
        CountDownLatch signal = new CountDownLatch(1);
        AtomicInteger count = new AtomicInteger(0);
        Thread t = new Thread(() ->  {
            while(run.get()) {
                latch.countUp();
                count.incrementAndGet();
                if(count.get() > 200) {
                    signal.countDown();
                }
            }
        });
        t.start();
        signal.await();
        latch.await(100);
        Assert.assertTrue(latch.getCount() > 100);
        run.set(false);
    }
    
    @Test
    public void testOneAwaitDoesntAffectOthers() throws InterruptedException {
        CountUpLatch latch = new CountUpLatch();
        CountDownLatch signal = new CountDownLatch(1);
        AtomicBoolean a = new AtomicBoolean(false);
        AtomicBoolean b = new AtomicBoolean(false);
        Thread t1 = new Thread(() ->  {
            try {
                latch.await(10);
                a.set(true);
                signal.countDown();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.setDaemon(true);
        t1.start();
        Thread t2 = new Thread(() ->  {
            try {
                latch.await(5);
                b.set(true);
                signal.countDown();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t2.setDaemon(true);
        t2.start();
        for(int i = 0; i < 5; ++i) {
            latch.countUp();
        }
        signal.await();
        Assert.assertTrue(b.get());
        Assert.assertFalse(a.get());
    }
    
    @Test
    public void testAwaitNotSignaledUntilCountIsReached() throws InterruptedException {
        CountUpLatch latch = new CountUpLatch();
        CountDownLatch signal = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);
        Thread t1 = new Thread(() -> {
            try {
                latch.await(2);
                signal.countDown();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.setDaemon(true);
        t1.start();
        latch.countUp();
        Assert.assertEquals(1, latch.getCount());
        Assert.assertFalse(failed.get());
    }
    
    @Test
    public void testAwaitSignaledIfCountIsReached() throws InterruptedException {
        CountUpLatch latch = new CountUpLatch();
        CountDownLatch signal = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(true);
        Thread t1 = new Thread(() -> {
            try {
                latch.await(2);
                failed.set(false);
                signal.countDown();
            }
            catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        t1.setDaemon(true);
        t1.start();
        latch.countUp();
        latch.countUp();
        signal.await();
        Assert.assertFalse(failed.get());
    }

}
