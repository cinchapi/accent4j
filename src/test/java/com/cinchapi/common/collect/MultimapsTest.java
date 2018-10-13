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

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

/**
 * Unit tests for {@link Multimaps}.
 *
 * @author Jeff Nelson
 */
public class MultimapsTest {

    @Test
    public void testAsMapWithSingleValueWherePossible() {
        Multimap<String, Object> mmap = LinkedHashMultimap.create();
        mmap.put("foo", 1);
        mmap.put("bar", 2);
        mmap.put("bar", 3);
        mmap.put("car", null);
        Map<String, Object> map = Multimaps
                .asMapWithSingleValueWherePossible(mmap);
        Assert.assertEquals(1, map.get("foo"));
        Assert.assertEquals(ImmutableSet.of(2, 3), map.get("bar"));
    }

}
