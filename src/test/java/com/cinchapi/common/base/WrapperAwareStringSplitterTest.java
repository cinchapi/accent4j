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

import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

/**
 * Unit tests for {@link WrapperAwareStringSplitter}.
 *
 * @author Jeff Nelson
 */
public class WrapperAwareStringSplitterTest {

    @Test
    public void testBracketAwareSplitter() {
        String string = "affiliation.name, applications.{submittedAt,job.company.{name, description},startDate}, resume, offers.address.state";
        StringSplitter it = WrapperAwareStringSplitter.bracketAware(string, ',',
                SplitOption.TRIM_WHITESPACE);
        Set<String> actual = ImmutableSet.copyOf(it.toArray());
        Set<String> expected = ImmutableSet.of("affiliation.name",
                "applications.{submittedAt,job.company.{name, description},startDate}",
                "resume", "offers.address.state");
        Assert.assertEquals(expected, actual);
    }
    
    @Test
    public void testBracketAwareSplitterReproA() {
        String string = "company.{name, description}";
        StringSplitter it = WrapperAwareStringSplitter.bracketAware(string, ',',
                SplitOption.TRIM_WHITESPACE);
        Set<String> actual = ImmutableSet.copyOf(it.toArray());
        Assert.assertEquals(ImmutableSet.of(string), actual);
    }

}
