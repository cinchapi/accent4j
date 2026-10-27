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

    /**
     * Custom logic that maps the short names of the {@link Site} constants,
     * such as {@code "21N"}, {@code "TN"}, and {@code "S+A"}, and returns
     * {@code null} for any other value.
     */
    private static final Function<Object, Site> LOGIC = value -> {
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
            return null;
        }
    };

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

    /**
     * <strong>Goal:</strong> Verify that {@code parseIgnoreCase} throws an
     * {@link IllegalArgumentException} for an ordinal that no constant has.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum with five constants.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code parseIgnoreCase(Site.class, "9")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalArgumentException} is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testParseIgnoreCaseThrowsForOrdinalOutOfRange() {
        Enums.parseIgnoreCase(Site.class, "9");
    }

    /**
     * <strong>Goal:</strong> Verify that {@code parseNameIgnoreCase} matches a
     * constant by name in any case.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code parseNameIgnoreCase(Site.class, "travel_noire")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@link Site#TRAVEL_NOIRE} is returned.
     */
    @Test
    public void testParseNameIgnoreCaseMatchesNameInAnyCase() {
        Assert.assertSame(Site.TRAVEL_NOIRE,
                Enums.parseNameIgnoreCase(Site.class, "travel_noire"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code parseNameIgnoreCase} never
     * reads a number as an ordinal.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum, whose constant at
     * ordinal 1 is {@link Site#AFROTECH}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code parseNameIgnoreCase(Site.class, "1")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalArgumentException} is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testParseNameIgnoreCaseThrowsForOrdinal() {
        Enums.parseNameIgnoreCase(Site.class, "1");
    }

    /**
     * <strong>Goal:</strong> Verify that {@code parseNameIgnoreCase} throws for
     * a value that names no constant.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code parseNameIgnoreCase(Site.class, "foo")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalArgumentException} is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testParseNameIgnoreCaseThrowsForUnknownName() {
        Enums.parseNameIgnoreCase(Site.class, "foo");
    }

    /**
     * <strong>Goal:</strong> Verify that {@code parseNameIgnoreCase} applies
     * the custom logic when no name matches.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum and {@link #LOGIC},
     * which maps {@code "s+a"} to {@link Site#SHADOW_AND_ACT}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code parseNameIgnoreCase(Site.class, "s+a", LOGIC)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@link Site#SHADOW_AND_ACT} is returned.
     */
    @Test
    public void testParseNameIgnoreCaseUsesCustomLogic() {
        Assert.assertSame(Site.SHADOW_AND_ACT,
                Enums.parseNameIgnoreCase(Site.class, "s+a", LOGIC));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseIgnoreCase} reads a
     * numeric string as an ordinal.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum, whose constant at
     * ordinal 1 is {@link Site#AFROTECH}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseIgnoreCase(Site.class, "1")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@link Site#AFROTECH} is returned.
     */
    @Test
    public void testTryParseIgnoreCaseReturnsConstantAtOrdinal() {
        Assert.assertSame(Site.AFROTECH,
                Enums.tryParseIgnoreCase(Site.class, "1"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseIgnoreCase} returns
     * {@code null} for an ordinal that no constant has, instead of throwing.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum with five constants.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseIgnoreCase(Site.class, "9")} and
     * {@code tryParseIgnoreCase(Site.class, -1)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Both calls return {@code null}.
     */
    @Test
    public void testTryParseIgnoreCaseReturnsNullForOrdinalOutOfRange() {
        Assert.assertNull(Enums.tryParseIgnoreCase(Site.class, "9"));
        Assert.assertNull(Enums.tryParseIgnoreCase(Site.class, -1));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseIgnoreCase} returns
     * {@code null} for a value that names no constant, instead of throwing.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseIgnoreCase(Site.class, "foo")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code null} is returned.
     */
    @Test
    public void testTryParseIgnoreCaseReturnsNullForUnknownName() {
        Assert.assertNull(Enums.tryParseIgnoreCase(Site.class, "foo"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseIgnoreCase} applies the
     * custom logic when no ordinal or name matches, and returns {@code null}
     * when the custom logic returns {@code null}.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum and {@link #LOGIC},
     * which maps {@code "21N"} to {@link Site#TWENTY_ONE_NINETY} and
     * {@code "foo"} to {@code null}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseIgnoreCase(Site.class, "21N", LOGIC)}.</li>
     * <li>Call {@code tryParseIgnoreCase(Site.class, "foo", LOGIC)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first call returns
     * {@link Site#TWENTY_ONE_NINETY} and the second returns {@code null}.
     */
    @Test
    public void testTryParseIgnoreCaseUsesCustomLogic() {
        Assert.assertSame(Site.TWENTY_ONE_NINETY,
                Enums.tryParseIgnoreCase(Site.class, "21N", LOGIC));
        Assert.assertNull(Enums.tryParseIgnoreCase(Site.class, "foo", LOGIC));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseNameIgnoreCase} matches
     * a constant by name in any case.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseNameIgnoreCase(Site.class, "Blavity")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@link Site#BLAVITY} is returned.
     */
    @Test
    public void testTryParseNameIgnoreCaseMatchesNameInAnyCase() {
        Assert.assertSame(Site.BLAVITY,
                Enums.tryParseNameIgnoreCase(Site.class, "Blavity"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseNameIgnoreCase} never
     * reads a number as an ordinal, and returns {@code null} for a value that
     * names no constant.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum, whose constant at
     * ordinal 1 is {@link Site#AFROTECH}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseNameIgnoreCase(Site.class, "1")},
     * {@code tryParseNameIgnoreCase(Site.class, 1)}, and
     * {@code tryParseNameIgnoreCase(Site.class, "foo")}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every call returns {@code null}.
     */
    @Test
    public void testTryParseNameIgnoreCaseReturnsNullForOrdinalOrUnknownName() {
        Assert.assertNull(Enums.tryParseNameIgnoreCase(Site.class, "1"));
        Assert.assertNull(Enums.tryParseNameIgnoreCase(Site.class, 1));
        Assert.assertNull(Enums.tryParseNameIgnoreCase(Site.class, "foo"));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code tryParseNameIgnoreCase} applies
     * the custom logic when no name matches, and returns {@code null} when the
     * custom logic returns {@code null}.
     * <p>
     * <strong>Start state:</strong> The {@link Site} enum and {@link #LOGIC},
     * which maps {@code "TN"} to {@link Site#TRAVEL_NOIRE} and {@code "foo"} to
     * {@code null}.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code tryParseNameIgnoreCase(Site.class, "TN", LOGIC)}.</li>
     * <li>Call {@code tryParseNameIgnoreCase(Site.class, "foo", LOGIC)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first call returns
     * {@link Site#TRAVEL_NOIRE} and the second returns {@code null}.
     */
    @Test
    public void testTryParseNameIgnoreCaseUsesCustomLogic() {
        Assert.assertSame(Site.TRAVEL_NOIRE,
                Enums.tryParseNameIgnoreCase(Site.class, "TN", LOGIC));
        Assert.assertNull(
                Enums.tryParseNameIgnoreCase(Site.class, "foo", LOGIC));
    }

}
