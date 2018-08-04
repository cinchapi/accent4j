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

import com.cinchapi.common.reflect.Reflection;

/**
 * Functions that eliminate the annoyance of checked {@link Exceptions}.
 * <p>
 * The pros and cons of checked exceptions in Java is ripe for endless debate.
 * In legitimate cases where you don't want to handle an exceptions in a method
 * AND you don't want to force callers of a method to handle them either (e.g.
 * by declaring that the method throws the exception), you can use this class to
 * replace or wrap the exception with a {@link RuntimeException} type that is
 * unchecked by the compiler.
 * </p>
 * 
 * @author Jeff Nelson
 */
public final class CheckedExceptions {

    /**
     * Take a checked {@link Exception} and throw it as a
     * {@link RuntimeException}.
     * <p>
     * Unlike {@link #wrapAsRuntimeException(Exception)}, this method replaces
     * {@code e} with a RuntimeException that has the same
     * {@link Exception#getStackTrace() stack trace} as {@code e}.
     * </p>
     * <h2>Example</h2>
     * 
     * <pre>
     * try {
     *     methodThatThrowsException();
     * }
     * catch (Exception e) {
     *     throw Exceptions.throwAsRuntimeException(e);
     * }
     * </pre>
     * 
     * @param e the checked {@link Exception} to replace
     * @return nothing, return type is only so that an invocation of this method
     *         can be chained with the <em>throw</em> keyword (see example
     *         above)
     * @throws RuntimeException
     */
    public static RuntimeException throwAsRuntimeException(Throwable e) {
        if(e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        else {
            RuntimeException re = new RuntimeException(AnyStrings
                    .format("{} {}", e.getClass().getName(), e.getMessage()));
            re.setStackTrace(e.getStackTrace());
            throw re;
        }
    }

    /**
     * Take a checked {@link Exception} and throw it as a
     * {@link RuntimeException}.
     * <p>
     * Unlike {@link #wrapAsRuntimeException(Exception, Class)}, this method
     * replaces {@code e} with an instance of the {@code desiredType} that has
     * the same {@link Exception#getStackTrace() stack trace} as {@code e}.
     * </p>
     * <h2>Example</h2>
     * 
     * <pre>
     * try {
     *     methodThatThrowsException();
     * }
     * catch (Exception e) {
     *     throw Exceptions.throwAsRuntimeException(e,
     *             IllegalArgumentException.class);
     * }
     * </pre>
     * 
     * @param e the checked {@link Exception} to replace
     * @param desiredType the type of {@link RuntimeException} with which to
     *            wrap {@code e}
     * @return nothing, return type is only so that an invocation of this method
     *         can be chained with the <em>throw</em> keyword (see example
     *         above)
     * @throws T
     */
    @SuppressWarnings("unchecked")
    public static <T extends RuntimeException> T throwAsRuntimeException(
            Throwable e, Class<T> desiredType) {
        if(e.getClass() == desiredType) {
            throw (T) e;
        }
        else {
            T re = Reflection.newInstance(desiredType);
            re.setStackTrace(e.getStackTrace());
            throw re;

        }
    }

    /**
     * Wrap a checked {@link Exception} within a {@link RuntimeException}.
     * <p>
     * Unlike {@link #throwAsRuntimeException(Exception)} this method does not
     * replace {@code e}. Instead it generates a new RuntimeException with
     * {@code e} as the cause.
     * </p>
     * <h2>Example</h2>
     * 
     * <pre>
     * try {
     *     methodThatThrowsException();
     * }
     * catch (Exception e) {
     *     throw Exceptions.wrapAsRuntimeException(e);
     * }
     * </pre>
     * 
     * @param e the checked {@link Exception} to wrap
     * @return nothing, return type is only so that an invocation of this method
     *         can be chained with the <em>throw</em> keyword (see example
     *         above)
     * @throws RuntimeException
     */
    public static RuntimeException wrapAsRuntimeException(Throwable e) {
        if(e instanceof RuntimeException) {
            throw (RuntimeException) e;
        }
        else {
            RuntimeException re = new RuntimeException(e);
            throw re;
        }
    }

    /**
     * Wrap a checked {@link Exception} within a {@link RuntimeException}.
     * <p>
     * Unlike {@link #throwAsRuntimeException(Exception, Class)} this method
     * does not replace {@code e}. Instead it generates a new RuntimeException
     * with {@code e} as the cause.
     * </p>
     * <h2>Example</h2>
     * 
     * <pre>
     * try {
     *     methodThatThrowsException();
     * }
     * catch (Exception e) {
     *     throw Exceptions.wrapAsRuntimeException(e);
     * }
     * </pre>
     * 
     * @param e the checked {@link Exception} to wrap
     * @param desiredType the type of {@link RuntimeException} with which to
     *            wrap {@code e}
     * @return nothing, return type is only so that an invocation of this method
     *         can be chained with the <em>throw</em> keyword (see example
     *         above)
     * @throws T
     */
    @SuppressWarnings("unchecked")
    public static <T extends RuntimeException> T wrapAsRuntimeException(
            Throwable e, Class<T> desiredType) {
        if(e.getClass() == desiredType) {
            throw (T) e;
        }
        else {
            T re = Reflection.newInstance(desiredType, e);
            throw re;
        }
    }

    private CheckedExceptions() {/* noinit */}

}
