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

import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.concurrent.NotThreadSafe;

import com.cinchapi.common.base.validate.Check;
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
     * The provided definitions.
     */
    private Map<Class<?>, Check<?>> definitions = Maps.newHashMap();

    /**
     * Provide a definition of this {@link Adjective} with respect to objects of
     * the provided {@code clazz} and its descendants.
     * 
     * @param clazz
     * @param definition
     */
    public <T> void define(Class<T> clazz, Check<T> definition) {
        definitions.put(clazz, definition);
    }

    /**
     * Return {@code true} if this {@link Adjective} describes the
     * {@code object}.
     * <p>
     * An {@link Adjective} describes an object if a
     * {@link #define(Class, Check) definition} has been provided for the
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
        Check<? super T> definition = (Check<? super T>) definitions.get(clazz);
        if(definition == null) { // See if an ancestor has been defined and pick
                                 // the closest one...
            Class<? super T> ancestor = null;
            for (Entry<Class<?>, Check<?>> entry : definitions.entrySet()) {
                Class<?> cls = entry.getKey();
                if(cls.isAssignableFrom(clazz) && (ancestor == null
                        || ancestor.isAssignableFrom(cls))) {
                    ancestor = (Class<? super T>) cls;
                    definition = (Check<? super T>) entry.getValue();
                }
            }
        }
        return definition != null ? definition.passes(object) : false;
    }

}
