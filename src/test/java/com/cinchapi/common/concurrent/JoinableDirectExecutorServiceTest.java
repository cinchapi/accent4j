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
package com.cinchapi.common.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

/**
 * Unit tests for the {@link JoinableExecutorService} that
 * {@link JoinableExecutorService#direct()} returns.
 *
 * @author Jeff Nelson
 */
public class JoinableDirectExecutorServiceTest extends
        AbstractExecutorServiceTest {

    /**
     * <strong>Goal:</strong> Verify that a join of indices on a direct
     * executor runs each index once, in order, on the calling thread.
     * <p>
     * <strong>Start state:</strong> A direct executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join five indices, each of which records itself.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> The indices ran once each, from {@code 0} to
     * {@code 4}.
     */
    @Test
    public void testJoinIndicesRunsEachIndexInOrder() {
        List<Integer> ran = new ArrayList<>();
        JoinableExecutorService.direct().join(5, ran::add);
        Assert.assertEquals(ImmutableList.of(0, 1, 2, 3, 4), ran);
    }

    /**
     * <strong>Goal:</strong> Verify that a join of indices on a direct
     * executor reports each run that throws, with its index, only after every
     * index runs.
     * <p>
     * <strong>Start state:</strong> A direct executor.
     * <p>
     * <strong>Workflow:</strong>
     * <ul>
     * <li>Join five indices, of which index 1 throws, and record the order of
     * the runs and the reports.</li>
     * </ul>
     * <p>
     * <strong>Expected:</strong> Every other index runs before the error
     * handler receives index 1.
     */
    @Test
    public void testJoinIndicesReportsErrorsAfterEveryIndexRuns() {
        List<String> events = new ArrayList<>();
        JoinableExecutorService.direct().join(
                (index, error) -> events.add("failed " + index), 5, index -> {
                    if(index == 1) {
                        throw new IllegalStateException();
                    }
                    else {
                        events.add("ran " + index);
                    }
                });
        Assert.assertEquals(ImmutableList.of("ran 0", "ran 2", "ran 3",
                "ran 4", "failed 1"), events);
    }

    @Override
    protected ExecutorService $getExecutorService() {
        return JoinableExecutorService.direct();
    }

}
