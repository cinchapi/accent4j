/*
 * Copyright (c) 2013-2026 Cinchapi Inc.
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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

/**
 * Tests that compare {@link Association} with {@link ReferenceAssociation} on
 * random data.
 * <p>
 * Each test gives both implementations the same inputs and the same operations,
 * then checks every observable result: the value or exception of each call, the
 * entries and their iteration order, the class of every nested container, which
 * nested containers share an identity, and whether a container that a caller
 * held before an operation is still in place and what it holds afterward. The
 * random inputs use dotted keys, integer components, gaps, negative indexes,
 * nulls, empty containers, lists that start with more than
 * {@link Association#MAX_LIST_GAP} empty containers,
 * {@link java.util.Set Sets}, plain {@link Iterable Iterables}, nested
 * {@link Association Associations}, and keys that conflict with each other in
 * either order.
 * </p>
 * <p>
 * Every assertion message starts with the seed of its trial, so a failure can
 * be replayed by starting a test from that seed.
 * </p>
 *
 * @author Jeff Nelson
 */
public class AssociationReferenceTest {

    /**
     * Assert that {@code actual} has the same entries as {@code expected}, in
     * the same order, with values that are the {@link #assertSameValue same}.
     *
     * @param context the prefix for assertion messages
     * @param expected the entries that the reference produced
     * @param actual the entries that {@link Association} produced
     */
    private static void assertSameEntries(String context, Map<?, ?> expected,
            Map<?, ?> actual) {
        Iterator<? extends Entry<?, ?>> expectedEntries = expected.entrySet()
                .iterator();
        Iterator<? extends Entry<?, ?>> actualEntries = actual.entrySet()
                .iterator();
        while (expectedEntries.hasNext()) {
            Entry<?, ?> expectedEntry = expectedEntries.next();
            Assert.assertTrue(context + " lacks key " + expectedEntry.getKey()
                    + " in " + actual, actualEntries.hasNext());
            Entry<?, ?> actualEntry = actualEntries.next();
            Assert.assertEquals(context + " key order in " + actual,
                    expectedEntry.getKey(), actualEntry.getKey());
            assertSameValue(context + "." + expectedEntry.getKey(),
                    expectedEntry.getValue(), actualEntry.getValue());
        }
        Assert.assertFalse(context + " has extra entries in " + actual,
                actualEntries.hasNext());
    }

    /**
     * Assert that {@code actual} has the same outcome as {@code expected}:
     * either the same exception class and message, or the
     * {@link #assertSameValue same} result.
     *
     * @param context the prefix for assertion messages
     * @param expected the outcome of the reference
     * @param actual the outcome of {@link Association}
     */
    private static void assertSameOutcome(String context, Outcome expected,
            Outcome actual) {
        if(expected.error != null) {
            Assert.assertNotNull(context + " expected " + expected.error
                    + " but got " + actual.value, actual.error);
            Assert.assertEquals(context + " exception class",
                    expected.error.getClass(), actual.error.getClass());
            if(!isRaisedByJvm(expected.error)) {
                Assert.assertEquals(context + " exception message",
                        expected.error.getMessage(), actual.error.getMessage());
            }
        }
        else {
            if(actual.error != null) {
                AssertionError failure = new AssertionError(
                        context + " expected " + expected.value + " but threw");
                failure.initCause(actual.error);
                throw failure;
            }
            else {
                assertSameValue(context + " result", expected.value,
                        actual.value);
            }
        }
    }

    /**
     * Assert that {@code actual} is the same value as {@code expected}: a
     * container of the same class with the same contents in the same order, or
     * else the very same object.
     *
     * @param context the prefix for assertion messages
     * @param expected the value that the reference produced
     * @param actual the value that {@link Association} produced
     */
    private static void assertSameValue(String context, Object expected,
            Object actual) {
        if(expected instanceof Map) {
            Assert.assertNotNull(context + " expected " + expected, actual);
            Assert.assertEquals(context + " class of " + actual,
                    expected.getClass(), actual.getClass());
            assertSameEntries(context, (Map<?, ?>) expected,
                    (Map<?, ?>) actual);
            if(expected instanceof LoggingMap) {
                Assert.assertEquals(context + " calls on " + actual,
                        ((LoggingMap) expected).calls,
                        ((LoggingMap) actual).calls);
            }
        }
        else if(expected instanceof Collection) {
            Assert.assertNotNull(context + " expected " + expected, actual);
            Assert.assertEquals(context + " class of " + actual,
                    expected.getClass(), actual.getClass());
            Collection<?> expectedElements = (Collection<?>) expected;
            Collection<?> actualElements = (Collection<?>) actual;
            Assert.assertEquals(context + " size of " + actual,
                    expectedElements.size(), actualElements.size());
            Iterator<?> actualIterator = actualElements.iterator();
            int position = 0;
            for (Object element : expectedElements) {
                assertSameValue(context + "[" + position + "]", element,
                        actualIterator.next());
                ++position;
            }
        }
        else {
            Assert.assertSame(context, expected, actual);
        }
    }

    /**
     * Add the {@code value} to {@code found} under its {@code path} if it is a
     * container, and then do the same for everything nested in it.
     *
     * @param path the path of the {@code value}
     * @param value the value to visit
     * @param found the containers found so far, keyed by path
     */
    private static void collectContainers(List<Object> path, Object value,
            Map<List<Object>, Object> found) {
        if(value instanceof Map) {
            found.put(path, value);
            for (Entry<?, ?> entry : ((Map<?, ?>) value).entrySet()) {
                List<Object> child = new ArrayList<>(path);
                child.add(entry.getKey());
                collectContainers(child, entry.getValue(), found);
            }
        }
        else if(value instanceof Collection) {
            found.put(path, value);
            int position = 0;
            for (Object element : (Collection<?>) value) {
                List<Object> child = new ArrayList<>(path);
                child.add(position);
                collectContainers(child, element, found);
                ++position;
            }
        }
    }

    /**
     * Return every map and collection nested in the {@code root}, keyed by its
     * path. A path is a list of map keys and collection positions, so that a
     * key {@code "0"} and a position {@code 0} stay distinct.
     *
     * @param root the entries of an association
     * @return the containers in depth-first order, keyed by path
     */
    private static Map<List<Object>, Object> containers(
            Map<String, Object> root) {
        Map<List<Object>, Object> found = new LinkedHashMap<>();
        for (Entry<String, Object> entry : root.entrySet()) {
            List<Object> path = new ArrayList<>();
            path.add(entry.getKey());
            collectContainers(path, entry.getValue(), found);
        }
        return found;
    }

    /**
     * Return a deep copy of the {@code value} that keeps the class of each
     * container and shares each leaf, so that two implementations that store a
     * container by reference cannot change each other's copy.
     *
     * @param value the value to copy
     * @return the copy, or the {@code value} itself when it is not a container
     */
    @SuppressWarnings("unchecked")
    private static Object copy(Object value) {
        if(value instanceof Association) {
            Association copy = Association.of();
            ((Association) value)
                    .forEach((key, element) -> copy.set(key, copy(element)));
            return copy;
        }
        else if(value instanceof ImmutableMap) {
            return ImmutableMap.copyOf(copyEntries((Map<String, Object>) value,
                    new LinkedHashMap<>()));
        }
        else if(value instanceof TreeMap) {
            return copyEntries((Map<String, Object>) value, new TreeMap<>());
        }
        else if(value instanceof LoggingMap) {
            return copyEntries((Map<String, Object>) value, new LoggingMap());
        }
        else if(value instanceof LinkedHashMap) {
            return copyEntries((Map<String, Object>) value,
                    new LinkedHashMap<>());
        }
        else if(value instanceof HashMap) {
            return copyEntries((Map<String, Object>) value, new HashMap<>());
        }
        else if(value instanceof ImmutableList) {
            return ImmutableList.copyOf(copyElements((Collection<Object>) value,
                    new ArrayList<>()));
        }
        else if(value instanceof LinkedList) {
            return copyElements((Collection<Object>) value, new LinkedList<>());
        }
        else if(value instanceof LinkedHashSet) {
            return copyElements((Collection<Object>) value,
                    new LinkedHashSet<>());
        }
        else if(value instanceof ArrayList) {
            return copyElements((Collection<Object>) value, new ArrayList<>());
        }
        else {
            return value;
        }
    }

    /**
     * Add a {@link #copy(Object) copy} of each element of the {@code source} to
     * the {@code destination}, in iteration order.
     *
     * @param source the elements to copy
     * @param destination the collection that receives the copies
     * @return the {@code destination}
     */
    private static Collection<Object> copyElements(Collection<Object> source,
            Collection<Object> destination) {
        for (Object element : source) {
            destination.add(copy(element));
        }
        return destination;
    }

    /**
     * Put a {@link #copy(Object) copy} of each value of the {@code source} into
     * the {@code destination} under the same key, in iteration order.
     *
     * @param source the entries to copy
     * @param destination the map that receives the copies
     * @return the {@code destination}
     */
    private static Map<String, Object> copyEntries(Map<String, Object> source,
            Map<String, Object> destination) {
        for (Entry<String, Object> entry : source.entrySet()) {
            destination.put(entry.getKey(), copy(entry.getValue()));
        }
        return destination;
    }

    /**
     * Return, for each path in {@code containers}, the first path whose
     * container is the same object, so that two structures can be compared for
     * which of their containers share an identity.
     *
     * @param containers the containers of a structure, keyed by path
     * @return the first path of the same object, keyed by path
     */
    private static Map<List<Object>, List<Object>> identities(
            Map<List<Object>, Object> containers) {
        Map<Object, List<Object>> first = new IdentityHashMap<>();
        Map<List<Object>, List<Object>> identities = new LinkedHashMap<>();
        for (Entry<List<Object>, Object> entry : containers.entrySet()) {
            first.putIfAbsent(entry.getValue(), entry.getKey());
            identities.put(entry.getKey(), first.get(entry.getValue()));
        }
        return identities;
    }

    /**
     * Return whether the {@code error} is of a kind that the JVM raises by
     * itself for an invalid operation, such as an array index out of range.
     * Once the code that raises it is hot, HotSpot may raise such an exception
     * without a message, so its message can differ between two implementations
     * of the same operation within one run.
     *
     * @param error the exception to check
     * @return {@code true} if the JVM may raise the {@code error} without a
     *         message
     */
    private static boolean isRaisedByJvm(RuntimeException error) {
        return error instanceof NullPointerException
                || error instanceof ArrayIndexOutOfBoundsException
                || error instanceof ClassCastException
                || error instanceof ArithmeticException
                || error instanceof ArrayStoreException;
    }

    /**
     * Return a list that starts with more than {@link Association#MAX_LIST_GAP}
     * empty maps and empty lists, which flatten to no entries, and ends with a
     * random value nested one level deeper than {@code depth}.
     *
     * @param random the source of randomness
     * @param depth the nesting depth of the list
     * @return the list
     */
    private static List<Object> randomEmptyRun(Random random, int depth) {
        List<Object> list = new ArrayList<>();
        int size = Association.MAX_LIST_GAP + 1 + random.nextInt(5);
        for (int i = 0; i < size; ++i) {
            list.add(random.nextBoolean() ? new LinkedHashMap<>()
                    : new ArrayList<>());
        }
        list.add(randomValue(random, depth + 1));
        return list;
    }

    /**
     * Return a random input for {@link Association#of(Map)} or
     * {@link Association#merge(Map)}: a map of nested values in one of several
     * map classes, sometimes with a {@code null} key.
     *
     * @param random the source of randomness
     * @return the input
     */
    private static Map<String, Object> randomInput(Random random) {
        Map<String, Object> map = randomMap(random, 0, 6,
                new LinkedHashMap<>());
        if(random.nextInt(20) == 0) {
            // Enough distinct keys that a HashMap container resizes.
            int size = 13 + random.nextInt(20);
            for (int i = 0; i < size; ++i) {
                map.put("k" + random.nextInt(40),
                        random.nextBoolean() ? randomLeaf(random)
                                : ImmutableMap.of(
                                        NAMES[random.nextInt(NAMES.length)],
                                        "x"));
            }
        }
        int roll = random.nextInt(100);
        if(roll < 8) {
            return new HashMap<>(map);
        }
        else if(roll < 12) {
            return new TreeMap<>(map);
        }
        else if(roll < 16 && !map.containsValue(null)) {
            return ImmutableMap.copyOf(map);
        }
        else if(roll < 18) {
            map.put(null, randomValue(random, 1));
            return map;
        }
        else {
            return map;
        }
    }

    /**
     * Return a random key for a map, drawn mostly from short names, and
     * otherwise from integers and from keys with dots, empty components and
     * negative or padded numbers.
     *
     * @param random the source of randomness
     * @param topLevel whether the key is for the outermost map, where an
     *            integer key makes the whole input invalid
     * @return the key
     */
    private static String randomKey(Random random, boolean topLevel) {
        int roll = random.nextInt(100);
        if(roll < (topLevel ? 82 : 55)) {
            return NAMES[random.nextInt(NAMES.length)];
        }
        else if(roll < 85) {
            return INDEXES[random.nextInt(INDEXES.length)];
        }
        else {
            return ODD_KEYS[random.nextInt(ODD_KEYS.length)];
        }
    }

    /**
     * Return a random leaf: {@code null} or one of the shared {@link #LEAVES}.
     *
     * @param random the source of randomness
     * @return the leaf
     */
    private static Object randomLeaf(Random random) {
        return random.nextInt(5) == 0 ? null
                : LEAVES[random.nextInt(LEAVES.length)];
    }

    /**
     * Return a random list of values nested one level deeper than
     * {@code depth}.
     *
     * @param random the source of randomness
     * @param depth the nesting depth of the list
     * @param size the number of elements
     * @return the list
     */
    private static List<Object> randomList(Random random, int depth, int size) {
        List<Object> list = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            list.add(randomValue(random, depth + 1));
        }
        return list;
    }

    /**
     * Return a random map of up to {@code maxSize} entries whose values are
     * nested one level deeper than {@code depth}.
     *
     * @param random the source of randomness
     * @param depth the nesting depth of the map
     * @param maxSize the largest number of entries to add
     * @param map the empty map to fill
     * @return the {@code map}
     */
    private static Map<String, Object> randomMap(Random random, int depth,
            int maxSize, Map<String, Object> map) {
        int size = random.nextInt(maxSize + 1);
        for (int i = 0; i < size; ++i) {
            map.put(randomKey(random, depth == 0),
                    randomValue(random, depth + 1));
        }
        return map;
    }

    /**
     * Return a random path for {@link Association#set(String, Object)}: one to
     * four components of names and integers, sometimes starting with an
     * integer, or one of the {@link #ODD_KEYS}.
     *
     * @param random the source of randomness
     * @return the path
     */
    private static String randomPath(Random random) {
        int roll = random.nextInt(100);
        if(roll < 6) {
            return ODD_KEYS[random.nextInt(ODD_KEYS.length)];
        }
        else {
            StringBuilder path = new StringBuilder(
                    roll < 9 ? INDEXES[random.nextInt(INDEXES.length)]
                            : NAMES[random.nextInt(NAMES.length)]);
            int count = 1 + random.nextInt(4);
            for (int i = 1; i < count; ++i) {
                path.append('.');
                path.append(random.nextBoolean()
                        ? NAMES[random.nextInt(NAMES.length)]
                        : INDEXES[random.nextInt(INDEXES.length)]);
            }
            return path.toString();
        }
    }

    /**
     * Return a random value nested at {@code depth}: a leaf, or a container of
     * a random class that holds more random values.
     *
     * @param random the source of randomness
     * @param depth the nesting depth of the value
     * @return the value
     */
    private static Object randomValue(Random random, int depth) {
        int roll = random.nextInt(100);
        if(depth >= 4 || roll < 35) {
            return randomLeaf(random);
        }
        else if(roll < 50) {
            return randomMap(random, depth, 3, new LinkedHashMap<>());
        }
        else if(roll < 53) {
            return randomMap(random, depth, 3, new HashMap<>());
        }
        else if(roll < 55) {
            return randomMap(random, depth, 3, new TreeMap<>());
        }
        else if(roll < 57) {
            Map<String, Object> map = randomMap(random, depth, 3,
                    new LinkedHashMap<>());
            map.values().removeIf(Objects::isNull);
            return ImmutableMap.copyOf(map);
        }
        else if(roll < 78) {
            return randomList(random, depth, random.nextInt(6));
        }
        else if(roll < 81) {
            return new LinkedHashSet<>(
                    randomList(random, depth, random.nextInt(4)));
        }
        else if(roll < 84) {
            return new LinkedList<>(
                    randomList(random, depth, random.nextInt(4)));
        }
        else if(roll < 87) {
            List<Object> list = randomList(random, depth, random.nextInt(4));
            Iterable<Object> iterable = list::iterator;
            return iterable;
        }
        else if(roll < 90) {
            List<Object> list = randomList(random, depth, random.nextInt(4));
            list.removeIf(Objects::isNull);
            return ImmutableList.copyOf(list);
        }
        else if(roll < 94) {
            Association association = Association.of();
            int size = random.nextInt(4);
            for (int i = 0; i < size; ++i) {
                association.set(NAMES[random.nextInt(NAMES.length)],
                        randomValue(random, depth + 1));
            }
            return association;
        }
        else if(roll < 96) {
            return randomList(random, depth, 10 + random.nextInt(30));
        }
        else if(roll < 97) {
            return randomEmptyRun(random, depth);
        }
        else {
            return randomMap(random, depth, 3, new LoggingMap());
        }
    }

    /**
     * Run one trial of random operations on a pair of empty associations made
     * by the {@code containers}, or on a pair that a random input filled.
     *
     * @param seed the seed of the trial
     * @param containers the kind of associations to compare
     */
    private static void runOperationsTrial(long seed, Containers containers) {
        Random random = new Random(seed);
        String context = "seed=" + seed + "L " + containers;
        Pair pair = new Pair(containers.newReference(),
                containers.newAssociation());
        if(random.nextBoolean()) {
            Map<String, Object> input = randomInput(random);
            pair.run(context + " merge(" + input + ")", expected -> {
                expected.merge(input);
                return null;
            }, actual -> {
                actual.merge(input);
                return null;
            });
        }
        int count = random.nextInt(8);
        for (int i = 0; i < count; ++i) {
            runRandomOperation(context + " #" + i, pair, random);
        }
    }

    /**
     * Apply one random operation to both sides of the {@code pair}: a
     * {@code set}, a {@code merge} of a random input, a {@code merge} with a
     * strategy, or a {@code merge} of the association into itself.
     *
     * @param context the prefix for assertion messages
     * @param pair the implementations to compare
     * @param random the source of randomness
     */
    private static void runRandomOperation(String context, Pair pair,
            Random random) {
        int roll = random.nextInt(100);
        if(roll < 40) {
            String path = randomPath(random);
            Object value = random.nextInt(5) < 3 ? randomLeaf(random)
                    : randomValue(random, 1);
            Object expectedValue = copy(value);
            Object actualValue = copy(value);
            pair.run(context + " set(" + path + ", " + value + ")",
                    expected -> expected.set(path, expectedValue),
                    actual -> actual.set(path, actualValue));
        }
        else if(roll < 80) {
            Map<String, Object> input = randomInput(random);
            pair.run(context + " merge(" + input + ")", expected -> {
                expected.merge(input);
                return null;
            }, actual -> {
                actual.merge(input);
                return null;
            });
        }
        else if(roll < 92) {
            Map<String, Object> input = randomInput(random);
            BiFunction<Object, Object, Object> strategy = STRATEGIES
                    .get(random.nextInt(STRATEGIES.size()));
            pair.run(context + " merge(" + input + ", strategy)", expected -> {
                expected.merge(input, strategy);
                return null;
            }, actual -> {
                actual.merge(input, strategy);
                return null;
            });
        }
        else {
            pair.run(context + " merge(itself)", expected -> {
                expected.merge(expected);
                return null;
            }, actual -> {
                actual.merge(actual);
                return null;
            });
        }
    }

    /**
     * Shared leaves for random values. Both implementations receive the same
     * leaf objects, so the tests compare leaves by identity.
     */
    private static final Object[] LEAVES = { "x", "y", "z", 1, 2L, true,
            new String[] { "array" } };

    /**
     * Short names that random keys and paths use often, so that random inputs
     * overlap.
     */
    private static final String[] NAMES = { "a", "b", "c", "tags" };

    /**
     * Integer components, with a gap, for list positions.
     */
    private static final String[] INDEXES = { "0", "1", "2", "3", "5" };

    /**
     * Keys with dots, empty components, negative or padded numbers, and indexes
     * that skip {@link Association#MAX_LIST_GAP} list positions, or one more,
     * when they start a new list.
     */
    private static final String[] ODD_KEYS = { "", ".", "a.", ".a", "a..b",
            "-1", "a.-1", "00", "0.a", "a.b", "a.0", "b.1.c", "tags.2", "c.0.0",
            "a.b.c", "tags.-1", "tags.1.a", "tags." + Association.MAX_LIST_GAP,
            "c." + (Association.MAX_LIST_GAP + 1) };

    /**
     * The merge strategies that random operations pass to
     * {@link Association#merge(Map, BiFunction)}.
     */
    private static final List<BiFunction<Object, Object, Object>> STRATEGIES = ImmutableList
            .of(MergeStrategies::theirs, MergeStrategies::ours,
                    MergeStrategies::concat);

    /**
     * The number of random trials in each test.
     */
    private static final int TRIALS = 3000;

    /**
     * The seed of the first trial in each test, chosen at random so that each
     * run covers new inputs.
     */
    private final long seed = new Random().nextLong();

    /**
     * <strong>Goal:</strong> Verify that {@code merge} still calls a subclass's
     * own {@code get} and {@code set} for each flattened entry, and that a
     * subclass whose {@code set} throws still fails the same way.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>For each trial, merge a random input into a
     * {@link RecordingAssociation} and its reference twin, then apply up to
     * seven random operations to both.</li>
     * <li>Merge a random input into a {@link ReadOnlyAssociation} and its
     * reference twin.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both recording subclasses see the same paths
     * in the same order and hold values with the same contents afterward; both
     * read-only subclasses throw the same exception or both succeed.
     */
    @Test
    public void testMergeCallsOverriddenAccessorsLikeReference() {
        for (int trial = 0; trial < TRIALS; ++trial) {
            long seed = this.seed + trial;
            Random random = new Random(seed);
            String context = "seed=" + seed + "L";
            RecordingReferenceAssociation recordingExpected = new RecordingReferenceAssociation();
            RecordingAssociation recordingActual = new RecordingAssociation();
            Pair pair = new Pair(recordingExpected, recordingActual);
            int count = 1 + random.nextInt(8);
            for (int i = 0; i < count; ++i) {
                runRandomOperation(context + " #" + i, pair, random);
            }
            Assert.assertEquals(context + " paths passed to set",
                    recordingExpected.paths, recordingActual.paths);
            assertSameValue(context + " values returned from get",
                    recordingExpected.values, recordingActual.values);

            Map<String, Object> input = randomInput(random);
            Outcome expected = Outcome.of(() -> {
                new ReadOnlyReferenceAssociation().merge(input);
                return null;
            });
            Outcome actual = Outcome.of(() -> {
                new ReadOnlyAssociation().merge(input);
                return null;
            });
            assertSameOutcome(context + " read-only merge(" + input + ")",
                    expected, actual);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Association#of(Map)} gives the
     * same result or exception as the reference for random maps.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Generate a random input map for each trial.</li>
     * <li>Call {@link ReferenceAssociation#of(Map)} and
     * {@link Association#of(Map)} on it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both calls throw the same exception, or both
     * return the same entries, in the same order, with nested containers of the
     * same classes and the same identity pattern.
     */
    @Test
    public void testOfMatchesReference() {
        for (int trial = 0; trial < TRIALS; ++trial) {
            long seed = this.seed + trial;
            Random random = new Random(seed);
            Map<String, Object> input = randomInput(random);
            String context = "seed=" + seed + "L of(" + input + ")";
            Outcome expected = Outcome.of(() -> ReferenceAssociation.of(input));
            Outcome actual = Outcome.of(() -> Association.of(input));
            if(expected.error == null && actual.error == null) {
                new Pair((ReferenceAssociation) expected.value,
                        (Association) actual.value).assertSameState(context);
            }
            else {
                assertSameOutcome(context, expected, actual);
            }
        }
    }

    /**
     * <strong>Goal:</strong> Verify that random sequences of {@code set},
     * {@code merge}, and {@code merge} with a strategy have the same results,
     * exceptions, and effects on containers that a caller holds as the
     * reference.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>For each trial, make an empty {@link Association#of()} and an empty
     * reference with {@link LinkedHashMap LinkedHashMaps} and
     * {@link ArrayList ArrayLists}, and merge a random input into both half of
     * the time.</li>
     * <li>Apply up to seven random operations to both, including a
     * {@code merge} of each association into itself.</li>
     * <li>Before each operation, record every nested container; after it,
     * compare both sides.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each operation returns the same value or
     * throws the same exception on both sides and leaves the same entries,
     * container classes, and identity pattern. Each container recorded before
     * the operation is still in place on one side exactly when it is on the
     * other, and holds the same contents.
     */
    @Test
    public void testOperationsMatchReference() {
        for (int trial = 0; trial < TRIALS; ++trial) {
            runOperationsTrial(seed + trial, Containers.ASSOCIATION_OF);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a subclass gets the same results from
     * {@code merge} and {@code set} as the reference with the same suppliers,
     * whichever container classes they return.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>For each {@link Containers} kind other than
     * {@link Containers#ASSOCIATION_OF}, run the random trials of
     * {@link #testOperationsMatchReference()} on a subclass and a reference
     * whose suppliers return that kind of container.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The same results as in
     * {@link #testOperationsMatchReference()}, including containers of the
     * classes that the suppliers return.
     */
    @Test
    public void testOperationsMatchReferenceForSubclassContainers() {
        for (Containers containers : Containers.values()) {
            if(containers != Containers.ASSOCIATION_OF) {
                for (int trial = 0; trial < TRIALS / 2; ++trial) {
                    runOperationsTrial(seed + trial, containers);
                }
            }
        }
    }

    /**
     * The kinds of containers that the compared associations use, each with a
     * factory for both implementations.
     *
     * @author Jeff Nelson
     */
    private enum Containers {

        /**
         * The {@link Association} that {@link Association#of()} returns, which
         * uses {@link LinkedHashMap LinkedHashMaps} and
         * {@link ArrayList ArrayLists}.
         */
        ASSOCIATION_OF(LinkedHashMap::new, ArrayList::new),

        /**
         * A subclass with {@link LinkedHashMap LinkedHashMaps} and
         * {@link ArrayList ArrayLists}.
         */
        LINKED_HASH_MAP_AND_ARRAY_LIST(LinkedHashMap::new, ArrayList::new),

        /**
         * A subclass with {@link HashMap HashMaps} and
         * {@link LinkedList LinkedLists}.
         */
        HASH_MAP_AND_LINKED_LIST(HashMap::new, LinkedList::new),

        /**
         * A subclass with {@link TreeMap TreeMaps} and
         * {@link ArrayList ArrayLists}.
         */
        TREE_MAP_AND_ARRAY_LIST(TreeMap::new, ArrayList::new),

        /**
         * A subclass with {@link HashMap HashMaps} and
         * {@link LinkedHashSet LinkedHashSets}.
         */
        HASH_MAP_AND_LINKED_HASH_SET(HashMap::new, LinkedHashSet::new),

        /**
         * A subclass with {@link LinkedHashMap LinkedHashMaps} and
         * {@link UniqueList UniqueLists}, whose {@code add} skips an element
         * that the list already holds.
         */
        LINKED_HASH_MAP_AND_UNIQUE_LIST(LinkedHashMap::new, UniqueList::new);

        /**
         * The supplier of map containers.
         */
        private final Supplier<Map<String, Object>> maps;

        /**
         * The supplier of collection containers.
         */
        private final Supplier<Collection<Object>> collections;

        /**
         * Construct a new instance.
         *
         * @param maps the supplier of map containers
         * @param collections the supplier of collection containers
         */
        Containers(Supplier<Map<String, Object>> maps,
                Supplier<Collection<Object>> collections) {
            this.maps = maps;
            this.collections = collections;
        }

        /**
         * Return an empty {@link Association} that uses these containers.
         *
         * @return the new {@link Association}
         */
        Association newAssociation() {
            if(this == ASSOCIATION_OF) {
                return Association.of();
            }
            else {
                return new Association() {

                    @Override
                    protected Supplier<Collection<Object>> collectionSupplier() {
                        return collections;
                    }

                    @Override
                    protected Supplier<Map<String, Object>> mapSupplier() {
                        return maps;
                    }

                };
            }
        }

        /**
         * Return an empty {@link ReferenceAssociation} that uses these
         * containers.
         *
         * @return the new {@link ReferenceAssociation}
         */
        ReferenceAssociation newReference() {
            return new ReferenceAssociation() {

                @Override
                protected Supplier<Collection<Object>> collectionSupplier() {
                    return collections;
                }

                @Override
                protected Supplier<Map<String, Object>> mapSupplier() {
                    return maps;
                }

            };
        }

    }

    /**
     * A {@link LinkedHashMap} that records each call to {@code put},
     * {@code merge} and {@code compute}, so that the tests can check that both
     * implementations call the same methods on a map that a caller supplied.
     *
     * @author Jeff Nelson
     */
    private static class LoggingMap extends LinkedHashMap<String, Object> {

        /**
         * The version of the serialized form.
         */
        private static final long serialVersionUID = 1L;

        /**
         * The name and key of each recorded call, in order.
         */
        private final List<String> calls = new ArrayList<>();

        @Override
        public Object compute(String key,
                BiFunction<? super String, ? super Object, ? extends Object> remappingFunction) {
            calls.add("compute " + key);
            return super.compute(key, remappingFunction);
        }

        @Override
        public Object merge(String key, Object value,
                BiFunction<? super Object, ? super Object, ? extends Object> remappingFunction) {
            calls.add("merge " + key);
            return super.merge(key, value, remappingFunction);
        }

        @Override
        public Object put(String key, Object value) {
            calls.add("put " + key);
            return super.put(key, value);
        }

    }

    /**
     * The value that a call returned, or the exception that it threw.
     *
     * @author Jeff Nelson
     */
    private static final class Outcome {

        /**
         * Run the {@code call} and capture its outcome.
         *
         * @param call the call to run
         * @return the {@link Outcome}
         */
        static Outcome of(Supplier<Object> call) {
            try {
                return new Outcome(call.get(), null);
            }
            catch (RuntimeException e) {
                return new Outcome(null, e);
            }
        }

        /**
         * The value that the call returned, if it returned.
         */
        private final Object value;

        /**
         * The exception that the call threw, or {@code null} if it returned.
         */
        private final RuntimeException error;

        /**
         * Construct a new instance.
         *
         * @param value the value that the call returned
         * @param error the exception that the call threw
         */
        private Outcome(Object value, RuntimeException error) {
            this.value = value;
            this.error = error;
        }

    }

    /**
     * A {@link ReferenceAssociation} and an {@link Association} that start with
     * the same entries and receive the same operations.
     *
     * @author Jeff Nelson
     */
    private static final class Pair {

        /**
         * The reference implementation.
         */
        private final ReferenceAssociation expected;

        /**
         * The implementation under test.
         */
        private final Association actual;

        /**
         * Construct a new instance.
         *
         * @param expected the reference implementation
         * @param actual the implementation under test
         */
        Pair(ReferenceAssociation expected, Association actual) {
            this.expected = expected;
            this.actual = actual;
        }

        /**
         * Assert that both sides hold the same entries, in the same order, with
         * nested containers of the same classes, and that the same paths share
         * an identity on both sides.
         *
         * @param context the prefix for assertion messages
         */
        void assertSameState(String context) {
            assertSameEntries(context + " state", expected, actual);
            Assert.assertEquals(context + " shared identities",
                    identities(containers(expected)),
                    identities(containers(actual)));
        }

        /**
         * Run an operation on both sides and assert that it has the same
         * outcome and the same effects, including on the containers that a
         * caller could hold from before the operation.
         *
         * @param context the prefix for assertion messages
         * @param onExpected the operation on the reference
         * @param onActual the operation on {@link Association}
         */
        void run(String context,
                Function<ReferenceAssociation, Object> onExpected,
                Function<Association, Object> onActual) {
            Map<List<Object>, Object> heldExpected = containers(expected);
            Map<List<Object>, Object> heldActual = containers(actual);
            Outcome expectedOutcome = Outcome
                    .of(() -> onExpected.apply(expected));
            Outcome actualOutcome = Outcome.of(() -> onActual.apply(actual));
            assertSameOutcome(context, expectedOutcome, actualOutcome);
            assertSameState(context);
            Map<List<Object>, Object> nowExpected = containers(expected);
            Map<List<Object>, Object> nowActual = containers(actual);
            for (List<Object> path : heldExpected.keySet()) {
                Assert.assertEquals(
                        context + " whether the container at " + path
                                + " is still in place",
                        nowExpected.get(path) == heldExpected.get(path),
                        nowActual.get(path) == heldActual.get(path));
                assertSameValue(context + " held container at " + path,
                        heldExpected.get(path), heldActual.get(path));
            }
        }

    }

    /**
     * An {@link Association} whose {@code fetch} makes up a value and whose
     * {@code set} always throws, the way a read-only view does.
     *
     * @author Jeff Nelson
     */
    private static class ReadOnlyAssociation extends Association {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T fetch(String path) {
            return (T) path;
        }

        @Override
        public <T> T set(String path, Object value) {
            throw new UnsupportedOperationException("read only");
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
     * The {@link ReferenceAssociation} twin of {@link ReadOnlyAssociation}.
     *
     * @author Jeff Nelson
     */
    private static class ReadOnlyReferenceAssociation
            extends ReferenceAssociation {

        @SuppressWarnings("unchecked")
        @Override
        public <T> T fetch(String path) {
            return (T) path;
        }

        @Override
        public <T> T set(String path, Object value) {
            throw new UnsupportedOperationException("read only");
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
     * An {@link Association} that records each path that {@code set} receives
     * and each value that {@code get} returns, the way a subclass that audits
     * or caches its reads would.
     *
     * @author Jeff Nelson
     */
    private static class RecordingAssociation extends Association {

        /**
         * The paths that {@code set} received, in order.
         */
        private final List<String> paths = new ArrayList<>();

        /**
         * The values that {@code get} returned, in order.
         */
        private final List<Object> values = new ArrayList<>();

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            values.add(value);
            return value;
        }

        @Override
        public <T> T set(String path, Object value) {
            paths.add(path);
            return super.set(path, value);
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
     * The {@link ReferenceAssociation} twin of {@link RecordingAssociation}.
     *
     * @author Jeff Nelson
     */
    private static class RecordingReferenceAssociation
            extends ReferenceAssociation {

        /**
         * The paths that {@code set} received, in order.
         */
        private final List<String> paths = new ArrayList<>();

        /**
         * The values that {@code get} returned, in order.
         */
        private final List<Object> values = new ArrayList<>();

        @Override
        public Object get(Object key) {
            Object value = super.get(key);
            values.add(value);
            return value;
        }

        @Override
        public <T> T set(String path, Object value) {
            paths.add(path);
            return super.set(path, value);
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
     * An {@link ArrayList} whose {@code add} skips an element that the list
     * already holds, as a list that keeps its elements unique does.
     *
     * @author Jeff Nelson
     */
    private static class UniqueList extends ArrayList<Object> {

        /**
         * The version of the serialized form.
         */
        private static final long serialVersionUID = 1L;

        @Override
        public boolean add(Object element) {
            return !contains(element) && super.add(element);
        }

    }

}
