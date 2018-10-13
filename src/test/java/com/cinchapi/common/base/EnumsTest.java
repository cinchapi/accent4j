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

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link Enums}.
 *
 * @author Jeff Nelson
 */
public class EnumsTest {

    enum Site {
        BLAVITY, AFROTECH, TWENTY_ONE_NINETY, TRAVEL_NOIRE, SHADOW_AND_ACT;
    }

    @Test
    public void testParseStringOrdinal() {
        Assert.assertSame(Site.AFROTECH,
                Enums.parseIgnoreCase(Site.class, "1"));
    }

    @Test
    public void testParseOrdinal() {
        Assert.assertSame(Site.AFROTECH, Enums.parseIgnoreCase(Site.class, 1));
    }

    @Test
    public void testParseIgnoreCase() {
        Assert.assertSame(Site.BLAVITY,
                Enums.parseIgnoreCase(Site.class, "blavity"));
    }

    @Test
    public void testParseCustomLogic() {
        Function<Object, Site> logic = value -> {
            String s = value.toString();
            if(s.replaceAll(" ", "").equalsIgnoreCase("21Ninety")
                    || s.equalsIgnoreCase("21N")) {
                return Site.TWENTY_ONE_NINETY;
            }
            else if(s.equalsIgnoreCase("TN")) {
                return Site.TRAVEL_NOIRE;
            }
            else if(s.equalsIgnoreCase("S&A") || s.equalsIgnoreCase("S+A")
                    || s.equalsIgnoreCase("SA")) {
                return Site.SHADOW_AND_ACT;
            }
            else {
                for (Site site : Site.values()) {
                    if(site.name().replaceAll("_", " ").equalsIgnoreCase(s
                            .replaceAll("&", "AND").replaceAll("\\+", "AND"))) {
                        return site;
                    }
                }
                return null;
            }
        };
        Assert.assertSame(Site.SHADOW_AND_ACT,
                Enums.parseIgnoreCase(Site.class, "s+a", logic));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseInvalid() {
        Enums.parseIgnoreCase(Site.class, "foo");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseCustomLogicInvalid() {
        Enums.parseIgnoreCase(Site.class, "foo",
                value -> value.equals("FOO") ? Site.AFROTECH : null);
    }

}
