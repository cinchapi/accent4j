/*
 * Copyright (c) 2013-2016 Cinchapi Inc.
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

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.io.CharStreams;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * Utility functions for safely handling {@link Process} objects.
 * 
 * @author Jeff Nelson
 */
public class Processes {

    /**
     * Execute {@link Process#waitFor()} while reading everything from the
     * {@code process}'s standard out and error to prevent the process from
     * hanging.
     * 
     * @param process the {@link Process} for which to wait
     * @return a map containing all the {@link ProcessData} (e.g. exit code,
     *         stdout and stderr)
     */
    public static Map<ProcessData, Object> waitFor(Process process) {
        AtomicBoolean finished = new AtomicBoolean(false);
        List<String> stdout = Lists.newArrayList();
        List<String> stderr = Lists.newArrayList();

        try {
            // Asynchronously exhaust stdout so process doesn't hang
            executor.execute(() -> {
                try {
                    InputStreamReader reader = new InputStreamReader(process
                            .getInputStream());
                    while (!finished.get()) {
                        stdout.addAll(CharStreams.readLines(reader));
                    }
                }
                catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            });

            // Asynchronously exhaust stderr so process doesn't hang
            executor.execute(() -> {
                try {
                    InputStreamReader reader = new InputStreamReader(process
                            .getErrorStream());
                    while (!finished.get()) {
                        stderr.addAll(CharStreams.readLines(reader));
                    }
                }
                catch (IOException e) {
                    throw Throwables.propagate(e);
                }
            });
            int code = process.waitFor();
            finished.set(true);
            Map<ProcessData, Object> data = new HashMap<>(3);
            data.put(ProcessData.EXIT_CODE, code);
            data.put(ProcessData.STDOUT, stdout);
            data.put(ProcessData.STDERR, stderr);
            return data;
        }
        catch (InterruptedException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * An {@link Executor} that is used to asynchronously read input from a
     * processe's standard out and error streams.
     */
    private static final ExecutorService executor = MoreExecutors
            .getExitingExecutorService((ThreadPoolExecutor) Executors
                    .newFixedThreadPool(Runtime.getRuntime()
                            .availableProcessors() * 2));

    /**
     * All the data that can be returned about a process.
     * 
     * @author Jeff Nelson
     */
    public static enum ProcessData {
        EXIT_CODE, STDERR, STDOUT
    }
}
