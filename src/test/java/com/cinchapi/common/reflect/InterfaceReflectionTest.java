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

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the interface reflection methods in the {@link Reflection}
 * utility class.
 *
 * @author Jeff Nelson
 */
public class InterfaceReflectionTest {

    public interface BaseInterface {
        default String baseDefaultMethod() {
            return "base_default";
        }

        default String overridableMethod() {
            return "base_overridable";
        }

        String abstractMethod();
    }

    public interface ExtendedInterface extends BaseInterface {
        default String extendedDefaultMethod() {
            return "extended_default";
        }

        default String overridableMethod() {
            return "extended_overridable";
        }
    }

    public interface SiblingInterface {
        default String siblingDefaultMethod() {
            return "sibling_default";
        }

        default String overridableMethod() {
            return "sibling_overridable";
        }
    }

    public interface DeepInterface extends ExtendedInterface {
        default String deepDefaultMethod() {
            return "deep_default";
        }
    }

    class ParentClass implements BaseInterface {
        @Override
        public String abstractMethod() {
            return "parent_abstract";
        }

        @Override
        public String overridableMethod() {
            return "parent_overridable";
        }
    }

    class ChildClass extends ParentClass implements ExtendedInterface, SiblingInterface {
        // Inherits overridableMethod from ParentClass
        // Implements abstractMethod from ParentClass
    }

    class GrandChildClass extends ChildClass implements DeepInterface {
        // Inherits everything from ChildClass
        // Gets deepDefaultMethod from DeepInterface
    }

    class OverrideTestClass implements BaseInterface {
        @Override
        public String abstractMethod() {
            return "override_abstract";
        }

        @Override
        public String overridableMethod() {
            return "override_overridable";
        }

        public String classMethod() {
            return "class_method";
        }
    }

    class NoOverrideClass implements BaseInterface {
        @Override
        public String abstractMethod() {
            return "no_override_abstract";
        }

        // Does not override overridableMethod, so it inherits the default
    }

    class SimpleClass {
        // No interfaces implemented
    }

    /**
     * An interface whose default methods read the state of their target, take
     * arguments, and throw the exceptions they receive.
     */
    public interface Greeter {

        /**
         * Return the name of this {@link Greeter}.
         *
         * @return the name
         */
        String name();

        /**
         * Return {@code greeting} followed by the {@link #name()} of this
         * {@link Greeter}.
         *
         * @param greeting the greeting
         * @return the greeting and the name, separated by a comma
         */
        default String greet(String greeting) {
            return greeting + ", " + name();
        }

        /**
         * Return {@code greeting} followed by the {@link #name()} of this
         * {@link Greeter} and then {@code punctuation}.
         *
         * @param greeting the greeting
         * @param punctuation the text to append
         * @return the greeting and the name, separated by a comma, followed by
         *         the punctuation
         */
        default String greet(String greeting, String punctuation) {
            return greet(greeting) + punctuation;
        }

        /**
         * Throw {@code exception}.
         *
         * @param exception the exception to throw
         * @return nothing, because the method always throws
         * @throws Exception always, {@code exception} itself
         */
        default String fail(Exception exception) throws Exception {
            throw exception;
        }
    }

    /**
     * A {@link Greeter} with a fixed name.
     */
    class Person implements Greeter {

        /**
         * The name that {@link #name()} returns.
         */
        private final String name;

        /**
         * Construct a new instance.
         *
         * @param name the name of this {@link Person}
         */
        Person(String name) {
            this.name = name;
        }

        @Override
        public String name() {
            return name;
        }
    }

    @Test
    public void testGetImplementedInterfaces() {
        // Test that getImplementedInterfaces returns interfaces directly implemented by the class
        // including interfaces extended by those directly implemented interfaces
        Set<Class<?>> childInterfaces = Reflection.getImplementedInterfaces(ChildClass.class);
        Assert.assertTrue(childInterfaces.contains(ExtendedInterface.class));
        Assert.assertTrue(childInterfaces.contains(SiblingInterface.class));
        Assert.assertTrue(childInterfaces.contains(BaseInterface.class)); // Extended by ExtendedInterface

        // Test that it doesn't include interfaces from superclasses
        Set<Class<?>> grandChildInterfaces = Reflection.getImplementedInterfaces(GrandChildClass.class);
        Assert.assertTrue(grandChildInterfaces.contains(DeepInterface.class));
        Assert.assertTrue(grandChildInterfaces.contains(ExtendedInterface.class)); // Extended by DeepInterface
        Assert.assertTrue(grandChildInterfaces.contains(BaseInterface.class)); // Extended by ExtendedInterface
        // Should NOT include SiblingInterface (from ChildClass superclass)
        Assert.assertFalse(grandChildInterfaces.contains(SiblingInterface.class));
    }

    @Test
    public void testGetAllImplementedInterfaces() {
        // Test that getAllImplementedInterfaces traverses the entire class hierarchy
        Set<Class<?>> grandChildAllInterfaces = Reflection.getAllImplementedInterfaces(GrandChildClass.class);

        // Should include interfaces from GrandChildClass
        Assert.assertTrue(grandChildAllInterfaces.contains(DeepInterface.class));
        Assert.assertTrue(grandChildAllInterfaces.contains(ExtendedInterface.class));
        Assert.assertTrue(grandChildAllInterfaces.contains(BaseInterface.class));

        // Should also include interfaces from ChildClass (superclass)
        Assert.assertTrue(grandChildAllInterfaces.contains(SiblingInterface.class));

        // Should also include interfaces from ParentClass (via ChildClass)
        Assert.assertTrue(grandChildAllInterfaces.contains(BaseInterface.class));
    }

    @Test
    public void testGetImplementedInterfacesObject() {
        ChildClass child = new ChildClass();
        Set<Class<?>> interfaces = Reflection.getImplementedInterfaces(child);

        Assert.assertTrue(interfaces.contains(ExtendedInterface.class));
        Assert.assertTrue(interfaces.contains(SiblingInterface.class));
        Assert.assertTrue(interfaces.contains(BaseInterface.class));
    }

    @Test
    public void testGetAllImplementedInterfacesObject() {
        GrandChildClass grandChild = new GrandChildClass();
        Set<Class<?>> allInterfaces = Reflection.getAllImplementedInterfaces(grandChild);

        Assert.assertTrue(allInterfaces.contains(DeepInterface.class));
        Assert.assertTrue(allInterfaces.contains(ExtendedInterface.class));
        Assert.assertTrue(allInterfaces.contains(BaseInterface.class));
        Assert.assertTrue(allInterfaces.contains(SiblingInterface.class));
    }

    @Test
    public void testGetAllDefaultInterfaceMethods() {
        Method[] defaultMethods = Reflection.getAllDefaultInterfaceMethods(ChildClass.class);

        // Should include default methods from all interfaces in the hierarchy
        Set<String> methodNames = new HashSet<>();
        for (Method method : defaultMethods) {
            methodNames.add(method.getName());
        }

        Assert.assertTrue(methodNames.contains("baseDefaultMethod"));
        Assert.assertTrue(methodNames.contains("extendedDefaultMethod"));
        Assert.assertTrue(methodNames.contains("siblingDefaultMethod"));
        Assert.assertTrue(methodNames.contains("overridableMethod")); // From SiblingInterface
    }

    @Test
    public void testGetAllDefaultInterfaceMethodsObject() {
        ChildClass child = new ChildClass();
        Method[] defaultMethods = Reflection.getAllDefaultInterfaceMethods(child);

        Set<String> methodNames = new HashSet<>();
        for (Method method : defaultMethods) {
            methodNames.add(method.getName());
        }

        Assert.assertTrue(methodNames.contains("baseDefaultMethod"));
        Assert.assertTrue(methodNames.contains("extendedDefaultMethod"));
        Assert.assertTrue(methodNames.contains("siblingDefaultMethod"));
    }

    @Test
    public void testGetAllNonOverriddenDefaultInterfaceMethods() {
        // Test with class that overrides some default methods
        Method[] nonOverriddenMethods = Reflection.getAllNonOverriddenDefaultInterfaceMethods(OverrideTestClass.class);

        Set<String> methodNames = new HashSet<>();
        for (Method method : nonOverriddenMethods) {
            methodNames.add(method.getName());
        }

        // Should include baseDefaultMethod (not overridden)
        Assert.assertTrue(methodNames.contains("baseDefaultMethod"));

        // Should NOT include overridableMethod (overridden by the class)
        Assert.assertFalse(methodNames.contains("overridableMethod"));
    }

    @Test
    public void testGetAllNonOverriddenDefaultInterfaceMethodsNoOverride() {
        // Test with class that doesn't override default methods
        Method[] nonOverriddenMethods = Reflection.getAllNonOverriddenDefaultInterfaceMethods(NoOverrideClass.class);

        Set<String> methodNames = new HashSet<>();
        for (Method method : nonOverriddenMethods) {
            methodNames.add(method.getName());
        }

        // Should include both default methods since neither is overridden
        Assert.assertTrue(methodNames.contains("baseDefaultMethod"));
        Assert.assertTrue(methodNames.contains("overridableMethod"));
    }

    @Test
    public void testGetAllNonOverriddenDefaultInterfaceMethodsObject() {
        OverrideTestClass obj = new OverrideTestClass();
        Method[] nonOverriddenMethods = Reflection.getAllNonOverriddenDefaultInterfaceMethods(obj);

        Set<String> methodNames = new HashSet<>();
        for (Method method : nonOverriddenMethods) {
            methodNames.add(method.getName());
        }

        Assert.assertTrue(methodNames.contains("baseDefaultMethod"));
        Assert.assertFalse(methodNames.contains("overridableMethod"));
    }

    @Test
    public void testInterfaceHierarchyWithMultipleInheritance() {
        // Test complex interface hierarchy
        Set<Class<?>> grandChildInterfaces = Reflection.getImplementedInterfaces(GrandChildClass.class);

        // Should include only interfaces directly implemented by GrandChildClass
        Assert.assertTrue(grandChildInterfaces.contains(DeepInterface.class));
        Assert.assertTrue(grandChildInterfaces.contains(ExtendedInterface.class));
        Assert.assertTrue(grandChildInterfaces.contains(BaseInterface.class));
        // Should NOT include SiblingInterface (from ChildClass superclass)
        Assert.assertFalse(grandChildInterfaces.contains(SiblingInterface.class));

        // Test that getAllImplementedInterfaces includes interfaces from parent classes
        Set<Class<?>> allInterfaces = Reflection.getAllImplementedInterfaces(GrandChildClass.class);
        // Should include SiblingInterface from ChildClass superclass
        Assert.assertTrue(allInterfaces.contains(SiblingInterface.class));
        Assert.assertNotEquals(grandChildInterfaces, allInterfaces); // They should be different
    }

    @Test
    public void testDefaultMethodInheritanceChain() {
        // Test that default methods are properly inherited through the interface chain
        Method[] defaultMethods = Reflection.getAllDefaultInterfaceMethods(GrandChildClass.class);

        Set<String> methodNames = new HashSet<>();
        for (Method method : defaultMethods) {
            methodNames.add(method.getName());
        }

        // Should include default methods from all interfaces in the hierarchy
        Assert.assertTrue(methodNames.contains("baseDefaultMethod")); // From BaseInterface
        Assert.assertTrue(methodNames.contains("extendedDefaultMethod")); // From ExtendedInterface
        Assert.assertTrue(methodNames.contains("deepDefaultMethod")); // From DeepInterface
        Assert.assertTrue(methodNames.contains("siblingDefaultMethod")); // From SiblingInterface
    }

    @Test
    public void testMethodOverrideDetection() {
        // Test that the method correctly identifies overridden vs non-overridden methods
        Method[] nonOverriddenMethods = Reflection.getAllNonOverriddenDefaultInterfaceMethods(ChildClass.class);

        Set<String> methodNames = new HashSet<>();
        for (Method method : nonOverriddenMethods) {
            methodNames.add(method.getName());
        }

        // ChildClass inherits overridableMethod from ParentClass, so it should not be available as a default method
        Assert.assertFalse(methodNames.contains("overridableMethod"));

        // Other default methods should still be available
        Assert.assertTrue(methodNames.contains("baseDefaultMethod"));
        Assert.assertTrue(methodNames.contains("extendedDefaultMethod"));
        Assert.assertTrue(methodNames.contains("siblingDefaultMethod"));
    }

    @Test
    public void testEmptyInterfaceSet() {
        // Test with a class that doesn't implement any interfaces
        Set<Class<?>> interfaces = Reflection.getImplementedInterfaces(SimpleClass.class);
        Assert.assertTrue(interfaces.isEmpty());

        Set<Class<?>> allInterfaces = Reflection.getAllImplementedInterfaces(SimpleClass.class);
        Assert.assertTrue(allInterfaces.isEmpty());
    }

    @Test
    public void testNoDefaultMethods() {
        // Test with a class that implements interfaces but has no default methods
        Method[] defaultMethods = Reflection.getAllDefaultInterfaceMethods(SimpleClass.class);
        Assert.assertEquals(0, defaultMethods.length);

        Method[] nonOverriddenMethods = Reflection.getAllNonOverriddenDefaultInterfaceMethods(SimpleClass.class);
        Assert.assertEquals(0, nonOverriddenMethods.length);
    }

    @Test
    public void testDifferenceBetweenGetImplementedAndGetAllImplemented() {
        // This test clearly demonstrates the difference between the two methods

        // getImplementedInterfaces should only return interfaces directly implemented by GrandChildClass
        Set<Class<?>> directInterfaces = Reflection.getImplementedInterfaces(GrandChildClass.class);
        Assert.assertTrue(directInterfaces.contains(DeepInterface.class));
        Assert.assertFalse(directInterfaces.contains(SiblingInterface.class)); // From ChildClass superclass

        // getAllImplementedInterfaces should return interfaces from the entire class hierarchy
        Set<Class<?>> allInterfaces = Reflection.getAllImplementedInterfaces(GrandChildClass.class);
        Assert.assertTrue(allInterfaces.contains(DeepInterface.class));
        Assert.assertTrue(allInterfaces.contains(SiblingInterface.class)); // From ChildClass superclass

        // The sets should be different
        Assert.assertNotEquals(directInterfaces, allInterfaces);
        Assert.assertTrue(allInterfaces.size() > directInterfaces.size());
    }

    @Test
    public void testInvokeDefaultInterfaceMethod() throws Exception {
        // Test basic invocation of a default interface method
        NoOverrideClass obj = new NoOverrideClass();
        Method method = BaseInterface.class.getDeclaredMethod("baseDefaultMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, method);
        Assert.assertEquals("base_default", result);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodWithParameters() throws Exception {
        // Test invocation of a default interface method with parameters
        NoOverrideClass obj = new NoOverrideClass();
        Method method = BaseInterface.class.getDeclaredMethod("overridableMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, method);
        Assert.assertEquals("base_overridable", result);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodFromExtendedInterface() throws Exception {
        // Test invocation of a default method from an extended interface
        ChildClass obj = new ChildClass();
        Method method = ExtendedInterface.class.getDeclaredMethod("extendedDefaultMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, method);
        Assert.assertEquals("extended_default", result);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodFromDeepInterface() throws Exception {
        // Test invocation of a default method from a deeply extended interface
        GrandChildClass obj = new GrandChildClass();
        Method method = DeepInterface.class.getDeclaredMethod("deepDefaultMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, method);
        Assert.assertEquals("deep_default", result);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodWithMultipleInterfaces() throws Exception {
        // Test invocation when object implements multiple interfaces with default methods
        ChildClass obj = new ChildClass();
        Method siblingMethod = SiblingInterface.class.getDeclaredMethod("siblingDefaultMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, siblingMethod);
        Assert.assertEquals("sibling_default", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeDefaultInterfaceMethodWithNonDefaultMethod() throws Exception {
        // Test that invoking a non-default method throws an exception
        NoOverrideClass obj = new NoOverrideClass();
        Method method = NoOverrideClass.class.getDeclaredMethod("abstractMethod");

        Reflection.invokeDefaultInterfaceMethod(obj, method);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeDefaultInterfaceMethodWithNonInterfaceMethod() throws Exception {
        // Test that invoking a method from a class (not interface) throws an exception
        NoOverrideClass obj = new NoOverrideClass();
        Method method = NoOverrideClass.class.getDeclaredMethod("abstractMethod");

        Reflection.invokeDefaultInterfaceMethod(obj, method);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvokeDefaultInterfaceMethodWithIncompatibleTarget() throws Exception {
        // Test that invoking with a target that doesn't implement the interface throws an exception
        SimpleClass obj = new SimpleClass(); // Doesn't implement BaseInterface
        Method method = BaseInterface.class.getDeclaredMethod("baseDefaultMethod");

        Reflection.invokeDefaultInterfaceMethod(obj, method);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodWithOverriddenMethod() throws Exception {
        // Test that invoking an overridden default method still works
        // The method should invoke the interface's default implementation, not the class override
        OverrideTestClass obj = new OverrideTestClass();
        Method method = BaseInterface.class.getDeclaredMethod("overridableMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, method);
        Assert.assertEquals("base_overridable", result);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodWithInheritedOverride() throws Exception {
        // Test that invoking a default method that was overridden by a superclass still works
        ChildClass obj = new ChildClass();
        Method method = BaseInterface.class.getDeclaredMethod("overridableMethod");

        Object result = Reflection.invokeDefaultInterfaceMethod(obj, method);
        Assert.assertEquals("base_overridable", result);
    }

    @Test
    public void testInvokeDefaultInterfaceMethodWithComplexInheritance() throws Exception {
        // Test invocation in a complex inheritance scenario
        GrandChildClass obj = new GrandChildClass();

        // Test method from base interface
        Method baseMethod = BaseInterface.class.getDeclaredMethod("baseDefaultMethod");
        Object baseResult = Reflection.invokeDefaultInterfaceMethod(obj, baseMethod);
        Assert.assertEquals("base_default", baseResult);

        // Test method from extended interface
        Method extendedMethod = ExtendedInterface.class.getDeclaredMethod("extendedDefaultMethod");
        Object extendedResult = Reflection.invokeDefaultInterfaceMethod(obj, extendedMethod);
        Assert.assertEquals("extended_default", extendedResult);

        // Test method from deep interface
        Method deepMethod = DeepInterface.class.getDeclaredMethod("deepDefaultMethod");
        Object deepResult = Reflection.invokeDefaultInterfaceMethod(obj, deepMethod);
        Assert.assertEquals("deep_default", deepResult);

        // Test method from sibling interface
        Method siblingMethod = SiblingInterface.class.getDeclaredMethod("siblingDefaultMethod");
        Object siblingResult = Reflection.invokeDefaultInterfaceMethod(obj, siblingMethod);
        Assert.assertEquals("sibling_default", siblingResult);
    }

    /**
     * <strong>Goal:</strong> Verify that repeated invocations of one default
     * method each run on their own target with their own arguments.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Invoke {@code Greeter.greet} on a {@link Person} named {@code Ada}
     * with {@code Hello}.</li>
     * <li>Invoke it on a {@link Person} named {@code Grace} with
     * {@code Hi}.</li>
     * <li>Invoke it on the first {@link Person} again with {@code Hey}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The invocations return {@code Hello, Ada},
     * {@code Hi, Grace}, and {@code Hey, Ada}.
     */
    @Test
    public void testInvokeDefaultInterfaceMethodRunsOnEachTargetWithItsArguments()
            throws Exception {
        Method greet = Greeter.class.getDeclaredMethod("greet", String.class);
        Person ada = new Person("Ada");
        Person grace = new Person("Grace");
        Assert.assertEquals("Hello, Ada",
                Reflection.invokeDefaultInterfaceMethod(ada, greet, "Hello"));
        Assert.assertEquals("Hi, Grace",
                Reflection.invokeDefaultInterfaceMethod(grace, greet, "Hi"));
        Assert.assertEquals("Hey, Ada",
                Reflection.invokeDefaultInterfaceMethod(ada, greet, "Hey"));
    }

    /**
     * <strong>Goal:</strong> Verify that invoking default methods with the same
     * name from different interfaces runs the body that each interface
     * declares.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Invoke {@code overridableMethod} of {@link ExtendedInterface}, then
     * of {@link SiblingInterface}, then of {@link BaseInterface}, on one
     * {@link ChildClass}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The invocations return
     * {@code extended_overridable}, {@code sibling_overridable}, and
     * {@code base_overridable}.
     */
    @Test
    public void testInvokeDefaultInterfaceMethodRunsBodyOfEachDeclaringInterface()
            throws Exception {
        ChildClass obj = new ChildClass();
        Method extended = ExtendedInterface.class
                .getDeclaredMethod("overridableMethod");
        Method sibling = SiblingInterface.class
                .getDeclaredMethod("overridableMethod");
        Method base = BaseInterface.class
                .getDeclaredMethod("overridableMethod");
        Assert.assertEquals("extended_overridable",
                Reflection.invokeDefaultInterfaceMethod(obj, extended));
        Assert.assertEquals("sibling_overridable",
                Reflection.invokeDefaultInterfaceMethod(obj, sibling));
        Assert.assertEquals("base_overridable",
                Reflection.invokeDefaultInterfaceMethod(obj, base));
    }

    /**
     * <strong>Goal:</strong> Verify that invoking overloads of one default
     * method runs the body of each overload.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Invoke {@code Greeter.greet(String)} on a {@link Person} named
     * {@code Ada} with {@code Hello}.</li>
     * <li>Invoke {@code Greeter.greet(String, String)} on the same
     * {@link Person} with {@code Hello} and {@code !}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The invocations return {@code Hello, Ada} and
     * {@code Hello, Ada!}.
     */
    @Test
    public void testInvokeDefaultInterfaceMethodRunsBodyOfEachOverload()
            throws Exception {
        Method one = Greeter.class.getDeclaredMethod("greet", String.class);
        Method two = Greeter.class.getDeclaredMethod("greet", String.class,
                String.class);
        Person ada = new Person("Ada");
        Assert.assertEquals("Hello, Ada",
                Reflection.invokeDefaultInterfaceMethod(ada, one, "Hello"));
        Assert.assertEquals("Hello, Ada!",
                Reflection.invokeDefaultInterfaceMethod(ada, two, "Hello",
                        "!"));
    }

    /**
     * <strong>Goal:</strong> Verify that an unchecked exception that a default
     * method throws reaches the caller unchanged.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Invoke {@code Greeter.fail} on a {@link Person} with an
     * {@link IllegalStateException}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The invocation throws that same
     * {@link IllegalStateException}.
     */
    @Test
    public void testInvokeDefaultInterfaceMethodThrowsUncheckedExceptionOfMethod()
            throws Exception {
        Method fail = Greeter.class.getDeclaredMethod("fail", Exception.class);
        IllegalStateException expected = new IllegalStateException();
        try {
            Reflection.invokeDefaultInterfaceMethod(new Person("Ada"), fail,
                    expected);
            Assert.fail("Expected an IllegalStateException");
        }
        catch (IllegalStateException e) {
            Assert.assertSame(expected, e);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that a checked exception that a default
     * method throws reaches the caller as the cause of a
     * {@link RuntimeException}.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Invoke {@code Greeter.fail} on a {@link Person} with an
     * {@link IOException}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The invocation throws a
     * {@link RuntimeException} whose cause is that same {@link IOException}.
     */
    @Test
    public void testInvokeDefaultInterfaceMethodWrapsCheckedExceptionOfMethod()
            throws Exception {
        Method fail = Greeter.class.getDeclaredMethod("fail", Exception.class);
        IOException expected = new IOException();
        try {
            Reflection.invokeDefaultInterfaceMethod(new Person("Ada"), fail,
                    expected);
            Assert.fail("Expected a RuntimeException");
        }
        catch (RuntimeException e) {
            Assert.assertSame(expected, e.getCause());
        }
    }

}
