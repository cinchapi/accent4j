/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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
package com.cinchapi.common.collect.lazy;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

import com.cinchapi.common.base.ReadOnlyIterator;

/**
 * A {@link LazyTransformSet} reads through to another {@link Set} and
 * transforms its items on the fly.
 *
 * @author Jeff Nelson
 */
public class LazyTransformSet<F, T> extends AbstractSet<T> {

    /**
     * Return a {@link LazyTransformSet} that uses the {@code transformer} to
     * transform the items in {@code from} on the fly.
     * 
     * @param from
     * @param transformer
     * @return the {@link LazyTransformSet}
     */
    public static <F, T> LazyTransformSet<F, T> of(Set<F> from,
            Function<F, T> transformer) {
        return new LazyTransformSet<>(from, transformer);
    }

    /**
     * The original {@link Set} whose items will be transformed.
     */
    private final Set<F> from;

    /**
     * The transforming function.
     */
    private final Function<F, T> transformer;

    /**
     * Construct a new instance.
     * 
     * @param from
     * @param transformer
     */
    private LazyTransformSet(Set<F> from, Function<F, T> transformer) {
        this.from = from;
        this.transformer = transformer;
    }

    @Override
    public Iterator<T> iterator() {
        return new ReadOnlyIterator<T>() {

            private final Iterator<F> it = from.iterator();;

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public T next() {
                return transformer.apply(it.next());
            }

        };
    }

    @Override
    public int size() {
        return from.size();
    }

}
