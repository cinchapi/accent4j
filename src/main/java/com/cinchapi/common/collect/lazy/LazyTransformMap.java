/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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
package com.cinchapi.common.collect.lazy;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 *
 *
 * @author jeff
 */
public class LazyTransformMap<FK, FV, TK, TV> extends AbstractMap<TK, TV> {

    private final Map<FK, FV> from;
    private final Function<FK, TK> keyTransformer;
    private final Function<FV, TV> valueTransformer;

    private LazyTransformMap(Map<FK, FV> from, Function<FK, TK> keyTransformer,
            Function<FV, TV> valueTransformer) {
        this.from = from;
        this.keyTransformer = keyTransformer;
        this.valueTransformer = valueTransformer;
    }

    @Override
    public Set<Entry<TK, TV>> entrySet() {
        return LazyTransformSet.of(from.entrySet(),
                entry -> new SimpleImmutableEntry<>(
                        keyTransformer.apply(entry.getKey()),
                        valueTransformer.apply(entry.getValue())));
    }

}
