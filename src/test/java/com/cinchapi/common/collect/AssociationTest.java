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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 *
 *
 * @author Jeff Nelson
 */
public class AssociationTest {
    
    @Test
    public void testCreateAssociation() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap.of("f",
                        ImmutableMap.of("g", ImmutableList.of("foo"))), "f",
                        ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                                ImmutableList.of(ImmutableMap.of("i",
                                        ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        Assert.assertEquals(map.entrySet(), assoc.entrySet());
    }

    @Test
    public void testSetA() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap.of("f",
                        ImmutableMap.of("g", ImmutableList.of("foo"))), "f",
                        ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                                ImmutableList.of(ImmutableMap.of("i",
                                        ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("a", "bar");
        assoc.set("e.f.g.1", "bar");
        Assert.assertEquals("bar", assoc.fetch("a"));
        System.out.println(assoc);
        System.out.println(assoc.flatten());
    }

}
