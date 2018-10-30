# Changelog

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