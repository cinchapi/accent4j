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
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;

/**
 * Utility functions for {@link Multimap}s.
 *
 * @author Jeff Nelson
 */
public final class Multimaps {

    /**
     * Return a {@link Map} that contains the data in the {@code multimap}.
     * <p>
     * This method is similar to {@link Multimap#asMap()} except it will flatten
     * a value collection into a single value if the collection only contains
     * one item.
     * </p>
     * 
     * @param multimap
     * @return a {@link Map} with the data in the {@code multimap}
     */
    public static <K> Map<K, Object> asMapWithSingleValueWherePossible(
            Multimap<K, Object> multimap) {
        return multimap.asMap().entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, e -> {
                    Collection<Object> value = e.getValue();
                    if(value.isEmpty()) {
                        return null;
                    }
                    else if(value.size() == 1) {
                        return Iterables.getOnlyElement(value);
                    }
                    else {
                        return value;
                    }
                }));
    }

    private Multimaps() {/* no-init */}

}
