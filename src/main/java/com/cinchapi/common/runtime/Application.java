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
package com.cinchapi.common.runtime;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

import com.cinchapi.common.base.AdHocIterator;
import com.cinchapi.common.base.CheckedExceptions;

/**
 * Utility functions for interacting with the current application instance.
 *
 * @author Jeff Nelson
 */
public final class Application {

    /**
     * Return a collection of {@link Path} objects that describe all the
     * elements that are on the current application's classpath.
     *
     * @return the current applications classpath
     */
    public static Collection<Path> classpath() {
        ClassLoader loader = Application.class.getClassLoader();
        if(loader == null) {
            loader = ClassLoader.getSystemClassLoader();
        }
        final URL[] urls;
        if(loader instanceof URLClassLoader) {
            urls = ((URLClassLoader) loader).getURLs();
        }
        else {
            // In Java 9+, the system class loader is no longer an instance of
            // URLClassLoader, so we fall back to parsing the java.class.path
            // system property.
            urls = classpathUrlsFromSystemProperty();
        }
        return new AbstractCollection<Path>() {

            @Override
            public Iterator<Path> iterator() {

                return new AdHocIterator<Path>() {
                    private int index = -1;

                    @Override
                    protected Path findNext() {
                        ++index;
                        if(index < urls.length) {
                            URL url = urls[index];
                            try {
                                Path path = new File(url.toURI()).toPath();
                                if(Files.exists(path)) {
                                    // It is possible to pass non-existing paths
                                    // to the JVM classpath, so we need to
                                    // ensure they exist here so the caller
                                    // isn't surprised
                                    return path;
                                }
                                else {
                                    return findNext();
                                }
                            }
                            catch (URISyntaxException e) {
                                throw CheckedExceptions
                                        .throwAsRuntimeException(e);
                            }
                        }
                        else {
                            return null;
                        }
                    }

                };
            }

            @Override
            public int size() {
                return urls.length;
            }

        };
    }

    /**
     * Return a {@link Path} to the java executable that was used to launch the
     * current application.
     *
     * @return the java path
     */
    public static Path whichJava() {
        return Paths.get(System.getProperty("java.home") + File.separator
                + "bin" + File.separator + "java");
    }

    /**
     * Parse the {@code java.class.path} system property and return an array
     * of {@link URL URLs} representing each classpath entry.
     *
     * @return the classpath URLs
     */
    protected static URL[] classpathUrlsFromSystemProperty() {
        String classpath = System.getProperty("java.class.path");
        if(classpath == null || classpath.isEmpty()) {
            return new URL[0];
        }
        else {
            String[] parts = classpath.split(File.pathSeparator);
            URL[] urls = new URL[parts.length];
            for (int i = 0; i < parts.length; i++) {
                try {
                    urls[i] = new File(parts[i]).toURI().toURL();
                }
                catch (java.net.MalformedURLException e) {
                    throw CheckedExceptions.throwAsRuntimeException(e);
                }
            }
            return urls;
        }
    }

}
