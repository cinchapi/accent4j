/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.base.AnyStrings;
import com.google.common.collect.Lists;

/**
 * Unit tests for {@link CoalescableTreeMap}.
 *
 * @author Jeff Nelson
 */
public class CoalescableTreeMapTest {

    @Test
    public void testCoalesce() {
        Random rand = new Random();
        Comparator<String> comparator = (s1, s2) -> {
            int c = s1.compareToIgnoreCase(s2);
            if(c == 0) {
                c = s1.compareTo(s2);
            }
            return c;
        };
        CoalescableTreeMap<String, String> map = new CoalescableTreeMap<>(
                comparator);
        String[] chars = "123456789abcdefghijklmnopqrstuvwxyz".split("");
        List<String> candidates = Lists.newArrayList();
        for (String $char : chars) {
            candidates.add($char + "eff");
            if(AnyStrings.tryParseNumber($char) == null) {
                candidates.add($char.toUpperCase() + "eff");
            }
        }
        java.util.Collections.shuffle(candidates);
        for (String candidate : candidates) {
            map.put(candidate,
                    "" + System.currentTimeMillis() + rand.nextInt());
        }
        Assert.assertEquals(candidates.size(), map.size());
        Map<String, String> data = map.coalesce("jeff",
                (key, candidate) -> key.equalsIgnoreCase(candidate));
        Assert.assertEquals(2, data.size());
        Iterator<String> it = data.keySet().iterator();
        Assert.assertEquals(map.get("Jeff"), data.get(it.next()));
        Assert.assertEquals(map.get("jeff"), data.get(it.next()));
        map.coalesce("1eff", (key, candidate) -> false); // No NPE
    }

    /**
     * <strong>Goal:</strong> Verify that coalescing keeps every key that the
     * map keeps apart, even when two of those keys are equal to each other.
     * <p>
     * <strong>Start state:</strong> A map that orders {@link Label Labels} by
     * text and then by kind, so two {@link Label Labels} with the same text
     * and different kinds are equal but are separate keys.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Map a {@link Label} with the text {@code b} under each of two kinds,
     * and map {@link Label Labels} with other text on either side.</li>
     * <li>Coalesce the first {@code b} key with a matcher that accepts any key
     * with the same text.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The result holds both {@code b} keys in the
     * map's order, and each one maps to its own value.
     */
    @Test
    public void testCoalesceKeepsDistinctKeysThatAreEqual() {
        CoalescableTreeMap<Label, String> map = new CoalescableTreeMap<>(
                Label.ORDER);
        Label first = new Label("b", 0);
        Label second = new Label("b", 1);
        map.put(new Label("a", 0), "a");
        map.put(first, "first");
        map.put(second, "second");
        map.put(new Label("c", 0), "c");
        Map<Label, String> data = map.coalesce(first,
                (key, candidate) -> key.text.equals(candidate.text));
        Assert.assertEquals(Lists.newArrayList("first", "second"),
                Lists.newArrayList(data.values()));
        Assert.assertEquals("first", data.get(first));
        Assert.assertEquals("second", data.get(second));
    }

    /**
     * <strong>Goal:</strong> Verify that a map in natural order coalesces the
     * consecutive keys that match, in ascending order.
     * <p>
     * <strong>Start state:</strong> A map, created without a
     * {@link Comparator}, from the integers 1 through 5 to their text.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Coalesce the key 3 with a matcher that accepts a key within 1 of
     * it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The result maps 2, 3 and 4, in that order, to
     * their text.
     */
    @Test
    public void testCoalesceInNaturalOrder() {
        CoalescableTreeMap<Integer, String> map = new CoalescableTreeMap<>();
        for (int i = 1; i <= 5; ++i) {
            map.put(i, Integer.toString(i));
        }
        Map<Integer, String> data = map.coalesce(3,
                (key, candidate) -> Math.abs(key - candidate) <= 1);
        Assert.assertEquals(Lists.newArrayList(2, 3, 4),
                Lists.newArrayList(data.keySet()));
        Assert.assertEquals(Lists.newArrayList("2", "3", "4"),
                Lists.newArrayList(data.values()));
    }

    /**
     * A key whose equality looks only at its {@link #text}, so two
     * {@link Label Labels} with the same text and different {@link #kind
     * kinds} are equal but sort apart in {@link #ORDER}.
     *
     * @author Jeff Nelson
     */
    private static final class Label {

        /**
         * Order {@link Label Labels} by {@link #text} and then by
         * {@link #kind}.
         */
        static final Comparator<Label> ORDER = Comparator
                .<Label, String> comparing(label -> label.text)
                .thenComparingInt(label -> label.kind);

        /**
         * The text, which alone decides equality.
         */
        final String text;

        /**
         * The kind, which only decides order.
         */
        final int kind;

        /**
         * Construct a new instance.
         *
         * @param text
         * @param kind
         */
        Label(String text, int kind) {
            this.text = text;
            this.kind = kind;
        }

        @Override
        public boolean equals(Object obj) {
            if(obj instanceof Label) {
                return text.equals(((Label) obj).text);
            }
            else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }

        @Override
        public String toString() {
            return text + "/" + kind;
        }

    }

}
