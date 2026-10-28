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

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Predicate;

import javax.annotation.concurrent.NotThreadSafe;

import com.google.common.collect.Maps;

/**
 * An {@link Adjective} is a stateful set of definitions that may describe
 * objects.
 * <p>
 * Each {@link Adjective} instance maintains its own set of definitions.
 * </p>
 *
 * @author Jeff Nelson
 */
@NotThreadSafe
public abstract class Adjective {

    /**
     * The provided definitions. While {@link #isShared} is {@code true}, other
     * instances may read this map too, so it must not change.
     */
    private Map<Class<?>, Predicate<?>> definitions;

    /**
     * Whether other instances may share the {@link #definitions}, so that a
     * {@link #define(Class, Predicate) definition} must go into a copy of them.
     */
    private boolean isShared;

    /**
     * Construct a new instance with no definitions.
     */
    public Adjective() {
        this(Collections.emptyMap());
    }

    /**
     * Construct a new instance whose definitions start as the provided
     * {@code defaults}.
     * <p>
     * The new instance never changes the {@code defaults}, so any number of
     * instances, on any threads, can share one map of them. A
     * {@link #define(Class, Predicate) definition} added to one of those
     * instances affects neither the {@code defaults} nor any other instance.
     * </p>
     * 
     * @param defaults the definitions that the new instance starts with, which
     *            the caller must never change
     */
    Adjective(Map<Class<?>, Predicate<?>> defaults) {
        this.definitions = defaults;
        this.isShared = true;
    }

    /**
     * Provide a definition of this {@link Adjective} with respect to objects of
     * the provided {@code clazz} and its descendants.
     * 
     * @param clazz the class of the objects that the {@code definition} applies
     *            to
     * @param definition the {@link Predicate} that decides whether this
     *            {@link Adjective} describes an object of the {@code clazz}
     */
    public <T> void define(Class<T> clazz, Predicate<T> definition) {
        if(isShared) {
            // For an object with definitions for unrelated ancestors, describes
            // uses the one it meets first in iteration order. Adding each entry
            // in turn gives the copy the capacity, and so the iteration order,
            // of a map that received each definition directly.
            Map<Class<?>, Predicate<?>> copy = Maps.newHashMap();
            for (Entry<Class<?>, Predicate<?>> entry : definitions.entrySet()) {
                copy.put(entry.getKey(), entry.getValue());
            }
            definitions = copy;
            isShared = false;
        }
        definitions.put(clazz, definition);
    }

    /**
     * Return {@code true} if this {@link Adjective} describes the
     * {@code object}.
     * <p>
     * An {@link Adjective} describes an object if a
     * {@link #define(Class, Predicate) definition} has been provided for the
     * object's class or an ancestor class. If no applicable definition exists,
     * this method returns {@code false}.
     * </p>
     * 
     * @param object
     * @return a boolean that indicates whether the {@code object} is described
     *         by this {@link Adjective}
     */
    @SuppressWarnings("unchecked")
    public <T> boolean describes(T object) {
        Class<T> clazz = (Class<T>) object.getClass();
        Predicate<? super T> definition = (Predicate<? super T>) definitions
                .get(clazz);
        if(definition == null) { // See if an ancestor has been defined and pick
                                 // the closest one...
            Class<? super T> ancestor = null;
            for (Entry<Class<?>, Predicate<?>> entry : definitions.entrySet()) {
                Class<?> cls = entry.getKey();
                if(cls.isAssignableFrom(clazz) && (ancestor == null
                        || ancestor.isAssignableFrom(cls))) {
                    ancestor = (Class<? super T>) cls;
                    definition = (Predicate<? super T>) entry.getValue();
                }
            }
        }
        return definition != null ? definition.test(object) : false;
    }

}
