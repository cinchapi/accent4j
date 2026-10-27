/*
 * Copyright (c) 2013-2018 Cinchapi Inc.
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
package com.cinchapi.common.unsafe;

import java.util.Map;

import com.cinchapi.common.reflect.Reflection;

/**
 * Hacks for environment variables
 *
 * @author Jeff Nelson
 */
public class EnvironmentVariables {

    /**
     * Set the environment variables to {@code vars}. This method will remove
     * any existing environment variables and set the ones provided.
     * <p>
     * NOTE: This does not modify any environmental variables in the underlying
     * system. Instead, it modifies the JVM's cached copy of the env vars.
     * </p>
     * 
     * @param vars
     */
    public static void setAs(Map<String, String> vars) {
        try {
            Class<?> processEnvironmentClass = Reflection
                    .getClassCasted("java.lang.ProcessEnvironment");
            Map<String, String> env = Reflection.getStatic("theEnvironment",
                    processEnvironmentClass);
            env.clear();
            env.putAll(vars);
            Map<String, String> cienv = Reflection.getStatic(
                    "theCaseInsensitiveEnvironment", processEnvironmentClass);
            cienv.clear();
            cienv.putAll(vars);
        }
        catch (Exception e) {
            Map<String, String> map = Reflection.get("m", System.getenv());
            map.clear();
            map.putAll(vars);
        }
    }

}
