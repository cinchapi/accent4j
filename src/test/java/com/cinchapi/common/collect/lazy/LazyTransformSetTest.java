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
package com.cinchapi.common.collect.lazy;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Unit tests for {@link LazyTransformSet} class.
 *
 * @author Jeff Nelson
 */
public class LazyTransformSetTest {

    @Test
    public void testLazyStreamSkip() {
        Set<Integer> original = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger count = new AtomicInteger(0);
        LazyTransformSet<Integer, String> transformed = LazyTransformSet
                .of(original, o -> {
                    count.incrementAndGet();
                    return o.toString();
                });
        transformed.stream().skip(3).limit(4).forEach(System.out::println);
        Assert.assertEquals(4, count.get());
    }

    @Test
    public void testLazyStreamDoubleSkip() {
        Set<Integer> original = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger count = new AtomicInteger(0);
        LazyTransformSet<Integer, String> transformed = LazyTransformSet
                .of(original, o -> {
                    count.incrementAndGet();
                    return o.toString();
                });
        transformed.stream().skip(3).skip(4).limit(4)
                .forEach(System.out::println);
        Assert.assertEquals(3, count.get());
    }

    @Test
    public void testLazyTransformOnlyHappensOnce() {
        Set<Integer> original = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger count = new AtomicInteger(0);
        Set<String> transformed = LazyTransformSet.of(original, o -> {
            count.incrementAndGet();
            return o.toString();
        });
        transformed = Sets.filter(transformed, i -> !i.equals("1"));
        Iterator<String> it = transformed.iterator();
        while (it.hasNext()) {
            it.next();
        }
        it = transformed.iterator();
        while (it.hasNext()) {
            it.next();
        }
        Assert.assertEquals(original.size(), count.get());
    }

    @Test
    public void testLazyTransformOnlyHappensOnceWithSkip() {
        Set<Integer> original = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger count = new AtomicInteger(0);
        Set<String> transformed = LazyTransformSet.of(original, o -> {
            count.incrementAndGet();
            return o.toString();
        });
        transformed.stream().skip(5).collect(Collectors.toList());
        Iterator<String> it = transformed.iterator();
        while (it.hasNext()) {
            it.next();
        }
        it = transformed.iterator();
        while (it.hasNext()) {
            it.next();
        }
        Assert.assertEquals(original.size(), count.get());
    }

    @Test
    public void testLazyTransformThreadSafetyDataIntegrity() throws InterruptedException {
        Set<Integer> original = ImmutableSet.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        AtomicInteger count = new AtomicInteger(0);
        Set<String> transformed = LazyTransformSet.of(original, o -> {
            count.incrementAndGet();
            return o.toString();
        });
        AtomicReference<Collection<String>> a0 = new AtomicReference<>();
        AtomicReference<Collection<String>> b0 = new AtomicReference<>();
        AtomicReference<Collection<String>> c0 = new AtomicReference<>();
        AtomicReference<Collection<String>> d0 = new AtomicReference<>();
        Thread a = new Thread(() -> {
            a0.set(transformed.stream().collect(Collectors.toSet()));
        });
        Thread b = new Thread(() -> {
            b0.set(transformed.stream().collect(Collectors.toSet()));
        });
        Thread c = new Thread(() -> {
            c0.set(transformed.stream().collect(Collectors.toSet()));
        });
        Thread d = new Thread(() -> {
            d0.set(transformed.stream().collect(Collectors.toSet()));
        });
        a.start();
        b.start();
        c.start();
        d.start();
        a.join();
        b.join();
        c.join();
        d.join();
        Assert.assertEquals(a0.get(), transformed);
        Assert.assertEquals(b0.get(), transformed);
        Assert.assertEquals(c0.get(), transformed);
        Assert.assertEquals(d0.get(), transformed);
    }
    
    @Test
    public void testEmptyLazyTransformSet() {
        Set<String> set = LazyTransformSet.of(ImmutableSet.of(), o -> o.toString());
        Assert.assertTrue(set.isEmpty());
    }

}
