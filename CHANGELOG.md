# Changelog

#### Version 1.18.3 (TBD)
* Fixed a bug where `Association.of(Map)` and `Association#merge(Map)` took time
  that grew with the square of the length of each list in the map. For example,
  `Association.of` took about 1.5 seconds for a map that held one list of 20,000
  strings, so a server that turned the JSON of a small request into an
  `Association` could spend seconds of CPU on it. Both methods take time in
  proportion to the size of the result, which holds a `null` for each list
  position that an integer key skips, and give the same result as before, down
  to the order of the entries, the type of each container, and whether a
  container that a caller already holds is replaced or updated. `merge` has this
  cost when the `Association` keeps its maps in `LinkedHashMap` or `HashMap`
  containers and its lists in `ArrayList` or `LinkedList` containers, and its
  class does not override `get`, `fetch`, `set` or `merge(Map, BiFunction)`, as
  for `Association.of()`. For any other `Association`, the cost of `merge` can
  still grow with the square of the length of a list. A subclass that overrides
  one of those methods has them called as `merge(map, MergeStrategies::theirs)`
  calls them. The suppliers that a subclass returns from `collectionSupplier()`
  and `mapSupplier()` must make a new, empty container on each call and have no
  other effect, because `merge` may call them fewer times than once for each
  component of each path.
* Fixed a bug that allowed a short path to make `Association` build a list of
  any size. A write fills each position between the end of a list and the
  position that an integer component of its path names with `null`, so
  `Association.of(Map)`, `Association#set`, `Association#merge` and the
  deprecated `AnyMaps.explode` could build a list of billions of elements from a
  key such as `a.2000000000` and exhaust the heap. One write fills at most
  `Association.MAX_LIST_GAP` (100) positions this way, and a write that would
  fill more throws an `IllegalArgumentException` before it changes anything. A
  position that a list in the input of `Association.of` or `Association#merge`
  supplies does not count, even when its element is an empty map or list, so a
  list of any length is unaffected. A caller whose paths skip more positions
  must first write the positions in between.
  ([GHSA-q78v-mv7v-mvfx](https://github.com/cinchapi/accent4j/security/advisories/GHSA-q78v-mv7v-mvfx))

#### Version 1.18.2 (October 28, 2026)
* Fixed a bug where `Processes.waitFor` sometimes returned no standard output or
  standard error for a process that wrote lines to it. It happened most often
  when the process exited quickly. The same bug affected
  `Processes.waitForSuccessfulCompletion`, `Processes.isPidRunning` and
  `Files.directoryChecksum`. If `Processes.waitFor` cannot read a stream, it
  throws an exception instead of waiting forever. If the calling thread is
  interrupted while it waits, its interrupt status stays set.
  ([GH-53](https://github.com/cinchapi/accent4j/issues/53))
* Fixed a bug where concurrent calls to `Processes.waitFor` could wait forever.
  It could happen when a JVM waited on at least twice as many processes as it
  had processors at once, and each process wrote more output than a pipe holds.
* The methods of `Enums` create less short-lived garbage. Previously, each call
  copied every constant of the enum that it searched.
* `ByteBuffers#getUtf8String` is faster, most of all when many threads decode at
  once, and so is `ByteBuffers#getString` for UTF-8. Previously, every call took
  a decoder from one pool that all threads shared and put it back when done. So
  threads that decoded at the same time slowed each other down. Now, each thread
  decodes with a decoder of its own. Each call returns the same string for the
  same bytes as before and still drops bytes that are not valid UTF-8.
* `Empty#is` and `Empty#ness` share one read-only set of default definitions
  among the instances that they return, and an instance copies the set only when
  you add a definition to it with `define`. So each call allocates 24 bytes
  instead of 240, which helps a caller that checks many values, such as a
  `Comparator` that calls `Empty.ness().describes(...)` for each comparison.
  Each call still returns a new instance, and a definition that you add to one
  instance doesn't affect any other instance or the defaults.

#### Version 1.18.1 (October 22, 2026)
* Fixed a bug where each call to `Logger.none()` created a temporary directory
  and opened four log files that stayed open for the life of the JVM. A caller
  that asked for it often, such as once per request, eventually ran out of
  memory or file handles. `Logger.none()` returns the same `Logger` on every
  call. ([GH-49](https://github.com/cinchapi/accent4j/issues/49))

#### Version 1.18.0 (October 16, 2026)

##### Joins in JoinableExecutorService

A join in a `JoinableExecutorService` is now fast for a batch of any size, even
when every worker is busy. A join lets a thread finish a batch of work, such as
a read from many files, with help from any worker that's free. The calling
thread works on the batch too, so a join should never take longer than running
its tasks alone.

Previously, a join put every task in the batch on the queue that all of the
workers share. Then the calling thread searched that queue for each of its tasks
that wasn't finished, to take it back and run it itself. Each search looked
through the queue while the workers and every other join waited for it. So a
batch twice as large took about four times as long, and with every worker busy,
a join of 200,000 tasks took more than five seconds.

Now, a join keeps its tasks off the queue and numbers them. The calling thread
takes the next number, runs that task, and repeats until no numbers are left.
Any worker that is free while the join runs helps, whether it was idle when the
join began or finished other work after that. Each helper takes the next number
from the same join, so no task runs twice.

Taking the next number is one quick step, however large the batch is. So when
every worker is busy, a join takes about as long as running its tasks alone, and
idle workers make it finish sooner. The join of 200,000 tasks with every worker
busy now takes about 20 milliseconds. Every form of `join` works this way.

* `JoinableExecutorService` has a new `join` form for a batch of tasks that you
  keep in an ordered collection, such as an array or a `List`. Instead of the
  tasks themselves, it takes their number and an action, which receives the
  index of a task and gets and runs that task. So it creates no `Future` or
  other object for each task, which suits a large batch of quick tasks. For
  example, this fetches each page on whichever threads are free and returns once
  every fetch is done:

  ```java
  String[] pages = new String[urls.length];
  executor.join(urls.length, i -> pages[i] = fetch(urls[i]));
  ```

  The tasks run in no set order. If a task throws, the join throws that error
  once every task has run. Another form passes each error and the index of its
  task to a handler instead. A class that implements `JoinableExecutorService`
  must now implement this form.

##### Miscellaneous
* `CoalescableTreeMap#coalesce` now returns a Map whose keys are the same as the
  keys actually stored in the original Map. Previously, each neighbor came back
  as the original Map stored it, but the key at the lookup position came back as
  the caller spelled it. For example, in a Map that ignores case and stores
  `foo`, a lookup of `FOO` previously returned the key `FOO` and now returns
  `foo`. The returned Map compares keys the same way as the original Map, so
  `get` with the lookup key finds the entry in both cases.
* `CoalescableTreeMap#coalesce` is now faster on every lookup, most of all on a
  large map or when many keys coalesce. A lookup searches the map twice, once on
  each side of the lookup key, and steps from each neighbor to the next. Each
  coalesced key costs one call to the matcher. Previously, a lookup searched the
  map again for every neighbor, so it cost three searches plus one per coalesced
  key. On a map of two million keys where two keys coalesce, a lookup makes
  about half as many key comparisons and takes about a third less time.

#### Version 1.17.3 (October 10, 2026)
* Fixed a bug in `CoalescableTreeMap#coalesce` that dropped entries when the
  map's `Comparator` kept apart two keys that are `equal` to each other. The
  result held only one of those keys, mapped to the value of the last one. The
  returned map compares its keys with the map's `Comparator`, so it holds each
  coalesced key with its own value, in the map's order.
* Fixed a bug in `CoalescableTreeMap#coalesce` that threw a
  `NullPointerException` when a coalesced neighbor of the lookup key mapped to
  `null`. The result omits each key that maps to `null`, and the neighbors
  beyond that key still coalesce.

#### Version 1.17.2 (October 3, 2026)
* `Reflection` remembers each field and method that it looks up by name, so
  repeated calls on the same class skip the search of the class hierarchy. This
  speeds up `get`, `set`, `getStatic`, `call`, `callIf`, `callIfAccessible`,
  `callStatic`, `callStaticIfAccessible`, `getDeclaredField`, `getMethodUnboxed`
  and `isDeclaredAnnotationPresentInHierarchy`, most of all for a field that a
  superclass declares. `getDeclaredField` returns the same `Field` object for
  repeated lookups, and `getMethodUnboxed` may return a `Method` object that
  other lookups share, so a caller must not change the accessibility of either.
* `Reflection#invokeDefaultInterfaceMethod` remembers the method handle that it
  builds for each default method, so repeated calls for the same method are much
  faster, most of all on Java 8. Each call still runs the method on its own
  target with its own arguments.
* `Reflection#newInstance` remembers the constructor that it chooses for a class
  and the classes of the arguments, so repeated calls with the same argument
  classes skip the search of the class's constructors. A call that no
  constructor accepts throws on every call.

#### Version 1.17.1 (September 18, 2026)
* Added `Enums#parseNameIgnoreCase` and `Enums#tryParseNameIgnoreCase`, which
  match an enum constant by name only, in any case, and never read a number as
  an ordinal. `parseNameIgnoreCase` throws an `IllegalArgumentException` when no
  constant matches, and `tryParseNameIgnoreCase` returns `null`. Each has an
  overload that applies custom logic as a last resort, like
  `Enums#parseIgnoreCase`.
* Added `Enums#tryParseIgnoreCase`, which looks up a constant the way
  `Enums#parseIgnoreCase` does but returns `null` instead of throwing when no
  constant matches. It has an overload that applies custom logic as a last
  resort.
* Fixed `Enums#parseIgnoreCase` to throw the documented
  `IllegalArgumentException` for an ordinal that no constant has, instead of an
  `ArrayIndexOutOfBoundsException`. The custom logic of the overload is now
  applied to such a value as a last resort.

#### Version 1.16.2 (February 22, 2026)
* Fixed issue with premature publishing to maven central

#### Version 1.16.1 (February 22, 2026)
* Fixed a bug in `ByteBuffers#getByteArray` that returned incorrect data for
  sliced `ByteBuffer` instances. The fast path incorrectly ignored the buffer's
  `arrayOffset`, causing it to return the parent buffer's full backing array
  instead of just the slice's content.
* Fixed `RuntimeDynamics#newAnonymousObject` to work on Java 9+ where
  `sun.misc.Unsafe#defineClass` was removed. The method now detects the running
  JVM version and uses `MethodHandles.Lookup#defineClass` (via reflection) on
  Java 9+, while preserving the original `sun.misc.Unsafe` path for Java 8
  compatibility.
* Added `Application#javaVersion` utility method that returns the major version
  of the currently running JVM (e.g., 8, 11, 17, 21), correctly handling both
  the old-style (`1.8`) and new-style (`11`) version formats.

#### Version 1.16.0 (December 18, 2025)
* Added comprehensive interface reflection utilities to the `Reflection` class
  for working with Java interfaces and default methods:
  * `getImplementedInterfaces()` - Returns interfaces directly implemented by a
    class, including interfaces extended by those directly implemented
    interfaces
  * `getAllImplementedInterfaces()` - Returns all interfaces from the entire
    class hierarchy, including interfaces from superclasses
  * `getAllDefaultInterfaceMethods()` - Returns all default interface methods
    available to a class from its implemented interfaces
  * `getAllNonOverriddenDefaultInterfaceMethods()` - Returns default interface
    methods that have not been overridden by the class or its hierarchy
  * `invokeDefaultInterfaceMethod()` - Invokes a default interface method on a
    target object using MethodHandles for proper access
  * Each method has both `Class<?>` and `Object` parameter variants for
    convenience
  * These utilities are particularly useful for reflection-based frameworks that
    need to understand interface inheritance and default method availability
* **Breaking Change**: `JoinableExecutorService` has been refactored from a
  concrete class to an interface. This change requires using one of the static
  factory methods to create instances instead of using constructors directly.
  The refactoring provides better abstraction and allows for different
  implementation strategies.
* Added `JoinableDirectExecutorService` implementation that executes tasks
  synchronously in the calling thread, enabling the same code constructs and
  execution paths when async behavior is not preferred or when configuration
  requires synchronous execution
* Fixed `Application.classpath()` to work on Java 9+ where the system class
  loader is no longer a `URLClassLoader`

#### Version 1.15.0 (April 13, 2025)
* Added a fluent builder API for `Benchmark` that provides a more intuitive way
  to configure and run benchmarks. The builder supports specifying time units,
  performing warmup runs, and executing benchmarks asynchronously to ensure fair
  comparisons between multiple benchmarks by eliminating the impact of JVM
  warmup and optimization order.

#### Version 1.14.0 (February 22, 2025)
* `JoinableExecutorService`: A new class in the `concurrent` package that
  enhances a standard `ExecutorService` with the ability for the calling thread
  to join task execution. This service allows threads to submit groups of tasks
  and then participate directly in executing those tasks, aiding in faster
  completion and improved resource utilization. It ensures tasks are initiated
  in iteration order but balances task execution across groups to maintain
  consistent system performance. This feature is beneficial for applications
  requiring high throughput and dynamic task management.
* Fixed a bug that caused the `Reflection#newInstance` method to throw a
  `NullPointerException` when a `null` was provided for a Nullable parameter.

##### Bug Fixes
* Fixed a bug that caused some methods in `Sequences` to have poor performance
  if the `toString` method of a potential `Sequence` was computationally
  expensive

#### Version 1.13.1 (October 1, 2022)
* Fixed a bug that caused a `NoSuchMethodException` to be thrown when using the
  `Reflection` utility to `call` a non-overriden interface-defined default
  method.

#### Version 1.13.0 (September 5, 2022)
* Added the `ByteBuffers#share` method that returns a new ByteBuffer containing
  a shared subseqence of a source `ByteBuffer` while incrementing the `position`
  of the source the same number of bytes that are shared.
* Upgraded `logback` dependency to version `1.2.11`
* Fixed a bug in `Reflection#call` that threw a `NoSuchMethodException` when
  trying to call an overriden method that accepts a generic parameter.

#### Version 1.12.1 (August 30, 2021)
* Improved the performance of `CountUpLatch` by using better synchronization
  control.
* Fixed a bug in `AnyStrings#tryParseNumber` that caused an error to be thrown
  instead of returning `null` when parsing strings with a leading `E` or `e`
  followed by digit characters (e.g. `e45`). These strings were mistaken for a
  number in scientific notation, but the parser has been fixed so that error no
  longer occurs.
* Optimized `ByteBuffers#getByteArray` to return the backing array of a
  `ByteBuffer` if it exists and the position of the `ByteBuffer` is `0` as well
  as the number of bytes `remaining` being equal to its `capacity`.

#### Version 1.12.0 (December 8, 2020)
* Fixed a bug that made it possible for the `ByteBuffer` returend from
  `ByteBuffers#get(ByteBuffer int)` to have a different byte order than the
  input source.
* Deprecated `ByteBuffers#encodeAsHex` in favor of
  `ByteBuffers#encodeAsHexString`.
* Deprecated `ByteBuffers#decodeFromHex` in favor of
  `ByteBuffers#decodeFromHexString`.

#### Version 1.11.0 (February 12, 2020)
* Added the `TriConsumer` functional interface that is similar to `BiConsumer`
  for three input arguments.

#### Version 1.10.0 (November 23, 2019)
* Added an improvement to `LazyTransformSet` that caches previously transformed
  values and makes a best effort to keep them from being transformed on
  subsequent iterations.
* Added an `ExecutorRaceService` that can be used to execute multiple tasks in
  parallel with the option to give one of the tasks a head start. The first of
  the tasks to complete has its `Future` returned to the caller.
* Added the `Multimaps#from` method that transforms a `Map` whose values are a
  `Set` of objects into a `Multimap`.

#### Version 1.9.0 (November 11, 2019)
* Added the `CoalescableTreeMap` data structure that contains a `coalesce`
  method to return values for a consecutive range of similar keys.

#### Version 1.8.4 (October 30, 2019)
* Fixed a bug that caused `Sequences#isSequence` to throw a
  `NullPointerException` when checking a `null` reference. This method will now
  return `false` when provided a `null` parameter.

#### Version 1.8.3 (October 26, 2019)
* Fixed a bug that caused `AnyStrings#join` to throw an
  `IndexOutOfBoundsException` when trying to join an empty array of arguments
  with a separator. These functions now correctly return an empty string.

#### Version 1.8.2 ( September 14, 2019)
* Fixed a bug that caused a `StringSplitter` to throw an `IllegalStateException`
  when `toArray()` would generate an empty array because the combination of the
  original input and split options did not produce any distinct tokens (i.e.
  using `SplitOption.TRIM_WHITESPACE` with an empty string input).

#### Version 1.8.1 (September 2, 2019)
* Added a version of `AnyObjects#split` that takes a `StringSplitter` as a
  parameter to provide more configurable split functionality.

#### Version 1.8.0 (August 16, 2019)
* Added `Benchmark#average` method that runs an action for a specified number of
  rounds and returns the **average** run time.
* Added a `CountUpLatch` synchronization construct to allow threads to wait
  until a latch has been incremented at least `n` times; even if `n` isn't known
  until after the incrementation begins.
* Added the `AnyObjects#split` functions to split the `toString()`
  representation of an `Object` or a `Sequence` of Objects by a delimiter;
  ultimately producing a flattened list of all the delimited substrings.

#### Version 1.7.1 (August 4, 2019)
* Fix a bug that causes the `WrapperAwareStringSplitter` to incorrectly split a
  string by a delimiter within a wrapped character sequence if there was no
  instance of the delimiter character appearing before the wrapped characters.

#### Version 1.7.0 (August 4, 2019)
* Added a `WrapperAwareStringSplitter` that can be used to split strings on a
  delimiter except for when that delimiter appears within a character sequence
  that is wrapped by two specified characters (i.e. beginning and ending
  parenthesis or left and right brackets).

#### Version 1.6.1 (July 15, 2019)
* Fixed a bug in `Reflection#getTypeArguments` that erroneously returned an
  empty collection of type arguments when a field was parameterized with a type
  that itself had a parameterized type (i.e. `List<AtomicReference<Integer>>`)
* Fixed a bug in `Reflection#getTypeArguments` that caused only a subset of type
  arguments to be returned in a field contained multiple type arguments, some of
  which were the same type (i.e. `Map<Integer, Integer>`).

#### Version 1.6.0 (June 23, 2019)
* Added the `com.cinchapi.script.ScriptObjectMirrors` utility class which
  contains the `javaify` method for casting script objects to their native Java
  counterparts.
* Added `com.cinchapi.common.collect.lazy` package which contains collections
  that facilitate efficient streaming by executing transformation logic on the
  fly. The package contains
  * `LazyTransformSet` - a `Set` that transforms items from another set using a
    function. The transformation only happens at the point of consumption.
* Added `Reflection#getAllDeclaredMethods` to return `Method` objects for all
  the non-base methods declared in a class hiearchy.

#### Version 1.5.3 (February 16, 2019)
* Fixed a bug in `AnyStrings#isWithinQuotes` that failed to account for all
  classes of unicode quote characters.
* Added functionality to `AnyStrings#replaceUnicodeConfusables` to specify
  characters that should not be replaced, even if they are a confusable.
* Added functionality to `AnyStrings#isWithinQuotes` to specify characters that
  should not be treated as quote characters, even if they actually are.

#### Version 1.5.2 (December 29, 2018)
* Fixed a bug where the `Reflection#getMethod` and other dependent functions
  (i.e. `call`, `callStatic`, etc) incorrectly determined an invocation was
  ambiguous if the invoked method was overloaded with corresponding parameters
  that are autoboxable (i.e. `long` and `Long`). For example, trying to invoke
  one of `foo(String arg1, long arg2)` and `foo(String arg1, Long arg2)` would
  previously fail.

#### Version 1.5.1 (November 28, 2018)
* Fixed a bug that caused an `ArrayBuilder` to fail when adding elements with
  different types even though those types had a common ancestor type.
* Fixed a bug that caused issues when using `Reflection` to determine the
  callability of or to invoke methods with a varargs parameter.

#### Version 1.5.0 (November 23, 2018)
* Added the `Types` utility class which contains functions to coerce objects to
  a specific type.

#### Version 1.4.0 (November 20, 2018)
* Added `Reflection#isCallableWith` that tests whether a `Method` is callable
  with an array of parameters.
* Added `ByteBuffers#getByteArray` and deprecated `ByteBuffers#toByteArray`
  because the implementation of the latter is incorrect when the `ByteBuffer` is
  backed by an array.

#### Version 1.3.0 (October 30, 2018)
* Added the `Continuation` data structure, which is an infinite `List` whose
  elements are generated on the fly using a `Function` or `Supplier`. When an
  element at an index is generated, it is always associated with that index for
  the duration of the continuation's lifetime. Because of these semantics, a
  `Continuation` is not directly mutable using the `add` or `remove` methods.
* Added the `CaseFormats` utility class to provide common operations related to
  the `CaseFormat` enum from Guava.
* Added `Sequences#isSequenceType` method to check whether a class is that of a
  `Sequence`.
* Added `Reflection#loadClassQuietly` to load a class without throwing a checked
  exception.
* Deprecated the `Check` interface in favor of the
  `java.util.function.Predicate`.
* Deprecated the `Generator` interface in favor of
  `java.util.function.Supplier`.

#### Version 1.2.0 (October 14, 2018)
* Add the `Adjective` framework. An `Adjective` is a group of class and
  class-hirearchy specific definitions that describe a trait that may apply to
  objects. Some adjectives may have built-in definitions, but all of them are
  intended to be customizable on a per-instance (e.g. no static state) basis
  using the `define` method. An adjective's applicability for an object can be
  tested using the `describes` method.
* The `Empty` adjective is provided. By default, `Empty` describes a null value,
  an empty String, an empty Iterable, or an empty Array.
* Deprecated the `AnyObjects#isNullOrEmpty`* and
  `AnyObjects#registerEmptyDefinition` methods in favor of using `Empty`
  instances when needed. These methods should be avoid because they rely on
  static state that can cause global conflicts.
* Added `Sequences` utility class containing operations for `Iterable` and
  `Array` objects.
* Added `Enums` utility class with functions to parse enums from values while
  ignoring `toString` case and/or using custom logic.
* Added the `Association#ensure` static factory method that only creates a new
  `Association` from a `Map` if the `Map` is not already an `Association`.
  Otherwise, the input instance is returned.
* Improved the efficiency of creating a new `Association` instance from an
  existing instance.
* Added a `TriFunction` functional interface that is a three-arity
  specialization of a `Function`.

#### Version 1.1.0 (September 3, 2018)
* Added the `Association` data type. An `Association` is a nested mapping
  encapsulated within a `Map<String, Object>` that has custom logic for handling
  navigable traversal paths (e.g. keys that use periods (e.g. `.` to indicate
  traversing from one level within the Map to a deeper level (e.g. a nested
  collection or Map))).
* Deprecated the `Collectives` functions.
* Deprecated the `AnyMaps#explode` and `AnyMaps#navigate` functions in favor of
  the `Association` data type.
* Added `MergeStrategies` class that contains `merge` functions for `Map` and
  `Association` objects. The provided strategies are `ours`, `theirs`, `concat`
  and `upsert`.

#### Version 1.0.0 (August 25, 2018)
* Initial Relase
