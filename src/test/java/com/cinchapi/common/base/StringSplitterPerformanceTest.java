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
package com.cinchapi.common.base;

import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import com.cinchapi.common.base.AnyStrings;
import com.cinchapi.common.base.QuoteAwareStringSplitter;
import com.cinchapi.common.base.StringSplitter;
import com.cinchapi.common.profile.Benchmark;

/**
 * Unit tests to verify that {@link StringSplitter} is faster than alternative
 * methods.
 * 
 * @author Jeff Nelson
 */
public class StringSplitterPerformanceTest {

    @Test
    @Ignore
    public void testSimpleSplit() {
        String string = "The Gangs All Here,www.youtube.com/embed/VlWsLs8G7Kg,,"
                + "\"Anacostia follows the lives of the residents of ANACOSTIA, "
                + "a small residential community in Washington D.C. as they "
                + "navigate through love, betrayal, deception, sex and murder\","
                + "ANACOSTIA,3,7,,Webseries,,,";
        int rounds = 5000;
        Benchmark builtIn = new Benchmark(TimeUnit.MICROSECONDS) {

            @Override
            public void action() {
                string.split(",");
            }

        };

        Benchmark splitter = new Benchmark(TimeUnit.MICROSECONDS) {

            @Override
            public void action() {
                new StringSplitter(string, ',').toArray();
            }

        };
        double builtInTime = builtIn.average(rounds);
        double splitterTime = splitter.average(rounds);
        System.out.println("Built-In: " + builtInTime);
        System.out.println("Splitter: " + splitterTime);
        Assert.assertTrue(splitterTime < builtInTime);
    }

    @Test
    public void testQuoteAwareSplit() {
        String string = "The Gangs All Here,www.youtube.com/embed/VlWsLs8G7Kg,,"
                + "\"Anacostia follows the lives of the residents of ANACOSTIA, "
                + "a small residential community in Washington D.C. as they "
                + "navigate through love, betrayal, deception, sex and murder\","
                + "ANACOSTIA,3,7,,Webseries,,,";
        int rounds = 5000;
        Benchmark builtIn = new Benchmark(TimeUnit.MICROSECONDS) {

            @Override
            public void action() {
                AnyStrings.splitStringByDelimiterButRespectQuotes(string, ",");
            }

        };

        Benchmark splitter = new Benchmark(TimeUnit.MICROSECONDS) {

            @Override
            public void action() {
                new QuoteAwareStringSplitter(string, ',').toArray();
            }

        };
        double builtInTime = builtIn.average(rounds);
        double splitterTime = splitter.average(rounds);
        System.out.println("Built-In: " + builtInTime);
        System.out.println("Splitter: " + splitterTime);
        Assert.assertTrue(splitterTime < builtInTime);
    }

}
