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

import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.Array;
import com.cinchapi.common.base.Verify;
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
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
                    if((index = Ints.tryParse(component)) != null) {
                        container = navigate(path, exploded);
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
                        container = navigate(path, exploded);
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

    /**
     * Return a possibly nested value in a {@link Map} that contains other maps
     * and collections as values.
     * 
     * @param path a navigable path key (e.g. foo.bar.1.baz)
     * @return the value
     */
    @SuppressWarnings("unchecked")
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

}
