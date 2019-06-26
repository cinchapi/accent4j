/*
 * Copyright (c) 2016 Cinchapi Inc.
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.Array;
import com.cinchapi.common.base.Verify;
import com.cinchapi.common.collect.lazy.JITFilterMap;
import com.cinchapi.common.collect.lazy.JITFilterValuesMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;

/**
 * A collection of functions that efficiently operate on {@link Map maps}.
 * 
 * @author Jeff Nelson
 */
public final class AnyMaps {

    /**
     * Return a {@link Map} that contains the specified
     * {@code key}/{@code value} pair.
     * 
     * @param key
     * @param value
     * @return the {@link Map}
     */
    public static <K, V> Map<K, V> create(K key, V value) {
        LinkedHashMap<K, V> map = Maps.newLinkedHashMap();
        map.put(key, value);
        return map;
    }

    /**
     * Explode a "flat" map that contains navigable keys to a nested structure
     * that can be traversed using the {@link #navigate(String, Map)} method.
     * <p>
     * For example, if the provided {@code map} has a key {@code foo.bar.0} that
     * maps to the value "baz", this method will return a map that contains a
     * mapping from {@code foo} to a mapping from {@code bar} to a list that
     * contains the value "baz" at position 0.
     * <p>
     * <p>
     * The map returned from this method is navigable such that a query for any
     * of the keys in the original {@code map} will return the same associated
     * value from the {@link #navigate(String, Map)} method. The advantage of
     * exploding a map is that it makes it possible to fetch nested inner
     * structures from a navigable query on one of the parent keys (e.g. the
     * example above would return a map if one were to navigate for "foo")
     * </p>
     * 
     * @param map
     * @return a nested map
     * @deprecated in favor of the {@link Association} framework. Create an
     *             {@link Association} using {@link Association#of(Map)} with a
     *             "flat" map. The map will be exploded, implicitly and
     *             traversable using the {@link Association#fetch(String)}
     *             method in a manner that is similar to the way the
     *             {@link #navigate(String, Map)} method worked.
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Deprecated
    public static Map<String, Object> explode(Map<String, Object> map) {
        Map<String, Object> exploded = Maps.newLinkedHashMap();
        map.forEach((key, value) -> {
            String[] components = key.split("\\.");
            String[] reversed = Array.reverse(components);
            Verify.thatArgument(Ints.tryParse(components[0]) == null,
                    "The map cannot contain keys that start with a numeric component. "
                            + "'{}' is an invalid key.",
                    key);
            String originalKey = key;
            try {
                for (String component : reversed) {
                    key = component;
                    components = Arrays.copyOf(components,
                            components.length - 1);
                    String path = String.join(".", components);
                    Object container;
                    Integer index;
                    container = navigate(path, exploded);
                    if((index = Ints.tryParse(component)) != null) {
                        if(container == null) {
                            container = Lists.newArrayList();
                        }
                        List list = (List) container;
                        for (int i = list.size(); i < index; ++i) { // Pad the
                                                                    // list, if
                                                                    // necessary
                            list.add(null);
                        }
                        if(index < list.size()) {
                            // This means we are modifying an existing item in
                            // the list, so we must be sure to upsert instead of
                            // adding and shifting elements around.
                            list.set(index, value);
                        }
                        else {
                            list.add(index, value);
                        }
                    }
                    else {
                        if(container == null) {
                            container = Maps.newLinkedHashMap();
                        }
                        ((Map) container).put(component, value);
                    }
                    value = container;
                }
                exploded.putAll((Map) value);
            }
            catch (ClassCastException e) {
                throw new IllegalArgumentException(AnyStrings.format(
                        "Cannot explode '{}' because the path leads to a different "
                                + "type tree than a previously exploded key.",
                        originalKey));
            }
        });
        return exploded;
    }

    /**
     * Merge the data {@code from} one {@link Map} {@code into} another one.
     * 
     * @param into the Map to merge into
     * @param from the Map to merge from
     * @return the merged {@link Map}
     */
    public static Map<String, Object> merge(Map<String, Object> into,
            Map<String, Object> from) {
        return merge(into, from, MergeStrategies::concat);
    }

    /**
     * Merge the data {@code from} one {@link Map} {@code into} another one
     * using the provided merge {@code strategy}.
     * 
     * @param into the Map to merge into
     * @param from the Map to merge from
     * @param strategy a {@link MergeStrategies merge strategy} to use when a
     *            key in the {@code from} map already exists in the the
     *            {@code into} map
     * @return the merged {@link Map}
     */

    public static <T, Q> Map<T, Q> filterValuesJustInTime(Map<T, Q> x, Predicate<Q> check) {
        return new JITFilterMap<>(x, check);
    }

    public static Map<String, Object> merge(Map<String, Object> into,
            Map<String, Object> from,
            BiFunction<Object, Object, Object> strategy) {
        Map<String, Object> merged = Maps.newHashMap(into);
        mergeInPlace(merged, from, strategy);
        return merged;
    }

    /**
     * Perform the {@link #merge(Map, Map)} of the data {@code from} the first
     * map {@code into} the second one in-place.
     * 
     * @param into
     * @param from
     */
    public static void mergeInPlace(Map<String, Object> into,
            Map<String, Object> from) {
        mergeInPlace(into, from, MergeStrategies::concat);
    }

    /**
     * Perform the {@link #merge(Map, Map)} of the data {@code from} the first
     * map {@code into} the second one in-place using the provided merge
     * {@code strategy}.
     * 
     * @param into
     * @param from
     */
    public static void mergeInPlace(Map<String, Object> into,
            Map<String, Object> from,
            BiFunction<Object, Object, Object> strategy) {
        from.forEach((key, value) -> {
            if(value == null) {
                boolean merged = false;
                while (!merged) {
                    if(into.containsKey(key)) {
                        Object ours = into.get(key);
                        merged = into.replace(key, ours,
                                strategy.apply(ours, value));
                    }
                    else {
                        merged = into.putIfAbsent(key,
                                strategy.apply(null, value)) == null;
                    }
                }
            }
            else {
                into.merge(key, value, strategy);
            }
        });
    }

    /**
     * Return a possibly nested value in a {@link Map} that contains other maps
     * and collections as values.
     * 
     * @param path a navigable path key (e.g. foo.bar.1.baz)
     * @return the value
     * @deprecated in favor if the {@link Association} framework. Use
     *             {@link Association#of(Map)} to create an {@link Association}
     *             followed by {@link Association#fetch(String)} to query the
     *             {@code path}
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> T navigate(String path,
            Map<String, ? extends Object> map) {
        T value = (T) map.get(path); // First, see if the path has been directly
                                     // added to the map
        if(value == null) {
            String[] components = path.split("\\.");
            Object lookup = map;
            for (String component : components) {
                Integer index;
                if(lookup == null) {
                    break;
                }
                else if((index = Ints.tryParse(component)) != null) {
                    lookup = lookup instanceof Iterable
                            ? Iterables.get((Iterable<T>) lookup, index)
                            : null;
                }
                else {
                    lookup = lookup instanceof Map
                            ? ((Map<String, Object>) lookup).get(component)
                            : null;
                }
            }
            if(lookup != null) {
                value = (T) lookup;
            }
        }
        return value;
    }

    /**
     * Return a <em>mutable</em>, insertion-ordered {@link LinkedHashMap}
     * instance with enough room to fit {@code capacity} items.
     * 
     * <p>
     * Use this method over
     * {@link com.google.common.collect.Maps#newLinkedHashMap()} when the size
     * of the map is known in advance and we are being careful to not oversize
     * collections.
     * </p>
     * 
     * @param capacity the initial capacity
     * @return a new, empty {@link LinkedHashMap}
     */
    public static <K, V> Map<K, V> newLinkedHashMapWithCapacity(int capacity) {
        return new LinkedHashMap<K, V>(capacity);
    }

    /**
     * Rename {@code key} to {@code newKey} within the {@code map} using the
     * concurrency guarantees of the {@code map}'s type.
     * 
     * @param key
     * @param newKey
     * @param map
     */
    public static <T> void rename(String key, String newKey,
            Map<String, T> map) {
        if(map.containsKey(key)) {
            T value = map.get(key);
            if(map.remove(key, value)) {
                map.put(newKey, value);
            }
            else {
                rename(key, newKey, map);
            }
        }
    }

}
