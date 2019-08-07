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
package com.cinchapi.common.io;

import org.fusesource.jansi.Ansi.Color;

/**
 * A collection of functions for generating {@link String strings} that are
 * formatted with ANSI escape sequences if the underlying platform supports
 * them.
 * <p>
 * These functions are designed for generating strings with one-off ANSI
 * formatting. If you need to generate a string that has multiple ANSI format
 * codes, use a {@link AnsiStringBuilder} instead.
 * </p>
 * 
 * @author Jeff Nelson
 */
public class AnsiStrings {

    /**
     * Return a {@link String} that prints as bold {@code text} on supported
     * platforms.
     * 
     * @param text the string to format
     * @return the formatted string
     */
    public static String bold(String text) {
        return format(text, Color.DEFAULT, true);
    }

    /**
     * Return a {@link String} that prints as {@code text} with the
     * specified {@code color}.
     * 
     * @param text the string to format
     * @param color the display {@link Color} for the text
     * @return the formatted string
     */
    public static String format(String text, Color color) {
        return format(text, color, false);
    }

    /**
     * Return a {@link String} that prints as {@code text} with the
     * specified {@code color} and {@code bold}ness.
     * 
     * @param text the string to format
     * @param color the display {@link Color} for the text
     * @param bold a flag that indicates whether the text should appear bold
     * @return the formatted string
     */
    public static String format(String text, Color color, boolean bold) {
        AnsiStringBuilder asb = new AnsiStringBuilder();
        return asb.append(text, color, bold).toString();
    }

    /**
     * Return a {@link String} that prints {@code text} with the specified
     * {@code bgColor}, {@code textColor} and {@code bold}ness.
     * 
     * @param text the string to format
     * @param color the highlight {@link Color} for the text
     * @return the formatted string
     */
    public static String highlight(String text, Color color) {
        return highlight(text, color, Color.DEFAULT);
    }

    /**
     * Return a {@link String} that prints {@code text} with the specified
     * {@code bgColor}, {@code textColor} and {@code bold}ness.
     * 
     * @param text the string to format
     * @param bgColor the highlight {@link Color} for the text
     * @param textColor the display {@link Color} for the text
     * @return the formatted string
     */
    public static String highlight(String text, Color bgColor, Color textColor) {
        return highlight(text, bgColor, textColor);
    }

    /**
     * Return a {@link String} that prints {@code text} with the specified
     * {@code bgColor}, {@code textColor} and {@code bold}ness.
     * 
     * @param text the string to format
     * @param bgColor the highlight {@link Color} for the text
     * @param textColor the display {@link Color} for the text
     * @param bold a flag that indicates whether the text should appear bold
     * @return the formatted string
     */
    public static String highlight(String text, Color bgColor, Color textColor,
            boolean bold) {
        AnsiStringBuilder asb = new AnsiStringBuilder();
        return asb.highlight(text, bgColor, textColor, bold).toString();
    }

    private AnsiStrings() {/* noinit */}
}
