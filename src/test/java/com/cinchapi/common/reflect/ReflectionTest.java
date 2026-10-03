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
import java.io.InputStream;
import java.io.Serializable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import com.cinchapi.common.base.Array;
import com.cinchapi.common.runtime.Application;
import com.google.common.base.CaseFormat;
import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

/**
 * Unit tests for the {@link Reflection} utility class.
 *
 * @author Jeff Nelson
 */
@SuppressWarnings("unused")
public class ReflectionTest {

    /**
     * Exercise reflective invocation with an argument from a separate
     * {@link ClassLoader}.
     *
     * @param queue the queue for the returned reference, or {@code null} for no
     *            queue
     * @param action the reflective invocation to run with the argument
     * @return a {@link WeakReference} to the {@link ClassLoader} that loaded
     *         the argument, with the loader's resources closed
     * @throws Exception if argument creation, invocation or resource closure
     *             fails
     */
    private static WeakReference<ClassLoader> callWithArgumentFromNewLoader(
            ReferenceQueue<ClassLoader> queue, Consumer<Object> action)
            throws Exception {
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] { getLocation(Payload.class) }, null)) {
            Object arg = loader.loadClass(Payload.class.getName())
                    .getDeclaredConstructor().newInstance();
            action.accept(arg);
            return new WeakReference<>(loader, queue);
        }
    }

    /**
     * Exercise reflective invocation with an argument whose class is a hidden
     * copy of {@link Payload} in the class path's {@link ClassLoader}.
     *
     * @param queue the queue for the returned reference
     * @param action the reflective invocation to run with the argument
     * @return a {@link WeakReference} to the hidden class
     * @throws Exception if class definition, instantiation or invocation fails
     */
    private static WeakReference<Class<?>> callWithArgumentOfHiddenClass(
            ReferenceQueue<Class<?>> queue, Consumer<Object> action)
            throws Exception {
        Class<?> type = defineHiddenCopyOf(Payload.class);
        action.accept(type.getDeclaredConstructor().newInstance());
        return new WeakReference<>(type, queue);
    }

    /**
     * Exercise a static method of a separately loaded copy of
     * {@link Reflection}.
     *
     * @param queue the queue for the returned reference, or {@code null} for no
     *            queue
     * @param method the name of the {@link Reflection} method to call
     * @param types the parameter types of the {@link Reflection} method
     * @param args the arguments to pass to the {@link Reflection} method; the
     *            class of each must be one that the bootstrap
     *            {@link ClassLoader} loads
     * @return a {@link WeakReference} to the {@link ClassLoader} that loaded
     *         the copy of {@link Reflection}, with the loader's resources
     *         closed
     * @throws Exception if loading, invocation or resource closure fails
     */
    private static WeakReference<ClassLoader> callFromNewLoader(
            ReferenceQueue<ClassLoader> queue, String method,
            Class<?>[] types, Object... args) throws Exception {
        URL[] locations = { getLocation(Reflection.class),
                getLocation(Lists.class) };
        try (URLClassLoader loader = new URLClassLoader(locations,
                ClassLoader.getSystemClassLoader().getParent())) {
            loader.loadClass(Reflection.class.getName())
                    .getMethod(method, types).invoke(null, args);
            return new WeakReference<>(loader, queue);
        }
    }

    /**
     * Define a class from the bytecode of {@code clazz} that Java can unload
     * while the {@link ClassLoader} of {@code clazz} stays in memory.
     * <p>
     * On Java 15 and later the result is a hidden class. On earlier versions it
     * is a VM anonymous class. Both report the {@link ClassLoader} of
     * {@code clazz}.
     * </p>
     *
     * @param clazz a class in the package of {@link ReflectionTest} whose class
     *            file is on the class path
     * @return the new class
     * @throws Exception if the class file cannot be read or the definition
     *             fails
     */
    private static Class<?> defineHiddenCopyOf(Class<?> clazz)
            throws Exception {
        String file = clazz.getName()
                .substring(clazz.getName().lastIndexOf('.') + 1) + ".class";
        byte[] bytes;
        try (InputStream input = clazz.getResourceAsStream(file)) {
            bytes = ByteStreams.toByteArray(input);
        }
        if(Application.javaVersion() >= 15) {
            // NOTE: The tests compile on Java 8, which has no
            // defineHiddenClass, so this calls it reflectively.
            Class<?> option = Class.forName(
                    "java.lang.invoke.MethodHandles$Lookup$ClassOption");
            Object options = java.lang.reflect.Array.newInstance(option, 0);
            Lookup lookup = (Lookup) Lookup.class
                    .getMethod("defineHiddenClass", byte[].class,
                            boolean.class, options.getClass())
                    .invoke(MethodHandles.lookup(), bytes, true, options);
            return lookup.lookupClass();
        }
        else {
            // NOTE: Java 17 and later have no defineAnonymousClass, so this
            // calls it reflectively to keep the tests compilable there.
            Field field = Class.forName("sun.misc.Unsafe")
                    .getDeclaredField("theUnsafe");
            field.setAccessible(true);
            Object unsafe = field.get(null);
            return (Class<?>) unsafe.getClass()
                    .getMethod("defineAnonymousClass", Class.class,
                            byte[].class, Object[].class)
                    .invoke(unsafe, ReflectionTest.class, bytes, null);
        }
    }

    /**
     * Run {@code action} and return the cause of the {@link RuntimeException}
     * that it throws.
     *
     * @param action the action to run
     * @return the cause of the {@link RuntimeException}, or {@code null} if
     *         {@code action} does not throw one
     */
    private static Throwable getCauseOfFailure(Runnable action) {
        Throwable cause = null;
        try {
            action.run();
        }
        catch (RuntimeException e) {
            cause = e.getCause();
        }
        return cause;
    }

    /**
     * Return the class path entry that holds {@code clazz}.
     *
     * @param clazz the class to locate; must have a non-null code source
     * @return the {@link URL} of its code source, which may be {@code null}
     */
    private static URL getLocation(Class<?> clazz) {
        return clazz.getProtectionDomain().getCodeSource().getLocation();
    }

    /**
     * Request garbage collection and wait for a reference in {@code queue}.
     *
     * @param queue the queue to observe
     * @return {@code true} if a reference is removed from {@code queue}, or
     *         {@code false} if the wait ends without one
     * @throws InterruptedException if the thread is interrupted while it waits
     *             for {@code queue}
     */
    private static boolean isCollected(ReferenceQueue<?> queue)
            throws InterruptedException {
        boolean collected = false;
        for (int i = 0; i < 20 && !collected; ++i) {
            System.gc();
            collected = queue.remove(100) != null;
        }
        return collected;
    }

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
        Assert.assertEquals(ImmutableList.of(String.class), Reflection
                .getTypeArguments("listOfString", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(A.class),
                Reflection.getTypeArguments("setOfA", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(Integer.class, Boolean.class),
                Reflection.getTypeArguments("mapIntegerToBoolean",
                        ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(), Reflection
                .getTypeArguments("noGenerics", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(Object.class),
                Reflection.getTypeArguments("noGenericsCollection",
                        ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(B.class), Reflection
                .getTypeArguments("collectionOfB", ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(Integer.class, Integer.class),
                Reflection.getTypeArguments("mapIntegerToInteger",
                        ClassWithGenerics.class));
        Assert.assertEquals(ImmutableList.of(AtomicReference.class),
                Reflection.getTypeArguments("listAtomicReference",
                        ClassWithGenerics.class));
    }

    @Test
    public void testIsCallableWith() {
        for (Method method : Foo.class.getDeclaredMethods()) {
            if(method.getName().equals("noArgs")) {
                Assert.assertTrue(Reflection.isCallableWith(method));
                Assert.assertFalse(
                        Reflection.isCallableWith(method, new Object() {}));
            }
            else if(method.getName().equals("objectArg")) {
                Assert.assertFalse(Reflection.isCallableWith(method));
                Assert.assertTrue(
                        Reflection.isCallableWith(method, new Object() {}));
                Assert.assertTrue(Reflection.isCallableWith(method, 1));
            }
            else if(method.getName().equals("superClassArg")) {
                Assert.assertTrue(Reflection.isCallableWith(method, 1));
                Assert.assertFalse(
                        Reflection.isCallableWith(method, new Object() {}));
            }
            else if(method.getName().equals("multipleArgs")) {
                Assert.assertTrue(
                        Reflection.isCallableWith(method, "foo", new Foo()));
                Assert.assertFalse(Reflection.isCallableWith(method, "foo",
                        new Object() {}));
            }
            else if(method.getName().equals("varArgs")) {
                Assert.assertTrue(Reflection.isCallableWith(method, "foo"));
                Assert.assertTrue(Reflection.isCallableWith(method));
            }
            else if(method.getName().equals("varArgs2")) {
                Assert.assertTrue(Reflection.isCallableWith(method, "foo"));
                Assert.assertTrue(Reflection.isCallableWith(method, "a", "b",
                        "c", "d", "e"));
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

    @Test
    public void testIsCallableWithReproA() {
        Assert.assertFalse(
                Reflection.isCallableWith(
                        Reflection.getMethodUnboxed(Foo.class, "reproA",
                                char.class, CaseFormat[].class),
                        Predicates.alwaysTrue()));
    }

    @Test
    public void testCallOverloadedVarArgsReproA() {
        Reflection.call(new Foo(), "overloadVarArgs");
        Reflection.call(new Foo(), "overloadVarArgs", "a");
        Reflection.call(new Foo(), "overloadVarArgs", "a", "b");
    }

    @Test
    public void testIsCallableWithReproB() {
        Object[] params = (Object[]) java.lang.reflect.Array
                .newInstance(Object.class, 1);
        params[0] = "foo";
        Assert.assertTrue(Reflection.isCallableWith(Reflection.getMethodUnboxed(
                Foo.class, "varArgs", String[].class), params));
    }

    @Test
    public void testCallReproC() {
        Reflection.call(new Foo(), "varArgs",
                ImmutableList.of(Array.containing("foo")).toArray());
        Assert.assertTrue(true); // lack of Exception means we pass
    }

    @Test
    public void testNoAmbiguityForOverloadedMethodsWithAutoboxedArgs() {
        Reflection.call(new Foo(), "verA", "foo", 1);
        Reflection.call(new Foo(), "verA", "foo", 1L);
        Reflection.call(new Foo(), "verA", "foo", new Long(1));
        Assert.assertTrue(true); // lack of Exception means we pass
    }

    @Test
    public void testCallOverrideWithGenericParameter() {
        GenericChild obj = new GenericChild();
        Reflection.call(obj, "put", "Company", "Cinchapi");
        Assert.assertTrue(true); // lack of Exception means we pass
    }

    @Test
    public void testCallDefaultInterfaceMethod() {
        ClassB obj = new ClassB();
        String expected = "Jeff Nelson";
        String actual = Reflection.call(obj, "foo", expected);
        Assert.assertEquals("foo_" + expected, actual);
        Assert.assertEquals("baz", Reflection.call(obj, "baz", expected));
    }

    @Test
    public void testNewInstanceWithNullValue() {
        String value = null;
        A a = Reflection.newInstance(A.class, value);
        Assert.assertEquals(value, a.string);
    }

    @Test
    public void testNewInstanceWithNullValues() {
        String string = "foo";
        Integer ivalue = 5;
        Long lvalue = 8L;
        Double dvalue = 3.0;
        String label = "label";

        F f;

        f = Reflection.newInstance(F.class, null, ivalue, label);
        Assert.assertNull(f.string);
        Assert.assertEquals(f.ivalue, ivalue);
        Assert.assertEquals(f.label, label);

        f = Reflection.newInstance(F.class, string, (Integer) null, label);
        Assert.assertNull(f.ivalue);
        Assert.assertEquals(f.string, string);
        // FIXME: The the F(String, Double, String) constructor was chosen
        // Assert.assertEquals(f.label, label);

        f = Reflection.newInstance(F.class, null, null, label);
        Assert.assertNull(f.string);
        Assert.assertNull(f.ivalue);
        Assert.assertNull(f.lvalue);
        Assert.assertNull(f.dvalue);
        // FIXME: The the F(String, Double, String) constructor was chosen
        // Assert.assertEquals(f.label, label);

        f = Reflection.newInstance(F.class, null, null, null);
        Assert.assertNull(f.string);
        Assert.assertNull(f.ivalue);
        Assert.assertNull(f.lvalue);
        Assert.assertNull(f.dvalue);
        Assert.assertNull(f.label);

        f = Reflection.newInstance(F.class, string, lvalue, null);
        Assert.assertEquals(string, f.string);
        Assert.assertNull(f.ivalue);
        Assert.assertEquals(lvalue, f.lvalue);
        Assert.assertNull(f.dvalue);
        Assert.assertNull(f.label);
    }

    /**
     * <strong>Goal:</strong> Verify that repeated lookups of a field return the
     * same {@link Field} object.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Look up the {@code string} field, which {@link A} declares, in
     * {@link B} twice.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both lookups return the same {@link Field}.
     */
    @Test
    public void testGetDeclaredFieldReturnsSameFieldForRepeatedLookups() {
        Field first = Reflection.getDeclaredField("string", B.class);
        Field second = Reflection.getDeclaredField("string", B.class);
        Assert.assertSame(first, second);
    }

    /**
     * <strong>Goal:</strong> Verify that repeated lookups of a method return
     * the same {@link Method} object.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Look up the {@code string(int)} method of {@link A} twice.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both lookups return the same {@link Method}.
     */
    @Test
    public void testGetMethodUnboxedReturnsSameMethodForRepeatedLookups() {
        Method first = Reflection.getMethodUnboxed(A.class, "string",
                int.class);
        Method second = Reflection.getMethodUnboxed(A.class, "string",
                int.class);
        Assert.assertSame(first, second);
    }

    /**
     * <strong>Goal:</strong> Verify that a change to the parameter type array
     * after a method lookup does not change what later lookups return.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Look up the {@code string(int)} method of {@link A} with an array
     * that holds {@code int.class}.</li>
     * <li>Replace the element of that array with {@link String
     * String.class}.</li>
     * <li>Look up {@code string(int)} again with a new array.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both lookups return the same {@link Method}.
     */
    @Test
    public void testGetMethodUnboxedReturnsSameMethodAfterCallerChangesTypes() {
        Class<?>[] types = { int.class };
        Method first = Reflection.getMethodUnboxed(A.class, "string", types);
        types[0] = String.class;
        Method second = Reflection.getMethodUnboxed(A.class, "string",
                int.class);
        Assert.assertSame(first, second);
    }

    /**
     * <strong>Goal:</strong> Verify that reading a field by name reads the
     * field that the object's own class declares when a subclass shadows a
     * field of its parent.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Read {@code name} from a {@link ShadowedParent} and from a
     * {@link ShadowingChild}, then from the {@link ShadowedParent} again.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each read returns the value of the field that
     * the object's class declares: {@code parent}, {@code child}, and then
     * {@code parent}.
     */
    @Test
    public void testGetReadsFieldThatEachClassDeclares() {
        ShadowedParent parent = new ShadowedParent();
        ShadowingChild child = new ShadowingChild();
        Assert.assertEquals("parent", Reflection.get("name", parent));
        Assert.assertEquals("child", Reflection.get("name", child));
        Assert.assertEquals("parent", Reflection.get("name", parent));
    }

    /**
     * <strong>Goal:</strong> Verify that calling an overloaded method by name
     * calls the overload that matches the type of each argument.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code describe} on an {@link Overloads} with a {@link String},
     * then with an {@link Integer}, then with a {@link String} again.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The calls return {@code string},
     * {@code integer}, and {@code string}.
     */
    @Test
    public void testCallCallsOverloadThatMatchesEachArgumentType() {
        Overloads overloads = new Overloads();
        Assert.assertEquals("string",
                Reflection.call(overloads, "describe", "a"));
        Assert.assertEquals("integer",
                Reflection.call(overloads, "describe", 1));
        Assert.assertEquals("string",
                Reflection.call(overloads, "describe", "b"));
    }

    /**
     * <strong>Goal:</strong> Verify that a private method stays inaccessible to
     * {@link Reflection#callIfAccessible(Object, String, Object...)} after
     * {@link Reflection#call(Object, String, Object...)} calls it.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call the private {@code string()} method of an {@link A} with
     * {@link Reflection#call(Object, String, Object...)}.</li>
     * <li>Call the same method with
     * {@link Reflection#callIfAccessible(Object, String, Object...)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first call returns the value; the second
     * throws a {@link RuntimeException}.
     */
    @Test
    public void testCallIfAccessibleFailsForPrivateMethodAfterCall() {
        A a = new A("foo");
        Assert.assertEquals("foo", Reflection.call(a, "string"));
        expectedException.expect(RuntimeException.class);
        Reflection.callIfAccessible(a, "string");
    }

    /**
     * <strong>Goal:</strong> Verify that reading a field that no class in the
     * hierarchy declares fails on every call.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Read the {@code missing} field from an {@link A} twice.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each read throws a {@link RuntimeException}
     * whose cause is a {@link NoSuchFieldException}.
     */
    @Test
    public void testGetThrowsForMissingFieldOnEveryCall() {
        A a = new A("foo");
        Assert.assertTrue(getCauseOfFailure(() -> Reflection.get("missing",
                a)) instanceof NoSuchFieldException);
        Assert.assertTrue(getCauseOfFailure(() -> Reflection.get("missing",
                a)) instanceof NoSuchFieldException);
    }

    /**
     * <strong>Goal:</strong> Verify that calling a method that no class in the
     * hierarchy declares fails on every call.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call the {@code missing} method on an {@link A} twice.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each call throws a {@link RuntimeException}
     * whose cause is a {@link NoSuchMethodException}.
     */
    @Test
    public void testCallThrowsForMissingMethodOnEveryCall() {
        A a = new A("foo");
        Assert.assertTrue(getCauseOfFailure(() -> Reflection.call(a,
                "missing")) instanceof NoSuchMethodException);
        Assert.assertTrue(getCauseOfFailure(() -> Reflection.call(a,
                "missing")) instanceof NoSuchMethodException);
    }

    /**
     * <strong>Goal:</strong> Verify that calling a method with an argument from
     * another {@link ClassLoader} does not keep that {@link ClassLoader} in
     * memory.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Load {@link Payload} in a new {@link ClassLoader} that does not
     * delegate to the class path.</li>
     * <li>Pass an instance of that {@link Payload} through
     * {@link Reflection#call(Object, String, Object...)} to
     * {@link ArrayList#add(Object)}, which the bootstrap loader loads, and to
     * {@code Sink#accept(Object)}, which the class path loads.</li>
     * <li>Drop all strong references to the {@link ClassLoader} and request
     * garbage collection.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The weak reference is enqueued, and its
     * referent is {@code null}.
     */
    @Test
    public void testCallDoesNotRetainClassLoaderOfArgument() throws Exception {
        ReferenceQueue<ClassLoader> queue = new ReferenceQueue<>();
        WeakReference<ClassLoader> loader = callWithArgumentFromNewLoader(
                queue, arg -> {
                    Reflection.call(new ArrayList<Object>(), "add", arg);
                    Reflection.call(new Sink(), "accept", arg);
                });
        Assert.assertTrue(isCollected(queue));
        Assert.assertNull(loader.get());
    }

    /**
     * <strong>Goal:</strong> Verify that calling a JDK method through a copy of
     * {@link Reflection} that another {@link ClassLoader} loads does not keep
     * that {@link ClassLoader} in memory.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Load {@link Reflection} and Guava in a new {@link ClassLoader} whose
     * parent cannot see them.</li>
     * <li>Call {@link ArrayList#size()} through that copy of
     * {@link Reflection#call(Object, String, Object...)}.</li>
     * <li>Drop all strong references to the {@link ClassLoader} and request
     * garbage collection.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The weak reference is enqueued, and its
     * referent is {@code null}.
     */
    @Test
    public void testCallDoesNotRetainClassLoaderOfReflection()
            throws Exception {
        ReferenceQueue<ClassLoader> queue = new ReferenceQueue<>();
        WeakReference<ClassLoader> loader = callFromNewLoader(queue, "call",
                new Class<?>[] { Object.class, String.class, Object[].class },
                new ArrayList<Object>(), "size", new Object[0]);
        Assert.assertTrue(isCollected(queue));
        Assert.assertNull(loader.get());
    }

    /**
     * <strong>Goal:</strong> Verify that calling a method with an argument
     * whose class Java can unload apart from its {@link ClassLoader} does not
     * keep that class in memory.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Define a hidden copy of {@link Payload} in the class path's
     * {@link ClassLoader}, as a VM anonymous class before Java 15.</li>
     * <li>Pass an instance through
     * {@link Reflection#call(Object, String, Object...)} to
     * {@code Sink#accept(Object)}, which the class path loads.</li>
     * <li>Drop all strong references to the hidden class and request garbage
     * collection.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The weak reference is enqueued, and its
     * referent is {@code null}.
     */
    @Test
    public void testCallDoesNotRetainHiddenClassOfArgument() throws Exception {
        ReferenceQueue<Class<?>> queue = new ReferenceQueue<>();
        WeakReference<Class<?>> type = callWithArgumentOfHiddenClass(queue,
                arg -> Reflection.call(new Sink(), "accept", arg));
        Assert.assertTrue(isCollected(queue));
        Assert.assertNull(type.get());
    }

    /**
     * <strong>Goal:</strong> Verify that creating an instance calls the
     * constructor that matches the type of each argument, in order.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Create an {@link Orders} with a {@link String} and an
     * {@link Integer}, then with an {@link Integer} and a {@link String}, then
     * with a {@link String} and an {@link Integer} again.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The instances record the constructors
     * {@code string, integer}, {@code integer, string}, and
     * {@code string, integer}.
     */
    @Test
    public void testNewInstanceCallsConstructorThatMatchesEachArgumentType() {
        Orders orders;
        orders = Reflection.newInstance(Orders.class, "a", 1);
        Assert.assertEquals("string, integer", orders.constructor);
        orders = Reflection.newInstance(Orders.class, 1, "a");
        Assert.assertEquals("integer, string", orders.constructor);
        orders = Reflection.newInstance(Orders.class, "b", 2);
        Assert.assertEquals("string, integer", orders.constructor);
    }

    /**
     * <strong>Goal:</strong> Verify that creating an instance with {@code null}
     * arguments calls the constructor that matches the type of each non-null
     * argument at its position.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Create an {@link Orders} with a {@link String} and {@code null},
     * then with {@code null} and a {@link String}, then with a {@link String}
     * and {@code null} again.</li>
     * <li>Create an {@link Orders} with one {@code null} argument.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The instances record the constructors
     * {@code string, integer}, {@code integer, string},
     * {@code string, integer}, and {@code integer}.
     */
    @Test
    public void testNewInstanceCallsConstructorThatMatchesEachNullArgument() {
        Orders orders;
        orders = Reflection.newInstance(Orders.class, "a", null);
        Assert.assertEquals("string, integer", orders.constructor);
        orders = Reflection.newInstance(Orders.class, null, "a");
        Assert.assertEquals("integer, string", orders.constructor);
        orders = Reflection.newInstance(Orders.class, "b", null);
        Assert.assertEquals("string, integer", orders.constructor);
        orders = Reflection.newInstance(Orders.class, (Object) null);
        Assert.assertEquals("integer", orders.constructor);
    }

    /**
     * <strong>Goal:</strong> Verify that creating an instance with arguments
     * that no constructor accepts fails on every call.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Create an {@link Orders} with two {@link String} arguments
     * twice.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each call throws a {@link RuntimeException}
     * whose cause is a {@link NoSuchMethodException}.
     */
    @Test
    public void testNewInstanceThrowsWhenNoConstructorMatchesOnEveryCall() {
        Assert.assertTrue(getCauseOfFailure(() -> Reflection.newInstance(
                Orders.class, "a", "b")) instanceof NoSuchMethodException);
        Assert.assertTrue(getCauseOfFailure(() -> Reflection.newInstance(
                Orders.class, "a", "b")) instanceof NoSuchMethodException);
    }

    /**
     * <strong>Goal:</strong> Verify that creating an instance with an argument
     * from another {@link ClassLoader} does not keep that {@link ClassLoader}
     * in memory.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Load {@link Payload} in a new {@link ClassLoader} that does not
     * delegate to the class path.</li>
     * <li>Pass an instance of that {@link Payload} through
     * {@link Reflection#newInstance(Class, Object...)} to the
     * {@link AtomicReference} constructor, which the bootstrap loader loads,
     * and to the {@code Sink(Object)} constructor, which the class path
     * loads.</li>
     * <li>Drop all strong references to the {@link ClassLoader} and request
     * garbage collection.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The weak reference is enqueued, and its
     * referent is {@code null}.
     */
    @Test
    public void testNewInstanceDoesNotRetainClassLoaderOfArgument()
            throws Exception {
        ReferenceQueue<ClassLoader> queue = new ReferenceQueue<>();
        WeakReference<ClassLoader> loader = callWithArgumentFromNewLoader(
                queue, arg -> {
                    Reflection.newInstance(AtomicReference.class, arg);
                    Reflection.newInstance(Sink.class, arg);
                });
        Assert.assertTrue(isCollected(queue));
        Assert.assertNull(loader.get());
    }

    /**
     * <strong>Goal:</strong> Verify that creating a JDK instance through a copy
     * of {@link Reflection} that another {@link ClassLoader} loads does not
     * keep that {@link ClassLoader} in memory.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Load {@link Reflection} and Guava in a new {@link ClassLoader} whose
     * parent cannot see them.</li>
     * <li>Create an {@link ArrayList} through that copy of
     * {@link Reflection#newInstance(Class, Object...)}.</li>
     * <li>Drop all strong references to the {@link ClassLoader} and request
     * garbage collection.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The weak reference is enqueued, and its
     * referent is {@code null}.
     */
    @Test
    public void testNewInstanceDoesNotRetainClassLoaderOfReflection()
            throws Exception {
        ReferenceQueue<ClassLoader> queue = new ReferenceQueue<>();
        WeakReference<ClassLoader> loader = callFromNewLoader(queue,
                "newInstance", new Class<?>[] { Class.class, Object[].class },
                ArrayList.class, new Object[0]);
        Assert.assertTrue(isCollected(queue));
        Assert.assertNull(loader.get());
    }

    /**
     * <strong>Goal:</strong> Verify that creating an instance with an argument
     * whose class Java can unload apart from its {@link ClassLoader} does not
     * keep that class in memory.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Define a hidden copy of {@link Payload} in the class path's
     * {@link ClassLoader}, as a VM anonymous class before Java 15.</li>
     * <li>Pass an instance through
     * {@link Reflection#newInstance(Class, Object...)} to the
     * {@code Sink(Object)} constructor, which the class path loads.</li>
     * <li>Drop all strong references to the hidden class and request garbage
     * collection.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The weak reference is enqueued, and its
     * referent is {@code null}.
     */
    @Test
    public void testNewInstanceDoesNotRetainHiddenClassOfArgument()
            throws Exception {
        ReferenceQueue<Class<?>> queue = new ReferenceQueue<>();
        WeakReference<Class<?>> type = callWithArgumentOfHiddenClass(queue,
                arg -> Reflection.newInstance(Sink.class, arg));
        Assert.assertTrue(isCollected(queue));
        Assert.assertNull(type.get());
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
        protected Map<Integer, Integer> mapIntegerToInteger;
        private List<AtomicReference<Integer>> listAtomicReference;

    }

    public class Foo {

        public void noArgs() {}

        public void objectArg(Object arg) {}

        public void superClassArg(Number arg) {}

        public void multipleArgs(String arg1, Foo arg2) {}

        public void varArgs(String... args) {}

        public void varArgs2(String arg, String... args) {}

        public void reproA(char c, CaseFormat... formats) {}

        public void overloadVarArgs() {}

        public void overloadVarArgs(String... args) {}

        public void verA(String arg0, long arg) {}

        public void verA(String arg0, Long arg) {}

    }

    public class GenericBase {

        public <T> void put(String key, T value) {

        }
    }

    public class GenericChild extends GenericBase {

        @Override
        public <T> void put(String key, T value) {

        }
    }

    class ClassA implements InterfaceA, InterfaceB {

        @Override
        public String baz(String value) {
            return value;
        }

    }

    class ClassB extends ClassA {

        @Override
        public String baz(String value) {
            return "baz";
        }
    }

    interface InterfaceA {

        public String baz(String value);

        public default String foo(String value) {
            return "foo_" + value;
        }

    }

    interface InterfaceB {
        public default String bar(String value) {
            return "bar_" + value;
        }

        public String baz(String value);
    }

    public static class F {

        String string;
        Integer ivalue;
        Long lvalue;
        String label;
        Double dvalue;
        String tag;

        public F(String string, Integer value, String label) {
            this.string = string;
            this.ivalue = value;
            this.label = label;
        }

        public F(String string, Long value, String label) {
            this.string = string;
            this.lvalue = value;
            this.label = label;
        }

        public F(Double value, String string, String label) {
            this.dvalue = value;
            this.string = string;
            this.label = label;
        }

        public F(String string, Double value, String tag) {
            this.string = string;
            this.dvalue = value;
            this.tag = tag;
        }
    }

    /**
     * A class whose {@code name} field a subclass shadows.
     *
     * @author Jeff Nelson
     */
    private static class ShadowedParent {

        /**
         * The name, which {@link ShadowingChild} shadows.
         */
        private final String name = "parent";
    }

    /**
     * A class that declares a field with the same name as a field of its
     * parent.
     *
     * @author Jeff Nelson
     */
    private static class ShadowingChild extends ShadowedParent {

        /**
         * The name, which shadows the field of {@link ShadowedParent}.
         */
        private final String name = "child";
    }

    /**
     * A class with an overloaded method whose overloads take one argument of
     * different types.
     *
     * @author Jeff Nelson
     */
    private static class Overloads {

        /**
         * Describe a {@link String} argument.
         *
         * @param value the argument
         * @return {@code string}
         */
        public String describe(String value) {
            return "string";
        }

        /**
         * Describe an {@link Integer} argument.
         *
         * @param value the argument
         * @return {@code integer}
         */
        public String describe(Integer value) {
            return "integer";
        }
    }

    /**
     * A class with constructors that accept a {@link String} and an
     * {@link Integer} in either order, or an {@link Integer} alone, and record
     * which one created the instance.
     *
     * @author Jeff Nelson
     */
    private static class Orders {

        /**
         * The parameter types of the constructor that created this instance.
         */
        private final String constructor;

        /**
         * Create an {@link Orders} that records {@code string, integer}.
         *
         * @param string the first argument
         * @param integer the second argument
         */
        Orders(String string, Integer integer) {
            this.constructor = "string, integer";
        }

        /**
         * Create an {@link Orders} that records {@code integer, string}.
         *
         * @param integer the first argument
         * @param string the second argument
         */
        Orders(Integer integer, String string) {
            this.constructor = "integer, string";
        }

        /**
         * Create an {@link Orders} that records {@code integer}.
         *
         * @param integer the argument
         */
        Orders(Integer integer) {
            this.constructor = "integer";
        }
    }

    /**
     * A class that a test loads in its own {@link ClassLoader} to pass as an
     * argument whose {@link ClassLoader} the class path cannot reach.
     *
     * @author Jeff Nelson
     */
    public static class Payload {}

    /**
     * A class that the class path loads, with a method that accepts any
     * argument.
     *
     * @author Jeff Nelson
     */
    private static class Sink {

        /**
         * Create a {@link Sink}.
         */
        Sink() {}

        /**
         * Create a {@link Sink} and do nothing with {@code value}.
         *
         * @param value the argument
         */
        Sink(Object value) {}

        /**
         * Accept {@code value} and do nothing with it.
         *
         * @param value the argument
         */
        public void accept(Object value) {}
    }

}
