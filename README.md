# Accent4J

Accent4J is a suite of utility libraries, helpers, and data structures that make Java idioms more fluent.

It was born from years of building real systems in Java and repeatedly solving the same problems: awkward reflection, boilerplate around collections, missing concurrency primitives, and the constant friction of working with strings, byte buffers, and types. Rather than copy-paste utility classes across projects, Accent4J consolidates them into a single, well-tested library.

## Philosophy

- **Practical over clever.** Every class exists because it solved a real problem in production code. Nothing is included speculatively.
- **Fluent APIs.** Methods are designed for readability and chaining. The goal is code that communicates intent.
- **Lazy when possible.** Collections and transformations defer work until results are actually needed.
- **Java 8 baseline, modern when available.** The library targets Java 8 but adapts at runtime to take advantage of newer JVM features (e.g., `MethodHandles` on Java 9+).

## What's Inside

Accent4J covers several areas, each organized into its own package:

- **Base utilities** &mdash; Common operations on objects, strings, and arrays that the standard library leaves out. Quote-aware string splitting, flexible number parsing, null/empty coalescing, validation helpers, and checked-to-unchecked exception conversion.

- **Collections** &mdash; Data structures beyond what `java.util` offers. Includes `Association` (a nested map with dot-notation path traversal and merge strategies), `Continuation` (an infinite lazy list), `Sequences` (a unified API over arrays and iterables), `CoalescableTreeMap` (range coalescing over sorted keys), and lazy transform wrappers.

- **Concurrency** &mdash; Primitives for coordination and execution. `JoinableExecutorService` lets the submitting thread participate in task execution rather than block idly. `CountUpLatch` waits for a counter to reach a target (the inverse of `CountDownLatch`). `ExecutorRaceService` runs competing tasks and returns the first result.

- **Reflection** &mdash; A comprehensive reflection layer that handles visibility, autoboxing, generics, overloaded methods, default interface methods, and the full class hierarchy &mdash; so you don't have to.

- **I/O** &mdash; `ByteBuffer` utilities with efficient slicing, hex encoding, and pooled decoders. ANSI terminal string formatting.

- **Runtime** &mdash; JVM introspection (version detection, classpath enumeration) and dynamic class generation that works across Java 8 through 21+.

- **Benchmarking** &mdash; A fluent benchmarking API with warmup support and async execution to eliminate JVM optimization bias.

- **Functional** &mdash; Extended functional interfaces (`TriFunction`, `TriConsumer`) for when `BiFunction` isn't enough.

- **Description** &mdash; A predicate-based system for classifying objects by properties (e.g., defining what "empty" means for your types).

## Installation

### Gradle

```groovy
implementation 'com.cinchapi:accent4j:<version>'
```

### Maven

```xml
<dependency>
    <groupId>com.cinchapi</groupId>
    <artifactId>accent4j</artifactId>
    <version>VERSION</version>
</dependency>
```

Accent4J is published to [Maven Central](https://central.sonatype.com/artifact/com.cinchapi/accent4j).

## License

[Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
