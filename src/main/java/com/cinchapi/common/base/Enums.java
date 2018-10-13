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

import java.util.function.Function;

/**
 * Utility functions for {@link Enum enums}.
 *
 * @author Jeff Nelson
 */
public final class Enums {

    /**
     * Attempt to parse an enum of to the {@code clazz} from the {@code value}
     * regardless of case. If {@code value} is a numeric string, an attempt will
     * be made to lookup the appropriate enum by its ordinal. Otherwise, an
     * attempt is made to match {@code value} with an enum by name, ignoring
     * case.
     * 
     * @param clazz
     * @param value
     * @return the parsed enum
     * @throws IllegalArgumentException if no enum is parsed
     */
    public static <T extends Enum<T>> T parseIgnoreCase(Class<T> clazz,
            Object value) throws IllegalArgumentException {
        return parseIgnoreCase(clazz, value, v -> null);
    }

    /**
     * Attempt to parse an enum of to the {@code clazz} from the {@code value}
     * regardless of case. If {@code value} is a numeric string, an attempt will
     * be made to lookup the appropriate enum by its ordinal. Otherwise, an
     * attempt is made to match {@code value} with an enum by name, ignoring
     * case. As a last resort, the provided {@code customLogic} is used to
     * determine the enum.
     * 
     * @param clazz
     * @param value
     * @param customLogic
     * @return the parsed enum
     * @throws IllegalArgumentException if no enum is parsed
     */
    public static <T extends Enum<T>> T parseIgnoreCase(Class<T> clazz,
            Object value, Function<Object, T> customLogic)
            throws IllegalArgumentException {
        String svalue = value.toString();
        Number num = null;
        if((num = (value instanceof Number ? (Number) value
                : AnyStrings.tryParseNumber(svalue))) != null) {
            return clazz.getEnumConstants()[num.intValue()];
        }
        else {
            try {
                return Enum.valueOf(clazz, svalue);
            }
            catch (IllegalArgumentException e) {
                // Make another attempt find the enum by uppercasing the value,
                // in accordance with enum naming conventions
                try {
                    return Enum.valueOf(clazz, svalue.toUpperCase());
                }
                catch (IllegalArgumentException e2) {
                    // As a last resort, attempt to apply custom logic provided
                    // by the caller
                    T parsed = customLogic.apply(value);
                    if(parsed != null) {
                        return parsed;
                    }
                    else {
                        throw e;
                    }
                }
            }
        }
    }

    private Enums() {/* no-init */}

}
