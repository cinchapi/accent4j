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
package com.cinchapi.common.base;

import java.util.Locale;
import java.util.function.Function;

import javax.annotation.Nullable;

/**
 * Utility functions for {@link Enum enums}.
 * <p>
 * The {@code parse} methods return a constant or throw an
 * {@link IllegalArgumentException}. The {@code tryParse} methods return the
 * same constant, or {@code null} in place of the exception. The methods whose
 * name contains {@code Name} match a value by constant name only. The other
 * methods first read a number, or a numeric string, as an ordinal.
 * </p>
 *
 * @author Jeff Nelson
 */
public final class Enums {

    /**
     * Return the constant of {@code clazz} that {@code value} identifies.
     * <p>
     * A {@link Number}, or a string that parses as a number, identifies the
     * constant at that ordinal. Any other value identifies the constant whose
     * name equals the value, or the value in upper case.
     * </p>
     *
     * @param clazz the enum to search
     * @param value the ordinal or name to look up; must not be {@code null}
     * @return the constant that {@code value} identifies
     * @throws IllegalArgumentException if no constant has the ordinal or the
     *             name
     */
    public static <T extends Enum<T>> T parseIgnoreCase(Class<T> clazz,
            Object value) throws IllegalArgumentException {
        return parseIgnoreCase(clazz, value, v -> null);
    }

    /**
     * Return the constant of {@code clazz} that {@code value} identifies.
     * <p>
     * A {@link Number}, or a string that parses as a number, identifies the
     * constant at that ordinal. Any other value identifies the constant whose
     * name equals the value, or the value in upper case. When neither applies,
     * {@code customLogic} decides, and a {@code null} from it means that no
     * constant matches.
     * </p>
     *
     * @param clazz the enum to search
     * @param value the ordinal or name to look up; must not be {@code null}
     * @param customLogic the last resort, applied to {@code value}; must not be
     *            {@code null}
     * @return the constant that {@code value} identifies
     * @throws IllegalArgumentException if no constant has the ordinal or the
     *             name, and {@code customLogic} returns {@code null}
     */
    public static <T extends Enum<T>> T parseIgnoreCase(Class<T> clazz,
            Object value, Function<Object, T> customLogic)
            throws IllegalArgumentException {
        T parsed = tryParseIgnoreCase(clazz, value, customLogic);
        if(parsed == null) {
            throw new IllegalArgumentException(
                    "No enum constant " + clazz.getCanonicalName() + "."
                            + value);
        }
        else {
            return parsed;
        }
    }

    /**
     * Return the constant of {@code clazz} whose name is {@code value}, in any
     * case. A number is never read as an ordinal.
     *
     * @param clazz the enum to search
     * @param value the name to look up; must not be {@code null}
     * @return the constant with that name
     * @throws IllegalArgumentException if no constant has the name
     */
    public static <T extends Enum<T>> T parseNameIgnoreCase(Class<T> clazz,
            Object value) throws IllegalArgumentException {
        return parseNameIgnoreCase(clazz, value, v -> null);
    }

    /**
     * Return the constant of {@code clazz} whose name is {@code value}, in any
     * case. A number is never read as an ordinal. When no name matches,
     * {@code customLogic} decides, and a {@code null} from it means that no
     * constant matches.
     *
     * @param clazz the enum to search
     * @param value the name to look up; must not be {@code null}
     * @param customLogic the last resort, applied to {@code value}; must not be
     *            {@code null}
     * @return the constant with that name
     * @throws IllegalArgumentException if no constant has the name, and
     *             {@code customLogic} returns {@code null}
     */
    public static <T extends Enum<T>> T parseNameIgnoreCase(Class<T> clazz,
            Object value, Function<Object, T> customLogic)
            throws IllegalArgumentException {
        T parsed = tryParseNameIgnoreCase(clazz, value, customLogic);
        if(parsed == null) {
            throw new IllegalArgumentException(
                    "No enum constant " + clazz.getCanonicalName() + "."
                            + value);
        }
        else {
            return parsed;
        }
    }

    /**
     * Return the constant of {@code clazz} that {@code value} identifies, or
     * {@code null} when there is none.
     * <p>
     * A {@link Number}, or a string that parses as a number, identifies the
     * constant at that ordinal. Any other value identifies the constant whose
     * name equals the value, or the value in upper case.
     * </p>
     *
     * @param clazz the enum to search
     * @param value the ordinal or name to look up; must not be {@code null}
     * @return the constant that {@code value} identifies, or {@code null}
     */
    @Nullable
    public static <T extends Enum<T>> T tryParseIgnoreCase(Class<T> clazz,
            Object value) {
        return tryParseIgnoreCase(clazz, value, v -> null);
    }

    /**
     * Return the constant of {@code clazz} that {@code value} identifies, or
     * {@code null} when there is none.
     * <p>
     * A {@link Number}, or a string that parses as a number, identifies the
     * constant at that ordinal. Any other value identifies the constant whose
     * name equals the value, or the value in upper case. When neither applies,
     * {@code customLogic} decides.
     * </p>
     *
     * @param clazz the enum to search
     * @param value the ordinal or name to look up; must not be {@code null}
     * @param customLogic the last resort, applied to {@code value}; must not be
     *            {@code null}
     * @return the constant that {@code value} identifies, or {@code null}
     */
    @Nullable
    public static <T extends Enum<T>> T tryParseIgnoreCase(Class<T> clazz,
            Object value, Function<Object, T> customLogic) {
        T parsed = findByOrdinal(clazz, value);
        if(parsed == null) {
            parsed = findByName(clazz, value.toString());
        }
        if(parsed == null) {
            parsed = customLogic.apply(value);
        }
        return parsed;
    }

    /**
     * Return the constant of {@code clazz} whose name is {@code value}, in any
     * case, or {@code null} when there is none. A number is never read as an
     * ordinal.
     *
     * @param clazz the enum to search
     * @param value the name to look up; must not be {@code null}
     * @return the constant with that name, or {@code null}
     */
    @Nullable
    public static <T extends Enum<T>> T tryParseNameIgnoreCase(Class<T> clazz,
            Object value) {
        return tryParseNameIgnoreCase(clazz, value, v -> null);
    }

    /**
     * Return the constant of {@code clazz} whose name is {@code value}, in any
     * case, or {@code null} when there is none. A number is never read as an
     * ordinal. When no name matches, {@code customLogic} decides.
     *
     * @param clazz the enum to search
     * @param value the name to look up; must not be {@code null}
     * @param customLogic the last resort, applied to {@code value}; must not be
     *            {@code null}
     * @return the constant with that name, or {@code null}
     */
    @Nullable
    public static <T extends Enum<T>> T tryParseNameIgnoreCase(Class<T> clazz,
            Object value, Function<Object, T> customLogic) {
        T parsed = findByName(clazz, value.toString());
        if(parsed == null) {
            parsed = customLogic.apply(value);
        }
        return parsed;
    }

    /**
     * Return the constant of {@code clazz} whose name equals {@code name}, or
     * else whose name equals {@code name} in upper case, or {@code null} when
     * there is none.
     *
     * @param clazz the enum to search
     * @param name the name to look up
     * @return the constant with that name, or {@code null}
     */
    @Nullable
    private static <T extends Enum<T>> T findByName(Class<T> clazz,
            String name) {
        String upper = name.toUpperCase(Locale.ROOT);
        T exact = null;
        T uppercased = null;
        for (T constant : clazz.getEnumConstants()) {
            if(constant.name().equals(name)) {
                exact = constant;
            }
            else if(constant.name().equals(upper)) {
                uppercased = constant;
            }
        }
        if(exact != null) {
            return exact;
        }
        else {
            return uppercased;
        }
    }

    /**
     * Return the constant of {@code clazz} at the ordinal that {@code value}
     * gives, or {@code null} when {@code value} is not a number and does not
     * parse as one, or when no constant has that ordinal.
     *
     * @param clazz the enum to search
     * @param value the ordinal, as a {@link Number} or a numeric string
     * @return the constant at that ordinal, or {@code null}
     */
    @Nullable
    private static <T extends Enum<T>> T findByOrdinal(Class<T> clazz,
            Object value) {
        Number ordinal;
        if(value instanceof Number) {
            ordinal = (Number) value;
        }
        else {
            ordinal = AnyStrings.tryParseNumber(value.toString());
        }
        T[] constants = clazz.getEnumConstants();
        if(ordinal != null && ordinal.intValue() >= 0
                && ordinal.intValue() < constants.length) {
            return constants[ordinal.intValue()];
        }
        else {
            return null;
        }
    }

    private Enums() {/* no-init */}

}
