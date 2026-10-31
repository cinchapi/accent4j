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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.Ignore;
import org.junit.Test;

import com.cinchapi.common.profile.Benchmark;

/**
 * Measure what an {@link Association} costs for the keys that most callers
 * write, which are keys naming one component.
 * <p>
 * An {@link Association} keeps its data so that a key naming several
 * components reads as a path into nested containers. A key naming one
 * component needs none of that, and a caller that serializes a record writes
 * almost nothing else, so what a one-component key costs sets what such a
 * caller pays. A {@link LinkedHashMap} of the same keys stands in for the
 * floor.
 * </p>
 * <p>
 * These tests measure rather than assert, so they are {@link Ignore ignored}.
 * JUnit skips an ignored class even when a filter selects it, so to run them,
 * remove the annotation in a local checkout and run
 * {@code ./gradlew test --tests "AssociationPerformanceTest"}. The printed
 * lines appear in the test report. The numbers move with the machine, so
 * compare runs from one machine.
 * </p>
 *
 * @author Jeff Nelson
 */
@Ignore
public class AssociationPerformanceTest {

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

    static {
        for (int i = 0; i < KEYS; ++i) {
            FLAT[i] = "key" + i;
            NESTED[i] = "root" + i + ".leaf";
        }
    }

    /**
     * Measure {@code work} over {@code ROUNDS} rounds and print its average
     * time for one round and for one key, after a warmup of a tenth as many
     * rounds that the average leaves out.
     *
     * @param label the operation being measured, for the printed line
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
        System.out.println(String.format("%-34s %,10.0f ns %,8.0f ns/key",
                label, average, average / KEYS));
    }

    /**
     * <strong>Goal:</strong> Measure {@link Association#get(Object)} for a key
     * that names one component, which is what a caller merging a map into a
     * map reads for each entry.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an {@link Association} holding {@code KEYS} keys.</li>
     * <li>Read each of those keys, {@code ROUNDS} times.</li>
     * <li>Read {@code KEYS} keys it does not hold, {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both average times are printed.
     */
    @Test
    public void testGetFlatKeys() {
        Association assoc = Association.of();
        for (String key : FLAT) {
            assoc.set(key, 1);
        }
        report("get present, one component", () -> {
            for (int i = 0; i < KEYS; ++i) {
                assoc.get(FLAT[i]);
            }
        });
        report("get absent, one component", () -> {
            for (int i = 0; i < KEYS; ++i) {
                assoc.get("absent" + i);
            }
        });
    }

    /**
     * <strong>Goal:</strong> Measure a fetch of a key that names one component
     * and was never written, which is what a write does before it stores.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Build an {@link Association} holding {@code KEYS} keys.</li>
     * <li>Fetch {@code KEYS} keys that it does not hold,
     * {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The average time is printed.
     */
    @Test
    public void testFetchAbsentFlatKeys() {
        Association assoc = Association.of();
        for (String key : FLAT) {
            assoc.set(key, 1);
        }
        report("fetch absent, one component", () -> {
            for (int i = 0; i < KEYS; ++i) {
                assoc.fetch("absent" + i);
            }
        });
    }

    /**
     * <strong>Goal:</strong> Measure a write of keys that name one component,
     * against a {@link LinkedHashMap} of the same keys.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Write {@code KEYS} keys naming one component into a fresh
     * {@link Association}, {@code ROUNDS} times.</li>
     * <li>Write the same keys into a fresh {@link LinkedHashMap},
     * {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both average times are printed.
     */
    @Test
    public void testWriteFlatKeys() {
        report("write, one component", () -> {
            Association assoc = Association.of();
            for (int i = 0; i < KEYS; ++i) {
                assoc.set(FLAT[i], i);
            }
        });
        report("write, LinkedHashMap floor", () -> {
            Map<String, Object> map = new LinkedHashMap<>();
            for (int i = 0; i < KEYS; ++i) {
                map.put(FLAT[i], i);
            }
        });
    }

    /**
     * <strong>Goal:</strong> Measure a write of keys that name two components,
     * which is the shape an {@link Association} exists to hold.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Write {@code KEYS} keys naming two components into a fresh
     * {@link Association}, {@code ROUNDS} times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The average time is printed.
     */
    @Test
    public void testWriteNestedKeys() {
        report("write, two components", () -> {
            Association assoc = Association.of();
            for (int i = 0; i < KEYS; ++i) {
                assoc.set(NESTED[i], i);
            }
        });
    }

}
