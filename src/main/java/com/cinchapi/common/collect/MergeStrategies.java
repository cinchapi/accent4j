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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A collection of strategies for
 * {@link Association#merge(java.util.Map, java.util.function.BiFunction)} and
 * {@link java.util.Map#merge(Object, Object, java.util.function.BiFunction)}.
 *
 * @author Jeff Nelson
 */
public final class MergeStrategies {

    private MergeStrategies() {/* no-init */}

    /**
     * Return a merge strategy that uses the incoming value if there is a
     * conflict with an existing value in the into {@link Map}.
     * 
     * @return the result of applying the strategy
     */
    public static Object theirs(Object ours, Object theirs) {
        return theirs;
    }

    /**
     * Return a merge strategy that uses the existing value if there is a
     * conflict with an incoming value in the from {@link Map}.
     * 
     * @return the result of applying the strategy
     */
    public static Object ours(Object ours, Object theirs) {
        return ours;
    }

    /**
     * Return a merge strategy that will concatenate the stored and incoming
     * values if there is a conflict between the into and from
     * {@link Map maps}.
     * <p>
     * This strategy will map the conflicting key to a collection that contains
     * the value from the into map (or values if the value is a
     * collection) followed by the value (or values) from the from map.
     * </p>
     * 
     * @return the result of applying the strategy
     */
    @SuppressWarnings("unchecked")
    public static Object concat(Object ours, Object theirs) {
        if(ours instanceof Collection && theirs instanceof Collection) {
            ((Collection<Object>) ours).addAll((Collection<Object>) theirs);
            return ours;
        }
        else if(ours instanceof Collection) {
            ((Collection<Object>) ours).add(theirs);
            return ours;
        }
        else if(theirs instanceof Collection) {
            List<Object> merged = Lists.newArrayList(ours);
            merged.addAll((Collection<Object>) theirs);
            return merged;
        }
        else {
            return Lists.newArrayList(ours, theirs);
        }
    }

    /**
     * "Upsert" {@code from} one object {@code into} another one. The nature of
     * the implementation changes depending on the types of the two objects. In
     * general, this method attempts to "merge" objects (depending upon a
     * definition of merging that makes sense for the object type) in a manner
     * that prefers {@code into} data if there's ever a conflict with
     * {@code from} data. The rules of upsertion within this method are that
     * <ul>
     * <li>If the two objects don't have the same type, then {@code #into} is
     * returned if it is not null; otherwise {@code from} is not
     * {@code null}.</li>
     * <li>If both objects are {@link Collection collections}, the items from
     * {@code into} are preferred if there is a corresponding item in the same
     * position in {@code from}. If the item at a position in {@code into} is
     * {@code null}, but there is a non-null item in the corresponding position
     * in {@code from}, the item in {@code from} is preferred.</li>
     * <li>If both objects are {@link Map maps}, this function will perform a
     * {@link Map#merge(String, Object, BiFunction} of the data in {@code from}
     * to {@code into} with a merge function that recursively calls this
     * method.</li>
     * </ul>
     * 
     * @param ours
     * @param theirs
     * 
     * 
     * @return the object after the upset or {@code null} if both {@code from}
     *         and {@code into} are null
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static Object upsert(Object ours, Object theirs) {
        if(theirs instanceof Collection && ours instanceof Collection) {
            Iterator<Object> tit = ((Collection<Object>) theirs).iterator();
            Iterator<Object> oit = ((Collection<Object>) ours).iterator();
            Collection<Object> collection = theirs instanceof Set
                    ? Sets.newLinkedHashSet()
                    : Lists.newArrayList();
            while (tit.hasNext()) {
                if(oit.hasNext()) {
                    Object f = tit.next();
                    Object i = oit.next();
                    collection.add(upsert(i, f));
                }
                else {
                    break;
                }
            }
            while (oit.hasNext()) {
                collection.add(oit.next());
            }
            while (tit.hasNext()) {
                collection.add(tit.next());
            }
            return collection;
        }
        else if(theirs instanceof Map && ours instanceof Map) {
            // Note the #upsert #into the destination map, happens in-place and
            // the same is returned.
            ((Map<String, Object>) theirs).forEach((key, value) -> {
                if(value == null) {
                    // The merge function prevents upserting a null value, so
                    // perform an explicit put as a workaround.
                    ((Map<String, Object>) ours).put(key, value);
                }
                else {
                    ((Map<String, Object>) ours).merge(key, value,
                            MergeStrategies::upsert);
                }
            });
            return ours;
        }
        else {
            return theirs != null ? theirs : ours;
        }
    }

}
