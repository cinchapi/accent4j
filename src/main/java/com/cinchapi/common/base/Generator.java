/*
 * Copyright (c) 2013-2017 Cinchapi Inc.
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
 * A {@link Generator} is like a {@link Function} in that it produces values,
 * but it doesn't require inputs to do so.
 *
 * @author Jeff Nelson
 * @deprecated use {@link Supplier} instead.
 */
@FunctionalInterface
@Deprecated
public interface Generator<T> {

    /**
     * Generate a value.
     * 
     * @return the generated value
     */
    public T generate();

}
