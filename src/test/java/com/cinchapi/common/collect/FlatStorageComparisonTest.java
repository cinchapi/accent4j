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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.cinchapi.common.profile.Benchmark;

/**
 * Compare what an {@link Association} costs against what a store that keeps
 * each path whole would cost, to say whether moving the nesting from the write
 * to the read is worth a redesign.
 * <p>
 * An {@link Association} nests a path as it is written, so a write pays for the
 * containers whether or not anything reads through them. {@link FlatStore}
 * keeps each path as the key it was given and builds the nested form only when
 * something asks for it, so a write is a map write and a read of a whole path
 * is a map read. What a read of part of a path costs, and what iterating the
 * nested form costs, moves to whoever asks.
 * </p>
 * <p>
 * {@link FlatStore} is a prototype rather than a replacement. It answers the
 * operations these tests measure and nothing else, so the figures say whether
 * the idea earns a real implementation, not that one exists.
 * </p>
 * <p>
 * These tests measure rather than assert, so they are {@link Ignore ignored}
 * and run on request with
 * {@code ./gradlew test --tests "FlatStorageComparisonTest"}. Run one test
 * method at a time, because the methods warm each other's code when they share
 * a JVM.
 * </p>
 *
 * @author Jeff Nelson
 */
@Ignore
public class FlatStorageComparisonTest {

    /**
     * The number of keys each round writes or reads.
     */
    private static final int KEYS = 20;

    /**
     * The number of rounds to measure.
     */
    private static final int ROUNDS = 20000;

    /**
     * The keys naming one component that each round uses.
     */
    private static final String[] FLAT = new String[KEYS];

    /**
     * The keys naming two components that each round uses.
     */
    private static final String[] NESTED = new String[KEYS];

    /**
     * The roots of {@link #NESTED}, which name part of a path rather than all
     * of one.
     */
    private static final String[] ROOTS = new String[KEYS];

    static {
        for (int i = 0; i < KEYS; ++i) {
            FLAT[i] = "key" + i;
            ROOTS[i] = "root" + i;
            NESTED[i] = ROOTS[i] + ".leaf";
        }
    }

    /**
     * Measure {@code work} over {@code ROUNDS} rounds and print its average
     * time for one round and for one key, after a warmup of a tenth as many
     * rounds that the average leaves out.
     *
     * @param label the operation and the store being measured
     * @param work the operation to measure
     */
    private static void report(String label, final Runnable work) {
        Benchmark benchmark = new Benchmark(TimeUnit.NANOSECONDS) {

            @Override
            public void action() {
                work.run();
            }

        };
        benchmark.run(ROUNDS / 10);
        double average = benchmark.average(ROUNDS);
        System.out.println(String.format("%-40s %,10.0f ns %,8.0f ns/key",
                label, average, average / KEYS));
    }

    /**
     * <strong>Goal:</strong> Verify that the prototype builds the same nested
     * form as an {@link Association}, so that the measurements compare two
     * stores that answer the same thing.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Write the same mix of paths, of one, two and three components and
     * sharing roots, into an {@link Association} and a {@link FlatStore}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The nested form of the {@link FlatStore}
     * equals the {@link Association}, and each store answers the same value
     * for every whole path and for every root.
     */
    @Test
    public void testPrototypeBuildsTheSameNestedForm() {
        String[] paths = { "name", "user.given", "user.family",
                "user.address.city", "user.address.zip", "active",
                "deep.a.b.c" };
        Association assoc = Association.of();
        FlatStore flat = new FlatStore();
        for (int i = 0; i < paths.length; ++i) {
            assoc.set(paths[i], "v" + i);
            flat.set(paths[i], "v" + i);
        }

        org.junit.Assert.assertEquals("nested form", new LinkedHashMap<>(assoc),
                new LinkedHashMap<>(flat.nested()));
        for (String path : paths) {
            org.junit.Assert.assertEquals(path, assoc.get(path),
                    flat.get(path));
        }
        for (String root : new String[] { "user", "user.address", "deep",
                "deep.a", "missing" }) {
            org.junit.Assert.assertEquals(root, assoc.get(root),
                    flat.get(root));
        }
    }

    /**
     * <strong>Goal:</strong> Measure a read of a key that the store holds and
     * of one it does not, for both stores, where the key names one component.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Fill an {@link Association} and a {@link FlatStore} with
     * {@code KEYS} keys naming one component.</li>
     * <li>Read each held key, then {@code KEYS} keys neither store holds, from
     * each store, {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each average time is printed.
     */
    @Test
    public void testReadOneComponent() {
        Association assoc = Association.of();
        FlatStore flat = new FlatStore();
        for (String key : FLAT) {
            assoc.set(key, 1);
            flat.set(key, 1);
        }
        report("read held, one component, Association", () -> {
            for (int i = 0; i < KEYS; ++i) {
                assoc.get(FLAT[i]);
            }
        });
        report("read held, one component, flat", () -> {
            for (int i = 0; i < KEYS; ++i) {
                flat.get(FLAT[i]);
            }
        });
        report("read absent, one component, Association", () -> {
            for (int i = 0; i < KEYS; ++i) {
                assoc.get("absent" + i);
            }
        });
        report("read absent, one component, flat", () -> {
            for (int i = 0; i < KEYS; ++i) {
                flat.get("absent" + i);
            }
        });
    }

    /**
     * <strong>Goal:</strong> Measure a read of part of a path, which an
     * {@link Association} answers from the containers it already built and a
     * {@link FlatStore} has to assemble.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Fill both stores with {@code KEYS} keys naming two components.</li>
     * <li>Read each root, which names only the first component, from each
     * store, {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both average times are printed.
     */
    @Test
    public void testReadPartOfAPath() {
        Association assoc = Association.of();
        FlatStore flat = new FlatStore();
        for (int i = 0; i < KEYS; ++i) {
            assoc.set(NESTED[i], i);
            flat.set(NESTED[i], i);
        }
        report("read part of a path, Association", () -> {
            for (int i = 0; i < KEYS; ++i) {
                assoc.get(ROOTS[i]);
            }
        });
        report("read part of a path, flat", () -> {
            for (int i = 0; i < KEYS; ++i) {
                flat.get(ROOTS[i]);
            }
        });
    }

    /**
     * <strong>Goal:</strong> Measure walking the nested form of a store that
     * holds keys naming two components, which an {@link Association} already
     * holds and a {@link FlatStore} has to build.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Fill both stores with {@code KEYS} keys naming two components.</li>
     * <li>Walk every entry of the nested form of each store, {@code ROUNDS}
     * times, reading each value so the walk is not discarded.</li>
     * <li>Walk the nested form of a {@link FlatStore} that is written again
     * before each walk, which is the cost when a write and a walk alternate.
     * </li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each average time is printed.
     */
    @Test
    public void testWalkNestedForm() {
        Association assoc = Association.of();
        FlatStore flat = new FlatStore();
        for (int i = 0; i < KEYS; ++i) {
            assoc.set(NESTED[i], i);
            flat.set(NESTED[i], i);
        }
        report("walk nested form, Association", () -> consume(assoc));
        report("walk nested form, flat", () -> consume(flat.nested()));
        report("walk nested form after a write, flat", () -> {
            flat.set("root0.leaf", 1);
            consume(flat.nested());
        });
    }

    /**
     * <strong>Goal:</strong> Measure a write of keys naming one component and
     * of keys naming two, for both stores.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Write {@code KEYS} keys naming one component into a fresh store of
     * each kind, {@code ROUNDS} times.</li>
     * <li>Write {@code KEYS} keys naming two components into a fresh store of
     * each kind, {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each average time is printed.
     */
    @Test
    public void testWrite() {
        report("write one component, Association", () -> {
            Association assoc = Association.of();
            for (int i = 0; i < KEYS; ++i) {
                assoc.set(FLAT[i], i);
            }
        });
        report("write one component, flat", () -> {
            FlatStore flat = new FlatStore();
            for (int i = 0; i < KEYS; ++i) {
                flat.set(FLAT[i], i);
            }
        });
        report("write two components, Association", () -> {
            Association assoc = Association.of();
            for (int i = 0; i < KEYS; ++i) {
                assoc.set(NESTED[i], i);
            }
        });
        report("write two components, flat", () -> {
            FlatStore flat = new FlatStore();
            for (int i = 0; i < KEYS; ++i) {
                flat.set(NESTED[i], i);
            }
        });
    }

    /**
     * Read every value of {@code map}, and of every map it holds, so that a
     * walk of it is not discarded as work with no result.
     *
     * @param map the map to walk
     * @return the number of values the walk read
     */
    @SuppressWarnings("unchecked")
    private static int consume(Map<String, Object> map) {
        int seen = 0;
        for (Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if(value instanceof Map) {
                seen += consume((Map<String, Object>) value);
            }
            else {
                ++seen;
            }
        }
        return seen;
    }

    /**
     * A store that keeps each path as the key it was given and builds the
     * nested form only when something asks for it.
     * <p>
     * This prototype answers a write, a read of a whole path, a read of part of
     * a path, and a walk of the nested form. It leaves out everything else an
     * {@link Association} provides, including lists inside a path, the limit on
     * how many list positions a write may skip, and the containers a subclass
     * supplies.
     * </p>
     *
     * @author Jeff Nelson
     */
    static class FlatStore {

        /**
         * Each path, whole, mapped to the value written under it.
         */
        private final Map<String, Object> paths = new LinkedHashMap<>();

        /**
         * The nested form of {@link #paths}, or {@code null} when a write has
         * happened since it was built.
         */
        private Map<String, Object> nested = null;

        /**
         * {@code true} once any path holds a dot, which is when a write to a
         * shorter path can shadow a longer one.
         */
        private boolean deep = false;

        /**
         * Return the value written under the whole {@code path}, or the nested
         * form of what the paths below it hold.
         *
         * @param path a path key
         * @return the value under {@code path}, or {@code null} if nothing is
         *         held there
         */
        @SuppressWarnings("unchecked")
        Object get(String path) {
            Object value = paths.get(path);
            if(value == null && deep) {
                // Nothing was written under the whole path, so a value can
                // only come from the containers that its components name.
                Object source = nested();
                int from = 0;
                while (source instanceof Map && from <= path.length()) {
                    int dot = path.indexOf('.', from);
                    String component = dot < 0 ? path.substring(from)
                            : path.substring(from, dot);
                    source = ((Map<String, Object>) source).get(component);
                    from = dot < 0 ? path.length() + 1 : dot + 1;
                }
                return from > path.length() ? source : null;
            }
            else {
                return value;
            }
        }

        /**
         * Return the nested form of every path this store holds, building it if
         * a write has happened since it was last built.
         *
         * @return the nested form
         */
        Map<String, Object> nested() {
            if(nested == null) {
                nested = build();
            }
            return nested;
        }

        /**
         * Write {@code value} under the whole {@code path}.
         *
         * @param path a path key
         * @param value the value to write
         * @return the value that {@code path} held before
         */
        Object set(String path, Object value) {
            nested = null;
            if(path.indexOf('.') >= 0) {
                deep = true;
            }
            else if(deep) {
                shadow(path);
            }
            return paths.put(path, value);
        }

        /**
         * Return the nested form of every path this store holds.
         *
         * @return a new nested form
         */
        @SuppressWarnings("unchecked")
        private Map<String, Object> build() {
            Map<String, Object> root = new LinkedHashMap<>();
            for (Entry<String, Object> entry : paths.entrySet()) {
                String path = entry.getKey();
                int dot = path.indexOf('.');
                if(dot < 0) {
                    root.put(path, entry.getValue());
                }
                else {
                    Map<String, Object> container = root;
                    int from = 0;
                    while (dot >= 0) {
                        String component = path.substring(from, dot);
                        Object held = container.get(component);
                        if(held instanceof Map) {
                            container = (Map<String, Object>) held;
                        }
                        else {
                            Map<String, Object> next = new LinkedHashMap<>();
                            container.put(component, next);
                            container = next;
                        }
                        from = dot + 1;
                        dot = path.indexOf('.', from);
                    }
                    container.put(path.substring(from), entry.getValue());
                }
            }
            return root;
        }

        /**
         * Remove every path that names {@code root} and then more, because a
         * write to {@code root} replaces what they held.
         *
         * @param root a path of one component
         */
        private void shadow(String root) {
            String prefix = root + '.';
            Iterator<String> it = paths.keySet().iterator();
            while (it.hasNext()) {
                if(it.next().startsWith(prefix)) {
                    it.remove();
                }
            }
        }

    }

}
