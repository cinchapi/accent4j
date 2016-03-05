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
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.reflect.Reflection;

/**
 * Unit tests for {@link ParentLastClassLoader}.
 * 
 * @author Jeff Nelson
 */
public class ParentLastClassLoaderTest {

    @Test
    public void testChildFirstPolicy() throws Exception {
        URL v1 = new File(Resources.getAbsolutePath("/foo-class-v1.jar"))
                .toURI().toURL();
        URL v2 = new File(Resources.getAbsolutePath("/foo-class-v2.jar"))
                .toURI().toURL();
        URLClassLoader parent = new URLClassLoader(new URL[] { v1 });
        ParentLastClassLoader loader = new ParentLastClassLoader(
                new URL[] { v2 }, parent);
        Class<?> cls = Class.forName("com.cinchapi.accent4j.test.FooClass",
                true, loader);
        Object instance = Reflection.newInstance(cls);
        int version = Reflection.call(instance, "getVersion");
        Assert.assertEquals(2, version);

    }

}
