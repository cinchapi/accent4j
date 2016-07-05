/*
 * Copyright (c) 2016 Cinchapi Inc.
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.AbstractList;
import java.util.Iterator;

import javax.annotation.Nullable;

import com.cinchapi.common.base.CheckedExceptions;
import com.cinchapi.common.base.ReadOnlyIterator;

/**
 * Utility methods for working with files.
 * 
 * @author Jeff Nelson
 */
public final class Files {

    /**
     * Expand the given {@code path} so that it contains completely normalized
     * components (e.g. ".", "..", and "~" are resolved to the correct absolute
     * paths).
     * 
     * @param path
     * @return the expanded path
     */
    public static String expandPath(String path) {
        return expandPath(path, null);
    }

    /**
     * Expand the given {@code path} so that it contains completely normalized
     * components (e.g. ".", "..", and "~" are resolved to the correct absolute
     * paths).
     * 
     * @param path
     * @param cwd
     * @return the expanded path
     */
    public static String expandPath(String path, String cwd) {
        path = path.replaceAll("~", USER_HOME);
        Path base = cwd == null || cwd.isEmpty() ? BASE_PATH : FileSystems
                .getDefault().getPath(cwd);
        return base.resolve(path).normalize().toString();
    }

    /**
     * Return an {@link Iterable} collection that lazily accumulates lines in
     * the underlying {@code file}.
     * <p>
     * This method is really just syntactic sugar for reading lines from a file,
     * so the returned collection doesn't actually allow any operations other
     * than forward iteration.
     * </p>
     * 
     * @param file the path to the file
     * @return an iterable collection of lines from the file
     */
    public static Iterable<String> readLines(final String file) {
        return readLines(file, null);
    }

    /**
     * Return an {@link Iterable} collection that lazily accumulates lines in
     * the underlying {@code file}.
     * <p>
     * This method is really just syntactic sugar for reading lines from a file,
     * so the returned collection doesn't actually allow any operations other
     * than forward iteration.
     * </p>
     * 
     * @param file the path to the file
     * @param cwd the working directory against which relative paths are
     *            resolved
     * @return an iterable collection of lines in the file
     */
    public static Iterable<String> readLines(final String file,
            @Nullable String cwd) {
        final String rwd = cwd == null ? WORKING_DIRECTORY : cwd;
        return new AbstractList<String>() {

            @Override
            public String get(int index) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<String> iterator() {
                return new ReadOnlyIterator<String>() {

                    String line = null;
                    BufferedReader reader;
                    {
                        try {
                            reader = new BufferedReader(new FileReader(
                                    expandPath(file, rwd)));
                            line = reader.readLine();
                        }
                        catch (IOException e) {
                            throw CheckedExceptions.throwAsRuntimeException(e);
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return this.line != null;
                    }

                    @Override
                    public String next() {
                        String result = line;
                        try {
                            line = reader.readLine();
                            if(line == null) {
                                reader.close();
                            }
                            return result;
                        }
                        catch (IOException e) {
                            throw CheckedExceptions.throwAsRuntimeException(e);
                        }
                    }

                };
            }

            @Override
            public int size() {
                int size = 0;
                Iterator<String> it = iterator();
                while (it.hasNext()) {
                    size += 1;
                    it.next();
                }
                return size;
            }

        };

    }

    /**
     * The user's home directory, which is used to expand path names with "~"
     * (tilde).
     */
    private static String USER_HOME = System.getProperty("user.home");

    /**
     * The working directory from which the current JVM process was launched.
     */
    private static String WORKING_DIRECTORY = System.getProperty("user.dir");

    /**
     * The base path that is used to resolve and normalize other relative paths.
     */
    private static Path BASE_PATH = FileSystems.getDefault().getPath(
            WORKING_DIRECTORY);

}
