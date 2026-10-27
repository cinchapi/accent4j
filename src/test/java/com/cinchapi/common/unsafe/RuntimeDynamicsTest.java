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
package com.cinchapi.common.unsafe;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link RuntimeDynamics}.
 *
 * @author Jeff Nelson
 */
public class RuntimeDynamicsTest {

    @Test
    public void testNewAnonymousObject() {
        Object obj = RuntimeDynamics.newAnonymousObject();
        Assert.assertNotNull(obj);
        Assert.assertNotEquals(Object.class, obj.getClass());
    }

    @Test
    public void testNewAnonymousObjectIsUnique() {
        Object a = RuntimeDynamics.newAnonymousObject();
        Object b = RuntimeDynamics.newAnonymousObject();
        Assert.assertNotEquals(a.getClass(), b.getClass());
    }
}
