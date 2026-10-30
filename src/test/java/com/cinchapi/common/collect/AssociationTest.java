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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import java.util.function.Supplier;

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

    /**
     * Return a list of {@value #LONG_LIST_SIZE} elements that the
     * {@code element} function makes from each position.
     *
     * @param element the function that makes the element at a position
     * @return the list
     */
    private static List<Object> longList(IntFunction<Object> element) {
        List<Object> list = new ArrayList<>(LONG_LIST_SIZE);
        for (int i = 0; i < LONG_LIST_SIZE; ++i) {
            list.add(element.apply(i));
        }
        return list;
    }

    /**
     * Return {@code {a=[[1, 2]], a.0.2=3, a.0=null, a.0.3=4}}. Its flattened
     * entries build a nested list, append to it, write {@code null} at the path
     * of the nested list, which keeps it, and then append to it again.
     *
     * @return the input, in a map that keeps its order and allows {@code null}
     */
    private static Map<String, Object> nestedListInput() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", ImmutableList.of(ImmutableList.of(1, 2)));
        input.put("a.0.2", 3);
        input.put("a.0", null);
        input.put("a.0.3", 4);
        return input;
    }

    /**
     * The number of elements in the lists of the tests that bound the time to
     * build a long list.
     */
    private static final int LONG_LIST_SIZE = 100000;

    @Test
    public void testAssociationEnsure() {
        Association assoc = Association.of();
        Map<String, Object> map = ImmutableMap.of();
        Association a = Association.ensure(assoc);
        Association b = Association.ensure(map);
        Assert.assertSame(assoc, a);
        Assert.assertNotSame(assoc, b);
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
    public void testFetchOrDefault() {
        Assert.assertEquals("foo",
                Association.of().fetchOrDefault("foo.bar.1.baz", "foo"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} merges
     * a list of {@value #LONG_LIST_SIZE} elements into an empty
     * {@link Association} within 10 seconds.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association} from
     * {@link Association#of()}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build a map whose {@code tags} key holds a list of
     * {@value #LONG_LIST_SIZE} distinct strings.</li>
     * <li>Merge the map into the empty {@link Association}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call returns within 10 seconds, and
     * {@code tags} holds an {@link ArrayList} of the same strings in the same
     * order.
     */
    @Test(timeout = 10000)
    public void testMergeBuildsLongListWithinTimeout() {
        List<Object> tags = longList(i -> "tag" + i);
        Association assoc = Association.of();
        assoc.merge(ImmutableMap.of("tags", tags));
        Assert.assertEquals(ArrayList.class, assoc.get("tags").getClass());
        Assert.assertEquals(tags, assoc.get("tags"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} merges
     * a list of {@value #LONG_LIST_SIZE} elements into an empty subclass of
     * {@link Association} within 10 seconds, as when a subclass fills itself
     * from a map in its constructor.
     * <p>
     * <strong>Start state:</strong> An empty anonymous subclass whose suppliers
     * return {@link LinkedHashMap LinkedHashMaps} and
     * {@link ArrayList ArrayLists}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build a map whose {@code tags} key holds a list of
     * {@value #LONG_LIST_SIZE} distinct strings.</li>
     * <li>Merge the map into the subclass instance.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call returns within 10 seconds, and
     * {@code tags} holds the same strings in the same order.
     */
    @Test(timeout = 10000)
    public void testMergeBuildsLongListWithinTimeoutForSubclass() {
        List<Object> tags = longList(i -> "tag" + i);
        Association assoc = new Association() {

            @Override
            protected Supplier<Collection<Object>> collectionSupplier() {
                return ArrayList::new;
            }

            @Override
            protected Supplier<Map<String, Object>> mapSupplier() {
                return LinkedHashMap::new;
            }

        };
        assoc.merge(ImmutableMap.of("tags", tags));
        Assert.assertEquals(tags, assoc.get("tags"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} calls a
     * subclass's own {@code set} once for each flattened entry, in order.
     * <p>
     * <strong>Start state:</strong> An empty {@link ListAssociation} whose
     * {@code set} records each path before it writes.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Merge the {@link #nestedListInput() nested list input}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code set} received {@code a.0.0},
     * {@code a.0.1}, {@code a.0.2}, {@code a.0} and {@code a.0.3}, and
     * {@code a} holds {@code [[1, 2, 3, 4]]}.
     */
    @Test
    public void testMergeCallsOverriddenSetForEachEntry() {
        List<String> paths = new ArrayList<>();
        Association assoc = new ListAssociation() {

            @Override
            public <T> T set(String path, Object value) {
                paths.add(path);
                return super.set(path, value);
            }

        };
        assoc.merge(nestedListInput());
        Assert.assertEquals(
                ImmutableList.of("a.0.0", "a.0.1", "a.0.2", "a.0", "a.0.3"),
                paths);
        Assert.assertEquals(ImmutableList.of(ImmutableList.of(1, 2, 3, 4)),
                assoc.get("a"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} goes
     * through a subclass's own {@link Association#merge(Map, BiFunction)}.
     * <p>
     * <strong>Start state:</strong> An empty {@link ListAssociation} whose
     * {@code merge(Map, BiFunction)} records each map it receives.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Merge the {@link #nestedListInput() nested list input} with
     * {@code merge(Map)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code merge(Map, BiFunction)} received the
     * input exactly once.
     */
    @Test
    public void testMergeCallsOverriddenStrategyMerge() {
        List<Map<String, Object>> received = new ArrayList<>();
        Association assoc = new ListAssociation() {

            @Override
            public void merge(Map<String, Object> map,
                    BiFunction<Object, Object, Object> strategy) {
                received.add(map);
                super.merge(map, strategy);
            }

        };
        Map<String, Object> input = nestedListInput();
        assoc.merge(input);
        Assert.assertEquals(1, received.size());
        Assert.assertSame(input, received.get(0));
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

    /**
     * <strong>Goal:</strong> Verify that a list that a subclass's own
     * {@code fetch} returned during {@link Association#merge(Map)} keeps its
     * contents when a later entry writes into that path.
     * <p>
     * <strong>Start state:</strong> An empty {@link ListAssociation} whose
     * {@code fetch} records the value it returns for each path.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Merge the {@link #nestedListInput() nested list input}, whose
     * {@code a.0} entry reads the nested list {@code [1, 2, 3]} before
     * {@code a.0.3} writes into it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The list recorded for {@code a.0} is still
     * {@code [1, 2, 3]}, and {@code a.0} holds a new list {@code [1, 2, 3, 4]}.
     */
    @Test
    public void testMergeKeepsListThatOverriddenFetchReturned() {
        Map<String, Object> returned = new LinkedHashMap<>();
        Association assoc = new ListAssociation() {

            @Override
            public <T> T fetch(String path) {
                T value = super.fetch(path);
                returned.put(path, value);
                return value;
            }

        };
        assoc.merge(nestedListInput());
        Object held = returned.get("a.0");
        Assert.assertEquals(ImmutableList.of(1, 2, 3), held);
        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4), assoc.fetch("a.0"));
    }

    /**
     * <strong>Goal:</strong> Verify that a list that a subclass's own
     * {@code get} returned during {@link Association#merge(Map)} keeps its
     * contents when a later entry writes into that path.
     * <p>
     * <strong>Start state:</strong> An empty {@link ListAssociation} whose
     * {@code get} records the value it returns for each key.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Merge the {@link #nestedListInput() nested list input}, whose
     * {@code a.0} entry reads the nested list {@code [1, 2, 3]} before
     * {@code a.0.3} writes into it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The list recorded for {@code a.0} is still
     * {@code [1, 2, 3]}, and {@code a.0} holds a new list {@code [1, 2, 3, 4]}.
     */
    @Test
    public void testMergeKeepsListThatOverriddenGetReturned() {
        Map<Object, Object> returned = new LinkedHashMap<>();
        Association assoc = new ListAssociation() {

            @Override
            public Object get(Object key) {
                Object value = super.get(key);
                returned.put(key, value);
                return value;
            }

        };
        assoc.merge(nestedListInput());
        Object held = returned.get("a.0");
        Assert.assertEquals(ImmutableList.of(1, 2, 3), held);
        Assert.assertNotSame(held, assoc.fetch("a.0"));
        Assert.assertEquals(ImmutableList.of(1, 2, 3, 4), assoc.fetch("a.0"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} into an
     * existing list replaces the list, so a caller that held the list sees no
     * change, while it updates an existing map in place.
     * <p>
     * <strong>Start state:</strong> An {@link Association} built from
     * {@code {tags=[a, b], meta={x=1}}}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Hold the list at {@code tags} and the map at {@code meta}.</li>
     * <li>Merge {@code {tags=[b, c, d], meta={y=2}}}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code tags} is a new list {@code [b, c, d]}
     * and the held list is still {@code [a, b]}; {@code meta} is the held map,
     * which now holds {@code {x=1, y=2}}.
     */
    @Test
    public void testMergeReplacesHeldListAndUpdatesHeldMap() {
        Association assoc = Association.of(ImmutableMap.of("tags",
                ImmutableList.of("a", "b"), "meta", ImmutableMap.of("x", 1)));
        List<Object> tags = assoc.fetch("tags");
        Map<String, Object> meta = assoc.fetch("meta");
        assoc.merge(ImmutableMap.of("tags", ImmutableList.of("b", "c", "d"),
                "meta", ImmutableMap.of("y", 2)));
        Assert.assertNotSame(tags, assoc.fetch("tags"));
        Assert.assertEquals(ImmutableList.of("b", "c", "d"),
                assoc.fetch("tags"));
        Assert.assertEquals(ImmutableList.of("a", "b"), tags);
        Assert.assertSame(meta, assoc.fetch("meta"));
        Assert.assertEquals(ImmutableMap.of("x", 1, "y", 2), meta);
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} builds a
     * list of {@value #LONG_LIST_SIZE} maps within 10 seconds, which requires a
     * cost that grows linearly with the length of the list when each element is
     * a container.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build a map whose {@code items} key holds a list of
     * {@value #LONG_LIST_SIZE} maps, each with a {@code name} and an
     * {@code index}.</li>
     * <li>Call {@link Association#of(Map)} on the map.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call returns within 10 seconds, and
     * {@code items} holds maps equal to the originals, in the same order.
     */
    @Test(timeout = 10000)
    public void testOfBuildsLongListOfMapsWithinTimeout() {
        List<Object> items = longList(
                i -> ImmutableMap.of("name", "item" + i, "index", i));
        Association assoc = Association.of(ImmutableMap.of("items", items));
        Assert.assertEquals(items, assoc.get("items"));
        Assert.assertEquals("item7", assoc.fetch("items.7.name"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} builds a
     * list of {@value #LONG_LIST_SIZE} elements within 10 seconds, which
     * requires a cost that grows linearly with the length of the list.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build a map whose {@code tags} key holds a list of
     * {@value #LONG_LIST_SIZE} distinct strings.</li>
     * <li>Call {@link Association#of(Map)} on the map.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call returns within 10 seconds, and
     * {@code tags} holds an {@link ArrayList} of the same strings in the same
     * order.
     */
    @Test(timeout = 10000)
    public void testOfBuildsLongListWithinTimeout() {
        List<Object> tags = longList(i -> "tag" + i);
        Association assoc = Association.of(ImmutableMap.of("tags", tags));
        Assert.assertEquals(ArrayList.class, assoc.get("tags").getClass());
        Assert.assertEquals(tags, assoc.get("tags"));
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
    public void testPutIsSameAsSet() {
        Association assoc1 = Association.of();
        Association assoc2 = Association.of();
        assoc1.put("a.1.b", ImmutableMap.of("c", "d"));
        assoc2.set("a.1.b", ImmutableMap.of("c", "d"));
        Assert.assertEquals("d", assoc1.fetch("a.1.b.c"));
        Assert.assertEquals("d", assoc2.fetch("a.1.b.c"));
    }

    @Test
    public void testSetNullValue() {
        Association assoc = Association.of();
        assoc.set("foo", null);
        Assert.assertTrue(true); // lack of Exception means we pass
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

    /**
     * <strong>Goal:</strong> Verify that
     * {@link Association#set(String, Object)} into a list that
     * {@link Association#of(Map)} built replaces the list, so a caller that
     * held the list sees no change.
     * <p>
     * <strong>Start state:</strong> An {@link Association} built from
     * {@code {tags=[a, b, c]}}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Hold the list that {@code fetch("tags")} returns.</li>
     * <li>Call {@code set("tags.1", "z")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code tags} is a new list {@code [a, z, c]},
     * and the held list is still {@code [a, b, c]}.
     */
    @Test
    public void testSetReplacesListThatOfBuilt() {
        Association assoc = Association
                .of(ImmutableMap.of("tags", ImmutableList.of("a", "b", "c")));
        List<Object> held = assoc.fetch("tags");
        assoc.set("tags.1", "z");
        Assert.assertNotSame(held, assoc.fetch("tags"));
        Assert.assertEquals(ImmutableList.of("a", "z", "c"),
                assoc.fetch("tags"));
        Assert.assertEquals(ImmutableList.of("a", "b", "c"), held);
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

    /**
     * An {@link Association} with {@link LinkedHashMap LinkedHashMaps} and
     * {@link ArrayList ArrayLists}, for tests that override one of its methods.
     *
     * @author Jeff Nelson
     */
    private abstract static class ListAssociation extends Association {

        @Override
        protected Supplier<Collection<Object>> collectionSupplier() {
            return ArrayList::new;
        }

        @Override
        protected Supplier<Map<String, Object>> mapSupplier() {
            return LinkedHashMap::new;
        }

    }

}
