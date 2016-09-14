/*
 * Copyright (c) 2013-2016 Cinchapi Inc.
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
package com.cinchapi.common.reflect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link Reflection} utility class.
 * 
 * @author Jeff Nelson
 */
@SuppressWarnings("unused")
public class ReflectionTest {

    private final Random random = new Random();

    @Test
    public void testInheritedGetValueFromSuperClass() {
        int expected = random.nextInt();
        B b = new B(expected);
        Assert.assertEquals("default", Reflection.get("string", b));
    }

    @Test
    public void testCallSuperClassMethod() {
        B b = new B(random.nextInt());
        Assert.assertEquals("default", Reflection.call(b, "string"));
        Assert.assertEquals("defaultdefaultdefault",
                Reflection.call(b, "string", 3));
    }

    @Test
    public void testGetValueFromClassA() {
        String expected = "" + random.nextInt();
        A a = new A(expected);
        Assert.assertEquals(expected, Reflection.get("string", a));
    }

    @Test
    public void testCallMethodInClassA() {
        String expected = "" + random.nextInt();
        A a = new A(expected);
        Assert.assertEquals(expected, Reflection.call(a, "string"));
        Assert.assertEquals(expected + expected + expected,
                Reflection.call(a, "string", 3));
    }

    @Test
    public void testCallMethodInClassB() {
        int expected = random.nextInt();
        B b = new B(expected);
        Assert.assertEquals((long) (expected * 10),
                (long) Reflection.call(b, "integer", 10));

    }

    @Test
    public void testGetValueFromClassB() {
        int expected = random.nextInt();
        B b = new B(expected);
        Assert.assertEquals(expected, (int) Reflection.get("integer", b));
    }

    @Test(expected = RuntimeException.class)
    public void testAttemptToGetValueForNonExistingFieldThrowsException() {
        A a = new A("" + random.nextInt());
        Reflection.get("foo", a);
    }

    @Test
    public void testConstructorAutoboxingSupport() {
        Integer integer = random.nextInt();
        B b = Reflection.newInstance(B.class, integer);
        Assert.assertNotNull(b);
    }

    @Test
    public void testMethodAutoboxingSupport() {
        int integer = random.nextInt();
        B b = new B(2);
        Reflection.call(b, "bigInteger", integer);
        Assert.assertTrue(true); // lack of exception means we passed...
    }

    @Test(expected = IllegalStateException.class)
    public void testCallIfNotAnnotated() {
        A a = new A("restricted");
        Reflection.callIf(
                (method) -> !method.isAnnotationPresent(Restricted.class), a,
                "restricted");
    }

    @Test
    public void testCallIf() {
        A a = new A("not restricted");
        Reflection.callIf(
                (method) -> !method.isAnnotationPresent(Restricted.class), a,
                "string");
    }

    @Test(expected = IllegalStateException.class)
    public void testCallIfNotPrivate() {
        A a = new A("not restricted");
        Reflection.callIf((method) -> method.isAccessible(), a, "string");
    }

    @Test
    public void testCallMethodWithNullArgument() {
        B b = new B(1);
        String arg = null;
        Reflection.call(b, "nullOkay", arg);
        Assert.assertTrue(true); // lack of NPE means test passes
    }

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Restricted {}

    private static class A {

        private final String string;

        public A(String string) {
            this.string = string;
        }

        private String string() {
            return string;
        }

        @Restricted
        public String restricted() {
            return string;
        }

        private String string(int count) {
            String result = "";
            for (int i = 0; i < count; i++) {
                result += string;
            }
            return result;
        }
    }

    private static class B extends A {

        private final int integer;

        public B(int integer) {
            super("default");
            this.integer = integer;
        }

        private long integer(int multiple) {
            return multiple * integer;
        }

        private long bigInteger(Integer multiple) {
            return multiple * integer;
        }

        private void nullOkay(String object) {}
    }

}
