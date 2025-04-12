# Changelog

#### Version 1.15.0 (TBD)
* Added a fluent builder API for `Benchmark` that provides a more intuitive way to configure and run benchmarks. The builder supports specifying time units, performing warmup runs, and executing benchmarks asynchronously to ensure fair comparisons between multiple benchmarks by eliminating the impact of JVM warmup and optimization order.

#### Version 1.14.0 (February 22, 2025)
* `JoinableExecutorService`: A new class in the `concurrent` package that enhances a standard `ExecutorService` with the ability for the calling thread to join task execution. This service allows threads to submit groups of tasks and then participate directly in executing those tasks, aiding in faster completion and improved resource utilization. It ensures tasks are initiated in iteration order but balances task execution across groups to maintain consistent system performance. This feature is beneficial for applications requiring high throughput and dynamic task management.
* Fixed a bug that caused the `Reflection#newInstance` method to throw a `NullPointerException` when a `null` was provided for a Nullable parameter.

##### Bug Fixes
* Fixed a bug that caused some methods in `Sequences` to have poor performance if the `toString` method of a potential `Sequence` was computationally expensive

#### Version 1.13.1 (October 1, 2022)
* Fixed a bug that caused a `NoSuchMethodException` to be thrown when using the `Reflection` utility to `call` a non-overriden interface-defined default method.

#### Version 1.13.0 (September 5, 2022)
* Added the `ByteBuffers#share` method that returns a new ByteBuffer containing a shared subseqence of a source `ByteBuffer` while incrementing the `position` of the source the same number of bytes that are shared.
* Upgraded `logback` dependency to version `1.2.11`
* Fixed a bug in `Reflection#call` that threw a `NoSuchMethodException` when trying to call an overriden method that accepts a generic parameter.

#### Version 1.12.1 (August 30, 2021)
* Improved the performance of `CountUpLatch` by using better synchronization control.
* Fixed a bug in `AnyStrings#tryParseNumber` that caused an error to be thrown instead of returning `null` when parsing strings with a leading `E` or `e` followed by digit characters (e.g. `e45`). These strings were mistaken for a number in scientific notation, but the parser has been fixed so that error no longer occurs.
* Optimized `ByteBuffers#getByteArray` to return the backing array of a `ByteBuffer` if it exists and the position of the `ByteBuffer` is `0` as well as the number of bytes `remaining` being equal to its `capacity`.

#### Version 1.12.0 (December 8, 2020)
* Fixed a bug that made it possible for the `ByteBuffer` returend from `ByteBuffers#get(ByteBuffer int)` to have a different byte order than the input source.
* Deprecated `ByteBuffers#encodeAsHex` in favor of `ByteBuffers#encodeAsHexString`.
* Deprecated `ByteBuffers#decodeFromHex` in favor of `ByteBuffers#decodeFromHexString`.

#### Version 1.11.0 (February 12, 2020)
* Added the `TriConsumer` functional interface that is similar to `BiConsumer` for three input arguments.

#### Version 1.10.0 (November 23, 2019)
* Added an improvement to `LazyTransformSet` that caches previously transformed values and makes a best effort to keep them from being transformed on subsequent iterations. 
* Added an `ExecutorRaceService` that can be used to execute multiple tasks in parallel with the option to give one of the tasks a head start. The first of the tasks to complete has its `Future` returned to the caller.
* Added the `Multimaps#from` method that transforms a `Map` whose values are a `Set` of objects into a `Multimap`.

#### Version 1.9.0 (November 11, 2019)
* Added the `CoalescableTreeMap` data structure that contains a `coalesce` method to return values for a consecutive range of similar keys.

#### Version 1.8.4 (October 30, 2019)
* Fixed a bug that caused `Sequences#isSequence` to throw a `NullPointerException` when checking a `null` reference. This method will now return `false` when provided a `null` parameter.

#### Version 1.8.3 (October 26, 2019)
* Fixed a bug that caused `AnyStrings#join` to throw an `IndexOutOfBoundsException` when trying to join an empty array of arguments with a separator. These functions now correctly return an empty string.

#### Version 1.8.2 ( September 14, 2019)
* Fixed a bug that caused a `StringSplitter` to throw an `IllegalStateException` when `toArray()` would generate an empty array because the combination of the original input and split options did not produce any distinct tokens (i.e. using `SplitOption.TRIM_WHITESPACE` with an empty string input). 

#### Version 1.8.1 (September 2, 2019)
* Added a version of `AnyObjects#split` that takes a `StringSplitter` as a parameter to provide more configurable split functionality.

#### Version 1.8.0 (August 16, 2019)
* Added `