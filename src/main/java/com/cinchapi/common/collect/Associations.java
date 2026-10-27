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
package com.cinchapi.common.collect;

import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Utilities for {@link Association} objects.
 *
 * @author Jeff Nelson
 */
public final class Associations {

    /**
     * Flatten the {@code value} using the {@code key} as an entrypoint and
     * consume the result using the {@code consumer}.
     * 
     * @param key
     * @param value
     * @param consumer
     */
    @SuppressWarnings("unchecked")
    public static void flattenAndConsume(String key, Object value,
            BiConsumer<String, Object> consumer) {
        if(value instanceof Map) {
            ((Map<String, Object>) value).forEach((k, v) -> {
                flattenAndConsume(key + "." + k, v, consumer);
            });
        }
        else if(value instanceof Iterable) {
            Iterator<Object> it = ((Iterable<Object>) value).iterator();
            int index = 0;
            while (it.hasNext()) {
                flattenAndConsume(key + "." + index, it.next(), consumer);
                ++index;
            }
        }
        else {
            consumer.accept(key, value);
        }
    }

    /**
     * Consume the flattened entries in the {@code map} using the
     * {@code consumer}.
     * 
     * @param map
     * @param consumer
     */
    public static void forEachFlattened(Map<String, Object> map,
            BiConsumer<String, Object> consumer) {
        map.forEach((key, value) -> {
            flattenAndConsume(key, value, consumer);
        });
    }

    private Associations() {/* no-init */}

}
