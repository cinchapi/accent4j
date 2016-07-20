/*
 * Copyright (c) 2013-2015 Cinchapi Inc.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import javax.annotation.Nullable;

import com.cinchapi.common.base.CheckedExceptions;

/**
 * A collection of tools for using reflection to access or modify objects.
 * <strong>Use with caution.</strong>
 * <p>
 * <em>This class helps you do some naughty things. Think carefully about whether
 * you should be using reflection in your application (its usage is usually a
 * sign of broken design, but there are some legitimate cases where its
 * necessary).</em>
 * </p>
 * <p>
 * Warning aside, if you're going to do reflection anyway, these functions
 * abstract away a lot of the bolierplate. They allow you to access variables,
 * constructors and methods no matter what visibility they are declared with and
 * no matter how far they are declared in the object hirearchy.
 * </p>
 * <h2>Known Limitations</h2>
 * <p>
 * <ul>
 * <li>Calling methods and constructors that take a mix of primitive types and
 * wrapper types does not work. Methods should be declared to take all of one or
 * the other.</li>
 * </ul>
 * </p>
 * 
 * @author Jeff Nelson
 */
public final class Reflection {

    /**
     * Return a {@link Callable} that can execute
     * {@link #call(Object, String, Object...)} asynchronously.
     * 
     * @param obj the object on which to call the method
     * @param methodName the name of the method to call
     * @param args the method parameters
     * @return a {@link Callable} that can be used for asynchronous execution
     */
    public static <T> Callable<T> callable(final Object obj,
            final String methodName, final Object... args) {
        return new Callable<T>() {

            @Override
            public T call() throws Exception {
                return Reflection.call(obj, methodName, args);
            }

        };
    }

    /**
     * Call the method named {@code methodName} on the specified {@code obj}
     * instance and provide all the {@code args} as parameters.
     * 
     * @param obj the object on which to call the method
     * @param methodName the name of the method to call
     * @param args the method parameters
     * @return the result of the method call
     */
    @SuppressWarnings("unchecked")
    public static <T> T call(Object obj, String methodName, Object... args) {
        try {
            Class<?> clazz = obj.getClass();
            Class<?>[] parameterTypes = new Class<?>[args.length];
            Class<?>[] altParameterTypes = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                parameterTypes[i] = args[i].getClass();
                altParameterTypes[i] = unbox(args[i].getClass());
            }
            Method method = null;
            while (clazz != null && method == null) {
                try {
                    method = clazz
                            .getDeclaredMethod(methodName, parameterTypes);
                }
                catch (NoSuchMethodException e) {
                    try {
                        // Attempt to find a method using the alt param types.
                        // This will usually bear fruit in cases where a method
                        // has a primitive type parameter and Java autoboxing
                        // causes the passed in parameters to have a wrapper
                        // type instead of the appropriate primitive type.
                        method = clazz.getDeclaredMethod(methodName,
                                altParameterTypes);
                    }
                    catch (NoSuchMethodException e2) {
                        clazz = clazz.getSuperclass();
                    }
                }
            }
            if(method != null) {
                method.setAccessible(true);
                return (T) method.invoke(obj, args);
            }
            else {
                throw new NoSuchMethodException("No method named " + methodName
                        + " that takes " + Arrays.toString(args) + " exists");
            }
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Retrieve the value of {@code variable} from the {@code obj}.
     * <p>
     * This is useful in situations when it is necessary to access an instance
     * variable that is not visible (i.e. when hacking the internals of a 3rd
     * party library).
     * </p>
     * 
     * @param variable the name of the variable to retrieve
     * @param obj the object from which to retrieve the value
     * @return the value of the {@code variable} on {@code obj}, if it exists;
     *         otherwise {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T get(String variable, Object obj) {
        try {
            Field field = getField(variable, obj);
            return (T) field.get(obj);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getStatic(String variable, Class<?> clazz) {
        try {
            Field field = getField(variable, clazz, null);
            return (T) field.get(null);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Given an object, return an array containing all the {@link Field} objects
     * that represent those declared within {@code obj's} entire class hierarchy
     * after the base {@link Object}.
     * 
     * @return the array of declared fields
     */
    public static Field[] getAllDeclaredFields(Object obj) {
        List<Field> fields = new ArrayList<Field>();
        Class<?> clazz = obj.getClass();
        while (clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if(!field.getName().equalsIgnoreCase("fields0")
                        && !field.isSynthetic()
                        && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    fields.add(field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        return fields.toArray(new Field[] {});
    }

    /**
     * Given a {@link Class}, create a new instance by calling the appropriate
     * constructor for the given {@code args}.
     * 
     * @param clazz the type of instance to construct
     * @param args the parameters to pass to the constructor
     * @return the new instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(Class<? extends T> clazz, Object... args) {
        try {
            Constructor<? extends T> toCall = null;
            outer: for (Constructor<?> constructor : clazz
                    .getDeclaredConstructors()) {
                Class<?>[] paramTypes = constructor.getParameterTypes();
                if(paramTypes == null && args == null) { // Handle no arg
                                                         // constructors
                    toCall = (Constructor<? extends T>) constructor;
                    break;
                }
                else if(args == null || paramTypes == null
                        || args.length != paramTypes.length) {
                    continue;
                }
                else {
                    for (int i = 0; i < args.length; ++i) {
                        Object arg = args[i];
                        Class<?> type = paramTypes[i];
                        if(!type.isAssignableFrom(arg.getClass())) {
                            continue outer;
                        }
                    }
                    toCall = (Constructor<? extends T>) constructor;
                    break;
                }
            }
            if(toCall != null) {
                toCall.setAccessible(true);
                return (T) toCall.newInstance(args);
            }
            else {
                throw new NoSuchMethodException("No constructor for " + clazz
                        + " accepts arguments: " + Arrays.toString(args));
            }
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Call {@code constructor} with {@code args} and return a new instance of
     * type {@code T}.
     * 
     * @param constructor the {@link Constructor} to use for creation
     * @param args the initialization args to pass to the constructor
     * @return an instance of the class to which the {@code constructor} belongs
     */
    public static <T> T newInstance(Constructor<? extends T> constructor,
            Object... args) {
        try {
            constructor.setAccessible(true);
            return constructor.newInstance(args);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Edit the value of {@code variable} on {@code obj}.
     * <p>
     * <strong>NOTE:</strong> This can have grave unintended consequences if you
     * alter the value of a variable after construction. Especially if other
     * parts of the code assume that {@code obj} is immutable, or the
     * {@code variable} is used in a multi threaded context.
     * </p>
     * 
     * @param variable the name of the variable to set
     * @param value the value to set
     * @param obj the object on which to set the value
     */
    public static void set(String variable, Object value, Object obj) {
        try {
            Field field = getField(variable, obj);
            field.set(obj, value);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    private static Field getField(String name, Class<?> clazz, Object obj) {
        try {
            Field field = null;
            while (clazz != null && field == null) {
                try {
                    field = clazz.getDeclaredField(name);
                }
                catch (NoSuchFieldException e) { // check the parent to see if
                                                 // the field was defined there
                    clazz = clazz.getSuperclass();
                }
            }
            if(field != null) {
                field.setAccessible(true);
                return field;
            }
            else {
                throw new NoSuchFieldException("No field name " + name
                        + " exists in the hirearchy of " + obj);
            }
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Return the {@link Field} object} that holds the variable with
     * {@code name} in {@code obj}, if it exists. Otherwise a
     * NoSuchFieldException is thrown.
     * <p>
     * This method will take care of making the field accessible.
     * </p>
     * 
     * @param name the name of the field to get
     * @param obj the object from which to get the field
     * @return the {@link Field} object
     * @throws NoSuchFieldException
     */
    private static Field getField(String name, Object obj) {
        return getField(name, obj.getClass(), obj);
    }

    /**
     * Return the unboxed version of the input {@code clazz}. This is usually
     * a class that represents a primitive for an autoboxed wrapper class.
     * Otherwise, the input {@code clazz} is returned.
     * 
     * @param clazz the {@link Class} to unbox
     * @return the unboxed class
     */
    private static Class<?> unbox(Class<?> clazz) {
        if(clazz == Integer.class) {
            return int.class;
        }
        else if(clazz == Long.class) {
            return long.class;
        }
        else if(clazz == Byte.class) {
            return byte.class;
        }
        else if(clazz == Short.class) {
            return short.class;
        }
        else if(clazz == Float.class) {
            return float.class;
        }
        else if(clazz == Double.class) {
            return double.class;
        }
        else if(clazz == Boolean.class) {
            return boolean.class;
        }
        else if(clazz == Character.class) {
            return char.class;
        }
        else {
            return clazz;
        }
    }

    private Reflection() {/* noinit */}

}
