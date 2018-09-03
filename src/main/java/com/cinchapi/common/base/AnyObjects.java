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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;

import com.cinchapi.common.base.validate.Check;

/**
 * Helper functions
 * <em>(that are not already contained in {@link java.util.Objects
 * Objects} or other common utility classes for the same purpose)</em> that
 * operate on any {@link Object}.
 * 
 * @author Jeff Nelson
 */
public final class AnyObjects {

    /**
     * The custom {@link EmptyDefinition empty definitions} that are provided
     * for classes.
     */
    private static final Map<Class<?>, EmptyDefinition<?>> defs = new HashMap<Class<?>, EmptyDefinition<?>>();

    static {
        registerEmptyDefinition(String.class, string -> string.isEmpty());
        registerEmptyDefinition(Iterable.class,
                iterable -> !iterable.iterator().hasNext());
        registerEmptyDefinition(Map.class, map -> map.isEmpty());
        registerEmptyDefinition(Object[].class, object -> object.length == 0);
    }

    /**
     * Check if {@code value} is {@code null} or semantically
     * {@link #isNullOrEmpty(Object) empty}. If so, throw an
     * {@link IllegalArgumentException}. Otherwise, return {@code value}.
     * 
     * @param value the value to check
     * @return {@code value} if it is not {@code null} or <em>empty</em>
     */
    public static <T> T checkNotNullOrEmpty(T value) {
        return checkNotNullOrEmpty(value, null);
    }

    /**
     * Check if {@code value} is {@code null} or semantically
     * {@link #isNullOrEmpty(Object) empty}. If so, throw an
     * {@link IllegalArgumentException} with {@code message}. Otherwise, return
     * {@code value}.
     * 
     * @param value the value to check
     * @param message the message for the exception
     * @return {@code value} if it is not {@code null} or <em>empty</em>
     */
    public static <T> T checkNotNullOrEmpty(T value, Object message) {
        if(!isNullOrEmpty(value)) {
            return value;
        }
        else {
            throw message == null ? new IllegalArgumentException()
                    : new IllegalArgumentException(message.toString());
        }
    }

    /**
     * Return {@code theDefault} unless the {@code preferredCheck} passes in
     * which case the {@code preferred} value is returned.
     * 
     * @param theDefault the value to return if the {@code preferredCheck} does
     *            not pass
     * @param preferred the value to return if the {@code preferredCheck} passes
     * @param preferredCheck the {@link Check} to run
     * @return {@code theDefault} or {@code preferred} value
     */
    public static <T> T defaultUnless(T theDefault, T preferred,
            Check<T> preferredCheck) {
        return preferredCheck.passes(preferred) ? preferred : theDefault;
    }

    /**
     * Given a list of {@code candidates}, return the first object that is an
     * instance of the type represented by {@code clazz}. If none of the
     * {@code candidates} are instances, throw an
     * {@link IllegalArgumentException}.
     * 
     * @param clazz the actual or superclass for which at least one of the
     *            candidate objects must be an instance
     * @param candidates the objects to check
     * @return the first of the {@code candidates} that is an instance of
     *         {@code clazz}
     * @throws IllegalArgumentException
     */
    @SuppressWarnings("unchecked")
    public static <T> T firstInstanceOf(Class<T> clazz, Object... candidates)
            throws IllegalArgumentException {
        for (Object candidate : candidates) {
            if(clazz.isAssignableFrom(candidate.getClass())) {
                return (T) candidate;
            }
        }
        throw new IllegalArgumentException();
    }

    /**
     * Return the first of the {@code candidates} that passes the {@code check}.
     * If none pass, return {@code null}.
     * 
     * @param check
     * @param candidates
     * @return the first of the {@code candidate} that passes or {@code null}
     */
    @SafeVarargs
    @Nullable
    public static <T> T firstThat(Check<T> check, T... candidates) {
        for (T candidate : candidates) {
            if(check.passes(candidate)) {
                return candidate;
            }
            else {
                continue;
            }
        }
        return null;
    }

    /**
     * Return {@code true} if {@code value} is {@code null} or <em>empty</em>
     * based on the semantics of the class.
     * <p>
     * Use the {@link #registerEmptyDefinition(Class, EmptyDefinition)} method
     * to provide a custom definition of emptiness for user defined classes.
     * </p>
     * 
     * @param value the value to check for {@code null} or emptiness
     * @return {@code true} if the value is considered null or empty
     */
    @SuppressWarnings("unchecked")
    public static <T> boolean isNullOrEmpty(T value) {
        if(value == null) {
            return true;
        }
        else {
            Class<T> clazz = (Class<T>) value.getClass();
            EmptyDefinition<T> def = (EmptyDefinition<T>) defs.get(clazz);
            if(def != null) {
                return def.metBy(value);
            }
            else {
                for (Entry<Class<?>, EmptyDefinition<?>> entry : defs
                        .entrySet()) {
                    Class<?> clz = entry.getKey();
                    if(clz.isAssignableFrom(clazz)) {
                        def = (EmptyDefinition<T>) entry.getValue();
                        return def.metBy(value);
                    }
                }
                return false;
            }
        }
    }

    /**
     * Return {@code true} if the possible nested {@code value} is not
     * considered empty.
     * <p>
     * If the {@code value} is a {@link Collection}, it is considered to be not
     * null or empty if at least one of its possibly nested items is not null or
     * empty.
     * </p>
     * <p>
     * If the {@code value} is a {@link Map}, it is considered to be not
     * null or empty if at least one of its possibly nested values is not null
     * or
     * empty.
     * </p>
     * <p>
     * Otherwise, the rules of {@link #isNullOrEmpty(Object)} apply.
     * </p>
     * 
     * @param value
     * @return {@code true} if the possibly nested value is considered null or
     *         empty
     */
    @SuppressWarnings("rawtypes")
    public static <T> boolean isNullOrEmptyNested(T value) {
        if(value == null) {
            return true;
        }
        else if(value instanceof Map) {
            Map<?, ?> map = (Map) value;
            for (Entry<?, ?> entry : map.entrySet()) {
                if(!isNullOrEmptyNested(entry.getValue())) {
                    return false;
                }
                else {
                    continue;
                }
            }
            return true;
        }
        else if(value instanceof Collection) {
            Collection<?> collection = (Collection) value;
            for (Object item : collection) {
                if(!isNullOrEmptyNested(item)) {
                    return false;
                }
                else {
                    continue;
                }
            }
            return true;
        }
        else {
            return isNullOrEmpty(value);
        }
    }

    /**
     * Return {@code null} the {@code check} passes.
     * 
     * @param value the value to return if the {@code check} passes
     * @param check the {@link Check} to run
     * @return {@code null} or {@code value}
     */
    public static <T> T nullUnless(T value, Check<T> check) {
        return check.passes(value) ? value : null;
    }

    /**
     * Register a custom {@link EmptyDefinition} for all objects that are either
     * a member of or {@link Class#isAssignableFrom(Class) assignable from}
     * {@code clazz}.
     * 
     * <p>
     * <strong>NOTE:</strong> This method is not thread safe, so avoid
     * dynamically registering definitions from multiple threads on-demand. This
     * function is designed to be used at a defined time during the
     * application's lifecycle (i.e. during bootstrap).
     * </p>
     * 
     * @param clazz the {@link Class} to register
     * @param def the {@link EmptyDefinition} to associate with {@code clazz}
     */
    public static <T> void registerEmptyDefinition(Class<T> clazz,
            EmptyDefinition<T> def) {
        defs.put(clazz, def);
    }

    private AnyObjects() {/* noinit */}

}
