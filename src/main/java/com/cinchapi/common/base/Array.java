/*
 * Copyright (c) 2013-2017 Cinchapi Inc.
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

/**
 * Array-related helper functions.
 *
 * @author Jeff Nelson
 */
public final class Array {

    /**
     * Return an array containing all the {@code args}.
     * <p>
     * This method is just syntactic sugar for easily passing array objects to
     * methods that don't take varargs.
     * </p>
     * 
     * @param args the elements that should go in the array.
     * @return args
     */
    @SafeVarargs
    public static <T> T[] containing(T... args) {
        return args;
    }

    private Array() {}

}
