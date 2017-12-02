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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;

/**
 * A collection of functions that efficiently operate on {@link String strings}.
 * <p>
 * In some cases, we provide optimized implementations of functionality that
 * exists within the {@link String} class itself or other frameworks.
 * </p>
 * 
 * @author Jeff Nelson
 */
public class AnyStrings {

    /**
     * The start of a placeholder sequence for the
     * {@link #format(String, Object...)} method.
     */
    private static final char PLACEHOLDER_BEGIN = '{';

    /**
     * The end of a placeholder sequence for the
     * {@link #format(String, Object...)} method.
     */
    private static final char PLACEHOLDER_END = '}';

    /**
     * <em>Inspired by the SLF4J logging framework!</em>
     * <p>
     * Take a {@code template} string that contains placeholders ({}) and inject
     * each of the {@code args} in their place, respectively.
     * </p>
     * <p>
     * This method behaves similarly to {@link MessageFormat#format(Object)}
     * except the placeholders do not need to be numbered (i.e. "foo {} bar" vs
     * "foo {0} bar"). In most cases, the implementation of this method is also
     * much more efficient.
     * </p>
     * <p>
     * If there are more placeholders than {@code args}, the extra placeholders
     * will be retained formatted string.
     * </p>
     * <p>
     * If there are more {@code args} than placeholders, the extra {@code args}
     * will be placed at the end of the formatted string.
     * </p>
     * <p>
     * If an {@link Exception} is included in the injected {@code args}, only
     * the Exception's message will be included in the formatted. If you want to
     * include the full stack trace of an Exception, place it as the last of the
     * {@code args} and make sure that there is no correspond placeholder.
     * <h2>Example (borrowed from the SLf4J docs)</h2>
     * 
     * <pre>
     * String s = &quot;Hello world&quot;;
     * try {
     *     Integer i = Integer.valueOf(s);
     * }
     * catch (NumberFormatException e) {
     *     System.out.println(AnyStrings.format(&quot;Failed to format {}&quot;, s, e));
     * }
     * </pre>
     * 
     * </p>
     * 
     * @param template a template that may or may not contain placeholders for
     *            variable {@code args}
     * @param args the values to inject in the {@code template} placeholders,
     *            respectively
     * @return the formatted string
     */
    public static String format(String template, Object... args) {
        if(args == null || args.length == 0) {
            return template;
        }
        else if(template.isEmpty()) {
            return formatExtraArgs(new StringBuilder(), args, 0, args.length)
                    .toString();
        }
        else {
            StringBuilder sb = new StringBuilder();
            char[] chars = template.toCharArray();
            int templateLength = chars.length;
            int argsLength = args.length;
            int templateIndex = 0;
            int argsIndex = 0;
            int copyOffset = 0;
            int copyLength = 0;
            while (templateIndex < templateLength) {
                char c = chars[templateIndex];
                int next = templateIndex + 1;
                char nextc = next < templateLength ? chars[next]
                        : Characters.NULL;
                if(c == Characters.ESCAPE && nextc == PLACEHOLDER_BEGIN) {
                    // When escaping the placeholder, simply append any
                    // characters that are currently buffered (see copyLength)
                    // and then skip the escape character before proceeding
                    if(copyLength > 0) {
                        sb.append(chars, copyOffset, copyLength);
                    }
                    sb.append(chars, templateIndex + 1, 1); // append
                                                            // PLACEHOLDER_BEGIN
                                                            // so that we don't
                                                            // execute the block
                                                            // below on the next
                                                            // loop
                    templateIndex += 2;
                    copyOffset = templateIndex;
                    copyLength = 0;
                }
                else if(c == PLACEHOLDER_BEGIN && nextc == PLACEHOLDER_END
                        && argsIndex < argsLength) {
                    sb.append(chars, copyOffset, copyLength);
                    sb.append(String.valueOf(args[argsIndex]));
                    templateIndex = next + 1;
                    ++argsIndex;
                    copyOffset = templateIndex;
                    copyLength = 0;
                }
                else {
                    ++templateIndex;
                    ++copyLength;
                }
            }
            sb.append(chars, copyOffset, copyLength);
            if(argsIndex < argsLength) {
                // If there are remaining args that weren't represented in the
                // template with variable markers, simply append them as a list
                sb.append(": ");
                formatExtraArgs(sb, args, argsIndex, argsLength);
            }
            return sb.toString();
        }
    }

    /**
     * Return {@code true} if {@code string} both starts and ends with single or
     * double quotes.
     * 
     * @param string
     * @return {@code true} if the string is between quotes
     */
    public static boolean isWithinQuotes(String string) {
        if(string.length() > 2) {
            char first = string.charAt(0);
            if(first == '"' || first == '\'') {
                char last = string.charAt(string.length() - 1);
                return first == last;
            }
        }
        return false;
    }

    /**
     * Similar to the {@link String#valueOf(char)} method, but this one will
     * return a cached copy of the string for frequently used characters.
     * 
     * @param c the character to convert
     * @return a string of length 1 containing the input char
     */
    public static String valueOfCached(char c) {
        if(c == '(') {
            return "(";
        }
        else if(c == ')') {
            return ")";
        }
        else {
            return String.valueOf(c);
        }
    }

    /**
     * Execute the rules for formatting {@code args} with no corresponding
     * placeholders in the original template.
     * 
     * @param sb the {@link StringBuilder} that contains the formatted string;
     *            modified in-place within this method and returned for
     *            convenience
     * @param args all of the placeholder values
     * @param argsIndex the index of the first of the {@code args} that is
     *            "extra"
     * @param argsLength the length of {@code args}, required so it is not
     *            computed twice
     * @return {@code sb} for convenience
     */
    private static StringBuilder formatExtraArgs(StringBuilder sb,
            Object[] args, int argsIndex, int argsLength) {
        for (int i = argsIndex; i < argsLength; ++i) {
            Object arg = args[i];
            int nextIndex = i + 1;
            if(nextIndex == argsLength && arg instanceof Exception) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ((Exception) arg).printStackTrace(new PrintStream(baos));
                sb.append(baos.toString());
            }
            else {
                sb.append(String.valueOf(arg));
            }
            if(nextIndex != argsLength) {
                sb.append(' ');
            }
        }
        return sb;
    }

}
