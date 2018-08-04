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
package com.cinchapi.common.base.compress;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

import com.cinchapi.common.base.CheckedExceptions;
import com.cinchapi.common.io.ByteBuffers;

/**
 * Functions for compressing and decompressing data.
 * 
 * @author Jeff Nelson
 */
public final class Compression {

    /**
     * Compress the {@code data} using the default {@link Strategy} and
     * {@link Technique}.
     * 
     * @param data the data to compress
     * @return a {@link ByteBuffer} with the compressed data
     */
    public static ByteBuffer compress(ByteBuffer data) {
        return compress(data, Strategy.BEST_COMPRESSION, Technique.FILTERED);
    }

    /**
     * Compress the {@code data} using the provided {@code strategy} and
     * {@code technique}.
     * 
     * @param data the data to compress
     * @param strategy the compression {@link Strategy}
     * @param technique the compression {@link Technique}
     * @return a {@link ByteBuffer} with the compressed data
     */
    public static ByteBuffer compress(ByteBuffer data, Strategy strategy,
            Technique technique) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Deflater deflater = new Deflater();
            deflater.setLevel(strategy.strategy());
            deflater.setStrategy(technique.technique());
            deflater.setInput(ByteBuffers.toByteArray(data));
            deflater.finish();

            byte[] buffer = new byte[1024];
            while (!deflater.finished()) {
                int count = deflater.deflate(buffer);
                baos.write(buffer, 0, count);
            }
            ByteBuffer compressed = ByteBuffer.wrap(baos.toByteArray());
            baos.close();
            return compressed;
        }
        catch (IOException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * Decompress the {@code data}.
     * 
     * @param data the data to decompress
     * @return a {@link ByteBuffer} with the decompressed data
     */
    public static ByteBuffer decompress(ByteBuffer data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Inflater inflater = new Inflater();
            inflater.setInput(ByteBuffers.toByteArray(data));
            byte[] buffer = new byte[1024];
            while (!inflater.finished()) {
                int count = inflater.inflate(buffer);
                baos.write(buffer, 0, count);
            }
            ByteBuffer decompressed = ByteBuffer.wrap(baos.toByteArray());
            baos.close();
            return decompressed;
        }
        catch (IOException | DataFormatException e) {
            throw CheckedExceptions.wrapAsRuntimeException(e);
        }
    }

    /**
     * A list of strategies that can be used in the
     * {@link Compression#compress(ByteBuffer, Strategy, Technique)} method.
     * 
     * @author Jeff Nelson
     */
    public enum Strategy {
        BEST_COMPRESSION(Deflater.BEST_COMPRESSION),
        BEST_SPEED(Deflater.BEST_SPEED),
        DEFAULT(Deflater.DEFAULT_COMPRESSION);

        int strategy;

        Strategy(int level) {
            this.strategy = level;
        }

        private int strategy() {
            return strategy;
        }
    }

    /**
     * A list of techniques that can be used in the
     * {@link Compression#compress(ByteBuffer, Strategy, Technique)} method.
     * 
     * @author Jeff Nelson
     */
    public enum Technique {
        DEFAULT(Deflater.DEFAULT_STRATEGY),
        FILTERED(Deflater.FILTERED),
        HUFFMAN_ONLY(Deflater.HUFFMAN_ONLY);

        int technique;

        Technique(int level) {
            this.technique = level;
        }

        private int technique() {
            return technique;
        }
    }

    private Compression() {/* no-op */}

}
