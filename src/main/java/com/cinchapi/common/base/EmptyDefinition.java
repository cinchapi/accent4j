/*
 * Copyright (c) 2015 Cinchapi Inc.
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

/**
 * A semantic definition of "empty" for all objects of a specific type.
 * 
 * @author Jeff Nelson
 * @deprecated see the {@link com.cinchapi.describe.Empty} construct for preferred usage
 */
@FunctionalInterface
@Deprecated
public interface EmptyDefinition<T> {

    /**
     * Return {@code true} if this definition of "empty" is met by
     * {@code object}.
     * 
     * @param object the object to test for "emptiness"
     * @return {@code true} if {@code object} is considered to be semantically
     *         empty
     */
    public boolean metBy(T object);

}
