/*
 * Copyright (c) 2016 Cinchapi Inc.
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
package com.cinchapi.common.base;

import javax.annotation.Nullable;

/**
 * A {@link ReadOnlyIterator} that can be used to return items from some ad hoc
 * collection of elements.
 * <p>
 * Any initialization that would normally go in a initialization block should be
 * implemented in the {@link #initialize()} method.
 * </p>
 * 
 * @author Jeff Nelson
 */
public abstract class AdHocIterator<T> extends ReadOnlyIterator<T> {

    /**
     * The element to return from {@link #next()}.
     */
    private T next = null;
    {
        initialize();
        next = findNext();
    }

    @Override
    public final boolean hasNext() {
        return next != null;
    }

    @Override
    public final T next() {
        T ret = next;
        next = findNext();
        return ret;
    }

    /**
     * Locate and return the next element that should be return in a call to
     * {@link #next()}. If there is no next element, return {@code null}.
     * 
     * @return the next element to return
     */
    @Nullable
    protected abstract T findNext();

    /**
     * Initialize the state of the {@link AdHocIterator}.
     */
    protected void initialize() {/* no-op */};

}
