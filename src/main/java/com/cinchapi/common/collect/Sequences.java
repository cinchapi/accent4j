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

import java.lang.reflect.Array;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

/**
 * Utility functions for dealing with sequences.
 * 
 * <p>
 * A Sequence is a one-dimensional container object. In Java, this includes
 * {@link Iterable} objects and {@link Array arrays}. Since these types have
 * divergent class hierarchies, these functions provide implementations for
 * common operations.
 * </p>
 *
 * @author Jeff Nelson
 */
public final class Sequences {

    /**
     * Return {@code true} if the {@link Class cls} is a sequence type.
     * 
     * @param cls
     * @return {@code true} if the class is a sequence type
     */
    public static boolean isSequenceType(Class<?> cls) {
        return Iterable.class.isAssignableFrom(cls) || cls.isArray();
    }

    /**
     * Return {@code true} if the {@code object} is a sequence.
     * 
     * @param object
     * @return {@code true} if the object is a sequence
     */
    public static boolean isSequence(Object object) {
        return isSequenceType(object.getClass());
    }

    /**
     * Performs the given action for each element of the {@code sequence} until
     * all elements have been processed or the action throws an exception.
     * Unless otherwise specified by the implementing class, actions are
     * performed in the order of iteration (if an iteration order is specified).
     * Exceptions thrown by the action are relayed to the caller.
     * 
     * @param sequence: a sequence
     * @param action: a function that takes a T and returns nothing
     */
    @SuppressWarnings("unchecked")
    public static <T> void forEach(Object sequence, Consumer<T> action) {
        if(sequence instanceof Iterable) {
            ((Iterable<T>) sequence).forEach(action);
        }
        else if(sequence.getClass().isArray()) {
            for (int i = 0; i < Array.getLength(sequence); ++i) {
                T item = (T) Array.get(sequence, i);
                action.accept(item);
            }
        }
        else {
            throw new IllegalArgumentException(
                    sequence + "is not a valid sequence");
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean contains(Object sequence, T element) {
        Preconditions.checkArgument(isSequence(sequence),
                sequence + " is not a valid sequence");
        if(sequence instanceof Iterable) {
            return Iterables.contains((Iterable<T>) sequence, element);
        }
        else {
            return stream(sequence).filter(Predicate.isEqual(element))
                    .count() > 0;
        }
    }

    /**
     * Return a {@link Stream} for the {@code sequence}.
     * 
     * @param sequence
     * @return a stream
     * @throws IllegalArgumentException if the parameter is not a
     *             {@link #isSequence(Object) sequence}
     */
    @SuppressWarnings("unchecked")
    public static <T> Stream<T> stream(Object sequence) {
        Preconditions.checkArgument(isSequence(sequence),
                sequence + " is not a valid sequence");
        if(sequence instanceof Iterable) {
            return Streams.stream((Iterable<T>) sequence);
        }
        else {
            List<T> gathered = Lists.newArrayList();
            forEach(sequence, e -> gathered.add((T) e));
            return gathered.stream();
        }
    }

    private Sequences() {/* no-init */}

}
