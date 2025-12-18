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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link Application}.
 *
 * @author Jeff Nelson
 */
public class ApplicationTest {

    @Test
    public void testClasspathUrlsFromSystemPropertyReturnsEntries() {
        URL[] urls = Application.classpathUrlsFromSystemProperty();
        Assert.assertTrue(urls.length > 0);
    }

    @Test
    public void testClasspathUrlsFromSystemPropertyCountMatchesSystemProperty() {
        URL[] urls = Application.classpathUrlsFromSystemProperty();
        String classpath = System.getProperty("java.class.path");
        String[] parts = classpath.split(File.pathSeparator);
        Assert.assertEquals(parts.length, urls.length);
    }

    @Test
    public void testClasspathUrlsFromSystemPropertyMatchesUrlClassLoader()
            throws Exception {
        ClassLoader loader = Application.class.getClassLoader();
        if(loader instanceof URLClassLoader) {
            // On Java 8, verify that the fallback produces paths that are a
            // subset of what URLClassLoader returns (the URLClassLoader may
            // include additional entries like Gradle's worker jar)
            URL[] fromClassLoader = ((URLClassLoader) loader).getURLs();
            URL[] fromSystemProperty = Application.classpathUrlsFromSystemProperty();

            Set<Path> classLoaderPaths = new HashSet<>();
            for (URL url : fromClassLoader) {
                Path path = new File(url.toURI()).toPath();
                if(Files.exists(path)) {
                    classLoaderPaths.add(path);
                }
            }

            for (URL url : fromSystemProperty) {
                Path path = new File(url.toURI()).toPath();
                if(Files.exists(path)) {
                    Assert.assertTrue(
                            "Path from java.class.path should be in "
                                    + "URLClassLoader: " + path,
                            classLoaderPaths.contains(path));
                }
            }
        }
    }

    @Test
    public void testClasspathReturnsExistingPaths() {
        Collection<Path> classpath = Application.classpath();
        for (Path path : classpath) {
            Assert.assertTrue(Files.exists(path));
        }
    }

}

