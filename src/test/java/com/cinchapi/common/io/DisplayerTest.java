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
