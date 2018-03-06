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

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of functions that efficiently operate on {@link Map maps}.
 * 
 * @author Jeff Nelson
 */
public final class AnyMaps {

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
