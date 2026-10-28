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
package com.cinchapi.common.process;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.process.Processes.ProcessResult;
import com.google.common.collect.ImmutableList;

/**
 * Unit tests for {@link Processes}.
 *
 * @author Jeff Nelson
 */
public class ProcessesTest {

    /**
     * <strong>Goal:</strong> Verify that {@link Processes#waitFor(Process)}
     * returns every line a {@link Process} wrote, even when the {@link Process}
     * exits before the call begins.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Start a shell that writes {@code out} to standard output and
     * {@code err} to standard error, then exits.</li>
     * <li>Wait for the shell to exit with {@link Process#waitFor()}.</li>
     * <li>Call {@link Processes#waitFor(Process)} on it.</li>
     * <li>Repeat 100 times.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every {@link ProcessResult} has exit code
     * {@code 0}, standard output {@code [out]} and standard error
     * {@code [err]}.
     *
     * @throws Exception if a shell cannot start or the wait is interrupted
     * @see <a href="https://github.com/cinchapi/accent4j/issues/53">GH-53</a>
     */
    @Test
    public void testGH53WaitForReturnsAllOutputWhenProcessAlreadyExited()
            throws Exception {
        for (int i = 0; i < 100; ++i) {
            Process process = new ProcessBuilder("/bin/sh", "-c",
                    "echo out; echo err >&2").start();
            process.waitFor();
            ProcessResult result = Processes.waitFor(process);
            Assert.assertEquals(0, result.exitCode());
            Assert.assertEquals(ImmutableList.of("out"), result.out());
            Assert.assertEquals(ImmutableList.of("err"), result.err());
        }
    }

}
