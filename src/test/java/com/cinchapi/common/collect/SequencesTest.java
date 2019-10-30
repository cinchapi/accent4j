/*
 * Copyright (c) 2013-2018 Cinchapi Inc.
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
package com.cinchapi.common.collect;

import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

/**
 * Unit tests for {@link Sequences}
 *
 * @author Jeff Nelson
 */
public class SequencesTest {

    @Test
    public void testIsSequence() {
        Assert.assertTrue(Sequences.isSequence(ImmutableList.of("foo")));
        Assert.assertTrue(Sequences.isSequence(ImmutableSet.of("foo")));
        Assert.assertFalse(Sequences.isSequence(ImmutableMap.of("foo", "foo")));
        Assert.assertTrue(
                Sequences.isSequence(ImmutableList.of("foo").toArray()));
    }

    @Test
    public void testForEachArray() {
        int[] sequence = { 2, 3, 9, 1, 10, 110, 1 };
        int expected = 0;
        for (int i = 0; i < sequence.length; ++i) {
            int item = sequence[i];
            expected += item;
        }
        AtomicInteger actual = new AtomicInteger(0);
        Sequences.<Integer> forEach(sequence, item -> actual.addAndGet(item));
        Assert.assertEquals(expected, actual.get());
    }

    @Test
    public void testContainsArray() {
        int[] sequence = { 2, 3, 9, 1, 10, 110, 1 };
        Assert.assertTrue(Sequences.contains(sequence, 9));
        Assert.assertFalse(Sequences.contains(sequence, 90));
    }

    @Test
    public void testContainsIterable() {
        Set<Integer> sequence = Sets
                .newHashSet(new Integer[] { 2, 3, 9, 1, 10, 110, 1 });
        Assert.assertTrue(Sequences.contains(sequence, 9));
        Assert.assertFalse(Sequences.contains(sequence, 90));
    }
    
    @Test
    public void testIsSequenceNullSafe() {
        Assert.assertFalse(Sequences.isSequence(null));
    }

}
