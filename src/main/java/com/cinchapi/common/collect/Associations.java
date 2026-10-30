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
import java.util.function.ObjIntConsumer;

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
    public static void flattenAndConsume(String key, Object value,
            BiConsumer<String, Object> consumer) {
        flattenAndConsume(key, value, consumer, (list, position) -> {});
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

    /**
     * Flatten the {@code value} using the {@code key} as an entrypoint, consume
     * the result using the {@code consumer}, and report each element of a list
     * in the {@code value} to {@code onElement}.
     * <p>
     * {@code onElement} receives the path of the list and the position of the
     * element before the {@code consumer} receives any entry of that element.
     * It receives every element, including one that is an empty container and
     * so gives the {@code consumer} no entries.
     * </p>
     *
     * @param key the path of the {@code value}
     * @param value the value to flatten
     * @param consumer the receiver of each flattened path and leaf value
     * @param onElement the receiver of the path of a list and the position of
     *            one of its elements
     */
    @SuppressWarnings("unchecked")
    static void flattenAndConsume(String key, Object value,
            BiConsumer<String, Object> consumer,
            ObjIntConsumer<String> onElement) {
        if(value instanceof Map) {
            ((Map<String, Object>) value).forEach((k, v) -> {
                flattenAndConsume(key + "." + k, v, consumer, onElement);
            });
        }
        else if(value instanceof Iterable) {
            Iterator<Object> it = ((Iterable<Object>) value).iterator();
            int index = 0;
            while (it.hasNext()) {
                onElement.accept(key, index);
                flattenAndConsume(key + "." + index, it.next(), consumer,
                        onElement);
                ++index;
            }
        }
        else {
            consumer.accept(key, value);
        }
    }

    /**
     * Consume the flattened entries in the {@code map} using the
     * {@code consumer}, and report each element of a list in the {@code map} to
     * {@code onElement}, as
     * {@link #flattenAndConsume(String, Object, BiConsumer, ObjIntConsumer)}
     * does.
     *
     * @param map the data to flatten
     * @param consumer the receiver of each flattened path and leaf value
     * @param onElement the receiver of the path of a list and the position of
     *            one of its elements
     */
    static void forEachFlattened(Map<String, Object> map,
            BiConsumer<String, Object> consumer,
            ObjIntConsumer<String> onElement) {
        map.forEach((key, value) -> {
            flattenAndConsume(key, value, consumer, onElement);
        });
    }

    private Associations() {/* no-init */}

}
