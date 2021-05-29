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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link AnyStrings}.
 *
 * @author Jeff Nelson
 */
public class AnyStringsTest {

    @Test
    public void testIsWithinQuotedString() {
        Assert.assertTrue(AnyStrings.isWithinQuotes("“abc”"));
        Assert.assertTrue(AnyStrings.isWithinQuotes("‘abc’"));
    }

    @Test
    public void testReplaceUnicodeSingleQuoteConfusables() {
        String expected = "'a'";
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables(expected));
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables("`a`"));
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables("’a’"));
    }

    @Test
    public void testReplaceUnicodeDoubleQuoteConfusables() {
        String expected = "\"a\"";
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables(expected));
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables("˝a˝"));
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables("″a‶"));
    }

    @Test
    public void testReplaceUnicodeSingleQuoteExcludeConfusables() {
        String expected = "`a`";
        Assert.assertEquals(expected,
                AnyStrings.replaceUnicodeConfusables("`a`", '`'));
    }
    
    @Test
    public void testJoinEmptyCharacterSeparator() {
        String expected = "";
        Assert.assertEquals(expected, AnyStrings.join(','));
    }
    
    @Test
    public void testJoinEmptyStringSeparator() {
        String expected = "";
        Assert.assertEquals(expected, AnyStrings.join(" and "));
    }
    
    @Test
    public void testTryParseNumberReproA() {
        String string = "e45";
        Assert.assertNull(AnyStrings.tryParseNumber(string));
    }
    
    @Test
    public void testTryParseNumberValidScientifiedNotation() {
        String string = "1e4";
        Assert.assertNotNull(AnyStrings.tryParseNumber(string));
    }
    
    @Test
    public void testTryParseNumberInvalidScientifiedNotation() {
        String string = "1e4e";
        Assert.assertNull(AnyStrings.tryParseNumber(string));
    }

}
