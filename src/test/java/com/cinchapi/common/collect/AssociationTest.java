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
import java.util.Collections;
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
     * Return a list of one more than {@value Association#MAX_LIST_GAP} copies
     * of the {@code empty} element, followed by the {@code last} element. When
     * {@code empty} is an empty container, flattening the list writes only the
     * {@code last} element, at a position past the limit.
     *
     * @param empty the element to repeat
     * @param last the element after the repeated ones
     * @return the list
     */
    private static List<Object> afterEmptyRun(Object empty, Object last) {
        List<Object> list = new ArrayList<>(
                Collections.nCopies(Association.MAX_LIST_GAP + 1, empty));
        list.add(last);
        return list;
    }

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
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} on a
     * subclass that overrides {@code set} counts the positions that a list of
     * the input supplies with empty elements as supplied, so that {@code set}
     * accepts the write past them.
     * <p>
     * <strong>Start state:</strong> An empty {@link ListAssociation} whose
     * {@code set} records each path before it writes.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Merge a map whose {@code a} entry is a list of one more than
     * {@value Association#MAX_LIST_GAP} empty maps, followed by {@code 1}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code set} received only the path of
     * {@code 1}, and {@code a} holds a {@code null} for each empty map,
     * followed by {@code 1}.
     */
    @Test
    public void testMergeCountsEmptyElementsAsSuppliedThroughOverriddenSet() {
        List<String> paths = new ArrayList<>();
        Association assoc = new ListAssociation() {

            @Override
            public <T> T set(String path, Object value) {
                paths.add(path);
                return super.set(path, value);
            }

        };
        assoc.merge(
                ImmutableMap.of("a", afterEmptyRun(ImmutableMap.of(), 1)));
        Assert.assertEquals(
                ImmutableList.of("a." + (Association.MAX_LIST_GAP + 1)),
                paths);
        Assert.assertEquals(afterEmptyRun(null, 1), assoc.fetch("a"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} and
     * {@link Association#merge(Map, BiFunction)} count the positions that a
     * list of the input supplies with empty elements as supplied.
     * <p>
     * <strong>Start state:</strong> Two empty {@link Association Associations}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build a map whose {@code a} entry is a list of one more than
     * {@value Association#MAX_LIST_GAP} empty maps, followed by {@code 1}.</li>
     * <li>Merge it into the first {@link Association}, and merge it into the
     * second with the {@link MergeStrategies#concat(Object, Object) concat}
     * strategy.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> In both, {@code a} holds a {@code null} for
     * each empty map, followed by {@code 1}.
     */
    @Test
    public void testMergeCountsEmptyElementsOfInputListAsSupplied() {
        Map<String, Object> input = ImmutableMap.of("a",
                afterEmptyRun(ImmutableMap.of(), 1));
        Association merged = Association.of();
        merged.merge(input);
        Association concatenated = Association.of();
        concatenated.merge(input, MergeStrategies::concat);
        Assert.assertEquals(afterEmptyRun(null, 1), merged.fetch("a"));
        Assert.assertEquals(afterEmptyRun(null, 1), concatenated.fetch("a"));
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
     * <strong>Goal:</strong> Verify that {@link Association#merge(Map)} rejects
     * an entry whose write would fill more than
     * {@value Association#MAX_LIST_GAP} list positions with {@code null}, and
     * keeps the entries that it wrote before that entry.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an ordered input whose {@code a} entry is {@code 1}, whose
     * {@code b} entry is a map keyed by one more than
     * {@value Association#MAX_LIST_GAP}, and whose {@code c} entry is
     * {@code 3}.</li>
     * <li>Merge the input.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The merge throws an
     * {@link IllegalArgumentException}, and the {@link Association} holds only
     * {@code {a=1}}.
     */
    @Test
    public void testMergeRejectsGapAndKeepsEarlierEntries() {
        Map<String, Object> input = new LinkedHashMap<>();
        input.put("a", 1);
        input.put("b", ImmutableMap
                .of(String.valueOf(Association.MAX_LIST_GAP + 1), 2));
        input.put("c", 3);
        Association assoc = Association.of();
        try {
            assoc.merge(input);
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals(ImmutableMap.of("a", 1), assoc);
        }
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

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} counts the
     * positions of list elements that hold only empty containers as supplied.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@link Association#of(Map)} on a map whose {@code a} entry is a
     * list of one more than {@value Association#MAX_LIST_GAP} copies of
     * {@code {tags=[]}}, followed by {@code {tags=[x]}}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code a} holds a {@code null} for each
     * {@code {tags=[]}}, followed by {@code {tags=[x]}}.
     */
    @Test
    public void testOfCountsElementsWithOnlyEmptyFieldsAsSupplied() {
        Map<String, Object> last = ImmutableMap.of("tags",
                ImmutableList.of("x"));
        Association assoc = Association.of(ImmutableMap.of("a", afterEmptyRun(
                ImmutableMap.of("tags", ImmutableList.of()), last)));
        Assert.assertEquals(afterEmptyRun(null, last), assoc.fetch("a"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} counts the
     * positions that a list of the input supplies with empty maps or empty
     * lists as supplied, so that a dense list of any length is accepted.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@link Association#of(Map)} on a map whose {@code a} entry is a
     * list of one more than {@value Association#MAX_LIST_GAP} empty maps,
     * followed by {@code 1}.</li>
     * <li>Do the same with empty lists instead of empty maps.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> In both, {@code a} holds a {@code null} for
     * each empty container, followed by {@code 1}.
     */
    @Test
    public void testOfCountsEmptyElementsOfInputListAsSupplied() {
        Association maps = Association.of(
                ImmutableMap.of("a", afterEmptyRun(ImmutableMap.of(), 1)));
        Association lists = Association.of(
                ImmutableMap.of("a", afterEmptyRun(ImmutableList.of(), 1)));
        Assert.assertEquals(afterEmptyRun(null, 1), maps.fetch("a"));
        Assert.assertEquals(afterEmptyRun(null, 1), lists.fetch("a"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} counts a
     * run of empty elements inside a list of the input as supplied.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@link Association#of(Map)} on a map whose {@code a} entry is
     * {@code 1}, then one more than {@value Association#MAX_LIST_GAP} empty
     * maps, then {@code 2}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code a} holds {@code 1}, a {@code null} for
     * each empty map, and {@code 2}.
     */
    @Test
    public void testOfCountsInteriorRunOfEmptyElementsAsSupplied() {
        List<Object> input = new ArrayList<>();
        input.add(1);
        input.addAll(afterEmptyRun(ImmutableMap.of(), 2));
        List<Object> expected = new ArrayList<>();
        expected.add(1);
        expected.addAll(afterEmptyRun(null, 2));
        Association assoc = Association.of(ImmutableMap.of("a", input));
        Assert.assertEquals(expected, assoc.fetch("a"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} rejects a
     * key whose integer component would fill billions of list positions with
     * {@code null}, instead of trying to build the list.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@link Association#of(Map)} on {@code {a.2147483646=1}}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call throws an
     * {@link IllegalArgumentException} within 10 seconds.
     */
    @Test(expected = IllegalArgumentException.class, timeout = 10000)
    public void testOfRejectsPathThatSkipsBillionsOfListPositions() {
        Association.of(ImmutableMap.of("a.2147483646", 1));
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

    /**
     * <strong>Goal:</strong> Verify that
     * {@link Association#set(String, Object)} counts the list positions that
     * every integer component of a path fills with {@code null}, and limits
     * their total to {@value Association#MAX_LIST_GAP}.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code x} at a path under {@code a} with two integer components
     * that each skip half of {@value Association#MAX_LIST_GAP}, rounded
     * down.</li>
     * <li>Set {@code y} at a path under {@code b} with two integer components
     * that each skip one more position than that.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first write succeeds; the second throws an
     * {@link IllegalArgumentException}, and {@code b} holds nothing.
     */
    @Test
    public void testSetCountsSkippedPositionsOfEveryComponent() {
        int half = Association.MAX_LIST_GAP / 2;
        Association assoc = Association.of();
        assoc.set("a." + half + "." + half, "x");
        Assert.assertEquals("x", assoc.fetch("a." + half + "." + half));
        try {
            assoc.set("b." + (half + 1) + "." + (half + 1), "y");
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertNull(assoc.fetch("b"));
        }
    }

    /**
     * <strong>Goal:</strong> Verify that
     * {@link Association#set(String, Object)} measures the positions that a
     * write skips from the end of the existing list, and leaves the
     * {@link Association} unchanged when it rejects a write.
     * <p>
     * <strong>Start state:</strong> An {@link Association} built from
     * {@code {tags=[a, b]}}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Hold the list at {@code tags}.</li>
     * <li>Set {@code x} at the position that skips one more than
     * {@value Association#MAX_LIST_GAP} positions past the end of the
     * list.</li>
     * <li>Set {@code x} at the position that skips exactly
     * {@value Association#MAX_LIST_GAP} positions past the end of the
     * list.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first write throws an
     * {@link IllegalArgumentException} and {@code tags} is still the held list
     * {@code [a, b]}; the second write succeeds and {@code tags} holds
     * {@code a}, {@code b}, {@value Association#MAX_LIST_GAP} {@code null}
     * elements and {@code x}.
     */
    @Test
    public void testSetMeasuresGapFromEndOfExistingList() {
        Association assoc = Association
                .of(ImmutableMap.of("tags", ImmutableList.of("a", "b")));
        List<Object> tags = assoc.fetch("tags");
        int last = 2 + Association.MAX_LIST_GAP;
        try {
            assoc.set("tags." + (last + 1), "x");
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertSame(tags, assoc.fetch("tags"));
            Assert.assertEquals(ImmutableList.of("a", "b"), tags);
        }
        assoc.set("tags." + last, "x");
        List<Object> expected = new ArrayList<>();
        expected.add("a");
        expected.add("b");
        expected.addAll(Collections.nCopies(Association.MAX_LIST_GAP, null));
        expected.add("x");
        Assert.assertEquals(expected, assoc.fetch("tags"));
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

    /**
     * <strong>Goal:</strong> Verify that an {@link Association} refuses a key
     * that names one component and reads as an integer, because the map cannot
     * hold a key whose first component is an integer.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "3"} to {@code "value"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The write throws an
     * {@link IllegalArgumentException} and the {@link Association} stays empty.
     */
    @Test
    public void testSetRefusesIntegerKeyWithOneComponent() {
        Association assoc = Association.of();
        try {
            assoc.set("3", "value");
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertTrue(assoc.isEmpty());
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a fetch of a key that names one
     * component and was never written answers {@code null}.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding one
     * unrelated key.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "present"} to {@code 1}.</li>
     * <li>Fetch {@code "absent"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The fetch answers {@code null}.
     */
    @Test
    public void testFetchOfAbsentKeyWithOneComponentIsNull() {
        Association assoc = Association.of();
        assoc.set("present", 1);

        Assert.assertNull(assoc.fetch("absent"));
    }

    /**
     * <strong>Goal:</strong> Verify that a write to a key that names one
     * component answers what that key held before, and that reading the key
     * answers what the write stored.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "name"} to {@code "first"} and keep what it
     * answers.</li>
     * <li>Set {@code "name"} to {@code "second"} and keep what it
     * answers.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first write answers {@code null}, the
     * second answers {@code "first"}, and the key then reads as
     * {@code "second"}.
     */
    @Test
    public void testSetOfKeyWithOneComponentAnswersThePreviousValue() {
        Association assoc = Association.of();

        Object first = assoc.set("name", "first");
        Object second = assoc.set("name", "second");

        Assert.assertNull(first);
        Assert.assertEquals("first", second);
        Assert.assertEquals("second", assoc.fetch("name"));
    }

    /**
     * <strong>Goal:</strong> Verify that a search for a value answers for an
     * {@link Association} that holds a {@code null} value.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding one key
     * whose value is {@code null} and one key whose value is not.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Search for the value that the second key holds.</li>
     * <li>Search for {@code null}.</li>
     * <li>Search for a value that neither key holds.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first two searches answer {@code true}
     * and the third answers {@code false}.
     */
    @Test
    public void testContainsValueAnswersWhenAPathHoldsNull() {
        Association assoc = Association.of();
        assoc.set("a", null);
        assoc.set("b", "value");

        Assert.assertTrue(assoc.containsValue("value"));
        Assert.assertTrue(assoc.containsValue(null));
        Assert.assertFalse(assoc.containsValue("absent"));
    }

    /**
     * <strong>Goal:</strong> Verify that a path whose value is {@code null}
     * answers the default value.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding one key
     * whose value is {@code null}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Fetch {@code "a"} with a default value.</li>
     * <li>Fetch a key that was never written, with a default value.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both fetches answer the default value.
     */
    @Test
    public void testFetchOrDefaultAnswersTheDefaultWhenThePathHoldsNull() {
        Association assoc = Association.of();
        assoc.set("a", null);

        Assert.assertEquals("default", assoc.fetchOrDefault("a", "default"));
        Assert.assertEquals("default",
                assoc.fetchOrDefault("absent", "default"));
    }

    /**
     * <strong>Goal:</strong> Verify that a read of a key that names one
     * component answers what an overridden {@code fetch} supplies, as it does
     * for a key that names more.
     * <p>
     * <strong>Start state:</strong> An {@link Association} whose {@code fetch}
     * makes up a value from the path, as a view that generates its entries
     * does.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Get {@code "host"}, which names one component.</li>
     * <li>Get {@code "a.b"}, which names two.</li>
     * <li>Ask whether the {@link Association} holds {@code "host"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both reads answer what {@code fetch}
     * supplies, and the {@link Association} holds {@code "host"}.
     */
    @Test
    public void testGetAnswersWhatAnOverriddenFetchSupplies() {
        Association assoc = new GeneratedAssociation();

        Assert.assertEquals("generated:host", assoc.get("host"));
        Assert.assertEquals("generated:a.b", assoc.get("a.b"));
        Assert.assertTrue(assoc.containsKey("host"));
    }

    /**
     * <strong>Goal:</strong> Verify that a fetch of a path that names no
     * component is refused.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Fetch {@code "."}, which names no component.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The fetch throws an
     * {@link IllegalArgumentException} that names the path.
     */
    @Test
    public void testFetchRefusesAPathOfOnlyDots() {
        Association assoc = Association.of();
        try {
            assoc.fetch(".");
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals("Invalid path .", e.getMessage());
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a key that names one component holds
     * a {@code null} value that a write stores there.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "a"} to {@code null}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The write answers {@code null}, the
     * {@link Association} holds one entry, and that entry maps {@code "a"} to
     * {@code null}.
     */
    @Test
    public void testSetStoresANullValueUnderAKeyOfOneComponent() {
        Association assoc = Association.of();

        Object previous = assoc.set("a", null);

        Assert.assertNull(previous);
        Assert.assertEquals(1, assoc.size());
        Assert.assertEquals(Collections.singletonMap("a", null),
                new LinkedHashMap<>(assoc));
    }

    /**
     * <strong>Goal:</strong> Verify that a write to a path that names several
     * components answers what the end of that path held before.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "user.name"} to {@code "first"} and keep what it
     * answers.</li>
     * <li>Set {@code "user.name"} to {@code "second"} and keep what it
     * answers.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first write answers {@code null}, the
     * second answers {@code "first"}, and the path then reads as
     * {@code "second"}.
     */
    @Test
    public void testSetOfPathOfSeveralComponentsAnswersThePreviousValue() {
        Association assoc = Association.of();

        Object first = assoc.set("user.name", "first");
        Object second = assoc.set("user.name", "second");

        Assert.assertNull(first);
        Assert.assertEquals("first", second);
        Assert.assertEquals("second", assoc.fetch("user.name"));
    }

    /**
     * <strong>Goal:</strong> Verify that the paths of an {@link Association}
     * hold every prefix of every flattened path, in the order the flattened
     * paths reach them.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding a nested
     * map whose second key holds a list.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "user.name"} to {@code "jeff"}.</li>
     * <li>Set {@code "user.tags"} to a list of two elements.</li>
     * <li>Read the paths.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The paths are {@code "user"},
     * {@code "user.name"}, {@code "user.tags"}, {@code "user.tags.0"} and
     * {@code "user.tags.1"}, in that order.
     */
    @Test
    public void testPathsHoldsEveryPrefixOfEveryFlattenedPath() {
        Association assoc = Association.of();
        assoc.set("user.name", "jeff");
        assoc.set("user.tags", ImmutableList.of("a", "b"));

        Assert.assertEquals(
                ImmutableList.of("user", "user.name", "user.tags",
                        "user.tags.0", "user.tags.1"),
                ImmutableList.copyOf(assoc.paths()));
    }

    /**
     * <strong>Goal:</strong> Verify that a write whose path names a list
     * position beyond the limit is refused and changes nothing.
     * <p>
     * <strong>Start state:</strong> An empty {@link Association}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set a path whose second component names the position one past
     * {@link Association#MAX_LIST_GAP}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The write throws an
     * {@link IllegalArgumentException} and the {@link Association} stays
     * empty.
     */
    @Test
    public void testSetRefusesAListPositionBeyondTheLimit() {
        Association assoc = Association.of();
        try {
            assoc.set("a." + (Association.MAX_LIST_GAP + 1), "value");
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertTrue(assoc.isEmpty());
        }
    }

    /**
     * An {@link Association} whose {@code fetch} makes up a value from the
     * path, as a view that generates its entries does.
     *
     * @author Jeff Nelson
     */
    private static class GeneratedAssociation extends Association {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T fetch(String path) {
            return (T) ("generated:" + path);
        }

        @Override
        protected Supplier<Collection<Object>> collectionSupplier() {
            return ArrayList::new;
        }

        @Override
        protected Supplier<Map<String, Object>> mapSupplier() {
            return LinkedHashMap::new;
        }

    }

    /**
     * <strong>Goal:</strong> Verify that removing a path of several components
     * takes the value from the container that holds it and answers it.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding a map of
     * two keys under one key of the map itself.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "user.name"} to {@code "jeff"}.</li>
     * <li>Set {@code "user.age"} to {@code 3}.</li>
     * <li>Remove {@code "user.name"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The removal answers {@code "jeff"}, the path
     * then reads as {@code null}, and the other key keeps its value.
     */
    @Test
    public void testRemoveOfPathOfSeveralComponentsTakesTheValue() {
        Association assoc = Association.of();
        assoc.set("user.name", "jeff");
        assoc.set("user.age", 3);

        Object removed = assoc.remove("user.name");

        Assert.assertEquals("jeff", removed);
        Assert.assertNull(assoc.fetch("user.name"));
        Assert.assertEquals(3, (Object) assoc.fetch("user.age"));
    }

    /**
     * <strong>Goal:</strong> Verify that removing a path whose last component
     * names a list position clears that position and leaves every other
     * position naming the element it named before.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding a list of
     * two elements.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "a"} to a list of two elements.</li>
     * <li>Remove {@code "a.0"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The removal answers the first element, that
     * position then reads as {@code null}, and the second position still reads
     * as the second element.
     */
    @Test
    public void testRemoveOfListPositionClearsThatPosition() {
        Association assoc = Association.of();
        assoc.set("a", Lists.newArrayList("x", "y"));

        Object removed = assoc.remove("a.0");

        Assert.assertEquals("x", removed);
        Assert.assertNull(assoc.fetch("a.0"));
        Assert.assertEquals("y", assoc.fetch("a.1"));
        Assert.assertEquals(Lists.newArrayList(null, "y"), assoc.fetch("a"));
    }

    /**
     * <strong>Goal:</strong> Verify that removing a path of several components
     * leaves the containers above it in place.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding one key
     * under one key of the map itself.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "user.name"} to {@code "jeff"}.</li>
     * <li>Remove {@code "user.name"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The {@link Association} still holds one
     * entry, and the map that held the removed key is empty.
     */
    @Test
    public void testRemoveLeavesTheContainersAbovePathInPlace() {
        Association assoc = Association.of();
        assoc.set("user.name", "jeff");

        assoc.remove("user.name");

        Assert.assertEquals(1, assoc.size());
        Assert.assertEquals(ImmutableMap.of(), assoc.fetch("user"));
    }

    /**
     * <strong>Goal:</strong> Verify that removing a key of one component takes
     * the entry out of the map itself.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding one key of
     * the map itself.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "a"} to {@code 1}.</li>
     * <li>Remove {@code "a"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The removal answers {@code 1} and the
     * {@link Association} is empty.
     */
    @Test
    public void testRemoveOfKeyOfOneComponentTakesTheEntry() {
        Association assoc = Association.of();
        assoc.set("a", 1);

        Object removed = assoc.remove("a");

        Assert.assertEquals(1, removed);
        Assert.assertTrue(assoc.isEmpty());
    }

    /**
     * <strong>Goal:</strong> Verify that removing a path that holds no value
     * answers {@code null} and changes nothing.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding one path of
     * two components.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Set {@code "user.name"} to {@code "jeff"}.</li>
     * <li>Remove {@code "user.absent"}, whose container exists.</li>
     * <li>Remove {@code "absent.absent"}, whose container does not.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both removals answer {@code null} and the
     * held path keeps its value.
     */
    @Test
    public void testRemoveOfAbsentPathAnswersNull() {
        Association assoc = Association.of();
        assoc.set("user.name", "jeff");

        Assert.assertNull(assoc.remove("user.absent"));
        Assert.assertNull(assoc.remove("absent.absent"));
        Assert.assertEquals("jeff", assoc.fetch("user.name"));
    }

    /**
     * <strong>Goal:</strong> Verify that a removal from a container that does
     * not permit the change is refused.
     * <p>
     * <strong>Start state:</strong> An {@link Association} holding an
     * immutable list under one key and an immutable map under another.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Remove the first position of the list.</li>
     * <li>Remove the key of the map.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both removals throw an
     * {@link UnsupportedOperationException}.
     */
    @Test
    public void testRemoveRefusesAContainerThatDoesNotPermitTheChange() {
        Association assoc = Association.of();
        assoc.set("list", ImmutableList.of("x", "y"));
        assoc.set("map", ImmutableMap.of("b", 1));

        try {
            assoc.remove("list.0");
            Assert.fail("Expected an UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e) {
            Assert.assertEquals(ImmutableList.of("x", "y"),
                    assoc.fetch("list"));
        }
        try {
            assoc.remove("map.b");
            Assert.fail("Expected an UnsupportedOperationException");
        }
        catch (UnsupportedOperationException e) {
            Assert.assertEquals(ImmutableMap.of("b", 1), assoc.fetch("map"));
        }
    }

    /**
     * <strong>Goal:</strong> Verify that building an {@link Association} from
     * a map holding a path that names no component is refused.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an {@link Association} from a map whose only key is
     * {@code "."}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The build throws an
     * {@link IllegalArgumentException} that names the path.
     */
    @Test
    public void testOfRefusesAPathOfOnlyDots() {
        try {
            Association.of(ImmutableMap.of(".", 1));
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertEquals("Invalid path .", e.getMessage());
        }
    }

    /**
     * <strong>Goal:</strong> Verify that building an {@link Association} from
     * a map holding a path whose component names a negative position of a
     * list that the map already supplied is refused.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an {@link Association} from a map that supplies a list under
     * {@code "a"} and then writes {@code "a.-1"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The build throws an
     * {@link IndexOutOfBoundsException}.
     */
    @Test
    public void testOfRefusesANegativePositionOfASuppliedList() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", ImmutableList.of("x"));
        map.put("a.-1", 1);
        try {
            Association.of(map);
            Assert.fail("Expected an IndexOutOfBoundsException");
        }
        catch (IndexOutOfBoundsException e) {
            Assert.assertTrue(true);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that building an {@link Association} from
     * a map holding a path whose first component reads as an integer is
     * refused.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an {@link Association} from a map whose only key is
     * {@code "0.a"}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The build throws an
     * {@link IllegalArgumentException} that names the path.
     */
    @Test
    public void testOfRefusesALeadingIntegerComponent() {
        try {
            Association.of(ImmutableMap.of("0.a", 1));
            Assert.fail("Expected an IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            Assert.assertTrue(e.getMessage().contains("'0.a'"));
        }
    }

}
