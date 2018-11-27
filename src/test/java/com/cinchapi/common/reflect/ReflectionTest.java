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

import java.io.FileNotFoundException;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

/**
 * Unit tests for the {@link Reflection} utility class.
 * 
 * @author Jeff Nelson
 */
@SuppressWarnings("unused")
public class ReflectionTest {

    private final Random random = new Random();

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test(expected = RuntimeException.class)
    public void testAttemptToGetValueForNonExistingFieldThrowsException() {
        A a = new A("" + random.nextInt());
        Reflection.get("foo", a);
    }

    @Test
    public void testCallIf() {
        A a = new A("not restricted");
        Reflection.callIf(
                (method) -> !method.isAnnotationPresent(Restricted.class), a,
                "string");
    }

    @Test(expected = RuntimeException.class)
    public void testCallIfAccessible() {
        A a = new A("foo");
        Reflection.callIfAccessible(a, "string");
    }

    @Test(expected = IllegalStateException.class)
    public void testCallIfNotAnnotated() {
        A a = new A("restricted");
        Reflection.callIf(
                (method) -> !method.isAnnotationPresent(Restricted.class), a,
                "restricted");
    }

    @Test(expected = IllegalStateException.class)
    public void testCallIfNotPrivate() {
        A a = new A("not restricted");
        Reflection.callIf(
                (method) -> !Modifier.isPrivate(method.getModifiers()), a,
                "string");
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
    public void testCallMethodSuperClassParameterType() {
        A a = new A("foo");
        List<String> list = Lists.newArrayList("1");
        List<String> listlist = Reflection.call(a, "list", list, list);
        Assert.assertEquals(2, listlist.size());
    }

    @Test
    public void testCallMethodSuperClassParameterTypeOneIsNull() {
        A a = new A("foo");
        List<String> list = Lists.newArrayList("1");
        List<String> listlist = Reflection.call(a, "list", list, null);
        Assert.assertEquals(1, listlist.size());
    }

    @Test
    public void testCallMethodWithNullArgument() {
        B b = new B(1);
        String arg = null;
        Reflection.call(b, "nullOkay", arg);
        Assert.assertTrue(true); // lack of NPE means test passes
    }

    @Test
    public void testCallOverloadedMethodName() {
        B b = new B(1);
        Reflection.call(b, "foo", "1");
        Reflection.call(b, "foo", 1);
        Assert.assertTrue(true); // lack of NSME means test passes
    }

    @Test
    public void testCallRedeclaredMethod() {
        B b = new B(1);
        Reflection.call(b, "redeclare");
    }

    @Test
    public void testCallSuperClassMethod() {
        B b = new B(random.nextInt());
        Assert.assertEquals("default", Reflection.call(b, "string"));
        Assert.assertEquals("defaultdefaultdefault",
                Reflection.call(b, "string", 3));
    }

    @Test
    public void testCheckedExceptionIsPreserved() {
        expectedException.expect(RuntimeException.class);
        String message = "This is the message I want to see";
        expectedException.expectMessage(message);
        A a = new A("foo");
        Reflection.call(a, "throwCheckedException", message);
    }

    @Test
    public void testConstructorAutoboxingSupport() {
        Integer integer = random.nextInt();
        B b = Reflection.newInstance(B.class, integer);
        Assert.assertNotNull(b);
    }

    @Test
    public void testGetMethodUnboxedDoesNotAcceptObjectAsBaseClass() {
        Reflection.getMethodUnboxed(E.class, "oneArg", String.class);
        Assert.assertTrue(true); // lack of Exception means test passes
    }

    @Test
    public void testGetClosestCommonAncestor() {
        abstract class D {}
        class DA extends D {}
        class DB extends D {}
        Assert.assertEquals(D.class,
                Reflection.getClosestCommonAncestor(DA.class, DB.class));
        @SuppressWarnings("serial")
        class DBA extends DB implements Serializable {}
        Assert.assertEquals(Serializable.class,
                Reflection.getClosestCommonAncestor(DBA.class, String.class));
        Assert.assertEquals(D.class,
                Reflection.getClosestCommonAncestor(DBA.class, DA.class));
        Assert.assertEquals(Object.class, Reflection
                .getClosestCommonAncestor(DBA.class, DA.class, C.class));
    }

    @Test
    public void testGetEnumValueByName() {
        Assert.assertEquals(C.BAZ, Reflection.getEnumValue(C.class, "BAZ"));
    }

    @Test
    public void testGetEnumValueByOrdinal() {
        Assert.assertEquals(C.FOO, Reflection.getEnumValue(C.class, 0));
        Assert.assertEquals(C.BAR, Reflection.getEnumValue(C.class, 1));
        Assert.assertEquals(C.BAZ, Reflection.getEnumValue(C.class, 2));
    }

    @Test
    public void testGetEnumValueByOrdinalGeneric() {
        Object ordinal = 0;
        Assert.assertEquals(C.FOO, Reflection.getEnumValue(C.class, ordinal));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetEnumValueByOrdinalOutOfBounds() {
        Assert.assertEquals(C.BAZ, Reflection.getEnumValue(C.class, 3));
    }

    @Test
    public void testGetValueFromClassA() {
        String expected = "" + random.nextInt();
        A a = new A(expected);
        Assert.assertEquals(expected, Reflection.get("string", a));
    }

    @Test
    public void testGetValueFromClassB() {
        int expected = random.nextInt();
        B b = new B(expected);
        Assert.assertEquals(expected, (int) Reflection.get("integer", b));
    }

    @Test
    public void testInheritedGetValueFromSuperClass() {
        int expected = random.nextInt();
        B b = new B(expected);
        Assert.assertEquals("default", Reflection.get("string", b));
    }

    @Test
    public void testIntegerAndLongInterchangeable() {
        A a = new A("foo");
        Reflection.call(a, "tryLong", 1);
    }

    @Test
    public void testGetMethodUnboxedCollections() {
        Reflection.getMethodUnboxed(A.class, "hasCollection", ArrayList.class);
        Assert.assertTrue(true); // lack of exception means we passed...
    }

    @Test
    public void testMethodAutoboxingSupport() {
        int integer = random.nextInt();
        B b = new B(2);
        Reflection.call(b, "bigInteger", integer);
        Assert.assertTrue(true); // lack of exception means we passed...
    }

    @Test
    public void testCallGenericArg() {
        E e = new E();
        Assert.assertEquals("Foo",
                Reflection.call(e, "genericArg", "foo", "Foo"));
        Assert.assertEquals(17L,
                (long) Reflection.call(e, "genericArg", "foo", 17L));
    }

    @Test
    public void testCallObjectArgInChildClass() {
        B b = new B(1);
        Assert.assertEquals(b.generic(1), Reflection.call(b, "generic", 1));
        Assert.assertEquals(b.generic("foo"),
                Reflection.call(b, "generic", "foo"));
    }

    @Test
    public void testIsAnnotationPresentInHierarchyFalse() {
        Method method = Reflection.getMethodUnboxed(ChildAnnotationHolder.class,
                "bar");
        Assert.assertFalse(Reflection.isDeclaredAnnotationPresentInHierarchy(
                method, Restricted.class));
    }

    @Test
    public void testIsAnnotationPresentInHierarchyTrue() {
        Method method = Reflection.getMethodUnboxed(ChildAnnotationHolder.class,
                "foo");
        Assert.assertTrue(Reflection.isDeclaredAnnotationPresentInHierarchy(
                method, Restricted.class));
    }

    @Test
    public void testGetTypeArguments() {
        Assert.assertEquals(ImmutableSet.of(String.class), Reflection
                .getTypeArguments("listOfString", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableSet.of(A.class),
                Reflection.getTypeArguments("setOfA", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableSet.of(Integer.class, Boolean.class),
                Reflection.getTypeArguments("mapIntegerToBoolean",
                        ClassWithGenerics.class));
        Assert.assertEquals(ImmutableSet.of(), Reflection
                .getTypeArguments("noGenerics", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableSet.of(), Reflection.getTypeArguments(
                "noGenericsCollection", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableSet.of(B.class), Reflection
                .getTypeArguments("collectionOfB", ClassWithGenerics.class));
    }
    
    @Test
    public void testIsCallableWith() {
        for(Method method : Foo.class.getDeclaredMethods()) {
            if(method.getName().equals("noArgs")) {
                Assert.assertTrue(Reflection.isCallableWith(method));
                Assert.assertFalse(Reflection.isCallableWith(method, new Object() {}));
            }
            else if(method.getName().equals("objectArg")) {
                Assert.assertFalse(Reflection.isCallableWith(method));
                Assert.assertTrue(Reflection.isCallableWith(method, new Object() {}));
                Assert.assertTrue(Reflection.isCallableWith(method, 1));
            }
            else if(method.getName().equals("superClassArg")) {
                Assert.assertTrue(Reflection.isCallableWith(method, 1));
                Assert.assertFalse(Reflection.isCallableWith(method, new Object() {}));
            }
            else if(method.getName().equals("multipleArgs")) {
                Assert.assertTrue(Reflection.isCallableWith(method, "foo", new Foo()));
                Assert.assertFalse(Reflection.isCallableWith(method, "foo", new Object() {}));
            }
            else if(method.getName().equals("varArgs")) {
                Assert.assertTrue(Reflection.isCallableWith(method, "foo")); 
                Assert.assertTrue(Reflection.isCallableWith(method)); 
            }
            else if(method.getName().equals("varArgs2")) {
                Assert.assertTrue(Reflection.isCallableWith(method, "foo")); 
                Assert.assertTrue(Reflection.isCallableWith(method, "a", "b", "c", "d", "e")); 
            }
        }
    }
    
    @Test
    public void testCallMethodWithVarArgs() {
        Reflection.call(new Foo(), "varArgs", "foo");
        Reflection.call(new Foo(), "varArgs2", "foo", "foo", "bar");
        Reflection.call(new Foo(), "varArgs");
        Reflection.call(new Foo(), "varArgs2", "foo");
        Assert.assertTrue(true); // lack of exception means we pass
    }

    private static class A {

        private final String string;

        public A(String string) {
            this.string = string;
        }

        public List<String> list(List<String> list, List<String> list2) {
            if(list2 != null) {
                list.addAll(list2);
            }
            return list;
        }

        public String redeclare() {
            return string;
        }

        @Restricted
        public String restricted() {
            return string;
        }

        public long tryLong(long l) {
            return l;
        }

        private String string() {
            return string;
        }

        private String string(int count) {
            String result = "";
            for (int i = 0; i < count; i++) {
                result += string;
            }
            return result;
        }

        private void hasCollection(List<String> foo) {}

        private void throwCheckedException(String message)
                throws FileNotFoundException {
            throw new FileNotFoundException(message);
        }

        public String generic(String foo) {
            return "parent";
        }
    }

    private static class B extends A {

        private final int integer;

        public B(int integer) {
            super("default");
            this.integer = integer;
        }

        public void foo(int integer) {

        }

        public void foo(String string) {

        }

        @Override
        public String redeclare() {
            return "" + integer;
        }

        private long bigInteger(Integer multiple) {
            return multiple * integer;
        }

        private long integer(int multiple) {
            return multiple * integer;
        }

        private void nullOkay(String object) {}

        public String generic(Object foo) {
            return "child";
        }
    }

    private static enum C {
        FOO, BAR, BAZ;
    }

    private class E {

        public void oneArg(String arg) {

        }

        public void oneArg(Object arg) {

        }

        public <T> T genericArg(String name, T arg) {
            return arg;
        }
    }

    private class ParentAnnotationHolder {

        @Restricted
        public void foo() {

        }

        public void bar() {

        }
    }

    private class ChildAnnotationHolder extends ParentAnnotationHolder {}

    @Retention(RetentionPolicy.RUNTIME)
    private @interface Restricted {}

    private class ClassWithGenerics {

        private List<String> listOfString;
        private Set<A> setOfA;
        public Map<Integer, Boolean> mapIntegerToBoolean;
        public Long noGenerics;
        public Collection<?> noGenericsCollection;
        protected Collection<B> collectionOfB;
    }
    
    public class Foo {
        
        public void noArgs() {}
        
        public void objectArg(Object arg) {}
        
        public void superClassArg(Number arg) {}
        
        public void multipleArgs(String arg1, Foo arg2) {}
        
        public void varArgs(String...args) {}
        
        public void varArgs2(String arg, String...args) {}
        
    }

}
