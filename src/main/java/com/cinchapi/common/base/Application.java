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
package com.cinchapi.common.base;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

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
        URL[] urls = ((URLClassLoader) loader).getURLs();
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
                                return new File(url.toURI()).toPath();
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

}
