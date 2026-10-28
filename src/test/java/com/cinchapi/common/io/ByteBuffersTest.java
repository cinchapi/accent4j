/*
 * Copyright (c) 2013-2021 Cinchapi Inc.
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

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Strings;

/**
 * Unit test for {@link ByteBuffers}.
 *
 * @author Jeff Nelson
 */
public class ByteBuffersTest {
    
    @Test
    public void testGetByteArrayFromByteBufferWithBackingArray() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(17);
        buffer.flip();
        Assert.assertSame(buffer.array(), ByteBuffers.getByteArray(buffer));
    }
    
    @Test
    public void testGetByteBufferFromBufferWithBackingArraySubset() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(17);
        buffer.flip();
        buffer.getInt();
        byte [] bytes = ByteBuffers.getByteArray(buffer);
        Assert.assertEquals(4, bytes.length);
        Assert.assertEquals(4, buffer.position());
        Assert.assertEquals(4, buffer.remaining());
        buffer.getShort();
        bytes = ByteBuffers.getByteArray(buffer);
        Assert.assertEquals(2, bytes.length);
    }
    
    @Test
    public void testGetByteBufferFromBufferWithBackingArraySubsetPosition() {
        ByteBuffer buffer = ByteBuffer.allocate(8);
        buffer.putLong(17);
        buffer.flip();
        buffer.position(1);
        byte[] bytes = ByteBuffers.getByteArray(buffer);
        Assert.assertNotSame(bytes, buffer.array());
        Assert.assertEquals(7, bytes.length);
    }
    
    @Test
    public void testGetByteArrayFromSlicedByteBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(9);
        buffer.put((byte) 0x01);
        buffer.putLong(17);
        buffer.flip();
        buffer.get(); // advance past the first byte
        ByteBuffer slice = buffer.slice();
        Assert.assertEquals(8, slice.remaining());
        Assert.assertEquals(8, slice.capacity());
        byte[] bytes = ByteBuffers.getByteArray(slice);
        Assert.assertEquals(8, bytes.length);
        // Verify the slice content matches, not the parent's array
        ByteBuffer expected = ByteBuffer.allocate(8);
        expected.putLong(17);
        expected.flip();
        Assert.assertArrayEquals(expected.array(), bytes);
    }

    @Test
    public void testGetByteBufferFromBufferWithoutBackingArray() {
        ByteBuffer buffer = ByteBuffer.allocateDirect(8);
        buffer.putLong(17);
        buffer.flip();
        Assert.assertFalse(buffer.hasArray());
        byte [] bytes = ByteBuffers.getByteArray(buffer);
        Assert.assertEquals(ByteBuffer.wrap(bytes), buffer);
        buffer.getInt();
        bytes = ByteBuffers.getByteArray(buffer);
        Assert.assertEquals(4, bytes.length);
        Assert.assertEquals(4, buffer.position());
        Assert.assertEquals(4, buffer.remaining());
        buffer.getShort();
        bytes = ByteBuffers.getByteArray(buffer);
        Assert.assertEquals(2, bytes.length);
    }

    /**
     * <strong>Goal:</strong> Verify that
     * {@link ByteBuffers#getUtf8String(ByteBuffer)} drops malformed bytes
     * instead of replacing them, and reads every byte from the position to the
     * limit of the buffer.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Wrap {@code x}, {@code caf}, the two bytes of U+00E9, the invalid
     * byte {@code 0xFF}, the lead byte {@code 0xC3} with no continuation byte,
     * {@code !} and {@code z} in a buffer.</li>
     * <li>Set the position after {@code x} and the limit before {@code z}.</li>
     * <li>Call {@link ByteBuffers#getUtf8String(ByteBuffer)}.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The result is {@code caf}, U+00E9 and
     * {@code !}, and the position of the buffer is at its limit.
     */
    @Test
    public void testGetUtf8StringDropsMalformedBytes() {
        byte[] bytes = { 'x', 'c', 'a', 'f', (byte) 0xC3, (byte) 0xA9,
                (byte) 0xFF, (byte) 0xC3, '!', 'z' };
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        buffer.position(1);
        buffer.limit(bytes.length - 1);
        Assert.assertEquals("caf\u00e9!", ByteBuffers.getUtf8String(buffer));
        Assert.assertEquals(bytes.length - 1, buffer.position());
    }

    /**
     * <strong>Goal:</strong> Verify that threads that call
     * {@link ByteBuffers#getUtf8String(ByteBuffer)} at the same time each get
     * the string for their own bytes.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Give each of 16 threads its own string, of its own length, made of
     * one, two, three and four byte characters.</li>
     * <li>Release every thread at once with a {@link CountDownLatch}.</li>
     * <li>Have each thread decode the UTF-8 bytes of its string 10,000 times
     * and count each result that equals its string.</li>
     * <li>Join each thread with a timeout.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every thread finished, and every decode
     * returned the string of the thread that made it.
     */
    @Test
    public void testGetUtf8StringReturnsEachThreadsStringWhenThreadsDecodeAtOnce()
            throws InterruptedException {
        int threads = 16;
        int decodes = 10000;
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger matches = new AtomicInteger();
        List<Thread> workers = new ArrayList<>();
        for (int i = 0; i < threads; ++i) {
            String expected = Strings.repeat("a\u00e9\u20ac\uD83D\uDE00", i + 1)
                    + i;
            byte[] bytes = expected.getBytes(StandardCharsets.UTF_8);
            Thread worker = new Thread(() -> {
                try {
                    start.await();
                    for (int j = 0; j < decodes; ++j) {
                        String actual = ByteBuffers
                                .getUtf8String(ByteBuffer.wrap(bytes));
                        if(expected.equals(actual)) {
                            matches.incrementAndGet();
                        }
                    }
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
            worker.start();
            workers.add(worker);
        }
        start.countDown();
        for (Thread worker : workers) {
            worker.join(TimeUnit.SECONDS.toMillis(30));
            Assert.assertFalse(worker.isAlive());
        }
        Assert.assertEquals(threads * decodes, matches.get());
    }

    /**
     * <strong>Goal:</strong> Verify that a call to
     * {@link ByteBuffers#getUtf8String(ByteBuffer)} that throws leaves the next
     * call on the same thread correct.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Decode a {@code null} buffer and catch the error.</li>
     * <li>On the same thread, decode the UTF-8 bytes of a string made of one,
     * two, three and four byte characters.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The first call throws a
     * {@link NullPointerException}, and the second returns the string.
     */
    @Test
    public void testGetUtf8StringDecodesAfterAFailedDecodeOnTheSameThread() {
        NullPointerException error = null;
        try {
            ByteBuffers.getUtf8String(null);
        }
        catch (NullPointerException e) {
            error = e;
        }
        Assert.assertNotNull(error);
        String expected = "caf\u00e9 \u20ac\uD83D\uDE00";
        Assert.assertEquals(expected, ByteBuffers.getUtf8String(
                ByteBuffer.wrap(expected.getBytes(StandardCharsets.UTF_8))));
    }

}
