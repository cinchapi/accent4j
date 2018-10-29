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

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.CaseFormat;

/**
 * Unit tests for {@link CaseFormats}.
 *
 * @author Jeff Nelson
 */
public class CaseFormatsTest {

    @Test
    public void testDetectHyphen() {
        Assert.assertEquals(CaseFormat.LOWER_HYPHEN,
                CaseFormats.detect("var-name"));
    }

    @Test
    public void testDetectUpperCamel() {
        Assert.assertEquals(CaseFormat.UPPER_CAMEL,
                CaseFormats.detect("VarName"));
    }

    @Test
    public void testDetectLowerCamel() {
        Assert.assertEquals(CaseFormat.LOWER_CAMEL,
                CaseFormats.detect("varName"));
    }

    @Test
    public void testDetectLowerUnderscore() {
        Assert.assertEquals(CaseFormat.LOWER_UNDERSCORE,
                CaseFormats.detect("var_name"));
    }

    @Test
    public void testDetectUpperUnderscore() {
        Assert.assertEquals(CaseFormat.UPPER_UNDERSCORE,
                CaseFormats.detect("VAR_NAME"));
    }

}
