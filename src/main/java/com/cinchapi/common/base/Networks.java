/*
 * Copyright (c) 2016 Cinchapi Inc.
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
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;

import com.google.common.base.Preconditions;

/**
 * Utilities for dealing with networking.
 * <p>
 * A port is <em>open</em> when no socket on the local machine binds it on all
 * addresses or on {@code 127.0.0.1}. A server that binds an open port on all
 * addresses is then the only socket that receives connections to the port at
 * {@code 127.0.0.1}. A port that a method of this class returns is open when
 * the method returns. Another process can bind the port after that, so the
 * caller's own bind can still fail.
 * </p>
 * 
 * @author Jeff Nelson
 */
public final class Networks {

    /**
     * Return the <em>companion</em> port for the specified one. The companion
     * port is one between {@value #MIN_PORT} and {@value #MAX_PORT} that is
     * guaranteed to be different from {@code port}.
     * 
     * @param port the port whose companion to return
     * @return the companion port
     * @deprecated the operating system assigns ports in the companion range to
     *             outgoing connections, so another socket can hold the
     *             companion port; use {@link #getPreferredOpenPort()} to choose
     *             a second port instead
     */
    @Deprecated
    public static int getCompanionPort(int port) {
        return port < PORT_RANGE ? MIN_PORT + port
                : (port % PORT_RANGE) + MIN_PORT;
    }

    /**
     * Return the <em>companion</em> port for the specified one. The companion
     * port is one between {@value #MIN_PORT} and {@value #MAX_PORT} that is
     * guaranteed to be different from {@code port}.
     * 
     * @param port the port to start from
     * @param rounds the number of times to take the companion of the port
     * @return the companion port
     * @deprecated the operating system assigns ports in the companion range to
     *             outgoing connections, so another socket can hold the
     *             companion port; use {@link #getPreferredOpenPort()} to choose
     *             a second port instead
     */
    @Deprecated
    public static int getCompanionPort(int port, int rounds) {
        for (int i = 0; i < rounds; i++) {
            port = getCompanionPort(port);
        }
        return port;
    }

    /**
     * Get the accessible IP Address for the {@code host}.
     * 
     * @param host
     * @return the ip address
     */
    public static String getIpAddress(String host) {
        try {
            if(host.equalsIgnoreCase("localhost")
                    || host.equalsIgnoreCase("127.0.0.1")
                    || host.equalsIgnoreCase("0.0.0.0")) {
                return InetAddress.getLocalHost().getHostAddress();
            }
            if(host.startsWith("http") || host.startsWith("https")) {
                host = new URL(host).getHost();
            }
            return InetAddress.getByName(host).getHostAddress();
        }
        catch (UnknownHostException | MalformedURLException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Return an open port from {@value #MIN_PORT} to {@value #MAX_PORT}.
     *
     * @return the port
     * @throws IllegalStateException if no port in the range is open
     * @deprecated the operating system assigns ports in this range to outgoing
     *             connections, so another socket can take the port before the
     *             caller binds it; use {@link #getPreferredOpenPort()} instead
     */
    @Deprecated
    public static int getOpenPort() {
        return getOpenPortWithinRange(MIN_PORT, MAX_PORT);
    }

    /**
     * Return an open port from {@code rangeStart} to {@code rangeEnd},
     * inclusive.
     *
     * @param rangeStart the lowest port to return
     * @param rangeEnd the highest port to return
     * @return the port
     * @throws IllegalArgumentException if {@code rangeStart} is less than 1,
     *         {@code rangeEnd} is less than {@code rangeStart}, or
     *         {@code rangeEnd} is greater than {@value #MAX_PORT}
     * @throws IllegalStateException if no port in the range is open
     */
    public static int getOpenPortWithinRange(int rangeStart, int rangeEnd) {
        Preconditions.checkArgument(rangeStart > 0,
                "rangeStart must be at least 1");
        Preconditions.checkArgument(rangeEnd >= rangeStart,
                "rangeEnd must be at least rangeStart");
        Preconditions.checkArgument(rangeEnd <= MAX_PORT,
                "rangeEnd must be at most %s", MAX_PORT);
        int span = rangeEnd - rangeStart + 1;
        // Start at a random port so that callers in different processes rarely
        // choose the same one.
        int offset = RAND.nextInt(span);
        int port = 0;
        for (int i = 0; i < span && port == 0; ++i) {
            int candidate = rangeStart + (offset + i) % span;
            if(isOpenPort(candidate)) {
                port = candidate;
            }
        }
        if(port > 0) {
            return port;
        }
        else {
            throw new IllegalStateException("No port from " + rangeStart
                    + " to " + rangeEnd + " is open");
        }
    }

    /**
     * Return an open port from {@value #PREFERRED_OPEN_PORT_RANGE_START} to
     * {@value #PREFERRED_OPEN_PORT_RANGE_END}. By default, Linux, macOS and
     * Windows assign only ports above this range to outgoing connections and to
     * binds to port 0, so another socket is unlikely to take the port before
     * the caller binds it.
     *
     * @return the port
     * @throws IllegalStateException if no port in the range is open
     */
    public static int getPreferredOpenPort() {
        return getOpenPortWithinRange(PREFERRED_OPEN_PORT_RANGE_START,
                PREFERRED_OPEN_PORT_RANGE_END);
    }

    /**
     * Return {@code true} if the {@code host} belongs to the local machine.
     * 
     * @param host
     * @return {@code true} if the {@code host} is local
     */
    public static boolean isLocalHost(String host) {
        try {
            InetAddress inet = InetAddress.getByName(host);
            if(inet.isAnyLocalAddress() || inet.isLoopbackAddress()) {
                return true;
            }
            else {
                return NetworkInterface.getByInetAddress(inet) != null;
            }
        }
        catch (SocketException | UnknownHostException e) {
            return false;
        }
    }

    /**
     * Return whether a socket that does not allow address reuse can bind
     * {@code address}. The socket is closed before this method returns.
     *
     * @param address the address and port to bind
     * @return {@code true} if the bind succeeds, or {@code false} if another
     *         socket's use of the port prevents it
     * @throws RuntimeException if the socket cannot be opened, or the bind
     *         fails for another reason
     */
    private static boolean canBind(InetSocketAddress address) {
        try (ServerSocket socket = new ServerSocket()) {
            // Address reuse lets the bind succeed beside a socket that binds
            // the port on one address alone, such as 127.0.0.1.
            socket.setReuseAddress(false);
            socket.bind(address);
            return true;
        }
        catch (BindException e) {
            return false;
        }
        catch (IOException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Return {@code true} if the {@code port} is open.
     *
     * @param port the port to check
     * @return {@code true} if the port is open
     * @throws RuntimeException if the check cannot open a socket, or a bind
     *         fails for a reason other than another socket's use of the port
     */
    private static boolean isOpenPort(int port) {
        // A bind on all addresses can succeed beside a socket that binds the
        // port on the IPv4 wildcard address alone, and a connection to
        // 127.0.0.1 then reaches that socket, so the port must also be free on
        // 127.0.0.1.
        return canBind(new InetSocketAddress(port))
                && canBind(new InetSocketAddress(LOOPBACK, port));
    }

    /**
     * The inclusive lower bound for the preferred open-port range.
     */
    public static final int PREFERRED_OPEN_PORT_RANGE_START = 16384;

    /**
     * The inclusive upper bound for the preferred open-port range.
     */
    public static final int PREFERRED_OPEN_PORT_RANGE_END = 32768 - 1;

    /**
     * Regex to match an IP Address.
     */
    public static final String IP_REGEX = "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)";

    /**
     * The IPv4 loopback address, which a connection to {@code localhost} can
     * reach.
     */
    private static final String LOOPBACK = "127.0.0.1";

    /**
     * The highest port number, and the highest port that
     * {@link #getCompanionPort(int)} and {@link #getOpenPort()} return.
     */
    private static final int MAX_PORT = 65535;

    /**
     * The lowest port that {@link #getCompanionPort(int)} and
     * {@link #getOpenPort()} return.
     */
    private static final int MIN_PORT = 49152;

    /**
     * The number of available ports that can be considered "companion" ports.
     */
    private static final int PORT_RANGE = MAX_PORT - MIN_PORT;

    /**
     * Class wide random number generator
     */
    private static final java.util.Random RAND = new java.util.Random();

    private Networks() {/* noop */}

}
