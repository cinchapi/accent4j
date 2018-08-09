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

import javax.annotation.Nullable;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

/**
 * A Collective is a {@link Collection} of {@link Object objects} that can
 * standalone or be contained within another structure.
 *
 * @author Jeff Nelson
 */
public final class Collectives {

    /**
     * Intelligently merged collectives {@code a} and {@code b}.
     * 
     * @param a
     * @param b
     * @return the merged collection
     */
    public static Collection<Object> merge(Collection<Object> a,
            Collection<Object> b) {
        int i = 0;
        int asize = a.size();
        int bsize = b.size();
        Collection<Object> merged = Lists
                .newArrayListWithCapacity(Math.max(asize, bsize));
        for (; i < Math.min(asize, bsize); ++i) {
            Object ai = Iterables.get(a, i);
            Object bi = Iterables.get(b, i);
            Object merger = merge(ai, bi);
            if(merger != null) {
                merged.add(merger);
            }
            else {
                if(ai != null) {
                    merged.add(ai);
                }
                if(bi != null) {
                    merged.add(bi);
                }
            }
        }
        for (; i < a.size(); ++i) {
            merged.add(Iterables.get(a, i));
        }
        for (; i < b.size(); ++i) {
            merged.add(Iterables.get(b, i));
        }
        return merged;
    }

    /**
     * Intelligently merge two collective maps.
     * 
     * @param into
     * @param from
     * @return the merged map
     */
    public static Map<String, Collection<Object>> merge(
            Map<String, Collection<Object>> into,
            Map<String, Collection<Object>> from) {
        return Streams
                .concat(into.entrySet().stream(), from.entrySet().stream())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue,
                        Collectives::merge));
    }

    /**
     * Merge {@code a} and {@code b}, if possible. If the objects cannot be
     * merged, this method returns {@code null}.
     * 
     * @param a
     * @param b
     * @return the merged object or {@code null} if merging isn't possible
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Object merge(@Nullable Object a, @Nullable Object b) {
        if(a != null && b == null) {
            return a;
        }
        else if(a == null && b != null) {
            return b;
        }
        else if(a instanceof Map && b instanceof Map) {
            return AnyMaps.merge((Map<String, Object>) a,
                    (Map<String, Object>) b);
        }
        else if(a instanceof Collection && b instanceof Collection) {
            return Collections.concat((Collection<Object>) a,
                    (Collection<Object>) b);
        }
        else {
            return null;
        }
    }

    /**
     * Intelligently merge the {@code from} collective {@code into} the other
     * one, in place.
     * 
     * @param into
     * @param from
     */
    public static void mergeInPlace(Map<String, Collection<Object>> into,
            Map<String, Collection<Object>> from) {
        into = merge(into, from);
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

    private Collectives() {/* no-init */}

}
