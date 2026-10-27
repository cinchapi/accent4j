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
package com.cinchapi.common.describe;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

/**
 * Unit test for {@link Empty}.
 *
 * @author Jeff Nelson
 */
public class EmptyTest {
    
    @Test
    public void testEmptyUseClosestCommonAncestor() {
        Empty empty = Empty.is();
        empty.define(HashSet.class, object -> true);
        empty.define(Set.class, object -> false);
        empty.define(Collection.class, object -> true);
        Assert.assertTrue(empty.describes(Sets.newHashSet()));
        Assert.assertFalse(empty.describes(Sets.newTreeSet()));
        Assert.assertTrue(empty.describes(Lists.newArrayList()));
    }
    
    @Test
    public void testEmptyNull() {
        Empty empty = Empty.is();
        Assert.assertTrue(empty.describes(null));
    }
    
    @Test
    public void testEmptyDefaults() {
        Empty empty = Empty.is();
        Assert.assertTrue(empty.describes(""));
        Assert.assertTrue(empty.describes(Lists.newArrayList()));
        Assert.assertTrue(empty.describes(new String[] {}));
    }

}
