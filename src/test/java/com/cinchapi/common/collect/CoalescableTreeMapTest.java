/*
 * Copyright (c) 2013-2019 Cinchapi Inc.
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
package com.cinchapi.common.collect;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.base.AnyStrings;
import com.google.common.collect.Lists;

/**
 * Unit tests for {@link CoalescableTreeMap}.
 *
 * @author Jeff Nelson
 */
public class CoalescableTreeMapTest {

    @Test
    public void testCoalesce() {
        Random rand = new Random();
        Comparator<String> comparator = (s1, s2) -> {
            int c = s1.compareToIgnoreCase(s2);
            if(c == 0) {
                c = s1.compareTo(s2);
            }
            return c;
        };
        CoalescableTreeMap<String, String> map = new CoalescableTreeMap<>(
                comparator);
        String[] chars = "123456789abcdefghijklmnopqrstuvwxyz".split("");
        List<String> candidates = Lists.newArrayList();
        for (String $char : chars) {
            candidates.add($char + "eff");
            if(AnyStrings.tryParseNumber($char) == null) {
                candidates.add($char.toUpperCase() + "eff");
            }
        }
        java.util.Collections.shuffle(candidates);
        for (String candidate : candidates) {
            map.put(candidate,
                    "" + System.currentTimeMillis() + rand.nextInt());
        }
        Assert.assertEquals(candidates.size(), map.size());
        Map<String, String> data = map.coalesce("jeff",
                (key, candidate) -> key.equalsIgnoreCase(candidate));
        Assert.assertEquals(2, data.size());
        Iterator<String> it = data.keySet().iterator();
        Assert.assertEquals(map.get("Jeff"), data.get(it.next()));
        Assert.assertEquals(map.get("jeff"), data.get(it.next()));
        map.coalesce("1eff", (key, candidate) -> false); // No NPE
    }

}
