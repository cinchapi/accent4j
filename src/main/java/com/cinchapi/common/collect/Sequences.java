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
import java.util.function.Consumer;

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
     * Return {@code true} if the {@code object} is a sequence.
     * 
     * @param object
     * @return {@code true} if the object is a sequence
     */
    public static boolean isSequence(Object object) {
        return object instanceof Iterable || object.getClass().isArray();
    }

    /**
     * Performs the given action for each element of the {@code sequence} until
     * all elements have been processed or the action throws an exception.
     * Unless otherwise specified by the implementing class, actions are
     * performed in the order of iteration (if an iteration order is specified).
     * Exceptions thrown by the action are relayed to the caller.
     * 
     * @param sequence
     * @param action
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

    private Sequences() {/* no-init */}

}
