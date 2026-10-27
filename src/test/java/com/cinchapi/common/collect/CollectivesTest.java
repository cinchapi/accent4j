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

import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Unit tests for {@link Collectives}.
 *
 * @author Jeff Nelson
 */
@SuppressWarnings("deprecation")
public class CollectivesTest {

    @Test
    public void testCollectivize() {
        Map<String, Object> map = ImmutableMap.of("a",
                ImmutableMap.of("b", ImmutableMap.of("c",
                        ImmutableList.of(ImmutableMap.of("d", "foo")))));
        Map<String, Collection<Object>> mmap = Collectives.within(map);
        mmap.forEach((key, value) -> {
            Assert.assertTrue(value instanceof Collection);
        });
    }

    @Test
    public void testMergeMap() {
        Collection<Object> merged = Collectives.merge(
                ImmutableList.of(ImmutableMap.of("name", "Bar")),
                ImmutableList.of(ImmutableMap.of("description", "Bar Bar")));
        Assert.assertEquals(ImmutableList
                .of(ImmutableMap.of("name", "Bar", "description", "Bar Bar")),
                merged);
    }

}
