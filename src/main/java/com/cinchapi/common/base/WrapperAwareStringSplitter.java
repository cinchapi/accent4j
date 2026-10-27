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
package com.cinchapi.common.base;

/**
 * A {@link StringSplitter} that is aware of a "wrapper" (e.g. a beginning and
 * end character group that captures a sequence of characters that should not be
 * split, even if a delimiter appears within the sequence).
 *
 * @author Jeff Nelson
 */
public class WrapperAwareStringSplitter extends StringSplitter {

    /**
     * Return a {@link StringSplitter} that does not split on a delimiter that
     * occurs between brackets.
     * 
     * @param string
     * @return the {@link StringSplitter}
     */
    public static StringSplitter bracketAware(String string) {
        return new WrapperAwareStringSplitter('{', '}', string);
    }

    /**
     * Return a {@link StringSplitter} that does not split on a delimiter that
     * occurs between brackets.
     * 
     * @param string
     * @param delimiter
     * @return the {@link StringSplitter}
     */
    public static StringSplitter bracketAware(String string, char delimiter) {
        return new WrapperAwareStringSplitter('{', '}', string, delimiter);
    }

    /**
     * Return a {@link StringSplitter} that does not split on a delimiter that
     * occurs between brackets.
     * 
     * @param string
     * @param delimiter
     * @param options
     * @return the {@link StringSplitter}
     */
    public static StringSplitter bracketAware(String string, char delimiter,
            SplitOption... options) {
        return new WrapperAwareStringSplitter('{', '}', string, delimiter,
                options);
    }

    /**
     * Return a {@link StringSplitter} that does not split on a delimiter that
     * occurs between brackets.
     * 
     * @param string
     * @param options
     * @return the {@link StringSplitter}
     */
    public static StringSplitter bracketAware(String string,
            SplitOption... options) {
        return new WrapperAwareStringSplitter('{', '}', string, options);
    }

    /**
     * The number of times the {@link #wrapperStart} character has occurred in
     * the string. The value of {@link #wrapperStartCount} and
     * {@link #wrapperEndCount} must be equal for {@link #isReadyToSplit()} to
     * return {@code true}.
     */
    private int wrapperStartCount = 0;

    /**
     * The character that indicates that the following characters are wrapped
     * and therefore not subject to the split rules.
     */
    private final char wrapperStart;

    /**
     * The character that indicates that the following characters are no longer
     * wrapped and therefore subject to the split rules.
     */
    private final char wraperEnd;

    /**
     * The number of times the {@link #wrapperEnd} character has occurred in
     * the string. The value of {@link #wrapperStartCount} and
     * {@link #wrapperEndCount} must be equal for {@link #isReadyToSplit()} to
     * return {@code true}.
     */
    private int wrapperEndCount = 0;

    /**
     * Construct a new instance.
     * 
     * @param string string the string to split
     */
    public WrapperAwareStringSplitter(char wrapperStart, char wrapperEnd,
            String string) {
        super(string);
        Verify.thatArgument(wrapperStart != wrapperEnd);
        this.wrapperStart = wrapperStart;
        this.wraperEnd = wrapperEnd;
        reset();
    }

    /**
     * Construct a new instance.
     * 
     * @param string string the string to split
     * @param delimiter the delimiter upon which to split
     */
    public WrapperAwareStringSplitter(char wrapperStart, char wrapperEnd,
            String string, char delimiter) {
        super(string, delimiter);
        Verify.thatArgument(wrapperStart != wrapperEnd);
        this.wrapperStart = wrapperStart;
        this.wraperEnd = wrapperEnd;
        reset();
    }

    /**
     * Construct a new instance.
     * 
     * @param string string the string to split
     * @param delimiter the delimiter upon which to split
     * @param options an array of {@link SplitOption options} to supplement the
     *            split behaviour
     */
    public WrapperAwareStringSplitter(char wrapperStart, char wrapperEnd,
            String string, char delimiter, SplitOption... options) {
        super(string, delimiter, options);
        Verify.thatArgument(wrapperStart != wrapperEnd);
        this.wrapperStart = wrapperStart;
        this.wraperEnd = wrapperEnd;
        reset();
    }

    /**
     * Construct a new instance.
     * 
     * @param string string the string to split
     * @param options an array of {@link SplitOption options} to supplement the
     *            split behaviour
     */
    public WrapperAwareStringSplitter(char wrapperStart, char wrapperEnd,
            String string, SplitOption... options) {
        super(string, options);
        Verify.thatArgument(wrapperStart != wrapperEnd);
        this.wrapperStart = wrapperStart;
        this.wraperEnd = wrapperEnd;
        reset();
    }

    @Override
    protected boolean isReadyToSplit() {
        return wrapperStartCount == wrapperEndCount;
    }

    @Override
    protected void updateIsReadyToSplit(char c) {
        if(c == wrapperStart) {
            wrapperStartCount++;
        }
        else if(c == wraperEnd) {
            wrapperEndCount++;
        }
    }

}
