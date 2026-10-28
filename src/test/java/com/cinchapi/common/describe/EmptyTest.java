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
package com.cinchapi.common.describe;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * Unit test for {@link Empty}.
 *
 * @author Jeff Nelson
 */
public class EmptyTest {
    
    @Test
    public void testEmptyUseClosestCommonAncestor() {
        Empty empty = Empty.is();
        empty.define(HashSet.class, object -> true);
        empty.define(Set.class, object -> false);
        empty.define(Collection.class, object -> true);
        Assert.assertTrue(empty.describes(Sets.newHashSet()));
        Assert.assertFalse(empty.describes(Sets.newTreeSet()));
        Assert.assertTrue(empty.describes(Lists.newArrayList()));
    }
    
    @Test
    public void testEmptyNull() {
        Empty empty = Empty.is();
        Assert.assertTrue(empty.describes(null));
    }
    
    @Test
    public void testEmptyDefaults() {
        Empty empty = Empty.is();
        Assert.assertTrue(empty.describes(""));
        Assert.assertTrue(empty.describes(Lists.newArrayList()));
        Assert.assertTrue(empty.describes(new String[] {}));
    }

    /**
     * <strong>Goal:</strong> Verify that a definition added to one
     * {@link Empty} does not affect another one.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get two default instances with {@link Empty#ness()}.</li>
     * <li>Define every {@link Integer} as empty in the first instance.</li>
     * <li>Ask each instance whether it describes a random integer.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first instance describes the integer, and
     * the second one does not.
     */
    @Test
    public void testDefineDoesNotAffectAnotherInstance() {
        Empty first = Empty.ness();
        Empty second = Empty.ness();
        first.define(Integer.class, integer -> true);
        int value = new Random().nextInt();
        Assert.assertTrue(first.describes(value));
        Assert.assertFalse(second.describes(value));
    }

    /**
     * <strong>Goal:</strong> Verify that a definition added to one
     * {@link Empty} does not change the defaults that later instances start
     * with.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#is()}.</li>
     * <li>Replace its definition for a {@link String} with one that never
     * describes a {@link String}.</li>
     * <li>Get new default instances with {@link Empty#is()} and
     * {@link Empty#ness()}.</li>
     * <li>Ask each instance whether it describes an empty {@link String}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The changed instance does not describe the
     * empty {@link String}, and each new instance does.
     */
    @Test
    public void testDefineDoesNotAffectLaterDefaultInstances() {
        Empty changed = Empty.is();
        changed.define(String.class, string -> false);
        Assert.assertFalse(changed.describes(""));
        Assert.assertTrue(Empty.is().describes(""));
        Assert.assertTrue(Empty.ness().describes(""));
    }

    /**
     * <strong>Goal:</strong> Verify that the default {@link Empty} describes a
     * {@link String} only when it has no characters.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#ness()}.</li>
     * <li>Ask whether it describes an empty {@link String} and a random
     * non-empty one.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes the empty {@link String} and not
     * the non-empty one.
     */
    @Test
    public void testDescribesStringOnlyWhenEmpty() {
        Empty empty = Empty.ness();
        Assert.assertTrue(empty.describes(""));
        Assert.assertFalse(empty.describes(UUID.randomUUID().toString()));
    }

    /**
     * <strong>Goal:</strong> Verify that the default {@link Empty} describes an
     * {@link Iterable} of any class only when it has no elements.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#ness()}.</li>
     * <li>Ask whether it describes an empty list, a list that holds a random
     * integer, and an {@link Iterable} that is not a {@link Collection} and
     * holds that integer.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes the empty list and neither of the
     * others.
     */
    @Test
    public void testDescribesIterableOnlyWhenEmpty() {
        Empty empty = Empty.ness();
        int value = new Random().nextInt();
        Iterable<Integer> iterable = () -> Lists.newArrayList(value)
                .iterator();
        Assert.assertTrue(empty.describes(Lists.newArrayList()));
        Assert.assertFalse(empty.describes(Lists.newArrayList(value)));
        Assert.assertFalse(empty.describes(iterable));
    }

    /**
     * <strong>Goal:</strong> Verify that the default {@link Empty} describes a
     * {@link java.util.Map Map} only when it has no entries.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#ness()}.</li>
     * <li>Ask whether it describes an empty map and a map with one random
     * entry.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes the empty map and not the other
     * one.
     */
    @Test
    public void testDescribesMapOnlyWhenEmpty() {
        Empty empty = Empty.ness();
        Assert.assertTrue(empty.describes(Maps.newHashMap()));
        Assert.assertFalse(empty.describes(ImmutableMap
                .of(UUID.randomUUID().toString(), new Random().nextInt())));
    }

    /**
     * <strong>Goal:</strong> Verify that the default {@link Empty} describes an
     * array only when it has no elements.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#ness()}.</li>
     * <li>Ask whether it describes an empty array and an array that holds a
     * random integer.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes the empty array and not the other
     * one.
     */
    @Test
    public void testDescribesArrayOnlyWhenEmpty() {
        Empty empty = Empty.ness();
        Assert.assertTrue(empty.describes(new int[] {}));
        Assert.assertFalse(
                empty.describes(new int[] { new Random().nextInt() }));
    }

    /**
     * <strong>Goal:</strong> Verify that the default {@link Empty} does not
     * describe an object whose class has no definition, directly or through an
     * ancestor.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#ness()}.</li>
     * <li>Ask whether it describes a random integer and a plain
     * {@link Object}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes neither.
     */
    @Test
    public void testDoesNotDescribeObjectWithoutDefinition() {
        Empty empty = Empty.ness();
        Assert.assertFalse(empty.describes(new Random().nextInt()));
        Assert.assertFalse(empty.describes(new Object()));
    }

    /**
     * <strong>Goal:</strong> Verify that an {@link Empty} built from one
     * definition describes objects by that definition alone.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an instance with
     * {@link Empty#is(Class, java.util.function.Predicate)} that defines
     * {@code 0} as an empty {@link Integer}.</li>
     * <li>Ask whether it describes {@code 0}, {@code 1} and an empty
     * {@link String}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes {@code 0} and neither of the
     * others.
     */
    @Test
    public void testIsWithDefinitionDescribesOnlyByThatDefinition() {
        Empty empty = Empty.is(Integer.class, value -> value == 0);
        Assert.assertTrue(empty.describes(0));
        Assert.assertFalse(empty.describes(1));
        Assert.assertFalse(empty.describes(""));
    }

    /**
     * <strong>Goal:</strong> Verify that a default {@link Empty} keeps its
     * default definitions after a definition is added to it.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get a default instance with {@link Empty#ness()}.</li>
     * <li>Define every {@link Integer} as empty in it.</li>
     * <li>Ask whether it describes an empty {@link String}, an empty list, an
     * empty map and a random non-empty {@link String}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> It describes each empty value and not the
     * non-empty {@link String}.
     */
    @Test
    public void testDefineKeepsDefaultDefinitions() {
        Empty empty = Empty.ness();
        empty.define(Integer.class, integer -> true);
        Assert.assertTrue(empty.describes(""));
        Assert.assertTrue(empty.describes(Lists.newArrayList()));
        Assert.assertTrue(empty.describes(Maps.newHashMap()));
        Assert.assertFalse(empty.describes(UUID.randomUUID().toString()));
    }

}
