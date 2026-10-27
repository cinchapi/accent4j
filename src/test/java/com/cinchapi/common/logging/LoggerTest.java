/*
 * Copyright (c) 2013-2026 Cinchapi Inc.
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
package com.cinchapi.common.logging;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for {@link Logger}.
 *
 * @author Jeff Nelson
 */
public class LoggerTest {

    /**
     * <strong>Goal:</strong> Verify that {@link Logger#none()} returns one
     * shared {@link Logger}, so a caller that asks for it often does not create
     * a new set of log files on each call.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Call {@link Logger#none()} 100 times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every call returns the same {@link Logger}.
     *
     * @see <a href="https://github.com/cinchapi/accent4j/issues/49">GH-49</a>
     */
    @Test
    public void testGH49NoneReturnsTheSameLoggerOnEveryCall() {
        Logger expected = Logger.none();
        for (int i = 0; i < 100; ++i) {
            Assert.assertSame(expected, Logger.none());
        }
    }

}
