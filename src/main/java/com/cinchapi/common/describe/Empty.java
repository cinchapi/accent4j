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
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

/**
 * A customizable {@link Adjective} that describes emptiness for objects.
 *
 * @author Jeff Nelson
 */
@NotThreadSafe
public class Empty extends Adjective {

    /**
     * The default set of definitions, which every instance from {@link #is()}
     * starts with. The map is read-only, so it is safe to share across
     * instances and threads.
     */
    private static final Map<Class<?>, Predicate<?>> DEFAULTS = newDefaults();

    /**
     * Create an {@link Empty} instance with the default set of definitions.
     * More definitions can be added using the {@link #define(Class, Predicate)}
     * method.
     * <p>
     * Each call returns a new instance, so a definition added to one instance
     * does not affect any other.
     * </p>
     * 
     * @return the {@link Empty} instance
     */
    public static Empty is() {
        return new Empty(DEFAULTS);
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
    public static <T> Empty is(Class<T> clazz, Predicate<T> check) {
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
    public static Empty is(Map<Class, Predicate> definitions) {
        Empty empty = new Empty();
        definitions.forEach((clazz, definition) -> {
            empty.define(clazz, definition);
        });
        return empty;
    }

    /**
     * Return a new, read-only map of the default set of definitions.
     * 
     * @return the default definitions
     */
    private static Map<Class<?>, Predicate<?>> newDefaults() {
        Map<Class<?>, Predicate<?>> definitions = Maps.newHashMap();
        definitions.put(String.class,
                (Predicate<String>) string -> string.isEmpty());
        definitions.put(Iterable.class,
                (Predicate<Iterable<?>>) iterable -> !iterable.iterator()
                        .hasNext());
        definitions.put(Map.class, (Predicate<Map<?, ?>>) map -> map.isEmpty());
        return Collections.unmodifiableMap(definitions);
    }

    private Empty() {/* no-init */}

    /**
     * Construct a new instance whose definitions start as the provided
     * {@code defaults}.
     * 
     * @param defaults the definitions that the new instance starts with, which
     *            it never changes
     */
    private Empty(Map<Class<?>, Predicate<?>> defaults) {
        super(defaults);
    }

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
