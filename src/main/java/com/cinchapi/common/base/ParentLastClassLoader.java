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

import java.net.URL;
import java.net.URLClassLoader;

/**
 * A {@link ClassLoader class loader} that delegates to the parent class loader
 * ONLY IF a class cannot be found in the locally. This is the opposite of a
 * ClassLoader's typical behaviour, which always checks the parent first.
 * 
 * @author Jeff Nelson
 */
public class ParentLastClassLoader extends URLClassLoader {

    /**
     * Construct a new instance.
     * 
     * @param urls
     */
    public ParentLastClassLoader(URL[] urls) {
        this(urls, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Construct a new instance.
     * 
     * @param urls
     * @param parent
     */
    public ParentLastClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Class<?> cls = findLoadedClass(name); // check to see if class is
                                              // already loaded
        if(cls == null) {
            try {
                cls = findClass(name); // check for class locally
            }
            catch (ClassNotFoundException e) {}
        }
        if(cls == null) {
            cls = this.getParent().loadClass(name); // defer to parent as a last
                                                    // resort
        }
        return cls;
    }

}
