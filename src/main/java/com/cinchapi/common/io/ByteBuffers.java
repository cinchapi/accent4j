/*
 * Copyright (c) 2013-2015 Cinchapi Inc.
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
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.cinchapi.common.base.CheckedExceptions;
import com.cinchapi.common.collect.concurrent.ConcurrentLoadingQueue;
import com.google.common.base.Preconditions;

/**
 * Additional utility methods for ByteBuffers that are not found in the
 * {@link ByteBuffer} class.
 * 
 * @author Jeff Nelson
 */
public abstract class ByteBuffers {

    /**
     * The name of the Charset to use for encoding/decoding. We use the name
     * instead of the charset object because Java caches encoders when
     * referencing them by name, but creates a new encorder object when
     * referencing them by Charset object.
     */
    private static final String CHARSET = StandardCharsets.UTF_8.name();

    /**
     * A collection of UTF-8 decoders that can be concurrently used. We use this
     * to avoid creating a new decoder every time we need to decode a string
     * while still allowing multi-threaded access.
     */
    private static final ConcurrentLinkedQueue<CharsetDecoder> DECODERS = ConcurrentLoadingQueue
            .create(new Callable<CharsetDecoder>() {

                @Override
                public CharsetDecoder call() throws Exception {
                    return StandardCharsets.UTF_8.newDecoder();
                }

            });

    /**
     * The number of UTF-8 decoders to create for concurrent access.
     */
    private static final int NUM_DECODERS = 10;

    static {
        try {
            for (int i = 0; i < NUM_DECODERS; ++i) {
                DECODERS.add(StandardCharsets.UTF_8.newDecoder());
            }
        }
        catch (Exception e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Return a byte array containing all the {@link ByteBuffer#remaining()}
     * content within the {@code buffer}. The {@code buffer}'s mark is set to
     * the current position. The {@code buffer}'s limit is preserved.
     * 
     * @param buffer
     * @return a byte array containing the remaining the content in the
     *         {@code buffer}.
     */
    public static byte[] getByteArray(ByteBuffer buffer) {
        buffer.mark();
        byte[] array = new byte[buffer.remaining()];
        buffer.get(array);
        buffer.reset();
        return array;
    }

    /**
     * Return a ByteBuffer that is a new read-only buffer that shares the
     * content of {@code source} and has the same byte order, but maintains a
     * distinct position, mark and limit.
     * 
     * @param source
     * @return the new, read-only byte buffer
     */
    public static ByteBuffer asReadOnlyBuffer(ByteBuffer source) {
        int position = source.position();
        source.rewind();
        ByteBuffer duplicate = source.asReadOnlyBuffer();
        duplicate.order(source.order()); // byte order is not natively preserved
                                         // when making duplicates:
                                         // http://blog.mustardgrain.com/2008/04/04/bytebufferduplicate-does-not-preserve-byte-order/
        source.position(position);
        duplicate.rewind();
        return duplicate;
    }

    /**
     * Return a clone of {@code buffer} that has a copy of <em>all</em> its
     * content and the same position and limit. Unlike the
     * {@link ByteBuffer#slice()} method, the returned clone
     * <strong>does not</strong> share its content with {@code buffer}, so
     * subsequent operations to {@code buffer} or its clone will be
     * completely independent and won't affect the other.
     * 
     * @param buffer
     * @return a clone of {@code buffer}
     */
    public static ByteBuffer clone(ByteBuffer buffer) {
        ByteBuffer clone = ByteBuffer.allocate(buffer.capacity());
        int position = buffer.position();
        int limit = buffer.limit();
        buffer.rewind();
        clone.put(buffer);
        buffer.position(position);
        clone.position(position);
        buffer.limit(limit);
        clone.limit(limit);
        return clone;
    }

    /**
     * Transfer the bytes from {@code source} to {@code destination} and resets
     * {@code source} so that its position remains unchanged. The position of
     * the {@code destination} is incremented by the number of bytes that are
     * transferred.
     * 
     * @param source
     * @param destination
     */
    public static void copyAndRewindSource(ByteBuffer source,
            ByteBuffer destination) {
        int position = source.position();
        destination.put(source);
        source.position(position);
    }

    /**
     * Decode the {@code hex}adeciaml string and return the resulting binary
     * data.
     * 
     * @param hex
     * @return the binary data
     */
    public static ByteBuffer decodeFromHex(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return ByteBuffer.wrap(data);
    }

    /**
     * Encode the remaining bytes in as {@link ByteBuffer} as a hex string and
     * maintain the current position.
     * 
     * @param buffer
     * @return the hex string
     */
    public static String encodeAsHex(ByteBuffer buffer) {
        StringBuilder sb = new StringBuilder();
        buffer.mark();
        while (buffer.hasRemaining()) {
            sb.append(String.format("%02x", buffer.get()));
        }
        buffer.reset();
        return sb.toString();
    }

    /**
     * Return a byte buffer that has the UTF-8 encoding for {@code string}. This
     * method uses some optimization techniques and is the preferable way to
     * convert strings to byte buffers than doing so manually.
     * 
     * @param string
     * @return the byte buffer with the {@code string} data.
     */
    public static ByteBuffer fromUtf8String(String string) {
        try {
            return ByteBuffer.wrap(string.getBytes(CHARSET));
        }
        catch (Exception e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Return a ByteBuffer that has a copy of {@code length} bytes from
     * {@code buffer} starting from the current position. This method will
     * advance the position of the source buffer.
     * 
     * @param buffer
     * @param length
     * @return a ByteBuffer that has {@code length} bytes from {@code buffer}
     */
    public static ByteBuffer get(ByteBuffer source, int length) {
        Preconditions.checkArgument(source.remaining() >= length,
                "The number of bytes remaining in the buffer cannot be less than length");
        byte[] backingArray = new byte[length];
        source.get(backingArray);
        ByteBuffer destination = ByteBuffer.wrap(backingArray);
        destination.order(source.order());
        return destination;
    }

    /**
     * Relative <em>get</em> method. Reads the byte at the current position in
     * {@code buffer} as a boolean, and then increments the position.
     * 
     * @param buffer
     * @return the boolean value at the current position
     */
    public static boolean getBoolean(ByteBuffer buffer) {
        return buffer.get() > 0 ? true : false;
    }

    /**
     * Relative <em>get</em> method. Reads the enum at the current position in
     * {@code buffer} and then increments the position by four.
     * 
     * @param buffer
     * @param clazz
     * @return the enum value at the current position
     */
    public static <T extends Enum<?>> T getEnum(ByteBuffer buffer,
            Class<T> clazz) {
        return clazz.getEnumConstants()[buffer.getInt()];
    }

    /**
     * Relative <em>get</em> method. Reads the {@code charset} encoded string at
     * the current position in {@code buffer}.
     * 
     * @param buffer
     * @param charset
     * @return the string value at the current position
     */
    public static String getString(ByteBuffer buffer, Charset charset) {
        CharsetDecoder decoder = null;
        try {
            if(charset == StandardCharsets.UTF_8) {
                while (decoder == null) {
                    decoder = DECODERS.poll();
                }
            }
            else {
                decoder = charset.newDecoder();
            }
            decoder.onMalformedInput(CodingErrorAction.IGNORE);
            return decoder.decode(buffer).toString();
        }
        catch (CharacterCodingException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
        finally {
            if(decoder != null && charset == StandardCharsets.UTF_8) {
                DECODERS.offer(decoder);
            }
        }
    }

    /**
     * Relative <em>get</em> method. Reads the UTF-8 encoded string at
     * the current position in {@code buffer}.
     * 
     * @param buffer
     * @return the string value at the current position
     */
    public static String getUtf8String(ByteBuffer buffer) {
        return getString(buffer, StandardCharsets.UTF_8);
    }

    /**
     * Put the UTF-8 encoding for the {@code source} string into the
     * {@code destination} byte buffer and increment the position by the length
     * of the strings byte sequence. This method uses some optimization
     * techniques and is the preferable way to add strings to byte buffers than
     * doing so manually.
     * 
     * @param source
     * @param destination
     */
    public static void putUtf8String(String source, ByteBuffer destination) {
        try {
            byte[] bytes = source.getBytes(CHARSET);
            destination.put(bytes);
        }
        catch (Exception e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Return a new ByteBuffer whose content is a shared subsequence of the
     * content in {@code buffer} starting at the current position to
     * current position + {@code length} (non-inclusive). Invoking this method
     * has the same affect as doing the following:
     * 
     * <pre>
     * buffer.mark();
     * int oldLimit = buffer.limit();
     * buffer.limit(buffer.position() + length);
     * 
     * ByteBuffer slice = buffer.slice();
     * 
     * buffer.reset();
     * buffer.limit(oldLimit);
     * </pre>
     * 
     * @param buffer
     * @param length
     * @return the new ByteBuffer slice
     * @see ByteBuffer#slice()
     */
    public static ByteBuffer slice(ByteBuffer buffer, int length) {
        return slice(buffer, buffer.position(), length);
    }

    /**
     * Return a new ByteBuffer whose content is a shared subsequence of the
     * content in {@code buffer} starting at {@code position} to
     * {@code position} + {@code length} (non-inclusive). Invoking this method
     * has the same affect as doing the following:
     * 
     * <pre>
     * buffer.mark();
     * int oldLimit = buffer.limit();
     * buffer.position(position);
     * buffer.limit(position + length);
     * 
     * ByteBuffer slice = buffer.slice();
     * 
     * buffer.reset();
     * buffer.limit(oldLimit);
     * </pre>
     * 
     * @param buffer
     * @param position
     * @param length
     * @return the new ByteBuffer slice
     * @see ByteBuffer#slice()
     */
    public static ByteBuffer slice(ByteBuffer buffer, int position,
            int length) {
        int oldPosition = buffer.position();
        int oldLimit = buffer.limit();
        buffer.position(position);
        buffer.limit(position + length);
        ByteBuffer slice = buffer.slice();
        buffer.limit(oldLimit);
        buffer.position(oldPosition);
        return slice;
    }

    /**
     * Return a byte array with the content of {@code buffer}. This method
     * returns the byte array that backs {@code buffer} if one exists, otherwise
     * it creates a new byte array with the content between the current position
     * of {@code buffer} and its limit.
     * 
     * @param buffer
     * @return the byte array with the content of {@code buffer}
     * @deprecated in favor of {@link #array(ByteBuffer)} because this method
     *             returns the entire backing byte array instead of a byte array
     *             only containing the remaining content if the buffer is backed
     *             by a byte array
     */
    @Deprecated
    public static byte[] toByteArray(ByteBuffer buffer) {
        if(buffer.hasArray()) {
            return buffer.array();
        }
        else {
            buffer.mark();
            byte[] array = new byte[buffer.remaining()];
            buffer.get(array);
            buffer.reset();
            return array;
        }
    }

    /**
     * Return a {@link CharBuffer} representation of the bytes in the
     * {@code buffer} encoded with the {@code charset}.
     * 
     * @param buffer
     * @param charset
     * @return the char buffer
     */
    public static CharBuffer toCharBuffer(ByteBuffer buffer, Charset charset) {
        buffer.mark();
        CharBuffer chars = charset.decode(buffer);
        buffer.reset();
        return chars;
    }
    /**
     * Return a UTF-8 {@link CharBuffer} representation of the bytes in the
     * {@code buffer}.
     * 
     * @param buffer
     * @return the char buffer
     */
    public static CharBuffer toUtf8CharBuffer(ByteBuffer buffer) {
        return toCharBuffer(buffer, StandardCharsets.UTF_8);
    }

}
