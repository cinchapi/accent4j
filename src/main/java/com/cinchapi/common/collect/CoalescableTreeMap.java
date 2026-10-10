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
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.function.BiPredicate;

/**
 * A {@link TreeMap} that contains {@link #coalesce(Object, BiPredicate)
 * functionality} that returns data for a range of consecutive keys that are
 * sufficiently similar.
 *
 * @author Jeff Nelson
 */
public class CoalescableTreeMap<K, V> extends TreeMap<K, V> {

    private static final long serialVersionUID = 1L;

    /**
     * Construct a new instance.
     */
    public CoalescableTreeMap() {
        super();
    }

    /**
     * Construct a new instance that orders its keys with {@code comparator}.
     *
     * @param comparator the {@link Comparator} that orders the keys
     */
    public CoalescableTreeMap(Comparator<K> comparator) {
        super(comparator);
    }

    /**
     * Return a {@link Map} containing the entries for keys nearby and including
     * {@code key} and that are determined by the {@code matcher} to pass the
     * desired threshold of similarity to {@code key}.
     * <p>
     * This method only coalesces consecutive ranges of entries that are
     * immediately "lower" or "higher" than the lookup {@code key}. This method
     * evaluates keys on both sides of {@code key} until it reaches one that the
     * {@code matcher} determines is not similar enough.
     * </p>
     * <p>
     * The returned {@link Map} is a copy in the order of this map. It compares
     * keys with the {@link #comparator() comparator} of this map, so it keeps
     * every key that this map keeps apart, even keys that are
     * {@link Object#equals(Object) equal} to each other. Each of its keys is
     * the key that this map holds, including the key that sorts in the place
     * of {@code key}, which coalesces even when the {@code matcher} rejects
     * it.
     * </p>
     * <p>
     * The returned {@link Map} omits each key that maps to {@code null},
     * including {@code key}. A neighbor that maps to {@code null} does not end
     * the range, so the keys beyond it coalesce when the {@code matcher}
     * accepts them.
     * </p>
     *
     * @param key
     * @param matcher a {@link BiPredicate} that takes the lookup {@code key}
     *            and a potentially coalescible key and returns a boolean that
     *            determines whether the key can be coalesced
     * @return A {@link Map} containing the entries that the {@code matcher}
     *         determines should coalesce to the values mapped from the
     *         {@code key}
     */
    public Map<K, V> coalesce(K key, BiPredicate<K, K> matcher) {
        Map<K, V> coalesced = new TreeMap<>(comparator());
        // Iterating away from the key in each direction reaches each neighbor
        // from the last one, instead of searching the whole map again for it.
        // The walk down starts at the key's own place, which is compared to
        // the key only if the matcher rejects it.
        boolean first = true;
        for (Entry<K, V> entry : headMap(key, true).descendingMap()
                .entrySet()) {
            if(matcher.test(key, entry.getKey())
                    || (first && compare(entry.getKey(), key) == 0)) {
                if(entry.getValue() != null) {
                    coalesced.put(entry.getKey(), entry.getValue());
                }
                first = false;
            }
            else {
                break;
            }
        }
        for (Entry<K, V> entry : tailMap(key, false).entrySet()) {
            if(matcher.test(key, entry.getKey())) {
                if(entry.getValue() != null) {
                    coalesced.put(entry.getKey(), entry.getValue());
                }
            }
            else {
                break;
            }
        }
        return coalesced;
    }

    /**
     * Compare {@code k1} to {@code k2} in the order of this map.
     *
     * @param k1
     * @param k2
     * @return a negative number, zero or a positive number as {@code k1} sorts
     *         before, in the place of or after {@code k2}
     */
    @SuppressWarnings("unchecked")
    private int compare(K k1, K k2) {
        Comparator<? super K> comparator = comparator();
        if(comparator != null) {
            return comparator.compare(k1, k2);
        }
        else {
            return ((Comparable<? super K>) k1).compareTo(k2);
        }
    }

}
