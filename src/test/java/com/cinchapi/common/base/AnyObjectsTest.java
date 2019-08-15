/*
 * Copyright (c) 2015 Cinchapi Inc.
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

import java.util.*;

import com.google.common.collect.ImmutableList;
import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.base.AnyObjects;

/**
 * Unit tests for {@link AnyObjects} functions.
 * 
 * @author Jeff Nelson
 */
@SuppressWarnings("deprecation")
public class AnyObjectsTest {

    @Test
    public void testIsNullOrEmptyString() {
        Assert.assertTrue(AnyObjects.isNullOrEmpty(""));
    }

    @Test
    public void testIsNullOrEmptyList() {
        Assert.assertTrue(AnyObjects.isNullOrEmpty(new ArrayList<Object>()));
    }

    @Test
    public void testIsNullOrEmptyMap() {
        Assert.assertTrue(AnyObjects
                .isNullOrEmpty(new HashMap<Object, Object>()));
        Assert.assertTrue(AnyObjects
                .isNullOrEmpty(new TreeMap<Object, Object>()));
    }

    @Test
    public void testIsNullOrEmptySet() {
        Assert.assertTrue(AnyObjects.isNullOrEmpty(new HashSet<Object>()));
        Assert.assertTrue(AnyObjects.isNullOrEmpty(new TreeSet<Object>()));
    }

    @Test
    public void testIsNullOrEmptyNull() {
        Assert.assertTrue(AnyObjects.isNullOrEmpty(null));
    }

    @Test
    public void split() {
        final List<String> result = AnyObjects.split(
                ImmutableList.of("One, Two", "Three", "Four, Five"));

        Assert.assertEquals(result.size(), 5);
        Assert.assertEquals(result.get(3), "Four");
        Assert.assertEquals(result.get(4), "Five");
    }

    @Test
    public void testFirstInstanceOf() {
        Integer a = 1;
        Assert.assertEquals(a, AnyObjects.firstInstanceOf(Number.class, "a",
                true, 1, 12, new Object()));
    }

}
