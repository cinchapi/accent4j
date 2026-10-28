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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Assert;
import org.junit.Test;

import com.cinchapi.common.process.Processes.ProcessResult;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

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

    /**
     * <strong>Goal:</strong> Verify that {@link Processes#waitFor(Process)}
     * gives a failure to read a stream to its caller as an exception instead of
     * a hang.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Create a {@link Process} stub that has exited with code {@code 0},
     * whose standard output throws an {@link IOException} on each read and
     * whose standard error is empty.</li>
     * <li>Call {@link Processes#waitFor(Process)} on it.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call throws a {@link RuntimeException}
     * whose cause is the {@link IOException} from the read, within the test
     * timeout.
     *
     * @see <a href="https://github.com/cinchapi/accent4j/issues/53">GH-53</a>
     */
    @Test(timeout = 10000)
    public void testGH53WaitForThrowsWhenStreamCannotBeRead() {
        IOException failure = new IOException("read failed");
        Process process = new Process() {

            @Override
            public void destroy() {}

            @Override
            public int exitValue() {
                return 0;
            }

            @Override
            public InputStream getErrorStream() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public InputStream getInputStream() {
                return new InputStream() {

                    @Override
                    public int read() throws IOException {
                        throw failure;
                    }

                };
            }

            @Override
            public OutputStream getOutputStream() {
                return new ByteArrayOutputStream();
            }

            @Override
            public int waitFor() {
                return 0;
            }

        };
        try {
            Processes.waitFor(process);
            Assert.fail("Expected a RuntimeException");
        }
        catch (RuntimeException e) {
            Assert.assertSame(failure, e.getCause());
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Processes#waitFor(Process)}
     * leaves the calling thread's interrupt status set when it throws because
     * of an interrupt.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Start a {@code sleep 5} process.</li>
     * <li>Interrupt the current thread.</li>
     * <li>Call {@link Processes#waitFor(Process)} on the process.</li>
     * <li>Destroy the process.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The call throws a {@link RuntimeException}
     * whose cause is an {@link InterruptedException}, and the current thread is
     * still interrupted.
     *
     * @throws Exception if the process cannot start
     */
    @Test(timeout = 10000)
    public void testWaitForKeepsInterruptStatusWhenInterrupted()
            throws Exception {
        Process process = new ProcessBuilder("sleep", "5").start();
        Thread.currentThread().interrupt();
        try {
            Processes.waitFor(process);
            Assert.fail("Expected a RuntimeException");
        }
        catch (RuntimeException e) {
            Assert.assertTrue(e.getCause() instanceof InterruptedException);
            Assert.assertTrue(Thread.interrupted());
        }
        finally {
            process.destroy();
        }
    }

    /**
     * <strong>Goal:</strong> Verify that {@link Processes#waitFor(Process)}
     * returns every line of both streams to each caller when many callers wait
     * at the same time, and that no call blocks forever.
     * <p>
     * <strong>Start state:</strong> No prior state needed.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Start twice as many caller threads as there are processors, and hold
     * them on a {@link CountDownLatch}.</li>
     * <li>Release the latch. Each caller starts a shell that writes a line of
     * 100000 zeros to standard error and then the same line to standard output,
     * and calls {@link Processes#waitFor(Process)} on it.</li>
     * <li>Wait up to 30 seconds for each caller's {@link ProcessResult}.</li>
     * <li>Destroy every shell.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every {@link ProcessResult} arrives in time
     * and has the line of zeros as its only line of standard output and of
     * standard error. Each line is more than a pipe holds, so a shell whose
     * stream nobody reads cannot exit.
     *
     * @throws Exception if a shell cannot start or a caller fails
     */
    @Test(timeout = 60000)
    public void testWaitForReturnsAllOutputWhenCalledConcurrently()
            throws Exception {
        int callers = Runtime.getRuntime().availableProcessors() * 2;
        ExecutorService threads = Executors.newFixedThreadPool(callers);
        List<Process> processes = new CopyOnWriteArrayList<>();
        String command = "printf '%0100000d\\n' 0 >&2; "
                + "printf '%0100000d\\n' 0";
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<Future<ProcessResult>> results = Lists.newArrayList();
            for (int i = 0; i < callers; ++i) {
                results.add(threads.submit(() -> {
                    start.await();
                    Process process = new ProcessBuilder("/bin/sh", "-c",
                            command).start();
                    processes.add(process);
                    return Processes.waitFor(process);
                }));
            }
            start.countDown();
            String line = Strings.repeat("0", 100000);
            for (Future<ProcessResult> result : results) {
                ProcessResult r = result.get(30, TimeUnit.SECONDS);
                Assert.assertEquals(ImmutableList.of(line), r.out());
                Assert.assertEquals(ImmutableList.of(line), r.err());
            }
        }
        finally {
            // A process that is still blocked after a failure must end, so that
            // its stream readers free their threads for later tests.
            processes.forEach(Process::destroy);
            threads.shutdownNow();
        }
    }

}
