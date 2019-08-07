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

import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DisplayerTest {
    @Test public void outputTest() {
        List<Map<String, String>> stuff = new ArrayList<>();
        stuff.add(new HashMap<>());
        stuff.get(0).put("One", "One,Two,Three");
        stuff.get(0).put("Two", "Two.Two.Three");

        stuff.add(new HashMap<>());
        stuff.get(1).put("Testing", "Testing Here");
        stuff.get(1).put("Testing2", "Testing,Here");

        String result = "One,\"One,Two,Three\"\n"
                + "One,One,Two,Three\n"
                + "Two,Two.Two.Three\n"
                + "Two,Two.Two.Three\n"
                + "Testing2,\"Testing,Here\"\n"
                + "Testing2,Testing,Here\n"
                + "Testing,Testing Here\n"
                + "Testing,Testing Here\n";

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        OutputStream stream = new PrintStream(out);
        Displayer.output(stuff, stream);

        assert out.toString().equals(result);
    }
}
