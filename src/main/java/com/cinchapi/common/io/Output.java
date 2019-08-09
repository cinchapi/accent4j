/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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

import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.CheckedExceptions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

/**
 * {@link Output} contains utility functions for pushing objects to an
 * {@link OutputStream} in various formats.
 *
 * @author Jeff Nelson
 */
public final class Output {

    /**
     * Output the {@code items} as a sequence of lines (one line per item) where
     * each key/value pair is separated by a {@code delimiter}. Conceptually,
     * this method can be used to transform a collection of {@link Map Maps} to
     * a CSV file.
     * <p>
     * The keys of all the {@code items} are joined to form a header line. If an
     * item does not contain a value for a header key, {@code null} is output
     * instead.
     * </p>
     * 
     * @param items
     * @param delimiter
     */
    public static <K, V> void delimitedLines(Iterable<Map<K, V>> items,
            char delimiter) {
        delimitedLines(items, delimiter, System.out);
    }

    /**
     * Output the {@code items} as a sequence of lines (one line per item) where
     * each key/value pair is separated by a {@code delimiter}. Conceptually,
     * this method can be used to transform a collection of {@link Map Maps} to
     * a CSV file.
     * <p>
     * The keys of all the {@code items} are joined to form a header line. If an
     * item does not contain a value for a header key, {@code null} is output
     * instead.
     * </p>
     * 
     * @param items
     * @param delimiter
     * @param output
     */
    public static <K, V> void delimitedLines(Iterable<Map<K, V>> items,
            char delimiter, OutputStream output) {
        Set<K> headers = Sets.newLinkedHashSet();
        for (Map<K, V> item : items) {
            for (K key : item.keySet()) {
                headers.add(key);
            }
        }
        PrintStream printer = new PrintStream(output);
        delimitedLine(headers, delimiter, printer);
        for (Map<K, V> item : items) {
            Iterable<V> values = Iterables.transform(headers,
                    header -> item.get(header));
            delimitedLine(values, delimiter, printer);
        }
    }

    /**
     * Output the {@code items} as a sequence of lines (one line per item) where
     * each key/value pair is separated by a {@code delimiter}. Conceptually,
     * this method can be used to transform a collection of {@link Map Maps} to
     * a CSV file.
     * <p>
     * The keys of all the {@code items} are joined to form a header line. If an
     * item does not contain a value for a header key, {@code null} is output
     * instead.
     * </p>
     * 
     * @param items
     * @param delimiter
     * @param fiter
     */
    public static <K, V> void delimitedLines(Iterable<Map<K, V>> items,
            char delimiter, Path file) {
        try {
            FileOutputStream fos = new FileOutputStream(
                    file.toAbsolutePath().toString());
            delimitedLines(items, delimiter, fos);
        }
        catch (FileNotFoundException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Output the {@code values} to the {@code output} where each one is
     * separated by a {@code delimiter}.
     * 
     * @param values
     * @param delimiter
     * @param output
     */
    private static <V> void delimitedLine(Iterable<V> values, char delimiter,
            OutputStream output) {
        PrintStream printer = output instanceof PrintStream
                ? (PrintStream) output
                : new PrintStream(output);
        boolean prependComma = false;
        for (V value : values) {
            if(prependComma) {
                printer.print(',');
            }
            printer.print(value != null ? AnyStrings.ensureWithinQuotesIfNeeded(
                    value.toString(), delimiter) : value);
            prependComma = true;
        }
        printer.println();
    }

    private Output() {/* no-init */}

}
