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

import java.util.AbstractMap;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;

/**
 * A Collective is a {@link Collection} of {@link Object objects} that can
 * standalone or be contained within another structure.
 *
 * @author Jeff Nelson
 */
public final class Collectives {

    /**
     * Return a {@link Map} containing all the data in the original {@code map}
     * where each value is wrapped in a {@link Collection}.
     * <p>
     * NOTE: This method will recursively extract collectives from the original
     * values, where possible
     * </p>
     * 
     * @param map
     * @return a {@link Map} where each value is a {@link Collection}
     */
    public static Map<String, Collection<Object>> within(
            Map<String, Object> map) {
        return map.entrySet().stream().map(Collectives::within)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    /**
     * Return an {@link Entry} where the original {@code entry} value is wrapped
     * in a {@link Collection}.
     * <p>
     * NOTE: This method will recursively extract collectives from the original
     * values, where possible
     * </p>
     * 
     * @param entry
     * @return an {@link Entry} whose value is a {@link Collection}
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Entry<String, Collection<Object>> within(
            Entry<String, Object> entry) {
        return new AbstractMap.SimpleImmutableEntry(entry.getKey(),
                over(entry.getValue()));
    }

    /**
     * Ensure that the {@code object} is already a {@link Collection} or is
     * transformed into one.
     * <p>
     * NOTE: This method will recursively extract collectives from the original
     * object, where possible
     * </p>
     * 
     * @param object
     * @return a {@link Collection}
     */
    @SuppressWarnings("unchecked")
    public static Collection<Object> over(Object object) {
        if(object instanceof Collection) {
            return ((Collection<Object>) object).stream()
                    .map(item -> item instanceof Map
                            ? within((Map<String, Object>) item)
                            : item)
                    .collect(Collectors.toList());
        }
        else if(object instanceof Map) {
            return ImmutableList.of(within((Map<String, Object>) object));
        }
        else {
            return ImmutableList.of(object);
        }

    }

    private Collectives() {/* no-init */}

}
