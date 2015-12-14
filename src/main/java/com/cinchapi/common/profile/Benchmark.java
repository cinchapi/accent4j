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
package com.cinchapi.common.profile;

import java.util.concurrent.TimeUnit;

/**
 * An abstraction for measuring the amount of time it takes to perform an
 * action.
 * <p>
 * When creating a {@link Benchmark} you simply define an {@link #action()
 * action} that is {@link #run() run once} or {@link #run(int) multiple times}.
 * As the action runs, the elapsed time is measured and returned in the
 * {@link TimeUnit} of choice.
 * </p>
 * 
 * @author Jeff Nelson
 */
public abstract class Benchmark {

    /**
     * The {@link TimeUnit} in which the elapsed time should be expressed when
     * returned to the caller.
     */
    private final TimeUnit unit;

    /**
     * Construct a new instance.
     * 
     * @param unit the desired {@link #unit time unit}
     */
    public Benchmark(TimeUnit unit) {
        this.unit = unit;
    }

    /**
     * Run the {@link #action() action} once and return the elapsed time,
     * expressed in the {@link TimeUnit} that was passed into the constructor.
     * 
     * @return the elapsed time
     */
    public final long run() {
        long start = System.nanoTime();
        action();
        long end = System.nanoTime();
        return unit.convert(end - start, TimeUnit.NANOSECONDS);
    }

    /**
     * Run the {@link #action action} the specified number of {@times} and
     * return the elapsed time, expressed in the {@link TimeUnit} that was
     * passed into the constructor.
     * 
     * @param rounds
     * @return the elapsed time
     */
    public final long run(int times) {
        long start = System.nanoTime();
        for (int i = 0; i < times; ++i) {
            action();
        }
        long end = System.nanoTime();
        return unit.convert(end - start, TimeUnit.NANOSECONDS);
    }

    public abstract void action();

}
