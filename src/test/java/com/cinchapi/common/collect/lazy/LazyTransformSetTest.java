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

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

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
        LazyTransformSet<Integer, String> transformed = LazyTransformSet.of(original, o -> {
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
        LazyTransformSet<Integer, String> transformed = LazyTransformSet.of(original, o -> {
            count.incrementAndGet();
            return o.toString();
        });
        transformed.stream().skip(3).skip(4).limit(4).forEach(System.out::println);
        Assert.assertEquals(3, count.get());
    }

}
