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

import java.util.List;

/**
 * 
 * 
 * @author Jeff Nelson
 */
public final class Verify {

    public static void elementAt(int position, Iterable<?> iterable) {

    }

    public static void elementAt(int position, List<?> list) {

    }

    public static void elementAt(int position, Object[] array) {

    }

    public static void elementAt(int position, String string) {

    }

    public static void isNull(Object object) {

    }

    /**
     * Verify that {@code object} is {@link Class#isAssignableFrom(Class) an
     * instance of} {@code type} or throw a {@link ClassCastException}.
     * <p>
     * <h2>Example</h2>
     * 
     * <pre>
     * Verify.isType(new ArrayList&lt;Object&gt;(), List.class); // does nothing
     * Verify.isType(new ArrayList&lt;Object&gt;(), Set.class); // throws ClassCastException
     * </pre>
     * 
     * </p>
     * 
     * @param object the object to check
     * @param type the expected {@link Class type} or super type for
     *            {@code object}
     */
    public static void isType(Object object, Class<?> type) {
        isType(object, type, null);
    }

    /**
     * Verify that {@code object} is {@link Class#isAssignableFrom(Class) an
     * instance of} {@code type} or throw a {@link ClassCastException}.
     * <p>
     * <h2>Example</h2>
     * 
     * <pre>
     * Verify.isType(new ArrayList&lt;Object&gt;(), List.class); // does nothing
     * Verify.isType(new ArrayList&lt;Object&gt;(), Set.class); // throws ClassCastException
     * </pre>
     * 
     * </p>
     * 
     * @param object the object to check
     * @param type the expected {@link Class type} or super type for
     *            {@code object}
     * @param errorMsgTemplate the template for the error message; see
     *            {@link AnyStrings#format(String, Object...)} for more
     *            information
     * @param errorMsgArgs the values to inject in the {@code errorMsgTemplate}
     *            placeholders; see {@link AnyStrings#format(String, Object...)}
     *            for more information
     */
    public static void isType(Object object, Class<?> type,
            String errorMsgTemplate, Object... errorMsgArgs) {
        if(!type.isAssignableFrom(object.getClass())) {
            throw new ClassCastException(AnyStrings.format(errorMsgTemplate,
                    errorMsgArgs));
        }
    }

    public static void notLargerThan(int size, Iterable<?> iterable) {

    }

    public static void notLargerThan(int size, List<?> list) {

    }

    public static void notLargerThan(int size, Object[] array) {

    }

    public static void notLargerThan(int size, String string) {

    }

    public static void notNull(Object object) {

    }

    public static void notSmallerThan(int size, Iterable<?> iterable) {

    }

    public static void notSmallerThan(int size, List<?> list) {

    }

    public static void notSmallerThan(int size, Object[] array) {

    }

    public static void notSmallerThan(int size, String string) {

    }

    public static void size(int size, Iterable<?> iterable) {

    }

    public static void size(int size, List<?> list) {

    }

    public static void size(int size, Object[] array) {

    }

    public static void size(int size, String string) {

    }

    public static void state(boolean condition) {

    }

    /**
     * Verify the truth of {@code condition} or throw an
     * {@link IllegalStateException}.
     * <p>
     * <h2>Example</h2>
     * 
     * <pre>
     * Verify.that(1 < 2); //does nothing
     * Verify.that(1 > 2); throws IllegalStateException
     * </pre>
     * 
     * </p>
     * 
     * @param condition the condition that should be {@code true}
     * @throws IllegalStateException if {@code condition} is false
     */
    public static void that(boolean condition) {
        that(condition, null);
    }

    /**
     * Verify the truth of the {@code condition} or throw an
     * {@link IllegalStateException}.
     * <p>
     * <h2>Example</h2>
     * 
     * <pre>
     * Verify.that(1 < 2); //does nothing
     * Verify.that(1 > 2); throws IllegalStateException
     * Verify.that(1 > 2, &quot;{} is not greater than {}&quot;, 1, 2); //throws IllegalStateException
     * </pre>
     * 
     * </p>
     * 
     * @param condition the condition that should be {@code true}
     * @param errorMsgTemplate the template for the error message; see
     *            {@link AnyStrings#format(String, Object...)} for more
     *            information
     * @param errorMsgArgs the values to inject in the {@code errorMsgTemplate}
     *            placeholders; see {@link AnyStrings#format(String, Object...)}
     *            for more information
     * @throws IllegalStateException if {@code condition} is false
     */
    public static void that(boolean condition, String errorMsgTemplate,
            Object... errorMsgArgs) {
        if(!condition) {
            throw new IllegalStateException(AnyStrings.format(errorMsgTemplate,
                    errorMsgArgs));
        }
    }

    /**
     * Verify the truth of the {@code condition} about a method argument or
     * throw an {@link IllegalArgumentException}.
     * 
     * @param condition the condition that should be {@code true}
     * @throws IllegalArgumentException if {@code condition} is false
     */
    public static void thatArgument(boolean condition) {
        thatArgument(condition, null);
    }

    /**
     * Verify the truth of the {@code condition} about a method argument or
     * throw an {@link IllegalArgumentException}.
     * 
     * @param condition the condition that should be {@code true}
     * @param errorMsgTemplate the template for the error message; see
     *            {@link AnyStrings#format(String, Object...)} for more
     *            information
     * @param errorMsgArgs the values to inject in the {@code errorMsgTemplate}
     *            placeholders; see {@link AnyStrings#format(String, Object...)}
     *            for more information
     * @throws IllegalArgumentException if {@code condition} is false
     */
    public static void thatArgument(boolean condition, String errorMsgTemplate,
            Object... errorMsgArgs) {
        if(!condition) {
            throw new IllegalArgumentException(AnyStrings.format(
                    errorMsgTemplate, errorMsgArgs));
        }
    }

    private Verify() {/* noinit */}

}
