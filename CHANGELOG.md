# Changelog

#### Version 1.8.2 ( September 14, 2019)
* Fixed a bug that caused a `StringSplitter` to throw an `IllegalStateException` when `toArray()` would generate an empty array because the combination of the original input and split options did not produce any distinct tokens (i.e. using `SplitOption.TRIM_WHITESPACE` with an empty string input). 

#### Version 1.8.1 (September 2, 2019)
* Added a version of `AnyObjects#split` that takes a `StringSplitter` as a parameter to provide more configurable split functionality.

#### Version 1.8.0 (August 16, 2019)
* Added `Benchmark#average` method that runs an action for a specified number of rounds and returns the **average** run time.
* Added a `CountUpLatch` synchronization construct to allow threads to wait until a latch has been incremented at least `n` times; even if `n` isn't known until after the incrementation begins.
* Added the `AnyObjects#split` functions to split the `toString()` representation of an `Object` or a `Sequence` of Objects by a delimiter; ultimately producing a flattened list of all the delimited substrings.

#### Version 1.7.1 (August 4, 2019)
* Fix a bug that causes the `WrapperAwareStringSplitter` to incorrectly split a string by a delimiter within a wrapped character sequence if there was no instance of the delimiter character appearing before the wrapped characters.

#### Version 1.7.0 (August 4, 2019)
* Added a `WrapperAwareStringSplitter` that can be used to split strings on a delimiter except for when that delimiter appears within a character sequence that is wrapped by two specified characters (i.e. beginning and ending parenthesis or left and right brackets).

#### Version 1.6.1 (July 15, 2019)
* Fixed a bug in `Reflection#getTypeArguments` that erroneously returned an empty collection of type arguments when a field was parameterized with a type that itself had a parameterized type (i.e. `List<AtomicReference<Integer>>`)
* Fixed a bug in `Reflection#getTypeArguments` that caused only a subset of type arguments to be returned in a field contained multiple type arguments, some of which were the same type (i.e. `Map<Integer, Integer>`).

#### Version 1.6.0 (June 23, 2019)
* Added the `com.cinchapi.script.ScriptObjectMirrors` utility class which contains the `javaify` method for casting script objects to their native Java counterparts.
* Added `com.cinchapi.common.collect.lazy` package which contains collections that facilitate efficient streaming by executing transformation logic on the fly. The package contains
  * `LazyTransformSet` - a `Set` that transforms items from another set using a function. The transformation only happens at the point of consumption.
* Added `Reflection#getAllDeclaredMethods` to return `Method` objects for all the non-base methods declared in a class hiearchy.

#### Version 1.5.3 (February 16, 2019)
* Fixed a bug in `AnyStrings#isWithinQuotes` that failed to account for all classes of unicode quote characters.
* Added functionality to `AnyStrings#replaceUnicodeConfusables` to specify characters that should not be replaced, even if they are a confusable.
* Added functionality to `AnyStrings#isWithinQuotes` to specify characters that should not be treated as quote characters, even if they actually are.

#### Version 1.5.2 (December 29, 2018)
* Fixed a bug where the `Reflection#getMethod` and other dependent functions (i.e. `call`, `callStatic`, etc) incorrectly determined an invocation was ambiguous if the invoked method was overloaded with corresponding parameters that are autoboxable (i.e. `long` and `Long`). For example, trying to invoke one of `foo(String arg1, long arg2)` and `foo(String arg1, Long arg2)` would previously fail.

#### Version 1.5.1 (November 28, 2018)
* Fixed a bug that caused an `ArrayBuilder` to fail when adding elements with different types even though those types had a common ancestor type.
* Fixed a bug that caused issues when using `Reflection` to determine the callability of or to invoke methods with a varargs parameter.

#### Version 1.5.0 (November 23, 2018)
* Added the `Types` utility class which contains functions to coerce objects to a specific type.

#### Version 1.4.0 (November 20, 2018)
* Added `Reflection#isCallableWith` that tests whether a `Method` is callable with an array of parameters.
* Added `ByteBuffers#getByteArray` and deprecated `ByteBuffers#toByteArray` because the implementation of the latter is incorrect when the `ByteBuffer` is backed by an array.

#### Version 1.3.0 (October 30, 2018)
* Added the `Continuation` data structure, which is an infinite `List` whose elements are generated on the fly using a `Function` or `Supplier`. When an element at an index is generated, it is always associated with that index for the duration of the continuation's lifetime. Because of these semantics, a `Continuation` is not directly mutable using the `add` or `remove` methods.
* Added the `CaseFormats` utility class to provide common operations related to the `CaseFormat` enum from Guava.
* Added `Sequences#isSequenceType` method to check whether a class is that of a `Sequence`.
* Added `Reflection#loadClassQuietly` to load a class without throwing a checked exception.
* Deprecated the `Check` interface in favor of the `java.util.function.Predicate`.
* Deprecated the `Generator` interface in favor of `java.util.function.Supplier`.

#### Version 1.2.0 (October 14, 2018)
* Add the `Adjective` framework. An `Adjective` is a group of class and class-hirearchy specific definitions that describe a trait that may apply to objects. Some adjectives may have built-in definitions, but all of them are intended to be customizable on a per-instance (e.g. no static state) basis using the `define` method. An adjective's applicability for an object can be tested using the `describes` method.
* The `Empty` adjective is provided. By default, `Empty` describes a null value, an empty String, an empty Iterable, or an empty Array.
* Deprecated the `AnyObjects#isNullOrEmpty`* and `AnyObjects#registerEmptyDefinition` methods in favor of using `Empty` instances when needed. These methods should be avoid because they rely on static state that can cause global conflicts. 
* Added `Sequences` utility class containing operations for `Iterable` and `Array` objects.
* Added `Enums` utility class with functions to parse enums from values while ignoring `toString` case and/or using custom logic.
* Added the `Association#ensure` static factory method that only creates a new `Association` from a `Map` if the `Map` is not already an `Association`. Otherwise, the input instance is returned.
* Improved the efficiency of creating a new `Association` instance from an existing instance.
* Added a `TriFunction` functional interface that is a three-arity specialization of a `Function`.

#### Version 1.1.0 (September 3, 2018)
* Added the `Association` data type. An `Association` is a nested mapping encapsulated within a `Map<String, Object>` that has custom logic for handling navigable traversal paths (e.g. keys that use periods (e.g. `.` to indicate traversing from one level within the Map to a deeper level (e.g. a nested collection or Map))).
* Deprecated the `Collectives` functions.
* Deprecated the `AnyMaps#explode` and `AnyMaps#navigate` functions in favor of the `Association` data type.
* Added `MergeStrategies` class that contains `merge` functions for `Map` and `Association` objects. The provided strategies are `ours`, `theirs`, `concat` and `upsert`.

#### Version 1.0.0 (August 25, 2018)
* Initial Relase