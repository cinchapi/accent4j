/*
 * Copyright (c) 2017 Cinchapi Inc.
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
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * Utils for Collections.
 * 
 * @author Jeff Nelson
 */
public final class Collections {

    /**
     * Add the {@code item} to the {@code collection} if it is not {@code null}.
     * 
     * @param collection the collection into which a non-null item is added
     * @param item a possibly non-null item
     */
    public static <T> void addIfNotNull(Collection<T> collection, T item) {
        if(item != null) {
            collection.add(item);
        }
    }

    /**
     * Ensure that the specified {@code collection} is a {@link Set} or
     * transform it into one if it is not.
     * 
     * @param collection the collection to ensure
     * @return a {@link Set} with all the distinct elements in the
     *         {@code collection}
     */
    public static <T> Set<T> ensureSet(Collection<T> collection) {
        if(collection instanceof Set) {
            return (Set<T>) collection;
        }
        else {
            return Sets.newLinkedHashSet(collection);
        }

    }

    private Collections() {/* no-init */}

}
