/*
 * Copyright (c) 2016 Cinchapi Inc.
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

import java.util.Random;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

public class ArrayBuilderTest {

    @Test
    public void testSize() {
        int size = new Random().nextInt(200) + 1;
        ArrayBuilder<UUID> builder = ArrayBuilder.builder();
        for (int i = 0; i < size; ++i) {
            builder.add(UUID.randomUUID());
        }
        UUID[] uuids = builder.build();
        Assert.assertEquals(size, uuids.length);
    }
    
    @Test(expected = IllegalStateException.class)
    public void testBuildEmptyArray(){
        ArrayBuilder<UUID> builder = ArrayBuilder.builder();
        UUID[] uuids = builder.build();
        Assert.assertEquals(0, uuids.length);
    }
    
    @Test
    public void testBuildArrayWithSubclass() {
        ArrayBuilder<Number> builder = ArrayBuilder.builder();
        builder.add(1);
        builder.add(2L);
        Number[] nums = builder.build();
        Assert.assertTrue(nums[0] instanceof Integer);
        Assert.assertTrue(nums[1] instanceof Long);
    }
}
