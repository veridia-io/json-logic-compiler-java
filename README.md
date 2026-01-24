# Veridia JSON Logic Compiler

A high-performance JSON-Logic execution engine for Java, designed for real-time
segmentation, metrics evaluation, and rule processing in the Veridia platform.

This project takes inspiration from the excellent work in  
https://github.com/jamsesso/json-logic-java,  
but diverges significantly in design and semantics to support:

- compiled expression trees (instead of dynamic AST evaluation)
- zero-allocation, lambda-based execution
- deterministic boolean logic (instead of JS-style value returns)
- array-based predicate operators (`all`, `some`, `none`)
- extended variable resolution semantics
- optional “coalesce” lookup behavior
- strong typing & predictable performance

This is **not** a full JSON-Logic implementation.  
It is an optimized, opinionated subset tailored for real-time decision systems.

---

## Installation

### Maven

```xml
<properties>
  <json-logic-compiler-java.version>1.0.23</json-logic-compiler-java.version>
</properties>

<dependency>
  <groupId>io.veridia</groupId>
  <artifactId>json-logic-compiler-java</artifactId>
  <version>${json-logic-compiler-java.version}</version>
</dependency>
```

### Gradle

```kotlin
val jsonLogicCompilerVersion = "1.0.23"
implementation("io.veridia:json-logic-compiler-java:$jsonLogicCompilerVersion")
```

---

## Goals

- Extremely fast evaluation suitable for millions of rule executions per second.
- Deterministic behavior (no JavaScript quirks).
- Support for segmentation-style operators like:
  - `all`
  - `some`
  - `none`
  - `in`
- Predictable truthiness rules identical to `json-logic-java`.

The engine minimizes allocations and compiles expressions down to reusable
`CompiledExpression` instances.

---

## Key Differences from Standard JSON-Logic

This implementation is **JSON-Logic–inspired**, but intentionally deviates from the
spec for clarity, speed, and predictability.

### 1. `{"var": ["a", "fallback"]}` is treated as **coalesce**
Instead of strict JSON-Logic behavior, when a variable lookup is provided as a list:

```json
{"var": ["attributes.status", "NONE"]}
````

The engine interprets it as:

```
return first non-null value
```

This dramatically simplifies real-world segmentation rules and avoids writing
explicit `{"??": [...]}` or chained `or` conditions.

### 2. Logical operators return booleans

In pure JSON-Logic:

```json
{"or": [0, false, "a"]} → "a"
```

In this engine:

```json
{"or": [0, false, "a"]} → true
```

Because segmentation rules need **boolean results**, not JS-like values.

### 3. Arrays inside literals must contain literals only

This is stricter than JSON-Logic to guarantee that array operators work efficiently
and predictably. Literal arrays cannot contain operators.

### 4. Comparison operators use **chained semantics**

As in JSON-Logic:

```json
{">=" : [a, b, c]}  → (a >= b) AND (b >= c)
```

### 5. Extended array operators

The following operators are added or behave differently than in standard JSON-Logic:

* `all` — returns true only if the condition matches every element.
* `some` — returns true if any element matches.
* `none` — returns true if no elements match.
* each receives `"item"` as the loop variable.

Example:

```json
{"none": [
  {"var": "attributes.tags"},
  {"==": [{"var": "item"}, "VIP"]}
]}
```

### 6. The engine is compiled, not interpreted

The expression tree is compiled into lambdas, meaning:

* no recursive evaluation
* near-zero overhead in hot loops
* safe for high-frequency rule execution

---

## What This Engine Does *Not* Support

* Full JSON-Logic operator set (e.g., `filter`, `map`, `reduce`—unless added)
* JavaScript-like value propagation through `and`/`or`
* Operators as literals inside arrays
* Dynamic schema inference
* Short-circuiting that returns non-boolean values

This is intentional to keep the engine small, fast, and predictable.

---

## Example

```json
{
  "and": [
    {">": [{"var": "age"}, 18]},
    {"none": [
      {"var": "attributes.tags"},
      {"==": [{"var": "item"}, "VIP"]}
    ]}
  ]
}
```

Compiled once → evaluated repeatedly in microseconds.

---

## License & Upstream Attribution

This project is released under the MIT License (see [LICENSE](LICENSE)). It is
**influenced by**
[json-logic-java](https://github.com/jamsesso/json-logic-java) and is not a fork;
it diverges significantly from the JSON-Logic specification.

### Original vs. Derived

- **Original work**: All compiler architecture, operators, and execution model
  in this repository are original unless explicitly noted otherwise. See
  [NOTICE.md](NOTICE.md) for the authoritative list.
- **Derived work**: Portions of the truthiness implementation in
  `src/main/java/io/veridia/jsonlogic/helpers/ToBoolean.java` are derived from
  json-logic-java and remain under the upstream MIT license. Additionally, some
  files in the test suite under `src/test/java/io/veridia/jsonlogic/` are adapted from json-logic-java for
  project-specific use cases.

Upstream license: https://github.com/jamsesso/json-logic-java/blob/49995a742d1b8e1e62416b16e29ae42156278439/LICENSE

Where applicable, behavior aligns with JSON-Logic 2.0 semantics, except where
modifications improve determinism, clarity, performance, or match our engine
requirements.

---

If you rely on this library in production, please review the deviations above
to understand how it differs from pure JSON-Logic.
