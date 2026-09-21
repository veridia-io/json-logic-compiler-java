# `json-logic-compiler-java` documentation

A small, dependency-light JSON-Logic-*inspired* rule compiler: parses a JSON rule tree into an
internal AST, compiles it into a lambda tree (`CompiledExpression`), and evaluates it against a
caller-supplied context (`Map`/POJO), with compiled results cached per `JsonLogic` instance. It is
not a spec-compliant JSON Logic implementation — see L1. Consumed today as a versioned Maven Central
dependency by `io.veridia:segmentation-engine-service` (a separate repo,
`veridia-io/api-gateways`), which drives its per-event, per-tenant rule evaluation off it.

Chapters, ids and cross-referencing follow that consuming repo's `docs/rules/process.md` — this is
this doc's first pass through that shape; every id below is freshly assigned (no prior doc existed).

<details>
<summary>Scope of `json-logic-compiler-java`</summary>

# 1. Scenarios

| Code | Name | Priority | Entrypoint | Payload | Summary |
|---|---|---|---|---|---|
| S1 | Boolean condition evaluation | Critical | `JsonLogic.check(String, Object)`, method call | `String` rule text + `Object` context (`Map`/POJO, or a JSON `String`) | Compiles (or reuses a cached compile) the rule and coerces the result to `boolean` via `ToBoolean.eval`. `JsonLogic.java:87-91`. |
| S2 | Value-expression evaluation | Critical | `JsonLogic.apply(String, Object)`, method call | same as S1 | Compiles/evaluates and returns the raw, uncoerced result. `JsonLogic.java:40-49`. |
| S3 | Custom operator registration | High | `JsonLogic.registerOperator(Operator)`, method call | `Operator` (`key()` + `compile(List<CompiledExpression>)`) | Adds one operator to one `JsonLogic` instance's registry, on top of the ~19 defaults. `JsonLogic.java:99-103`. |

### Examples

- `segmentation-engine-service` constructs three separate `JsonLogic` instances (one each for
  segment, aggregate, and metric rules) and drives S1/S2 off each; only one of the three has ever
  registered a custom operator via S3 (`AssignGroupOperator`, `assignGroup`) — see that repo's
  `docs/json-logic.md` A4.

# 2. Assumptions and limitations

Domain-level: what a rule author and a caller can rely on, independent of how the compiler is built.

## Assumptions

**A1 — Fully dynamic, duck-typed evaluation; no declared type system.** A context value's Java type
is whatever the caller put there — there is no type enum and no authoring-time schema. Every operator
coerces at eval time via `helpers/ToBoolean.eval`/`helpers/ToDouble.eval`. *Enables:* a caller hands
in a plain `Map`/POJO with zero schema declaration or registration step. Proven by
`NumberTests.testConvertAllNumericInputToDouble` (`NumberTests.java:12-27`): `{"var": ...}` returns
whatever numeric subtype (`Double`/`Float`/`Integer`/`Short`/`Long`) was stored, unchanged — coercion
is deferred entirely to whichever operator consumes it. *Scenarios:* S1, S2.

## Limits

**L1 — Intentional subset, not full JSON Logic.** By its own README: no `filter`/`map`/`reduce`;
literal JSON arrays may hold only literals, never nested operators; `and`/`or` always return
`boolean`, not JS-style last-value propagation; `{"var": [a, b, ...]}` is an N-way coalesce (first
non-null across every listed path), not the spec's single-path-plus-default pair. Ref:
`README.md`. *Scenarios:* S1, S2.

**L2 — No date/time support of any kind.** No `java.time.*` import, no epoch-millis convention, no
ISO-8601 parsing, no timezone concept anywhere in `src/main`. A date/time-aware predicate is a
from-scratch addition, not an extension of an existing partial mechanism. *Scenarios:* S1, S2.

**L3 — No authoring-time validation pass.** `JsonLogic.apply`/`check` (`JsonLogic.java:40-91`) do no
schema validation of the rule or the context beyond Jackson parsing — a malformed or mistyped
expression is only ever discovered at evaluation time, and even then fails silently rather than
raising an error (see G2). *Scenarios:* S1, S2.

# 3. Design

## Responsibilities

| Responsibility | Calls | Description | Scenario |
|---|---|---|---|
| **R1 — Parsing.** | method call | `LogicParser.parse` walks a Jackson `JsonNode` into an internal `Expr` AST (constant / `var` / operator node). | S1, S2 |
| **R2 — Compilation & caching.** | method call | `JsonLogicCompiler.compile` reduces an `Expr` tree into a `CompiledExpression` lambda tree (`ctx -> Object`), resolving each operator node through `OperatorRegistry`. `JsonLogic` caches the result per instance, keyed by the raw expression string (`compiledExpressionsCache`, Guava, 1000-entry max, no time-based eviction). `JsonLogic.java:21,62-77`, `JsonLogicCompiler.java:17-57`. | S1, S2 |
| **R3 — Operator registry & default operators.** | method call | `OperatorRegistry` (`Map<String, Operator>`) holds the ~19 operators `JsonLogicCompiler.registerDefaultOperators` registers by default (`==`, `!=`, `!`, `!!`, `and`, `or`, `>`, `<`, `>=`, `<=`, `+ - * / %`, `min`, `max`, `round`, `greatest`, `least`, `all`, `some`, `none`, `in`, `contains`, `starts_with`, `ends_with`, `regex_match`, `exists`, `not_exists`), plus any operator a caller adds via S3. Lookup miss throws `UnsupportedOperationException` at **compile time**, not eval time. `JsonLogicCompiler.java:59-99`, `OperatorRegistry.java:9-19`. | S1, S2, S3 |
| **R4 — `var`/field resolution.** | method call | Handled specially in `JsonLogicCompiler.compile` (not through the `Operator` SPI): a flat key uses `PathResolver.resolveSingle`, a dotted path uses `PathResolver.resolve` against a pre-split array computed once at compile time. Both walk only `Map`/`List`/array — no bean/POJO field access. `JsonLogicCompiler.java:24-49`, `PathResolver.java:34-73,80-102`. | S1, S2 |

## Assumptions and limitations (design-level)

Continues the A#/L# sequence from chapter 2.

### Assumptions

**A2 — The compiled-expression cache is indefinite and keyed by expression text alone, not by
context.** Repeat calls to `apply`/`check` with the same rule string skip parsing and compiling
entirely; the cache never expires by time, only by the instance's 1000-entry size bound.
`JsonLogic.java:21,27-29,68-74`. *Enables:* near-zero repeat-evaluation cost on a hot per-event path.
*Scenarios:* S1, S2, S3.

## Constraints

**C1 — A `CompiledExpression` must compute any per-evaluation-varying value inside the lambda
`eval(Object ctx)` returns, never during `compile(List<CompiledExpression> args)`.** *if not holds:*
because of A2, a value memoized at compile time is frozen for the cache entry's entire lifetime and
returned on every subsequent cache hit — not just the first evaluation. Contrast
`ExpandabilityTests.AssignGroupOperator.compile` (`ExpandabilityTests.java:24-49`), which *does*
eagerly call `.eval(...)` on two of its arguments at compile time (lines 31-32) — safe there only
because those arguments are themselves compile-time constants (a literal count and a literal
experiment name), never a wall-clock read or anything else that should vary per call. Any future
operator that reads the current time (a `now()`-style generator) must not repeat that eager-eval
pattern. *Scenarios:* S1, S2, S3.

## Known gaps

| Gap | Severity | Ref | Scenario | Threatens |
|---|---|---|---|---|
| **G1 — No date/time operator or value type exists.** An absent capability, not a confirmed defect — see L2. | Medium | L2 | S1, S2 | — |
| **G2 — Silent-fail coercion on type mismatch.** A non-coercible value (e.g. a non-numeric string compared via `>`) returns a default (`0`/`false`) rather than raising an error, and nothing upstream of evaluation catches this. | Medium | `helpers/ToDouble.java:20-22` (catches `NumberFormatException`, returns `0`) | S1, S2 | A1 |

</details>

# 4. Implementation

## Entry points

| Message | Entrypoint | Ref | Scenario |
|---|---|---|---|
| Rule text + context | `JsonLogic.check(String, Object)` | `JsonLogic.java:87-91` | S1 |
| Rule text + context | `JsonLogic.apply(String, Object)` | `JsonLogic.java:40-49` | S2 |
| `Operator` implementation | `JsonLogic.registerOperator(Operator)` | `JsonLogic.java:99-103` | S3 |

## Known issues

None currently tracked.

# 5. Tests

Representative, not exhaustive — one row per test suite that anchors a scenario or a specific claim.
Every existing suite predates this doc and is numbered `T1` per suite (each covers exactly one
scenario), per `docs/rules/process.md`'s Identifiers and references.

| Test | Scenario | Ref | Proves |
|---|---|---|---|
| T1 | S1, S2 | `NumberTests.java:12-27` (`testConvertAllNumericInputToDouble`) | A1: `var` returns the raw Java numeric subtype unchanged; coercion is deferred to the consuming operator, not performed at resolution time |
| T1 | S3 | `ExpandabilityTests.java:59-83` | R3/C1: a custom operator (`assignGroup`) can be added via `registerOperator` with zero changes to any core file; also the source of the eager-eval-at-compile-time pattern C1 says must not be repeated for a time-varying value |

# Terms
Vocabulary of `json-logic-compiler-java`

| Term | Meaning |
|---|---|
| coalesce | `{"var": [a, b, ...]}`'s meaning in this compiler: the first non-null value across every listed path — not the spec's single-path-plus-one-default pair (L1). |
| compiled expression | A `CompiledExpression`: a rule reduced to a lambda tree (`Object eval(Object ctx)`), produced once by `JsonLogicCompiler.compile` and cached per `JsonLogic` instance (A2). |
| operator (SPI) | The compiler's extension point — the `Operator` interface (`key()` + `compile(List<CompiledExpression>)`), registered into an `OperatorRegistry` either by default (`JsonLogicCompiler.registerDefaultOperators`) or per instance via `JsonLogic.registerOperator` (S3). |
| `OperatorRegistry` | The `Map<String, Operator>` backing operator dispatch; lookup is by string key at compile time only, never per evaluation. |
| `PathResolver` | The dotted-path resolver `var` compiles down to; walks only `Map`/`List`/array, never a POJO's fields. |
