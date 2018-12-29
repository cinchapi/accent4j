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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.cinchapi.common.base.AnyObjects;
import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.Array;
import com.cinchapi.common.base.ArrayBuilder;
import com.cinchapi.common.base.CheckedExceptions;
import com.cinchapi.common.base.TernaryTruth;
import com.cinchapi.common.base.Verify;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * A collection of tools for using reflection to access or modify objects.
 * <strong>Use with caution.</strong>
 * <p>
 * <em>This class helps you do some naughty things. Think carefully about
 * whether
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
     * Use reflection to call an instance method on {@code obj} with the
     * specified {@code args}.
     * 
     * @param obj
     * @param methodName
     * @param args
     * @return the result of calling the method
     */
    public static <T> T call(Object obj, String methodName, Object... args) {
        return call(true, obj, methodName, args);
    }

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
     * Call {@code methodName} on the specified {@code obj} with the specified
     * {@code args} if and only if the {@code evaluate} function returns
     * {@code true}.
     * 
     * @param evaluate the {@link Function} that is given the (possibly cached)
     *            {@link Method} instance that corresponds to {@code methodName}
     *            ; use this to evaluate whether the method should be called
     * @param obj the Object on which the method is called
     * @param methodName the name of the method to call
     * @param args the args to pass to the method
     * @return the result of calling the method
     */
    @SuppressWarnings("unchecked")
    public static <T> T callIf(Function<Method, Boolean> evaluate, Object obj,
            String methodName, Object... args) {
        Method method = getMethod(true, methodName, obj.getClass(), args);
        if(evaluate.apply(method)) {
            try {
                method.setAccessible(true);
                return (T) method.invoke(obj, args);
            }
            catch (ReflectiveOperationException e) {
                Throwable ex = e;
                if(ex instanceof InvocationTargetException
                        && e.getCause() != null) {
                    ex = ex.getCause();
                }
                else {
                    ex = Throwables.getRootCause(ex);
                }
                throw CheckedExceptions.wrapAsRuntimeException(ex);
            }
        }
        else {
            throw new IllegalStateException(
                    "Cannot call " + method + " reflectively because "
                            + "the evaluation function returned false");
        }
    }

    /**
     * Use reflection to call an instance method on {@code obj} with the
     * specified {@code args} if and only if that method is natively accessible
     * according to java language access rules.
     * 
     * @param obj
     * @param methodName
     * @param args
     * @return the result of calling the method
     */
    public static <T> T callIfAccessible(Object obj, String methodName,
            Object... args) {
        return call(false, obj, methodName, args);
    }

    /**
     * Use reflection to call a static method in {@code clazz} with the
     * specified {@code args}.
     * 
     * @param clazz the {@link Class} instance
     * @param method the method name
     * @param args the args to pass to the method upon invocation
     * @return the result of the method invocation
     */
    public static <T> T callStatic(Class<?> clazz, String method,
            Object... args) {
        return callStatic(true, clazz, method, args);
    }

    /**
     * Use reflection to call a static method in {@code clazz} with the
     * specified {@code args} if and only if that method is natively accessible
     * according to java language access rules.
     * 
     * @param clazz the {@link Class} instance
     * @param method the method name
     * @param args the args to pass to the method upon invocation
     * @return the result of the method invocation
     */
    public static <T> T callStaticIfAccessible(Class<?> clazz, String method,
            Object... args) {
        return callStatic(false, clazz, method, args);
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
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Given a {@link Class}, return an array containing all the {@link Field}
     * objects that represent those declared within the class's entire hierarchy
     * after the base {@link Object} class.
     * 
     * @param clazz the {@link Class} to inspect
     * @return the array of declared fields
     */
    public static Field[] getAllDeclaredFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<Field>();
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
     * Given an object, return an array containing all the {@link Field} objects
     * that represent those declared within {@code obj's} entire class hierarchy
     * after the base {@link Object}.
     * 
     * @param obj the {@link Object} to inspect
     * @return the array of declared fields
     */
    public static Field[] getAllDeclaredFields(Object obj) {
        return getAllDeclaredFields(obj.getClass());
    }

    /**
     * Reflectively get the value of the {@code field} from the provided
     * {@code object} and attempt an automatic type cast.
     * 
     * @param field the {@link Field} object representing the desired variable
     * @param object the object whose value for the {@code field} should be
     *            retrieved
     * @return the value of {@code field} in {@code object} if it exists,
     *         otherwise {@code null}
     */
    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getCasted(Field field, Object object) {
        try {
            field.setAccessible(true);
            return (T) field.get(object);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * This is literally just syntactic sugar for {@link Class#forName(String)}
     * that doesn't throw a checked exception.
     * 
     * @param name the name of the class
     * @return the {@link Class} object if can be found
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getClassCasted(String name) {
        try {
            return (Class<T>) Class.forName(name);
        }
        catch (ClassNotFoundException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Return the closest common ancestor class for all of the input
     * {@code classes}.
     * 
     * @param classes the classes that have a common ancestor
     * @return the closest common ancestor
     */
    public static Class<?> getClosestCommonAncestor(Class<?>... classes) {
        return Iterables.getFirst(getCommonAncestors(classes), Object.class);
    }

    /**
     * Return all the common ancestors for the {@code classes}.
     * 
     * @param classes
     * @return the common ancestors of the specified classes
     */
    public static Set<Class<?>> getCommonAncestors(Class<?>... classes) {
        Verify.thatArgument(classes.length > 0);
        Set<Class<?>> rollingIntersect = Sets
                .newLinkedHashSet(getClassAncestors(classes[0]));
        for (int i = 1; i < classes.length; i++) {
            rollingIntersect.retainAll(getClassAncestors(classes[i]));
        }
        return AnyObjects.defaultUnless(Collections.singleton(Object.class),
                rollingIntersect, set -> !set.isEmpty());
    }

    /**
     * Return the field with {@code name} that is declared in the class or class
     * hierarchy.
     * 
     * @param name the field name
     * @param clazz the class that contains the field
     * @return the {@link Field} object
     */
    public static Field getDeclaredField(String name, Class<?> clazz) {
        return getField(name, clazz);
    }

    /**
     * Return the field with {@code name} that is declared in the {@code obj}'s
     * class or class hierarchy.
     * 
     * @param name the field name
     * @param obj the object whose class contains the field
     * @return the {@link Field} object
     */
    public static Field getDeclaredField(String name, Object obj) {
        return getField(name, obj);
    }

    /**
     * Return the enum value at position {@code ordinal} for the
     * {@code enumType}.
     * 
     * @param enumType the {@link Enum} class
     * @param ordinal the value position
     * @return the {@link Enum} value
     */
    public static Enum<?> getEnumValue(Class<? extends Enum<?>> enumType,
            int ordinal) {
        try {
            return enumType.getEnumConstants()[ordinal];
        }
        catch (IndexOutOfBoundsException e) {
            throw new IllegalArgumentException(AnyStrings.format(
                    "No enum value for {} with ordinal {}", enumType, ordinal));
        }
    }

    /**
     * Return the enum value from {@code enumType} with the specified
     * {@code identifier}.
     * 
     * @param enumType the {@link Enum} class
     * @param identifier the value name or ordinal
     * @return the {@link Enum} value
     */
    public static Enum<?> getEnumValue(Class<? extends Enum<?>> enumType,
            Object identifier) {
        if(identifier instanceof Integer) {
            return getEnumValue(enumType, (int) identifier);
        }
        else {
            return getEnumValue(enumType, identifier.toString());
        }
    }

    /**
     * Return the enum value from {@code enumType} with the specified
     * {@code name}.
     * 
     * @param enumType the {@link Enum} class
     * @param name the value name
     * @return the {@link Enum} value
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static Enum<?> getEnumValue(Class<? extends Enum<?>> enumType,
            String name) {
        return Enum.valueOf((Class<? extends Enum>) enumType, name);
    }

    /**
     * Return a {@link Method} instance from {@code clazz} named {@code method}
     * (that takes arguments of {@code paramTypes} respectively)
     * while making a best effort attempt to unbox primitive parameter types
     * 
     * @param clazz the class instance in which the method is contained
     * @param method the name of the method
     * @param paramTypes the types for the respective paramters
     * @return a {@link Method} instance that has been set to be accessible
     */
    public static Method getMethodUnboxed(Class<?> clazz, String method,
            Class<?>... paramTypes) {
        return getMethod(null, true, method, clazz, paramTypes);
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public static <T> T getStatic(String variable, Class<?> clazz) {
        try {
            Field field = getField(variable, clazz);
            return (T) field.get(null);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Return a collection containing the type arguments for the provided
     * {@code field}. If there are no type arguments, the collection that is
     * returned is empty.
     * 
     * @param field
     * @return the type arguments
     */
    @Nonnull
    public static Collection<Class<?>> getTypeArguments(Field field) {
        try {
            ParameterizedType parameterized = (ParameterizedType) field
                    .getGenericType();
            Type[] types = parameterized.getActualTypeArguments();
            Set<Class<?>> typeArgs = Sets
                    .newLinkedHashSetWithExpectedSize(types.length);
            for (Type type : types) {
                typeArgs.add((Class<?>) type);
            }
            return typeArgs;
        }
        catch (ClassCastException e) {
            return ImmutableSet.of();
        }
    }

    /**
     * Return a collection containing the type arguments for the provided
     * {@code field} in {@code clazz}. If there are no type arguments, the
     * collection that is returned is empty.
     * 
     * @param field
     * @param clazz
     * @return the type arguments
     */
    public static Collection<Class<?>> getTypeArguments(String field,
            Class<?> clazz) {
        return getTypeArguments(getDeclaredField(field, clazz));
    }

    /**
     * Return a collection containing the type arguments for the provided
     * {@code field} of the {@code object}'s class. If there are no type
     * arguments, the collection that is returned is empty.
     * 
     * @param field
     * @param object
     * @return the type arguments
     */
    public static Collection<Class<?>> getTypeArguments(String field,
            Object object) {
        return getTypeArguments(getDeclaredField(field, object));
    }

    /**
     * Return {@code true} if the {@code method} is callable with the provided
     * {@code params}.
     * 
     * @param method
     * @param params
     * @return {@code true} if the provided {@code params} match the expected
     *         parameter types of the {@code method}
     */
    public static boolean isCallableWith(Method method, Object... params) {
        TernaryTruth callable = isDefinitelyCallableWith(method, params);
        return callable == TernaryTruth.TRUE || callable == TernaryTruth.UNSURE;
    }

    /**
     * Return {@code true} if the {@code method} has an {@link Annotation
     * annotation} of {@code annotationClass} declared in its hierarchy.
     * <p>
     * Technically, annotations on elements other than classes cannot be
     * inherited in Java. However, there are some cases when it is useful to
     * know whether a version of a method, either in a child or super class,
     * does have an annotation, which this method will indicate.
     * </p>
     * <p>
     * A return type of {@code true} doesn't necessarily mean that the
     * {@code method} was directly annotated with an annotation of the specified
     * class. It simply means, that a version of the method was at some point in
     * the hierarchy. If necessary, the caller must do further inspection to
     * retrieve the exact annotation state.
     * </p>
     * 
     * @param method
     * @param annotationClass
     * @return {@code true} if a version of the method in the hierarchy was
     *         annotated
     */
    public static boolean isDeclaredAnnotationPresentInHierarchy(Method method,
            Class<? extends Annotation> annotationClass) {
        if(method.isAnnotationPresent(annotationClass)) {
            return true;
        }
        else {
            Class<?> clazz = method.getDeclaringClass().getSuperclass();
            if(clazz != null) {
                try {
                    method = getMethod(null, false, method.getName(), clazz,
                            method.getParameterTypes());
                    return isDeclaredAnnotationPresentInHierarchy(method,
                            annotationClass);
                }
                catch (Exception e) {
                    return false;
                }
            }
            else {
                return false;
            }
        }
    }

    /**
     * Implementation of {@link ClassLoader#loadClass(String)} that throws an
     * {@link RuntimeException} instead of a checked exception.
     * 
     * @param name
     * @param classLoader
     * @return the loaded class
     */
    public static Class<?> loadClassQuietly(String name,
            ClassLoader classLoader) {
        try {
            return classLoader.loadClass(name);
        }
        catch (ClassNotFoundException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
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
                        Class<?> altType = getAltType(type);
                        if(!type.isAssignableFrom(arg.getClass())
                                && !altType.isAssignableFrom(arg.getClass())) {
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
        catch (InvocationTargetException e) {
            throw CheckedExceptions
                    .wrapAsRuntimeException(e.getTargetException());
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
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
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Return a new instance of the specified {@code clazz} by calling the
     * appropriate constructor with the specified {@code args}.
     * 
     * @param clazz the fully qualified name of the {@link Class}
     * @param args the args to pass to the constructor
     * @return the new instance
     */
    @SuppressWarnings("unchecked")
    public static <T> T newInstance(String clazz, Object... args) {
        try {
            return (T) newInstance(Class.forName(clazz), args);
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Edit the value of all the variables that correspond to the keys in the
     * {@code data} with their associated values.
     * <p>
     * <strong>NOTE:</strong> This can have grave unintended consequences if you
     * alter the value of a variable after construction. Especially if other
     * parts of the code assume that {@code obj} is immutable, or the
     * {@code variable} is used in a multi threaded context.
     * </p>
     * <p>
     * This method will throw an error if one or more of the keys in the
     * {@code data} map does not correspond to a named variable within the
     * {@code obj}'s class.
     * </p>
     * 
     * @param data a mapping from variable name to value
     * @param obj the object on which to set the data
     */
    public static void set(Map<String, Object> data, Object obj) {
        data.forEach((variable, value) -> {
            set(variable, value, obj);
        });
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
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Use reflection to call an instance method on {@code obj} with the
     * specified {@code args}.
     * 
     * @param setAccessible an indication as to whether the reflective call
     *            should suppress Java language access checks or not
     * @param obj
     * @param methodName
     * @param args
     * @return the result of calling the method
     */
    private static <T> T call(boolean setAccessible, Object obj,
            String methodName, Object... args) {
        Method method = getMethod(setAccessible, methodName, obj.getClass(),
                args);
        return invoke(method, obj, args);
    }

    /**
     * Do the work to use reflection to call a static method in {@code clazz}
     * with the specified {@code args} while optionally obeying the native java
     * language access rules.
     * 
     * @param setAccessible a flag that determines whether java language access
     *            rules will be overridden
     * @param clazz the {@link Class} instance
     * @param methodName the method name
     * @param args the args to pass to the method upon invocation
     * @return the result of the method invocation
     */
    private static <T> T callStatic(boolean setAccessible, Class<?> clazz,
            String methodName, Object... args) {
        Method method = getMethod(setAccessible, methodName, clazz, args);
        return invoke(method, null, args);
    }

    /**
     * Return the boxed version of {@code clazz} if it is a primitive, or the
     * unboxed version if it is a wrapper.
     * 
     * @param clazz the class for which the alt type is returned
     * @return the alt type
     */
    private static Class<?> getAltType(Class<?> clazz) {
        if(clazz.isPrimitive()) {
            if(clazz == int.class) {
                return Integer.class;
            }
            else if(clazz == long.class) {
                return Long.class;
            }
            else if(clazz == float.class) {
                return Float.class;
            }
            else if(clazz == double.class) {
                return Double.class;
            }
            else if(clazz == short.class) {
                return Short.class;
            }
            else if(clazz == byte.class) {
                return Byte.class;
            }
            else if(clazz == char.class) {
                return Character.class;
            }
            else if(clazz == boolean.class) {
                return Boolean.class;
            }
            else {
                return clazz;
            }
        }
        else {
            return unbox(clazz);
        }
    }

    /**
     * Get all the ancestors for {@code clazz} in an ordered set.
     * 
     * @param clazz
     * @return the ancestors of {@code clazz}
     */
    private static Set<Class<?>> getClassAncestors(Class<?> clazz) {
        Preconditions.checkArgument(clazz != null);
        Set<Class<?>> classes = Sets.newLinkedHashSet();
        Set<Class<?>> nextLevel = Sets.newLinkedHashSet();
        nextLevel.add(clazz);
        do {
            classes.addAll(nextLevel);
            Set<Class<?>> thisLevel = Sets.newLinkedHashSet(nextLevel);
            nextLevel.clear();
            for (Class<?> each : thisLevel) {
                Class<?> superClass = each.getSuperclass();
                if(superClass != null && superClass != Object.class) {
                    nextLevel.add(superClass);
                }
                for (Class<?> eachInt : each.getInterfaces()) {
                    nextLevel.add(eachInt);
                }
            }
        }
        while (!nextLevel.isEmpty());
        return classes;
    }

    /**
     * Return the value of the {@link Field} called {@code name} in
     * {@code clazz} from the specified {@code obj}.
     * 
     * @param name the name of the field
     * @param clazz the {@link Class} in which the field is defined
     * @return the associated {@link Field} object
     */
    private static Field getField(String name, Class<?> clazz) {
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
                        + " exists in the hirearchy of " + clazz);
            }
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Return the {@link Field} object that holds the variable with {@code name}
     * in {@code obj}, if it exists. Otherwise a
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
        return getField(name, obj.getClass());
    }

    /**
     * Return a set of classes that are considered to be interchangeable with
     * {@code clazz}.
     * 
     * @param clazz
     * @return a set of classes
     */
    private static Set<Class<?>> getInterchangeableClasses(Class<?> clazz) {
        if(clazz == int.class) {
            return Sets.newHashSet(long.class, Long.class, Integer.class);
        }
        else if(clazz == Integer.class) {
            return Sets.newHashSet(long.class, Long.class, int.class);
        }
        else if(clazz == long.class) {
            return Sets.newHashSet(int.class, Long.class, Integer.class);
        }
        else if(clazz == Long.class) {
            return Sets.newHashSet(long.class, int.class, Integer.class);
        }
        else {
            return Collections.emptySet();
        }
    }

    /**
     * Return the {@link Method} object called {@code name} in {@code clazz}
     * that accepts the specified {@code args} and optionally ignore the native
     * java language access rules.
     * 
     * @param setAccessible a flag that indicates whether the native java
     *            language access rules should be ignored
     * @param name the method name
     * @param clazz the {@link Class} in which the method is defined
     * @param args the parameters defined in the method's signature
     * @return the associated {@link Method} object
     */
    private static Method getMethod(boolean setAccessible, String name,
            Class<?> clazz, Object... args) {
        Class<?>[] paramTypes = new Class<?>[args.length];
        for (int i = 0; i < paramTypes.length; ++i) {
            Object arg = args[i];
            paramTypes[i] = arg == null ? null : arg.getClass();
        }
        return getMethod(args, setAccessible, name, clazz, paramTypes);
    }

    /**
     * Return the {@link Method} object called {@code name} in {@code clazz}
     * that accepts the specified {@code args} and optionally ignore the native
     * java language access rules.
     * 
     * @param args (optional) args to plug into the params
     * @param setAccessible a flag that indicates whether the native java
     *            language access rules should be ignored
     * @param name the method name
     * @param clazz the {@link Class} in which the method is defined
     * @param paramType the parameters defined in the method's signature
     * @return the associated {@link Method} object
     */
    private static Method getMethod(@Nullable Object[] args,
            boolean setAccessible, String name, Class<?> clazz,
            Class<?>... paramTypes) {
        List<Method> potential = Lists.newArrayListWithCapacity(1);
        List<Method> deferred = Lists.newArrayListWithCapacity(1);
        try {
            while (clazz != null) {
                for (Method method : Arrays.stream(clazz.getDeclaredMethods())
                        .filter(method -> method.getName().equals(name))
                        .collect(Collectors.toList())) {
                    TernaryTruth callable = isDefinitelyCallableWith(method,
                            paramTypes);
                    if(callable == TernaryTruth.TRUE && method
                            .getParameterCount() == paramTypes.length) {
                        potential.add(method);
                    }
                    else if(callable != TernaryTruth.FALSE) {
                        deferred.add(method);
                    }
                }
                if(potential.isEmpty()) {
                    clazz = clazz.getSuperclass();
                }
                else {
                    break;
                }
            }
            if(potential.size() == 2) {
                // UTIL-12: Handle a corner case where an overloaded method
                // takes corresponding boxable parameters
                List<Class<?>> paramTypesA = Arrays
                        .stream(potential.get(0).getParameterTypes())
                        .map(Reflection::unbox).collect(Collectors.toList());
                List<Class<?>> paramTypesB = Arrays
                        .stream(potential.get(1).getParameterTypes())
                        .map(Reflection::unbox).collect(Collectors.toList());
                if(paramTypesA.equals(paramTypesB)) {
                    potential.remove(1);
                }
            }
            int matches = potential.size();
            Method method;
            if(matches < 1) {
                if(deferred.size() == 1) {
                    method = deferred.get(0);
                }
                else {
                    throw new NoSuchMethodException("Could not find method '"
                            + name + "' that is invokable with: "
                            + Arrays.asList(args != null ? args : paramTypes));
                }
            }
            else if(matches > 1) {
                throw new IllegalArgumentException("Trying to invoke method "
                        + "'" + name + "' with "
                        + Arrays.asList(args != null ? args : paramTypes)
                        + " isn't possible because there are too many null "
                        + "values and it is impossible to decide which "
                        + "method is desired");
            }
            else {
                method = potential.get(0);
            }
            method.setAccessible(setAccessible);
            return method;
        }
        catch (ReflectiveOperationException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Invoke {@code method} with {@code args}. If {@code object} is
     * {@code null}, this is a static invocation.
     * 
     * @param method
     * @param object
     * @param args
     * @return the result of the method invocation
     */
    @SuppressWarnings("unchecked")
    private static <T> T invoke(Method method, @Nullable Object object,
            Object... args) {
        try {
            int count;
            // @formatter:off
            if((count = method.getParameterCount()) > 0 // its possible for method to have varags
                    && (args.length >= count - 1) // the number of arguments provided is at least equal to the number of non vararg parameters 
                    && (method.getParameterTypes()[count - 1].isArray() // the last paramter expected is an array type
                            && (args.length == 0 || !args[args.length - 1] // no args were provided OR the last arg provided is NOT an array (meaning its not a vararg invocation literal)
                                    .getClass().isArray()))) {
             // @formatter:on
                // First, check to see if the last parameter type is a vararg
                // (e.g. array) and wrap the dangling args accordingly.
                ArrayBuilder<Object> _args = ArrayBuilder.builder();
                int i = 0;
                for (; i < count - 1; ++i) {
                    _args.add(args[i]);
                }
                ArrayBuilder<Object> varargs = ArrayBuilder.builder();
                for (; i < args.length; ++i) {
                    varargs.add(args[i]);
                }
                _args.add((Object) (varargs.length() > 0 ? varargs.build()
                        : java.lang.reflect.Array.newInstance(
                                method.getParameterTypes()[count - 1]
                                        .getComponentType(),
                                0)));
                args = _args.build();
            }
            return (T) method.invoke(object, args);
        }
        catch (ReflectiveOperationException e) {
            Throwable ex = e;
            if(ex instanceof InvocationTargetException
                    && e.getCause() != null) {
                ex = ex.getCause();
            }
            else {
                ex = Throwables.getRootCause(ex);
            }
            throw CheckedExceptions.wrapAsRuntimeException(ex);
        }
    }

    /**
     * Return a {@link TernaryTruth} value that indicates whether the
     * {@code method} is definitely callable with the provided
     * {@code paramTypes}.
     * <p>
     * This function is used to determine if the signature of the {@code method}
     * contains specific parameters types (e.g. not the {@link Object.class}
     * type}. If any of the parameters types are super generic, this method will
     * return {@link TernaryTruth#UNSURE} instead of {@link TernaryTruth#TRUE}
     * if all other factors indicate that the method is callable with the param
     * types.
     * </p>
     * 
     * @param method
     * @param paramTypes
     * @return a ternary truth value that indicates the level of certainty that
     *         the method is callable with the provided param types.
     */
    private static TernaryTruth isDefinitelyCallableWith(Method method,
            Class<?>... paramTypes) {
        TernaryTruth callable = TernaryTruth.TRUE;
        Class<?>[] expectedParamTypes = method.getParameterTypes();
        if(paramTypes.length == expectedParamTypes.length) {
            for (int i = 0; i < paramTypes.length; ++i) {
                Class<?> actual = paramTypes[i];
                if(actual != null) {
                    Class<?> expected = expectedParamTypes[i];
                    if(expected == actual || expected == unbox(actual)
                            || (expected != Object.class
                                    && expected.isAssignableFrom(actual))
                            || getInterchangeableClasses(actual)
                                    .contains(expected)) {
                        continue;
                    }
                    else if(expected == Object.class) {
                        // All value types inherit from Object
                        callable = TernaryTruth.UNSURE;
                        continue;
                    }
                    else {
                        callable = TernaryTruth.FALSE;
                        break;
                    }
                }
                else {
                    // A null value can be assigned to any object type.
                    continue;
                }
            }
        }
        else {
            callable = TernaryTruth.FALSE;
        }
        if((callable != TernaryTruth.TRUE && callable != TernaryTruth.UNSURE)
                && expectedParamTypes.length > 0
                && expectedParamTypes[expectedParamTypes.length - 1].isArray()
                && (paramTypes.length >= expectedParamTypes.length - 1)) {
            Class<?> varArgType = expectedParamTypes[expectedParamTypes.length
                    - 1].getComponentType();
            // Check for the corner case where the method has a varargs
            // parameter. If so, see if all the dangling parameters fit in with
            // the varargs
            if(paramTypes.length == expectedParamTypes.length - 1) {
                // Handle corner case where no args are provided for a varargs
                // parameter
                callable = isDefinitelyCallableWith(method,
                        ArrayBuilder.<Class<?>> builder().add(paramTypes)
                                .add(java.lang.reflect.Array
                                        .newInstance(varArgType, 0).getClass())
                                .build());
            }
            else {
                for (int i = expectedParamTypes.length
                        - 1; i < paramTypes.length; ++i) {
                    Class<?> paramType = paramTypes[i];
                    if(paramType == varArgType) {
                        callable = TernaryTruth.TRUE;
                    }
                    else {
                        callable = TernaryTruth.FALSE;
                        break;
                    }
                }
            }

        }
        return callable;
    }

    /**
     * Return a {@link TernaryTruth} value that indicates whether the
     * {@code method} is definitely callable with the provided
     * {@code paramTypes}.
     * <p>
     * This function is used to determine if the signature of the {@code method}
     * contains specific parameters types (e.g. not the {@link Object.class}
     * type}. If any of the parameters types are super generic, this method will
     * return {@link TernaryTruth#UNSURE} instead of {@link TernaryTruth#TRUE}
     * if all other factors indicate that the method is callable with the param
     * types.
     * </p>
     * 
     * @param method
     * @param paramTypes
     * @return a ternary truth value that indicates the level of certainty that
     *         the method is callable with the provided param types.
     */
    private static TernaryTruth isDefinitelyCallableWith(Method method,
            Object... params) {
        return isDefinitelyCallableWith(method,
                Arrays.stream(params).map(Object::getClass)
                        .collect(Collectors.toList())
                        .toArray(Array.containing()));
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
