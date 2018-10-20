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

import java.util.AbstractList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import com.google.common.collect.Lists;

/**
 * A {@link Continuation} is a {@link List} whose elements are generated on
 * the fly using a {@link Function} or {@link Supplier}. Therefore, the size of
 * the list is "infinite" (e.g. {@link Integer#MAX_VALUE}. Previously generated
 * elements are remembered such that subsequent access to an index is always
 * idempotent.
 * <p>
 * A {@link Continuation} cannot be directly mutated with {@link #add(Object)}
 * or {@link #remove(int)} methods.
 * <p>
 *
 * @author Jeff Nelson
 */
public class Continuation<T> extends AbstractList<T> {

    /**
     * Return a {@link Continuation} that contains elements supplied by the
     * {@code generator}.
     * <p>
     * The provided {@code generator} will be passed the index of an element
     * that is being accessed in the list, which may be useful in generating the
     * element that should be contained at the index.
     * <p>
     * 
     * @param generator
     * @return a new {@link Continuation}
     */
    public static <T> Continuation<T> of(Function<Integer, T> generator) {
        return new Continuation<>(generator);
    }

    /**
     * Return a {@link Continuation} that contains elements supplied by the
     * {@code generator}.
     * 
     * @param generator
     * @return a new {@link Continuation}
     */
    public static <T> Continuation<T> of(Supplier<T> generator) {
        return of(ignore -> generator.get());
    }

    /**
     * The list of previously generated elements.
     */
    private final List<T> generated;

    /**
     * The generating function. The function should return an element, given the
     * element's index.
     */
    private final Function<Integer, T> generator;

    /**
     * Construct a new instance.
     * 
     * @param generator
     */
    public Continuation(Function<Integer, T> generator) {
        this.generator = generator;
        this.generated = Lists.newArrayList();
    }

    @Override
    public T get(int index) {
        for (int i = generated.size(); i <= index; ++i) {
            // The requested index is larger than the current list. So, backfill
            // the items between the end of the list and the requested index
            // with null.
            generated.add(null);
        }
        T element = generated.get(index);
        if(element == null) {
            element = generator.apply(index);
            generated.set(index, element);
        }
        return element;
    }


    @Override
    public int size() {
        return Integer.MAX_VALUE;
    }

}
