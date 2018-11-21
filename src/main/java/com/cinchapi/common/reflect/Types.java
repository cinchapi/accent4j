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
package com.cinchapi.common.reflect;

import java.lang.reflect.Array;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.Enums;
import com.cinchapi.common.collect.Sequences;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.primitives.Ints;
import com.google.common.primitives.Longs;

/**
 * Utility class for {@link Type} related functions.
 *
 * @author Jeff Nelson
 */
public final class Types {

    private Types() {/* no-init */}

    /**
     * Reference to the nashorn script engine, which is used for parsing JSON.
     */
    private static final ScriptEngine nashorn = new ScriptEngineManager()
            .getEngineByName("javascript");

    /**
     * Coerce the {@code object} in{@code to} the provided {@link Type}, if
     * possible. If the coercion cannot be done, an
     * {@link UnsupportedOperationException} is thrown.
     * 
     * @param object
     * @param to
     * @return an instance of {@link Type} {@code to} that is coerced from the
     *         original {@code object}
     * @throws UnsupportedOperationException
     */
    public static <T> T coerce(Object object, Type to) {
        return coerce(object, to, (type, string) -> null);
    }

    /**
     * Coerce the {@code object} in{@code to} the provided {@link Type}, if
     * possible. If the coercion cannot be done, an
     * {@link UnsupportedOperationException} is thrown.
     * 
     * @param object
     * @param to
     * @param converter
     * @return an instance of {@link Type} {@code to} that is coerced from the
     *         original {@code object}
     * @throws UnsupportedOperationException
     */
    public static <T> T coerce(Object object, Type to,
            Function<String, T> converter) {
        return coerce(object, to, (type, string) -> converter.apply(string));
    }

    /**
     * Coerce the {@code object} in{@code to} the provided {@link Type}, if
     * possible. If the coercion cannot be done, an
     * {@link UnsupportedOperationException} is thrown.
     * 
     * @param object
     * @param to
     * @param converter
     * @return an instance of {@link Type} {@code to} that is coerced from the
     *         original {@code object}
     * @throws UnsupportedOperationException
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> T coerce(Object object, Type to,
            BiFunction<Type, String, T> converter)
            throws UnsupportedOperationException {
        if(to instanceof Class) {
            Class<?> type = (Class<?>) to;
            Object coerced = null;
            if(type == object.getClass()
                    || type.isAssignableFrom(object.getClass())) {
                coerced = object;
            }
            else if(type.isEnum()) {
                coerced = Enums.parseIgnoreCase((Class<? extends Enum>) type,
                        object);
            }
            else if(Number.class.isAssignableFrom(type)) {
                coerced = AnyStrings.tryParseNumberStrict(object.toString());
            }
            else if(type == int.class) {
                coerced = Ints.tryParse(object.toString());
            }
            else if(type == long.class) {
                coerced = Longs.tryParse(object.toString());
            }
            else if(type == float.class) {
                coerced = Floats.tryParse(object.toString());
            }
            else if(type == double.class) {
                coerced = Doubles.tryParse(object.toString());
            }
            else if(type == String.class) {
                coerced = object.toString();
            }
            else if(type == char.class || type == Character.class
                    && object.toString().length() == 1) {
                coerced = object.toString().charAt(0);
            }
            else if(type == boolean.class || type == Boolean.class) {
                coerced = AnyStrings.tryParseBoolean(object.toString());
            }
            else if(type == Map.class || Sequences.isSequenceType(type)) {
                // Assume that the String representation contains JSON and use
                // nashorn to parse.
                try {
                    coerced = nashorn.eval(
                            "Java.asJSONCompatible(" + object.toString() + ")");
                    if(type.isArray()) {
                        // If the desired type is an array, we must coerce each
                        // of the elements to the array's component type.
                        Class<?> componentType = type.getComponentType();
                        Object array = Array.newInstance(componentType,
                                ((Collection) coerced).size());
                        AtomicInteger index = new AtomicInteger(0);
                        Sequences.forEach(coerced,
                                item -> Array.set(array,
                                        index.getAndIncrement(),
                                        Types.coerce(item, componentType)));
                        coerced = array;
                    }
                    else if(Map.class.isAssignableFrom(type)) {
                        coerced = ImmutableMap.copyOf((Map) coerced);
                    }
                    else {
                        coerced = ImmutableList.copyOf((Iterable) coerced);
                    }
                }
                catch (ScriptException e) {}
            }
            else {
                try {
                    coerced = type.cast(object);
                }
                catch (ClassCastException e) {}
                if(coerced == null) {
                    // As a last resort, try applying the #converter to the
                    // string representation of the argument
                    try {
                        Object converted = converter.apply(to,
                                object.toString());
                        coerced = type.isAssignableFrom(converted.getClass())
                                ? converted
                                : null;
                    }
                    catch (Exception e) {}
                }
            }
            if(coerced != null) {
                return (T) coerced;
            }
            else {
                throw new UnsupportedOperationException(
                        "Unable to coerce " + object + " into " + to);
            }
        }
        else {
            throw new UnsupportedOperationException("Unsupported type " + to);
        }
    }

}
