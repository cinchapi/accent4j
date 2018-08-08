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
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

/**
 * Unit tests for {@link AnyMaps}.
 *
 * @author Jeff Nelson
 */
public class AnyMapsTest {

    @Test
    public void testRename() {
        Map<String, Object> map = Maps.newHashMap();
        Object value = 17;
        map.put("foo", value);
        AnyMaps.rename("foo", "bar", map);
        Assert.assertFalse(map.containsKey("foo"));
        Assert.assertNull(map.get("foo"));
        Assert.assertTrue(map.containsKey("bar"));
        Assert.assertEquals(value, map.get("bar"));
    }

    @Test
    public void testNavigateWhenKeyPresent() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("foo", ImmutableMap.of("bar", ImmutableMap.of("baz", 1)));
        map.put("foo.bar.baz.car.bang", 2);
        Assert.assertEquals(1, (int) AnyMaps.navigate("foo.bar.baz", map));
        Assert.assertEquals(2,
                (int) AnyMaps.navigate("foo.bar.baz.car.bang", map));
        Assert.assertNull(AnyMaps.navigate("foo.bar.baz.car", map));
        Assert.assertNull(AnyMaps.navigate("foo.bar.baz.1", map));
        Assert.assertEquals(ImmutableMap.of("baz", 1),
                AnyMaps.navigate("foo.bar", map));
    }

    @Test
    public void testExplodeOneComponent() {
        Map<String, Object> map = ImmutableMap.of("a", 1);
        Assert.assertEquals(map, AnyMaps.explode(map));
    }

    @Test
    public void testExplode() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("d.1", 10);
        map.put("d.4", 11);
        map.put("d.2", 2);
        map.put("a.d.2.f", 7);
        map.put("a.b.c", 1);
        map.put("a.b.d", 2);
        map.put("a.b.e.0", 3);
        map.put("a.b.e.1", 4);
        map.put("c", 5);
        map.put("a.c.e", 6);
        Map<String, Object> exploded = AnyMaps.explode(map);
        System.out.println(exploded);
        Assert.assertEquals(10, (int) AnyMaps.navigate("d.1", exploded));
        Assert.assertEquals(11, (int) AnyMaps.navigate("d.4", exploded));
        Assert.assertEquals(2, (int) AnyMaps.navigate("d.2", exploded));
        Assert.assertEquals(7, (int) AnyMaps.navigate("a.d.2.f", exploded));
        Assert.assertEquals(1, (int) AnyMaps.navigate("a.b.c", exploded));
        Assert.assertEquals(2, (int) AnyMaps.navigate("a.b.d", exploded));
        Assert.assertEquals(3, (int) AnyMaps.navigate("a.b.e.0", exploded));
        Assert.assertEquals(4, (int) AnyMaps.navigate("a.b.e.1", exploded));
        Assert.assertEquals(5, (int) AnyMaps.navigate("c", exploded));
        Assert.assertEquals(6, (int) AnyMaps.navigate("a.c.e", exploded));
    }

    @Test
    public void testExplodeAndNavigateNested() {
        Map<String, Object> map = Maps.newHashMap();
        map.put("a.b.c", 10);
        map.put("a.b.b", 11);
        map.put("a.c.1", 12);
        Assert.assertEquals(
                ImmutableMap.of("b", ImmutableMap.of("c", 10, "b", 11), "c",
                        Lists.newArrayList(null, 12)),
                AnyMaps.navigate("a", AnyMaps.explode(map)));
        Assert.assertEquals(Lists.newArrayList(null, 12),
                AnyMaps.navigate("a.c", AnyMaps.explode(map)));
        Assert.assertEquals(ImmutableMap.of("c", 10, "b", 11),
                AnyMaps.navigate("a.b", AnyMaps.explode(map)));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testExplodeDoesNotAccceptLeadingNumericComponent() {
        AnyMaps.explode(ImmutableMap.of("1.a.b.c", 1));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void testCannotChangeParentComponentType() {
        AnyMaps.explode(ImmutableMap.of("a.b", 1, "a.1", 2));
    }
    
    @Test
    public void testExplodeDuplicateBugReproA() {
        Map<String, Object> map = ImmutableMap.of("AUTHORIZED_KEYS.0.KEY", "a", "AUTHORIZED_KEYS.0.TAG", "b");
        Map<String, Object> exploded = AnyMaps.explode(map);
        Assert.assertEquals(1, ((List<?>) exploded.get("AUTHORIZED_KEYS")).size());
    }
    
    @Test
    public void testToMultimap() {
        Map<String, Object> map = ImmutableMap.of("foo", "bar", "baz", ImmutableMap.of("baz", 1));
        Map<String, Collection<Object>> mmap = AnyMaps.toMultimap(map);
        mmap.forEach((key, value) -> {
            Assert.assertTrue(value instanceof Collection);
        });
    }
}
