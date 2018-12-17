/*
 * Copyright (c) 2018 Cinchapi Inc.
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
package com.cinchapi.script;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.cinchapi.common.reflect.Reflection;

/**
 * Utility class for ScriptObjectMirror objects.
 *
 * @author Jeff Nelson
 */
public class ScriptObjectMirrors {

    /**
     * If {@code object} is an instance of
     * jdk.nashorn.api.scripting.ScriptObjectMirror, convert it and any of its
     * nested items to a more native java format.
     * 
     * @param object
     * @return the javaified object
     */
    @SuppressWarnings("unchecked")
    public static <T> T javaify(Object object) {
        if(object instanceof Map) {
            // The #object is probably actually an instance of
            // jdk.nashorn.api.scripting.ScriptObjectMirror which has a toString
            // of [Object object]. In order to get the object's properties into
            // a Java friendly Map format we make copies of the nested elements.
            Map<String, Object> map = (Map<String, Object>) object;
            boolean isArray;
            try {
                isArray = Reflection.call(map, "isArray");
            }
            catch (Exception e) {
                isArray = false;
            }
            if(isArray) {
                return (T) map.values().stream()
                        .map(ScriptObjectMirrors::javaify)
                        .collect(Collectors.toList());
            }
            else {
                return (T) map.entrySet().stream().collect(LinkedHashMap::new,
                        (m, e) -> m.put(e.getKey(), e.getValue()),
                        LinkedHashMap::putAll);
            }
        }
        else {
            return (T) object;
        }
    }

}
