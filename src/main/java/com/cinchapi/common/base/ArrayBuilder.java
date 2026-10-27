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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import com.cinchapi.common.reflect.Reflection;

/**
 * A builder for arrays.
 * <p>
 * Use an {@link ArrayBuilder} when there is a need to efficiently produce
 * arrays with an unknown length.
 * </p>
 * <p>
 * When trying to construct an array of an unknown length, you can create a
 * {@link List} and use the {@link List#toArray()} method, but this approach is
 * ugly and inefficient. This builder provides a much more fluent syntax and
 * minimizes the amount of memory necessary to build the array dynamically.
 * </p>
 * 
 * @author Jeff Nelson
 */
public class ArrayBuilder<T> {

    /**
     * Return a new {@link ArrayBuilder}.
     * 
     * @return the builder
     */
    public static <T> ArrayBuilder<T> builder() {
        return new ArrayBuilder<T>();
    }

    /**
     * The number of elements that have been {@link #add(Object) added} to
     * the array.
     */
    private int length = 0;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg0 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg1 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg2 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg3 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg4 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg5 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg6 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg7 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg8 = null;

    /**
     * The first 10 args are assigned to instance variables for memory
     * efficiency.
     */
    private T arg9 = null;

    /**
     * A {@link List} that holds any remaining args after the first 10 have been
     * added.
     */
    private List<T> moreArgs = null;

    private Class<?> type;

    /**
     * Construct a new instance.
     */
    private ArrayBuilder() {/* no-op */}

    /**
     * Add all the {@code args} to the array.
     * 
     * @param args the arg to add
     * @return this
     */
    public ArrayBuilder<T> add(T[] args) {
        for (T arg : args) {
            add(arg);
        }
        return this;
    }

    /**
     * Add {@code arg} to the array.
     * 
     * @param arg the arg to add
     * @return this
     */
    public ArrayBuilder<T> add(T arg) {
        switch (length) {
        case 0:
            arg0 = arg;
            break;
        case 1:
            arg1 = arg;
            break;
        case 2:
            arg2 = arg;
            break;
        case 3:
            arg3 = arg;
            break;
        case 4:
            arg4 = arg;
            break;
        case 5:
            arg5 = arg;
            break;
        case 6:
            arg6 = arg;
            break;
        case 7:
            arg7 = arg;
            break;
        case 8:
            arg8 = arg;
            break;
        case 9:
            arg9 = arg;
            break;
        default:
            if(moreArgs == null) {
                moreArgs = new ArrayList<T>();
            }
            moreArgs.add(arg);
            break;
        }
        ++length;
        type = (type == null || arg.getClass() == type) ? arg.getClass()
                : Reflection.getClosestCommonAncestor(type, arg.getClass());
        return this;
    }

    /**
     * Build the array.
     * 
     * @return the array
     */
    @SuppressWarnings("unchecked")
    public T[] build() {
        if(arg0 == null) {
            throw new IllegalStateException();
        }
        else {
            T[] array = (T[]) Array.newInstance(type, length);
            T current = arg0;
            boolean exit = false;
            int index = 0;
            while (!exit) {
                if(current == null) {
                    exit = true;
                    continue;
                }
                else {
                    array[index] = current;
                    switch (index) {
                    case 0:
                        current = arg1;
                        break;
                    case 1:
                        current = arg2;
                        break;
                    case 2:
                        current = arg3;
                        break;
                    case 3:
                        current = arg4;
                        break;
                    case 4:
                        current = arg5;
                        break;
                    case 5:
                        current = arg6;
                        break;
                    case 6:
                        current = arg7;
                        break;
                    case 7:
                        current = arg8;
                        break;
                    case 8:
                        current = arg9;
                        break;
                    default:
                        current = null;
                        break;
                    }
                    ++index;
                    continue;
                }
            }
            if(length > 10) {
                for (T arg : moreArgs) {
                    array[index] = arg;
                    ++index;
                }
            }
            return array;
        }
    }

    /**
     * Return the the number of items that have been added.
     * 
     * @return the length of the array that will be {@link #build() built}
     */
    public int length() {
        return length;
    }

}
