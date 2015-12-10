/*
 * Copyright (c) 2013-2015 Cinchapi Inc.
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

/**
 * A {@link StringBuilder}-like interface for creating strings that are
 * formatted with ANSI escape sequences.
 * <p>
 * Builders of this type wrap the {@link Ansi} API to provide a more intuitive,
 * albeit more limited, interface. Use the {@link Ansi} builder directly if you
 * need advanced functionality that is not provided in this class.
 * </p>
 * <p>
 * <strong>NOTE:</strong> The underlying {@link Ansi} API is smart enough to
 * strip out formatting codes for consoles that do not support them.
 * </p>
 * 
 * @author Jeff Nelson
 */
public class AnsiStringBuilder {
    // Install support for the AnsiConsole and ensure that it is uninstalled
    // when the JVM dies
    static {
        AnsiConsole.systemInstall();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                AnsiConsole.systemUninstall();
            }
        });
    }

    /**
     * The instance of {@link Ansi} that does all the work. Here, we merely
     * abstract the API with a more intuitive interface.
     */
    private final Ansi ansi = Ansi.ansi();

    /**
     * Append the {@code text} to this builder and format it to appear with
     * the specified {@code color} and {@code bold}ness.
     * 
     * @param text
     *            the string to append
     * @param color
     *            the display {@link Color} for the text
     * @param bold
     *            a flag that indicates whether the text should appear bold
     * @return this
     */
    public AnsiStringBuilder append(String text, Color color, boolean bold) {
        ansi.fg(color);
        if(bold) {
            ansi.bold();
        }
        else {
            ansi.boldOff();
        }
        ansi.a(text);
        ansi.reset();
        return this;
    }

    /**
     * Append the {@code text} to this builder and apply no formatting.
     * 
     * @param text
     *            the string to append
     * @return this
     */
    public AnsiStringBuilder append(String text) {
        return append(text, false);
    }

    /**
     * Append the {@code text} to this builder and format it to appear with
     * the specified {@code bold}ness.
     * 
     * @param text
     *            the string to append
     * @param bold
     *            a flag that indicates whether the text should appear bold
     * @return this
     */
    public AnsiStringBuilder append(String text, boolean bold) {
        return append(text, Color.DEFAULT, bold);
    }

    /**
     * Append the {@code text} to this builder and format it to appear with
     * the specified {@code color}.
     * 
     * @param text
     *            the string to append
     * @param color
     *            the display {@link Color} for the text
     * @return this
     */
    public AnsiStringBuilder append(String text, Color color) {
        return append(text, color, false);
    }

    /**
     * Append the {@code text} to this builder and highlight it with a
     * background of {@code color}.
     * 
     * @param text
     *            the string to append
     * @param color
     *            the highlight {@link Color} for the text
     * @return this
     */
    public AnsiStringBuilder highlight(String text, Color color) {
        return highlight(text, color, false);
    }

    /**
     * Append the {@code text} to this builder, highlight it with {@code color}
     * and format it to appear with the specified {@code bold} ness.
     * 
     * @param text
     *            the string to append
     * @param color
     *            the highlight {@link Color} for the text
     * @param bold
     *            a flag that indicates whether the text should appear bold
     * @return this
     */
    public AnsiStringBuilder highlight(String text, Color color, boolean bold) {
        return highlight(text, color, Color.DEFAULT, bold);
    }

    /**
     * Append the {@code text} to this builder, highlight it with
     * {@code bgColor} and format it to appear with the specified
     * {@code textColor} and {@code bold}ness.
     * 
     * @param text
     *            the string to append
     * @param bgColor
     *            the highlight {@link Color} for the text
     * @param textColor
     *            the display {@link Color} for the text
     * @param bold
     *            a flag that indicates whether the text should appear bold
     * @return this
     */
    public AnsiStringBuilder highlight(String text, Color bgColor,
            Color textColor, boolean bold) {
        ansi.bg(bgColor);
        ansi.fg(textColor);
        if(bold) {
            ansi.bold();
        }
        else {
            ansi.boldOff();
        }
        ansi.a(text);
        ansi.reset();
        return this;
    }

    /**
     * Append the {@code text} to this builder, highlight it with
     * {@code bgColor} and format it to appear with the specified
     * {@code textColor}.
     * 
     * @param text
     *            the string to append
     * @param bgColor
     *            the highlight {@link Color} for the text
     * @param textColor
     *            the display {@link Color} for the text
     * @return this
     */
    public AnsiStringBuilder highlight(String text, Color bgColor,
            Color textColor) {
        return highlight(text, bgColor, textColor, false);
    }

    @Override
    public String toString() {
        return ansi.toString();
    }
}
