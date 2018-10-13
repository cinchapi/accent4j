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
package com.cinchapi.common.describe;

import java.lang.reflect.Array;
import java.util.Map;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.cinchapi.common.base.validate.Check;
import com.google.common.collect.ImmutableMap;

/**
 * A customizable {@link Adjective} that describes emptiness for objects.
 *
 * @author Jeff Nelson
 */
@NotThreadSafe
public class Empty extends Adjective {

    /**
     * Create an {@link Empty} instance with the default set of definitions.
     * More definitions can be added using the {@link #define(Class, Check)}
     * method.
     * 
     * @return the {@link Empty} instance
     */
    public static Empty is() {
        Empty empty = new Empty();
        empty.define(String.class, string -> string.isEmpty());
        empty.define(Iterable.class,
                iterable -> !iterable.iterator().hasNext());
        empty.define(Map.class, map -> map.isEmpty());
        return empty;
    }

    /**
     * Alias for {@link Empty#is()}.
     * <p>
     * Returns the default definition of {@link Empty}.
     * </p>
     * 
     * @return the {@link Empty} instance
     */
    public static Empty ness() {
        return is();
    }

    /**
     * Create an {@link Empty} instance with the {@code check} for the specified
     * {@code clazz}. More definitions can be added using the
     * {@link #define(Class, Check)} method.
     * 
     * @param clazz
     * @param check
     * @return the {@link Empty} instance
     */
    public static <T> Empty is(Class<T> clazz, Check<T> check) {
        return is(ImmutableMap.of(clazz, check));
    }

    /**
     * Create an {@link Empty} instance with the provided set of
     * {@code definitions}. More definitions can be added using the
     * {@link #define(Class, Check)} method.
     * 
     * @param definitions
     * @return the {@link Empty} instance
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Empty is(Map<Class, Check> definitions) {
        Empty empty = new Empty();
        definitions.forEach((clazz, definition) -> {
            empty.define(clazz, definition);
        });
        return empty;
    }

    private Empty() {/* no-init */}

    @Override
    public <T> boolean describes(@Nullable T object) {
        if(object == null) {
            return true;
        }
        else if(object.getClass().isArray()) {
            return Array.getLength(object) == 0;
        }
        else {
            return super.describes(object);
        }
    }

}
