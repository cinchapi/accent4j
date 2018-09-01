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
import java.util.List;

import com.google.common.collect.Lists;

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

}
