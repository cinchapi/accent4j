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
package com.cinchapi.common.logging;

import java.io.File;

import javax.annotation.concurrent.Immutable;

import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.FixedWindowRollingPolicy;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.SizeBasedTriggeringPolicy;

/**
 * A programmatically configurable logging facade.
 * 
 * @author Jeff Nelson
 */
@Immutable
public final class Logger {

    /**
     * Return a builder for constructing a new {@link Logger} instance.
     * 
     * @return the builder
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * The internal logger for the {@link #debug(String, Object...)} method.
     */
    private final ch.qos.logback.classic.Logger debug;

    /**
     * The directory where the log files are placed.
     */
    private final String directory;

    /**
     * A flag that indicates whether the Logger should log to the console.
     */
    private final boolean enableConsoleLogging;

    /**
     * The internal logger for the {@link #error(String, Object...)} method.
     */
    private final ch.qos.logback.classic.Logger error;

    /**
     * The internal logger for the {@link #info(String, Object...)} method.
     */
    private final ch.qos.logback.classic.Logger info;

    /**
     * The level at which the Logger is configured.
     */
    private final Level level;

    /**
     * The max file size of a log before its rolled over and archived.
     */
    private final String maxFileSize;

    /**
     * The internal logger for the {@link #warn(String, Object...)} method.
     */
    private final ch.qos.logback.classic.Logger warn;

    /**
     * Construct a new instance.
     * 
     * @param name
     * @param level
     * @param directory
     * @param maxFileSize
     */
    private Logger(String name, Level level, String directory,
            String maxFileSize, boolean enableConsoleLogging) {
        Preconditions.checkNotNull(name);
        Preconditions.checkNotNull(level);
        Preconditions.checkNotNull(directory);
        Preconditions.checkNotNull(maxFileSize);
        this.level = level;
        this.directory = directory;
        this.maxFileSize = maxFileSize;
        this.enableConsoleLogging = enableConsoleLogging;
        this.info = setup(name + ":info", "info.log");
        this.error = setup(name + ":error", "error.log");
        this.warn = setup(name + ":warn", "warn.log");
        this.debug = setup(name + ":debug", "debug.log");
    }

    /**
     * Print a message to the DEBUG log.
     * 
     * @param format the message format
     * @param params the message parameters
     */
    public void debug(String format, Object... params) {
        debug.debug(format, params);
    }

    /**
     * Print a message to the ERROR log.
     * 
     * @param format the message format
     * @param params the message parameters
     */
    public void error(String format, Object... params) {
        error.error(format, params);
    }

    /**
     * Print a message to the INFO log.
     * 
     * @param format the message format
     * @param params the message parameters
     */
    public void info(String format, Object... params) {
        info.info(format, params);
    }

    /**
     * Intercept the logging for {@code clazz} and route it to this Logger at
     * the specified {@code level}.
     * 
     * @param clazz the {@link Class} whose logs should be intercepted
     * @param level the {@link Level} to use for the class's logs
     */
    public void intercept(Class<?> clazz, Level level) {
        setup(clazz.getName(), level.levelStr.toLowerCase() + ".log");
    }

    /**
     * Print a message to the WARN log.
     * 
     * @param format the message format
     * @param params the message parameters
     */
    public void warn(String format, Object... params) {
        warn.warn(format, params);
    }

    /**
     * Programmatically configure an internal logger.
     * 
     * @param name the name of the internal logger
     * @param file the the internal logger uses
     * @return the internal logger
     */
    private ch.qos.logback.classic.Logger setup(String name, String file) {
        if(enableConsoleLogging) {
            ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                    .getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
            root.detachAndStopAllAppenders();
        }
        LoggerContext context = (LoggerContext) LoggerFactory
                .getILoggerFactory();
        // Configure Pattern
        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setPattern("%date [%thread] %level - %msg%n");
        encoder.setContext(context);
        encoder.start();

        // Create File Appender
        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<ILoggingEvent>();
        appender.setFile(directory + File.separator + file);
        appender.setContext(context);

        // Configure Rolling Policy
        FixedWindowRollingPolicy rolling = new FixedWindowRollingPolicy();
        rolling.setMaxIndex(1);
        rolling.setMaxIndex(5);
        rolling.setContext(context);
        rolling.setFileNamePattern(directory + File.separator + file
                + ".%i.zip");
        rolling.setParent(appender);
        rolling.start();

        // Configure Triggering Policy
        SizeBasedTriggeringPolicy<ILoggingEvent> triggering = new SizeBasedTriggeringPolicy<ILoggingEvent>();
        triggering.setMaxFileSize(maxFileSize);
        triggering.start();

        // Configure File Appender
        appender.setEncoder(encoder);
        appender.setRollingPolicy(rolling);
        appender.setTriggeringPolicy(triggering);
        appender.start();

        // Get Logger
        ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(name);
        logger.addAppender(appender);
        logger.setLevel(level);
        logger.setAdditive(true);
        return logger;
    }

    /**
     * The builder...
     * 
     * @author Jeff Nelson
     */
    public static class Builder {
        private String directory;

        private boolean enableConsoleLogging = false;
        private Level level = Level.INFO;
        private String maxFileSize = "10MB";
        private String name;

        private Builder() {/* no-op */}

        /**
         * Builder the {@link Logger}.
         * 
         * @return the Logger
         */
        public Logger build() {
            return new Logger(name, level, directory, maxFileSize,
                    enableConsoleLogging);
        }

        /**
         * Set the directory in which the log files are placed.
         * 
         * @param directory the log directory
         * @return the builder
         */
        public Builder directory(String directory) {
            this.directory = directory;
            return this;
        }

        /**
         * Specify whether the returned {@link Logger} should log to the
         * console.
         * 
         * @param enable a boolean that indicates if consle logging should be
         *            enabled
         * @return this
         */
        public Builder enableConsoleLogging(boolean enable) {
            this.enableConsoleLogging = enable;
            return this;
        }

        /**
         * Set the log {@link Level level} of the returned instance.
         * 
         * @param level the log level
         * @return the builder
         */
        public Builder level(Level level) {
            this.level = level;
            return this;
        }

        /**
         * Set the maximum size to allow a log file to grow before rolling.
         * 
         * @param maxFileSize the max log file size
         * @return the builder
         */
        public Builder maxFileSize(String maxFileSize) {
            this.maxFileSize = maxFileSize;
            return this;
        }

        /**
         * Set the name of the returned {@link Logger} instance.
         * 
         * @param name the logger name
         * @return the builder
         */
        public Builder name(String name) {
            this.name = name;
            return this;
        }
    }

}
