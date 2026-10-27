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

import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

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
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        Assert.assertEquals(map.entrySet(), assoc.entrySet());
    }

    @Test
    public void testSetValue() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("a", "bar");
        Assert.assertEquals("bar", assoc.fetch("a"));
    }

    @Test
    public void testSetObjectCollectionValue() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("b.1.e", "bar");
        Assert.assertEquals(ImmutableMap.of("e", "bar"), assoc.fetch("b.1"));
        Assert.assertEquals(
                Lists.newArrayList("c", ImmutableMap.of("e", "bar")),
                assoc.fetch("b"));
    }

    @Test
    public void testSetObjectCollectionValue2() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("c.1.e", "bar");
        Assert.assertEquals("bar", assoc.fetch("c.1.e"));
        Assert.assertNull(assoc.fetch("c.0"));
        Assert.assertEquals(
                Lists.newArrayList(null, ImmutableMap.of("e", "bar")),
                assoc.fetch("c"));
    }

    @Test
    public void testSetObjectCollectionValue3() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("d.e", "bar");
        Assert.assertEquals("bar", assoc.fetch("d.e"));
    }

    @Test
    public void testSetObjectCollectionValue4() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("d.1.e", "bar");
        Assert.assertNull(assoc.fetch("d.e"));
    }

    @Test
    public void testSetObjectValue() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("d.e", "bar");
        Assert.assertEquals("bar", assoc.fetch("d.e"));
        Assert.assertEquals(ImmutableMap.of("e", "bar"), assoc.fetch("d"));
    }

    @Test
    public void testSetObjectObject() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        assoc.set("d", ImmutableMap.of("e", "bar"));
        Assert.assertEquals("bar", assoc.fetch("d.e"));
        Assert.assertEquals(ImmutableMap.of("e", "bar"), assoc.fetch("d"));
    }

    @Test
    public void testPutIsSameAsSet() {
        Association assoc1 = Association.of();
        Association assoc2 = Association.of();
        assoc1.put("a.1.b", ImmutableMap.of("c", "d"));
        assoc2.set("a.1.b", ImmutableMap.of("c", "d"));
        Assert.assertEquals("d", assoc1.fetch("a.1.b.c"));
        Assert.assertEquals("d", assoc2.fetch("a.1.b.c"));
    }

    @Test
    public void testPaths() {
        Map<String, Object> map = ImmutableMap.of("a", "foo", "b",
                ImmutableList.of("c", "d"), "d", ImmutableMap.of("e", "foo"),
                "e",
                ImmutableMap
                        .of("f", ImmutableMap.of("g", ImmutableList.of("foo"))),
                "f",
                ImmutableList.of(ImmutableMap.of("h", "foo", "i", "z"),
                        ImmutableList.of(ImmutableMap.of("i",
                                ImmutableList.of("bar", "baz")))));
        Association assoc = Association.of(map);
        System.out.println(assoc.paths());
    }

    @Test
    public void testSetNullValue() {
        Association assoc = Association.of();
        assoc.set("foo", null);
        Assert.assertTrue(true); // lack of Exception means we pass
    }

    @Test
    public void testMergeConcatStrategy() {
        Association assoc = Association.of();
        List<String> values = ImmutableList.of("a", "b", "c", "d", "e");
        values.forEach(value -> assoc.merge(ImmutableMap.of("foo", value),
                MergeStrategies::concat));
        Assert.assertEquals(ImmutableMap.of("foo",
                ImmutableList.of("a", "b", "c", "d", "e")), assoc);
    }
    
    @Test
    public void testFetchOrDefault() {
        Assert.assertEquals("foo", Association.of().fetchOrDefault("foo.bar.1.baz", "foo"));
    }
    
    @Test
    public void testAssociationOfAssociation() {
        Association assoc = Association.of();
        assoc.set("a.b", 1);
        assoc.set("b", true);
        assoc.set("a.b.1", false);
        assoc.set("foo", "bar");
        Association assoc2 = Association.of(assoc);
        Assert.assertEquals(assoc, assoc2);
        int hashCode = assoc2.hashCode();
        assoc.set("foo", "FOO");
        Assert.assertNotEquals(assoc, assoc2);
        Assert.assertEquals(hashCode, assoc2.hashCode());
        Assert.assertNotEquals(hashCode, assoc.hashCode());
    }
    
    @Test
    public void testAssociationEnsure() {
        Association assoc = Association.of();
        Map<String, Object> map = ImmutableMap.of();
        Association a = Association.ensure(assoc);
        Association b = Association.ensure(map);
        Assert.assertSame(assoc, a);
        Assert.assertNotSame(assoc, b);
    }

}
