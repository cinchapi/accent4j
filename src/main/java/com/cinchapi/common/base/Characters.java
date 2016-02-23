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

/**
 * Constants and functions for {@link Character Characters}.
 * 
 * @author Jeff Nelson
 */
public final class Characters {

    /**
     * The {@code null} literal character.
     */
    public static final char NULL = '\0';

    /**
     * The "escape" literal character.
     */
    public static final char ESCAPE = '\\';

    /**
     * Return {@code true} if {@code c} is an escape sequence.
     * 
     * @param c a character
     * @return {@code true} if {@code c} is an escape sequence
     */
    public static boolean isEscapeSequence(char c) {
        switch (c) {
        case '\t':
        case '\b':
        case '\n':
        case '\r':
        case '\f':
        case '\'':
        case '\"':
        case '\\':
            return true;
        default:
            return false;
        }
    }

    /**
     * If {@code c} is an {@link #isEscapeSequence(char) escape sequence} return
     * the character that is being escaped (i.e. '\n' will return 'n').
     * Otherwise, return the null literal character '0'.
     * 
     * @param c a character
     * @return the escaped character if {@code c} is an escape sequence,
     *         otherwise '0'
     */
    public static char getEscapedCharOrNullLiteral(char c) {
        switch (c) {
        case '\t':
            return 't';
        case '\b':
            return 'b';
        case '\n':
            return 'n';
        case '\r':
            return 'r';
        case '\f':
            return 'f';
        case '\'':
            return '\'';
        case '\"':
            return '"';
        case '\\':
            return '\\';
        default:
            return '0';
        }
    }

    private Characters() {/* noinit */}

}
