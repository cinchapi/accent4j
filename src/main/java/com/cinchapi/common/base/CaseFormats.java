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
package com.cinchapi.common.base;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.base.CaseFormat;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

/**
 * Utilities for {@link CaseFormat case formats}.
 *
 * @author Jeff Nelson
 */
public final class CaseFormats {

    /**
     * Detect the {@link CaseFormat} that describes the {@code string}.
     * 
     * @param string
     * @return the best fit {@link CaseFormat} description
     */
    public static CaseFormat detect(String string) {
        if(string.contains("-")) {
            return CaseFormat.LOWER_HYPHEN;
        }
        else if(string.contains("_")) {
            for (char c : string.toCharArray()) {
                if(Character.isUpperCase(c)) {
                    return CaseFormat.UPPER_UNDERSCORE;
                }
            }
            return CaseFormat.LOWER_UNDERSCORE;
        }
        else if(Character.isLowerCase(string.toCharArray()[0])) {
            return CaseFormat.LOWER_CAMEL;
        }
        else {
            return CaseFormat.UPPER_CAMEL;
        }
    }

    /**
     * Generates all possible case formats for a given set of strings.
     * 
     * @param strings
     * @return a {@link Set} that contains all possible {@link CaseFormat case
     *         formats} for each of the {@code strings}
     */
    public static Map<String, CaseFormat> allVariationsOf(String... strings) {
        Map<String, CaseFormat> formatted = Maps.newLinkedHashMap();
        CaseFormat[] formats = CaseFormat.values();
        for (String string : strings) {
            CaseFormat original = detect(string);
            formatted.put(string, original);
            for (CaseFormat target : Arrays.stream(formats)
                    .filter(Predicates.equalTo(original).negate())
                    .collect(Collectors.toList())) {
                formatted.put(original.to(target, string), target);
            }
        }
        return formatted;
    }

    private CaseFormats() {/* no-init */}

}
