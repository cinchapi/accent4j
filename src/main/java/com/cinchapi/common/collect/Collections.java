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

import java.util.AbstractList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.collect.Streams;

/**
 * Utils for Collections.
 * 
 * @author Jeff Nelson
 */
public final class Collections {

    /**
     * Add the {@code items} to the {@code collection} if it is not
     * {@code null}.
     * 
     * @param collection a collection into which a non-null collection of
     *            {@code items} is added
     * @param items a possibly non-null {@link Collection} of items
     */
    public static <T> void addAllIfNotNull(Collection<T> collection,
            @Nullable Collection<T> items) {
        if(items != null) {
            collection.addAll(items);
        }
    }

    /**
     * Add the {@code item} to the {@code collection} if it is not {@code null}.
     * 
     * @param collection the collection into which a non-null item is added
     * @param item a possibly non-null item
     */
    public static <T> void addIfNotNull(Collection<T> collection,
            @Nullable T item) {
        if(item != null) {
            collection.add(item);
        }
    }

    /**
     * Concatenate two collections.
     * 
     * @param c1
     * @param c2
     * @return a collection containing all the values in {@code c1} and
     *         {@code c2}.
     */
    public static <T> Collection<T> concat(Collection<T> c1, Collection<T> c2) {
        return Streams.concat(c1.stream(), c2.stream())
                .collect(Collectors.toList());
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

    /**
     * Return a {@link List} that is a read-only view to the {@code collection}.
     * 
     * @param collection
     * @return the {@link List} view
     */
    public static <T> List<T> viewOf(Collection<T> collection) {
        return new AbstractList<T>() {

            @Override
            public T get(int index) {
                return Iterables.get(collection, index);
            }

            @Override
            public int size() {
                return collection.size();
            }

        };
    }

    private Collections() {/* no-init */}

}
