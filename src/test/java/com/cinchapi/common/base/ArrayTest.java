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
package com.cinchapi.common.base;

import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;


/**
 * Unit tests for {@link Array} class.
 *
 * @author Jeff Nelson
 */
public class ArrayTest {
    
    @Test
    public void testReverseInPlace() {
        Random rand = new Random();
        int count = Math.abs(rand.nextInt()) % 100 + 1;
        ArrayBuilder<Integer> ab = ArrayBuilder.builder();
        for(int i = 0; i < count; ++i) {
            ab.add(rand.nextInt());
        }
        Integer[] array = ab.build();
        List<Integer> expected0 = Lists.newArrayList(array);
        Collections.reverse(expected0);
        Integer[] expected = expected0.toArray(Array.containing());
        Array.reverseInPlace(array);
        Assert.assertArrayEquals(expected, array);
    }
    
    @Test
    public void testReverse() {
        Random rand = new Random();
        int count = Math.abs(rand.nextInt()) % 100 + 1;
        ArrayBuilder<Integer> ab = ArrayBuilder.builder();
        for(int i = 0; i < count; ++i) {
            ab.add(rand.nextInt());
        }
        Integer[] array = ab.build();
        List<Integer> expected0 = Lists.newArrayList(array);
        Collections.reverse(expected0);
        Integer[] expected = expected0.toArray(Array.containing());
        Assert.assertArrayEquals(expected, Array.reverse(array));
    }

}
