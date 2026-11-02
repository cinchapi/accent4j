/*
 * Copyright (c) 2013-2026 Cinchapi Inc.
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

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link Networks}.
 *
 * @author Jeff Nelson
 */
public class NetworksTest {

    /**
     * Bind a {@link ServerSocket} that does not allow address reuse to
     * {@code port} on all addresses and then on {@code 127.0.0.1}, closing each
     * one, so that the call fails if any other socket holds the port on either
     * address.
     *
     * @param port the port to bind
     * @throws IOException if either bind fails
     */
    private static void assertNoSocketHolds(int port) throws IOException {
        try (ServerSocket all = new ServerSocket()) {
            all.setReuseAddress(false);
            all.bind(new InetSocketAddress(port));
        }
        try (ServerSocket loopback = new ServerSocket()) {
            loopback.setReuseAddress(false);
            loopback.bind(new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"), port));
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPortWithinRange()}
     * includes both ends of its range.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Take a port from {@code getPreferredOpenPort()}.</li>
     * <li>Call {@code getOpenPortWithinRange()} with that port as both the
     * start and the end of the range.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> {@code getOpenPortWithinRange()} returns that
     * port.
     */
    @Test(timeout = 10000)
    public void testGetOpenPortWithinRangeReturnsOnlyPortInRangeWhenItIsOpen() {
        int port = Networks.getPreferredOpenPort();
        Assert.assertEquals(port, Networks.getOpenPortWithinRange(port, port));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPortWithinRange()}
     * rejects a range whose end is above the highest port.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getOpenPortWithinRange(65535, 65536)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalArgumentException} is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetOpenPortWithinRangeThrowsWhenEndIsAboveHighestPort() {
        Networks.getOpenPortWithinRange(65535, 65536);
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPortWithinRange()}
     * rejects a range whose end is below its start.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getOpenPortWithinRange(20000, 19999)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalArgumentException} is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetOpenPortWithinRangeThrowsWhenEndIsBelowStart() {
        Networks.getOpenPortWithinRange(20000, 19999);
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPortWithinRange()}
     * rejects a range that starts at port 0.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getOpenPortWithinRange(0, 100)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalArgumentException} is thrown.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testGetOpenPortWithinRangeThrowsWhenStartIsZero() {
        Networks.getOpenPortWithinRange(0, 100);
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getPreferredCompanionPort()}
     * returns the companion that its documented formula gives, so a program can
     * compute the companion of a port without calling it.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getPreferredCompanionPort()} for ports below, inside, and
     * above the preferred range.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each companion is 16384 plus the remainder of
     * the port plus 8192, divided by 16384.
     */
    @Test
    public void testGetPreferredCompanionPortFollowsDocumentedFormula() {
        Assert.assertEquals(24577, Networks.getPreferredCompanionPort(1));
        Assert.assertEquals(29143, Networks.getPreferredCompanionPort(4567));
        Assert.assertEquals(24576, Networks.getPreferredCompanionPort(16384));
        Assert.assertEquals(16384, Networks.getPreferredCompanionPort(24576));
        Assert.assertEquals(24575, Networks.getPreferredCompanionPort(32767));
        Assert.assertEquals(24575, Networks.getPreferredCompanionPort(65535));
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getPreferredCompanionPort()}
     * returns a port in the preferred range that differs from its input, for
     * every valid port.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getPreferredCompanionPort()} for each port from 1 to
     * 65535.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each companion is from
     * {@value Networks#PREFERRED_OPEN_PORT_RANGE_START} to
     * {@value Networks#PREFERRED_OPEN_PORT_RANGE_END} and differs from the port
     * it came from.
     */
    @Test
    public void testGetPreferredCompanionPortIsAnotherPortInRange() {
        int start = Networks.PREFERRED_OPEN_PORT_RANGE_START;
        int end = Networks.PREFERRED_OPEN_PORT_RANGE_END;
        for (int port = 1; port <= 65535; ++port) {
            int companion = Networks.getPreferredCompanionPort(port);
            Assert.assertTrue(String.valueOf(port),
                    companion >= start && companion <= end);
            Assert.assertNotEquals(port, companion);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getPreferredCompanionPort()}
     * rejects a number that is not a valid port.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getPreferredCompanionPort()} with {@code -1}, {@code 0},
     * and {@code 65536}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Each call throws
     * {@link IllegalArgumentException}.
     */
    @Test
    public void testGetPreferredCompanionPortThrowsForInvalidPort() {
        for (int port : new int[] { -1, 0, 65536 }) {
            try {
                Networks.getPreferredCompanionPort(port);
                Assert.fail("Returned a companion for " + port);
            }
            catch (IllegalArgumentException e) {
                // The port is outside 1 to 65535, as the caller expects.
            }
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getPreferredOpenPort()} returns
     * a port in the preferred range that no socket holds on all addresses or on
     * {@code 127.0.0.1}.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getPreferredOpenPort()}.</li>
     * <li>Bind a {@link ServerSocket} that does not allow address reuse to
     * the port on all addresses, and close it.</li>
     * <li>Bind another such {@link ServerSocket} to the port on
     * {@code 127.0.0.1}, and close it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The port is from 16384 to 32767, and both
     * binds succeed.
     *
     * @throws IOException if a bind fails
     */
    @Test(timeout = 10000)
    public void testGetPreferredOpenPortReturnsPortInRangeThatNoSocketHolds()
            throws IOException {
        int port = Networks.getPreferredOpenPort();
        Assert.assertTrue(String.valueOf(port), port >= 16384 && port <= 32767);
        assertNoSocketHolds(port);
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPort()} returns a port
     * from 49152 to 65535, as its documentation states.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getOpenPort()} 100 times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every port is from 49152 to 65535.
     *
     * @see <a href="https://github.com/cinchapi/accent4j/issues/60">GH-60</a>
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testGH60GetOpenPortReturnsPortInDocumentedRange() {
        for (int i = 0; i < 100; ++i) {
            int port = Networks.getOpenPort();
            Assert.assertTrue(String.valueOf(port),
                    port >= 49152 && port <= 65535);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPortWithinRange()}
     * fails, rather than returning the port or running forever, when the only
     * port in its range is held on all addresses.
     * <p>
     * <strong>Start state:</strong> A {@link ServerSocket} that listens on all
     * addresses.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getOpenPortWithinRange()} with the port of that
     * {@link ServerSocket} as both the start and the end of the range.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalStateException} is thrown.
     *
     * @throws IOException if the {@link ServerSocket} cannot be opened
     *
     * @see <a href="https://github.com/cinchapi/accent4j/issues/60">GH-60</a>
     */
    @Test(timeout = 10000, expected = IllegalStateException.class)
    public void testGH60GetOpenPortWithinRangeThrowsWhenOnlyPortIsHeldOnAllAddresses()
            throws IOException {
        try (ServerSocket holder = new ServerSocket(0)) {
            int port = holder.getLocalPort();
            Networks.getOpenPortWithinRange(port, port);
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@code getOpenPortWithinRange()} does
     * not return a port that another socket holds on {@code 127.0.0.1} alone,
     * because a server on all addresses would then lose the requests that a
     * client sends to {@code localhost}.
     * <p>
     * <strong>Start state:</strong> A {@link ServerSocket} that listens on
     * {@code 127.0.0.1} and allows address reuse, as Java's server sockets do
     * by default on Linux and macOS.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@code getOpenPortWithinRange()} with the port of that
     * {@link ServerSocket} as both the start and the end of the range.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> An {@link IllegalStateException} is thrown.
     *
     * @throws IOException if the {@link ServerSocket} cannot be opened
     *
     * @see <a href="https://github.com/cinchapi/accent4j/issues/60">GH-60</a>
     */
    @Test(timeout = 10000, expected = IllegalStateException.class)
    public void testGH60GetOpenPortWithinRangeThrowsWhenOnlyPortIsHeldOnLoopback()
            throws IOException {
        try (ServerSocket holder = new ServerSocket()) {
            holder.bind(new InetSocketAddress(
                    InetAddress.getByName("127.0.0.1"), 0));
            int port = holder.getLocalPort();
            Networks.getOpenPortWithinRange(port, port);
        }
    }

}
