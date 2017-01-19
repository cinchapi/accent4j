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
package com.cinchapi.common.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.AbstractList;
import java.util.Iterator;

import javax.annotation.Nullable;

import com.cinchapi.common.base.ArrayBuilder;
import com.cinchapi.common.base.CheckedExceptions;
import com.cinchapi.common.base.Platform;
import com.cinchapi.common.base.ReadOnlyIterator;
import com.cinchapi.common.process.Processes;
import com.cinchapi.common.process.Processes.ProcessResult;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.hash.Hashing;

/**
 * Utility methods for working with files.
 * 
 * @author Jeff Nelson
 */
public final class Files {

    /**
     * Delete {@code directory}. If files are added to the directory while its
     * being deleted, this method will make a best effort to delete those files
     * as well.
     * 
     * @param directory
     */
    public static void deleteDirectory(String directory) {
        try (DirectoryStream<Path> stream = java.nio.file.Files
                .newDirectoryStream(Paths.get(directory))) {
            for (Path path : stream) {
                if(java.nio.file.Files.isDirectory(path)) {
                    deleteDirectory(path.toString());
                }
                else {
                    java.nio.file.Files.delete(path);
                }
            }
            java.nio.file.Files.delete(Paths.get(directory));
        }
        catch (IOException e) {
            if(e.getClass() == DirectoryNotEmptyException.class) {
                deleteDirectory(directory);
            }
            else {
                throw Throwables.propagate(e);
            }
        }
    }

    /**
     * Return the checksum for all the files in the {@code directory}.
     * 
     * @param directory the directory whose checksum is generated
     * @return the checksum of the directory
     */
    public static String directoryChecksum(String directory) {
        String hash = Platform.isMacOsX() ? "shasum -a 256" : "sha256sum";
        try {
            Process process = Processes
                    .getBuilderWithPipeSupport("find . -type f -exec " + hash
                            + " --binary {} \\; | sort -k 2 | " + hash
                            + " | cut -d ' ' -f 1")
                    .directory(Paths.get(expandPath(directory)).toFile())
                    .start();
            ProcessResult result = Processes.waitFor(process);
            if(result.exitCode() == 0) {
                return result.out().get(0);
            }
            else {
                throw new RuntimeException(result.err().toString());
            }
        }
        catch (IOException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }

    }

    /**
     * A version of the {@link #directoryChecksum(String)} method that uses
     * metadata caching for the {@code directory} to speed up checksum
     * calculations, where possible.
     * <p>
     * Do not use this method for security operations because there are subtle
     * race conditions and possible exploits that can happen when using metadata
     * caching. However, this method is appropriate to use for validating the
     * integrity of files in non-critical situations or those where the
     * likelihood of tampering is low.
     * </p>
     * 
     * @param directory the directory whose checksum is generated
     * @return the checksum of the directory
     */
    public static String directoryChecksumCached(String directory) {
        Path cache = Paths.get(USER_HOME)
                .resolve(Paths.get(".cinchapi", "accent4j", ".checksums",
                        Hashing.sha256()
                                .hashString(directory, StandardCharsets.UTF_8)
                                .toString()));
        File file = cache.toFile();
        try {
            String checksum;
            long directoryLastModified = getMostRecentFileModifiedTime(
                    directory).toMillis();
            if(file.exists() && getMostRecentFileModifiedTime(cache.toString())
                    .toMillis() > directoryLastModified) {
                checksum = Iterables
                        .getOnlyElement(readLines(cache.toString()));
            }
            else {
                checksum = directoryChecksum(directory);
                Thread.sleep(1000); // avoid race condition with file
                                    // modification timestamps because many file
                                    // systems only have granularity within 1
                                    // second.
                file.getParentFile().mkdirs();
                file.createNewFile();
                com.google.common.io.Files.write(checksum, file,
                        StandardCharsets.UTF_8);
            }
            return checksum;

        }
        catch (IOException | InterruptedException e) {
            throw CheckedExceptions.throwAsRuntimeException(e);
        }
    }

    /**
     * Expand the given {@code path} so that it contains completely normalized
     * components (e.g. ".", "..", and "~" are resolved to the correct absolute
     * paths).
     * 
     * @param path
     * @return the expanded path
     */
    public static String expandPath(String path) {
        return expandPath(path, null);
    }

    /**
     * Expand the given {@code path} so that it contains completely normalized
     * components (e.g. ".", "..", and "~" are resolved to the correct absolute
     * paths).
     * 
     * @param path
     * @param cwd
     * @return the expanded path
     */
    public static String expandPath(String path, String cwd) {
        path = path.replaceAll("~", USER_HOME);
        Path base = cwd == null || cwd.isEmpty() ? BASE_PATH
                : FileSystems.getDefault().getPath(cwd);
        return base.resolve(path).normalize().toString();
    }

    /**
     * Get a consistent file path that represents the hash for the specified
     * {@code key}.
     * <p>
     * This method does <strong>NOT</strong> create a file at the returned path,
     * or any of the parent directories.
     * </p>
     * 
     * @param key the key to hash
     * @return the hashed file path
     */
    public static Path getHashedFilePath(String key) {
        String hash = Hashing.sha256().hashString(key, StandardCharsets.UTF_8)
                .toString();
        ArrayBuilder<String> array = ArrayBuilder.builder();
        array.add(".hash");
        StringBuilder sb = new StringBuilder();
        char[] chars = hash.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            sb.append(c);
            if(i >= 4 && i % 4 == 0) {
                array.add(sb.toString());
                sb.setLength(0);
            }
        }
        return Paths.get(USER_HOME, array.build());
    }

    /**
     * Get the the modified time of the most recently changed file within the
     * {@code directory}. Please note that this method differs from other
     * methods that check the modified timestamp of the directory itself as
     * opposed to checking the modified timestamps of the contents of the
     * directory.
     * <p>
     * This method will recursively search subdirectories for file modifications
     * as well.
     * </p>
     * 
     * @param directory the directory to check
     * @return the modified timestamp of the most recently updated file
     */
    public static FileTime getMostRecentFileModifiedTime(String directory) {
        Path path = Paths.get(directory);
        File file = path.toFile();
        FileTime recent = FileTime.fromMillis(0);
        if(file.isDirectory()) {
            for (File f : file.listFiles()) {
                FileTime proposed = f.isDirectory()
                        ? getMostRecentFileModifiedTime(f.getAbsolutePath())
                        : FileTime.fromMillis(f.lastModified());
                if(proposed.compareTo(recent) > 0) {
                    recent = proposed;
                }
            }
            return recent;
        }
        else {
            return FileTime.fromMillis(file.lastModified());
        }
    }

    /**
     * Get a consistent temporary directory file path that represents the hash
     * for the specified {@code key}.
     * <p>
     * This method does <strong>NOT</strong> create a file at the returned path,
     * or any of the parent directories.
     * </p>
     * 
     * @param key the key to hash
     * @return the hashed file path
     */
    public static Path getTemporaryHashedFilePath(String key) {
        String hash = Hashing.sha256().hashString(key, StandardCharsets.UTF_8)
                .toString();
        ArrayBuilder<String> array = ArrayBuilder.builder();
        StringBuilder sb = new StringBuilder();
        char[] chars = hash.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            char c = chars[i];
            sb.append(c);
            if(i >= 4 && i % 4 == 0) {
                array.add(sb.toString());
                sb.setLength(0);
            }
        }
        return Paths.get(TMPDIR.toAbsolutePath().toString(), array.build());
    }

    /**
     * Return an {@link Iterable} collection that lazily accumulates lines in
     * the underlying {@code file}.
     * <p>
     * This method is really just syntactic sugar for reading lines from a file,
     * so the returned collection doesn't actually allow any operations other
     * than forward iteration.
     * </p>
     * 
     * @param file the path to the file
     * @return an iterable collection of lines from the file
     */
    public static Iterable<String> readLines(final String file) {
        return readLines(file, null);
    }

    /**
     * Return an {@link Iterable} collection that lazily accumulates lines in
     * the underlying {@code file}.
     * <p>
     * This method is really just syntactic sugar for reading lines from a file,
     * so the returned collection doesn't actually allow any operations other
     * than forward iteration.
     * </p>
     * 
     * @param file the path to the file
     * @param cwd the working directory against which relative paths are
     *            resolved
     * @return an iterable collection of lines in the file
     */
    public static Iterable<String> readLines(final String file,
            @Nullable String cwd) {
        final String rwd = cwd == null ? WORKING_DIRECTORY : cwd;
        return new AbstractList<String>() {

            @Override
            public String get(int index) {
                throw new UnsupportedOperationException();
            }

            @Override
            public Iterator<String> iterator() {
                return new ReadOnlyIterator<String>() {

                    String line = null;
                    BufferedReader reader;
                    {
                        try {
                            reader = new BufferedReader(
                                    new FileReader(expandPath(file, rwd)));
                            line = reader.readLine();
                        }
                        catch (IOException e) {
                            throw CheckedExceptions.throwAsRuntimeException(e);
                        }
                    }

                    @Override
                    public boolean hasNext() {
                        return this.line != null;
                    }

                    @Override
                    public String next() {
                        String result = line;
                        try {
                            line = reader.readLine();
                            if(line == null) {
                                reader.close();
                            }
                            return result;
                        }
                        catch (IOException e) {
                            throw CheckedExceptions.throwAsRuntimeException(e);
                        }
                    }

                };
            }

            @Override
            public int size() {
                int size = 0;
                Iterator<String> it = iterator();
                while (it.hasNext()) {
                    size += 1;
                    it.next();
                }
                return size;
            }

        };

    }

    /**
     * Create a temporary directory with the specified {@code prefix}.
     * 
     * @param prefix the directory name prefix
     * @return the path to the temporary directory
     */
    public static String tempDir(String prefix) {
        try {
            return java.nio.file.Files.createTempDirectory(prefix).toString();
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * Return the base temporary directory that is used by the JVM without
     * throwing a checked exception.
     * 
     * @return the base tmpdir
     */
    private static Path getBaseTemporaryDirectory() {
        try {
            return java.nio.file.Files.createTempFile("cnch", ".a4j")
                    .getParent();
        }
        catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

    /**
     * The base temporary directory used by the JVM.
     */
    private final static Path TMPDIR = getBaseTemporaryDirectory();

    /**
     * The user's home directory, which is used to expand path names with "~"
     * (tilde).
     */
    private static String USER_HOME = System.getProperty("user.home");

    /**
     * The working directory from which the current JVM process was launched.
     */
    private static String WORKING_DIRECTORY = System.getProperty("user.dir");

    /**
     * The base path that is used to resolve and normalize other relative paths.
     */
    private static Path BASE_PATH = FileSystems.getDefault()
            .getPath(WORKING_DIRECTORY);

}
