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

package com.cinchapi.common.io;

import com.cinchapi.common.base.AnyStrings;

import java.io.*;
import java.util.Map;

public class Displayer {
    public static void output(Iterable<Map<String, ?>> items,
            OutputStream output) {
        PrintStream printer = new PrintStream(output);

        for(Map<String, ?> item : items) {
            for(Map.Entry<String, ?> entry : item.entrySet()) {
                outputItem(printer, entry.getKey(), entry.getValue());
            }
        }
    }

    private static void outputItem(PrintStream printer, String k, Object v) {
        if(v instanceof String) {
            printer.println(k + "," + AnyStrings
                .ensureWithinQuotesIfNeeded((String) v, ','));
        }
        printer.println(k + "," + v);
    }
}
