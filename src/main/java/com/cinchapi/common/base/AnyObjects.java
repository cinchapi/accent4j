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

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.cinchapi.common.collect.Sequences;
import com.cinchapi.common.describe.Empty;

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
     * An {@link Empty} instance to implement empty checking methods.
     */
    private static Empty EMPTY = Empty.is();

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
     * This function takes a generic {@code value} and a character as the
     * delimiter.
     *
     * <p>
     * Firstly, if the generic is a sequence, then we iterate through each item
     * and split the string representation, by the delimiter, of each item
     * individually, trimming any whitespace away. We then convert this new
     * sequence to a list and return that.
     * </p>
     *
     * <p>
     * Secondly, if the generic is not a sequence, then we simply convert the
     * item to a String and split by the delimiter, again trimming the
     * whitespace, and then we convert that new sequence to a list and return
     * that.
     * </p>
     *
     * @param value The generic value
     * @param delimiter The character that we use to specify the boundary
     *            between different parts of the text.
     * @param The type of the generic value
     * @return a list based on the {@code value} being split
     */
    public static <T> List<String> split(T value, char delimiter) {
        Function<Object, String[]> splitter = item -> new StringSplitter(
                item.toString(), delimiter, SplitOption.TRIM_WHITESPACE)
                        .toArray();
        return Sequences.isSequence(value)
                ? Sequences.stream(value).map(splitter).flatMap(Arrays::stream)
                        .collect(Collectors.toList())
                : Arrays.asList(splitter.apply(value));
    }

    /**
     * Perform a {@link #split(Object, char)} based on the a comma delimiter.
     * 
     * @param value the generic value
     * @param the type of the generic value
     * @return a list based on the {@code value} being split
     */
    public static <T> List<String> split(T value) {
        return split(value, ',');
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
            Predicate<T> preferredCheck) {
        return preferredCheck.test(preferred) ? preferred : theDefault;
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
    public static <T> T firstThat(Predicate<T> check, T... candidates) {
        for (T candidate : candidates) {
            if(check.test(candidate)) {
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
     * @return {@code true} if
     * @deprecated use the {@link com.cinchapi.common.describe.Empty} framework
     *             instead
     */
    @Deprecated
    public static <T> boolean isNullOrEmpty(T value) {
        return EMPTY.describes(value);
    }

    /**
     * Return {@code null} the {@code check} passes.
     * 
     * @param value the value to return if the {@code check} passes
     * @param check the {@link Check} to run
     * @return {@code null} or {@code value}
     */
    public static <T> T nullUnless(T value, Predicate<T> check) {
        return check.test(value) ? value : null;
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
     * @deprecated use the {@link com.cinchapi.common.describe.Empty} framework
     *             instead
     */
    @SuppressWarnings("deprecation")
    @Deprecated
    public static <T> void registerEmptyDefinition(Class<T> clazz,
            EmptyDefinition<T> def) {
        EMPTY.define(clazz, object -> def.metBy(object));
    }

    private AnyObjects() {/* noinit */}

}
