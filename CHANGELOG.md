# Changelog

#### Version 1.1.0 (TBD)
* Added the `Association` data type. An `Association` is a nested mapping encapsulated within a `Map<String, Object>` that has custom logic for handling navigable traversal paths (e.g. keys that use periods (e.g. `.` to indicate traversing from one level within the Map to a deeper level (e.g. a nested collection or Map))).
* Deprecated the `Collectives` functions.
* Deprecated the `AnyMaps#explode` and `AnyMaps#navigate` functions in favor of the `Association` data type.

#### Version 1.0.0 (August 25, 2018)
* Initial Relase