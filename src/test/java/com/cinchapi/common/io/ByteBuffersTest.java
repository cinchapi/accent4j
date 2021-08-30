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

import org.junit.Assert;
import org.junit.Test;

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

}
