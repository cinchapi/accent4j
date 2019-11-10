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

import java.util.Map.Entry;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiPredicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A {@link TreeMap} that contains {@link #coalesce(Object, BiPredicate)
 * functionality} that returns data for a range of consecutive keys that are sufficiently similar.
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

    public CoalescableTreeMap(Comparator<K> comparator) {
        super(comparator);
    }

    /**
     * Return a {@link Set} containing the values mapped from {@code key} and
     * any "nearby" keys that are determined by the {@code matcher} to pass the
     * desired threshold of similarity to {@code key}.
     * <p>
     * This method only coalesces consecutive ranges of keys that are
     * immediately "lower" or "higher" than the lookup {@code key}. This method
     * evaluates keys on both side of {@code key} until it reaches on that the
     * {@code matcher} determines is not similar enough.
     * </p>
     * 
     * @param key
     * @param matcher a {@link BiPredicate} that takes the lookup {@code key}
     *            and a potentially coalescible key and returns a boolean that
     *            determines whether the key can be coalesced
     * @return A {@link Set} containing the values that the {@code matcher}
     *         determines should coalesce to the values mapped from the
     *         {@code key}
     */
    public Set<V> coalesce(K key, BiPredicate<K, K> matcher) {
        V matched = get(key);
        List<V> lower = Lists.newArrayList();
        List<V> higher = Lists.newArrayList();
        Set<V> coalesced = Sets.newLinkedHashSet();
        K current = key;
        while (current != null) {
            Entry<K, V> next = lowerEntry(current);
            if(next != null && matcher.test(key, next.getKey())) {
                lower.add(0, next.getValue());
                current = next.getKey();
            }
            else {
                current = null;
            }
        }
        current = key;
        while (current != null) {
            Entry<K, V> next = higherEntry(current);
            if(next != null && matcher.test(key, next.getKey())) {
                higher.add(0, next.getValue());
                current = next.getKey();
            }
            else {
                current = null;
            }
        }
        coalesced.addAll(lower);
        if(matched != null) {
            coalesced.add(matched);
        }
        coalesced.addAll(higher);
        return coalesced;
    }

}
