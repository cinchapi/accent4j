/*
 * Copyright (c) 2015 Cinchapi Inc.
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

/**
 * Unit tests for the {@link Verify} functions.
 * 
 * @author Jeff Nelson
 */
public class VerifyTest {
    
    @Rule
    public ExpectedException ex = ExpectedException.none();

    @Test
    public void verifyThatTrue() {
        Verify.that(1 < 2);
    }
    
    @Test
    public void verifyThatFalse(){
        ex.expect(IllegalStateException.class);
        Verify.that(1 > 2);
    }
    
    @Test
    public void verifyThatFalseMsg(){
        ex.expect(IllegalStateException.class);
        ex.expectMessage("1 is not greater than 2");
        Verify.that(1 > 2, "{} is not greater than {}", 1, 2);
    }
    
    @Test
    public void verifyIsTypeTrue(){
        Verify.isType(new ArrayList<Object>(), List.class);
    }
    
    @Test
    public void verifyIsTypeFalse(){
        ex.expect(ClassCastException.class);
        Verify.isType(new ArrayList<Object>(), Set.class);
    }
    
    @Test
    public void verifyThatArgumentTrue(){
        Verify.thatArgument(1 < 2);
    }
    
    @Test
    public void verifyThatArgumentFalse(){
        ex.expect(IllegalArgumentException.class);
        Verify.thatArgument(1 > 2);
    }

}
