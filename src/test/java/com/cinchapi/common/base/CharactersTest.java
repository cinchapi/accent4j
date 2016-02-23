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


import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link Characters} util class.
 * 
 * @author Jeff Nelson
 */
public class CharactersTest {
    
    @Test
    public void testIsEscapeSequence(){
        Assert.assertTrue(Characters.isEscapeSequence('\t'));
        Assert.assertTrue(Characters.isEscapeSequence('\b'));
        Assert.assertTrue(Characters.isEscapeSequence('\n'));
        Assert.assertTrue(Characters.isEscapeSequence('\r'));
        Assert.assertTrue(Characters.isEscapeSequence('\f'));
        Assert.assertTrue(Characters.isEscapeSequence('\''));
        Assert.assertTrue(Characters.isEscapeSequence('\"'));
        Assert.assertTrue(Characters.isEscapeSequence('\\'));
        Assert.assertFalse(Characters.isEscapeSequence('q'));
        Assert.assertFalse(Characters.isEscapeSequence('t'));
    }
    
    @Test
    public void testGetEscapedCharacterOrNull(){
        Assert.assertEquals('t', Characters.getEscapedCharOrNullLiteral('\t'));
        Assert.assertEquals('b', Characters.getEscapedCharOrNullLiteral('\b'));
        Assert.assertEquals('n', Characters.getEscapedCharOrNullLiteral('\n'));
        Assert.assertEquals('r', Characters.getEscapedCharOrNullLiteral('\r'));
        Assert.assertEquals('f', Characters.getEscapedCharOrNullLiteral('\f'));
        Assert.assertEquals('\'', Characters.getEscapedCharOrNullLiteral('\''));
        Assert.assertEquals('"', Characters.getEscapedCharOrNullLiteral('\"'));
        Assert.assertEquals('\\', Characters.getEscapedCharOrNullLiteral('\\'));
        Assert.assertEquals('0', Characters.getEscapedCharOrNullLiteral('t'));
        Assert.assertEquals('0', Characters.getEscapedCharOrNullLiteral('q'));
    }

}
