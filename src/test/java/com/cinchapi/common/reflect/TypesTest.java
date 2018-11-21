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
package com.cinchapi.common.reflect;

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Unit tests for {@link Types}.
 *
 * @author Jeff Nelson
 */
public class TypesTest {
    
    @Test
    public void testEnum() {
        Assert.assertEquals(CaseFormat.values()[2], Types.coerce(2, CaseFormat.class));
        Assert.assertEquals(CaseFormat.UPPER_CAMEL, Types.coerce("upper_camel", CaseFormat.class));
    }
    
    @Test
    public void testJsonObject() {
        String object = "{\"name\": \"Jeff Nelson\"}";
        Map<String, String> map = Types.coerce(object, Map.class);
        Assert.assertEquals(ImmutableMap.of("name", "Jeff Nelson"), map);
    }
    
    @Test
    public void testJsonArray() {
        String object = "[1, 2, 3, 4]";
        int[] array = Types.coerce(object, int[].class);
        Assert.assertArrayEquals(new int[] {1, 2, 3, 4}, array);
        
        List<Integer> list = Types.coerce(object, List.class);
        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4), list);
    }
    


}
