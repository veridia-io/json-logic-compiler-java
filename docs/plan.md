# Plan: Scala 3 rewrite (`rewrite/scala`)

Revision 2 — revised once after a self-review (`check`) pass; see "Changes from revision 1" at the
end of each section where the gap check moved something. No prior review.md exists for this branch
yet; this plan precedes it per `docs/rules/process.md`.

## Modules touched

| Module/Tool | Why touched |
|---|---|
| `json-logic-compiler-java` | Full rewrite of `src/main` from Java 11 to Scala 3 (circe for JSON, ADTs, pattern matching), keeping Maven coordinates and the Java-facing API surface unchanged or minimally adjusted. Tests ported to ScalaTest. |

**Out of scope, explicitly:** `api-gateways`/`segmentation-engine-service` itself. That repo is touched
only by a local, uncommitted verification step (see Verification, below) — no PR there is part of
this plan. Publishing to Maven Central (a human-triggered GitHub Release) is also out of scope; this
plan only prepares the repo to be tagged.

## json-logic-compiler-java

Desired end state: the same Maven artifact (`io.veridia:json-logic-compiler-java`), the same public
classes at the same fully-qualified names (`io.veridia.jsonlogic.{JsonLogic, Operator,
CompiledExpression}`, `io.veridia.jsonlogic.helpers.{ToBoolean, ToDouble, DateHelper}`), implemented
in Scala 3 internally, with every scenario in `docs/json-logic-compiler-java.md` (S1/S2/S3) behaving
identically from a Java caller's point of view — including `segmentation-engine-service`'s one
external `Operator` implementation (`AssignGroupOperator`), which should need **zero** source changes.
`main` shrinks by 30-50% in the actual library (excluding the two Java SPI files described below),
while becoming denser (ADTs + pattern matching replacing the current one-class-many-nullable-fields
`Expr` and the 23-file, one-operator-per-file layout).

### New or changed scenarios

None. S1 (`check`), S2 (`apply`), S3 (`registerOperator`) keep their signatures (`check`'s return type
stays primitive `boolean`, `apply`'s stays `Object`). The only externally visible ripple: `apply`/
`check`/`compile` throw an **unchecked** `JsonLogicException` instead of the checked
`JsonProcessingException` — confirmed safe (see Change in assumptions/constraints, A7).

### Change in responsibilities

| Responsibility | Today (R#) | After this branch |
|---|---|---|
| Parsing | R1 — `LogicParser` walks a Jackson `JsonNode` into `Expr` (one class, four nullable fields) | A circe `Json` walked into a Scala 3 `enum Expr` (`Const`/`Var`/`Op`) via pattern matching on `.asObject`/`.asArray`/scalar accessors |
| Compilation & caching | R2 — `JsonLogicCompiler.compile` + Guava `Cache` in `JsonLogic` | Same shape (`Expr => CompiledExpression`, cached per instance, keyed by expression text), cache backed by **Caffeine** (see A8) instead of Guava |
| Operator registry & defaults | R3 — `OperatorRegistry` (`ConcurrentHashMap`) + 23 one-operator-per-file classes | Registry backed by `scala.collection.concurrent.TrieMap` (stdlib, lock-free); ~41 default operator keys regrouped into 7 domain objects (see File plan) |
| `var`/field resolution | R4 — `PathResolver`, walks `Map`/`List`/array only | Unchanged in behavior; ported near-verbatim (it's already dense, Java-interop-shaped code — a case-worthy exception to rewriting everything). Still no bean/POJO access. |

### File plan

Two plain Java files — the SPI a Java caller implements or lambda-constructs against, kept in Java
specifically to guarantee unsurprising `@FunctionalInterface`/lambda semantics for
`AssignGroupOperator` with zero risk from Scala's trait-to-interface compilation:

- `src/main/java/io/veridia/jsonlogic/CompiledExpression.java` — unchanged, still `Object eval(Object ctx)`.
- `src/main/java/io/veridia/jsonlogic/Operator.java` — unchanged, still `key()` + `compile(List<CompiledExpression>)`.

Everything else in Scala 3 (`src/main/scala/io/veridia/jsonlogic/...`):

| File | Replaces | Notes |
|---|---|---|
| `JsonLogic.scala` | `JsonLogic.java` | Java-facing signatures (`String`, `Object`, `boolean`, `java.util.List`) at the boundary; Scala idioms behind it |
| `JsonLogicException.scala` | (new) | Unchecked; wraps a circe parse failure or malformed-context error |
| `Expr.scala` | `LogicParser.Expr` | `enum Expr { case Const(value), Var(path, default), Op(name, args) }` |
| `LogicParser.scala` | `LogicParser.java` | circe `Json => Expr`; also the literal-array/object materializer |
| `values/Values.scala` | `helpers/ToBoolean.java`, `helpers/ToDouble.java` (impl only — see below), plus new `jsonToValue`/`jsonNumberToValue` | Central place for `Any`-typed value coercion |
| `helpers/ToBoolean.scala`, `helpers/ToDouble.scala` | same-named Java files | Kept at the same package/name for `AssignGroupOperator`'s `import io.veridia.jsonlogic.helpers.ToDouble;` — Scala `object`s get synthetic static-forwarder classes, so `ToDouble.eval(x)` compiles unchanged from Java |
| `helpers/DateHelper.scala` | `helpers/DateHelper.java` | Near-verbatim port; already dense, `java.time`-only, no JSON coupling |
| `JsonLogicCompiler.scala` | `JsonLogicCompiler.java` | `Expr => CompiledExpression`, default-operator registration |
| `OperatorRegistry.scala` | `OperatorRegistry.java` | `TrieMap`-backed |
| `PathResolver.scala` | `PathResolver.java` | Near-verbatim port |
| `cache/BoundedCache.scala` | Guava `CacheBuilder` call sites | Thin Caffeine wrapper, shared by `JsonLogic`'s expression cache and the regex operator's pattern cache |
| `operators/Equality.scala` | `EqualityOperator`, `InequalityOperator` | `==`, `!=` |
| `operators/NumericOps.scala` | `NumericComparisonOperator`, `GreatestLeastOperator` | `>`,`<`,`>=`,`<=`,`greatest`,`least` |
| `operators/Arithmetic.scala` | `MathOperator`, `RoundOperator` | `+ - * / %`,`min`,`max`,`round` |
| `operators/Logic.scala` | `LogicOperator`, `NotOperator` | `and`,`or`,`!`,`!!` |
| `operators/ArrayPredicates.scala` | `AllOperator`, `ArrayHasOperator`, `InOperator` | `all`,`some`,`none`,`in` |
| `operators/Strings.scala` | `StringOperator`, `StringRegexOperator` | `contains`,`starts_with`,`ends_with`,`regex_match` |
| `operators/Presence.scala` | `PresenceOperator` | `exists`,`not_exists` |
| `operators/DateTime.scala` | `NowOperator`,`TodayOperator`,`DateAddOperator`,`DateTruncateOperator`,`DateDiffOperator`,`DateEqOperator`,`ChronoFieldExtractorOperator` | all date/time operators, one domain file |

~33 main files -> 2 Java + ~17 Scala. Grouping is production-code only, per the ask — test files stay
close to today's one-suite-per-concern layout (see Tests, below) so scenario coverage stays easy to
audit file-by-file against `docs/json-logic-compiler-java.md`'s chapter 5.

### Change in assumptions, constraints, or limits

- **A7 (new) — `apply`/`check`/`compile` throw unchecked `JsonLogicException`, not checked
  `JsonProcessingException`.** *Enables:* zero source changes anywhere in `segmentation-engine-service`
  — confirmed no call site wraps these calls in `try/catch`; all three declare `throws
  JsonProcessingException` (a now-vacuous-but-legal declaration) or rely on `JsonProcessingException
  extends IOException` one level up (`PipelineProcessingService`). *Costs:* nothing found; this is a
  strict compatibility improvement, not a trade. *Scenarios:* S1, S2, S3.
- **A8 (new) — the compiled-expression cache and the regex pattern cache use Caffeine, not a
  hand-rolled structure, and not a plain `synchronized`/`ConcurrentHashMap` map.** *Enables:* both
  caches are hit on every single `apply`/`check` call across all three of
  `segmentation-engine-service`'s process-wide singleton `JsonLogic` instances — L2's 2,000-5,000+ RPS
  target means a naive single-lock structure would serialize rule evaluation across every tenant on
  one instance, a real availability risk per the root `CLAUDE.md`'s "this system does not have the
  luxury of low load." Caffeine is already trusted elsewhere in this org's stack (`DefinitionsService`
  in `segmentation-engine-service`), is lighter than full Guava, and preserves the exact documented
  bound (1000 entries / 1024 patterns, size-only eviction, no TTL). *Costs:* one small non-JDK,
  non-Scala dependency, replacing a different one (Guava) — net dependency count roughly flat, but
  each dependency is more purpose-built. *Scenarios:* S1, S2, S3.
- **A9 (new) — JSON-literal numeric values (in rule text, or in a JSON-string `ctx`) preserve
  Jackson's default numeric-type inference: a fractional/exponent literal becomes `java.lang.Double`;
  an integral literal becomes the smallest of `Integer`/`Long`/`BigInteger` that fits.** *Enables:* a
  literal returned by pass-through (a `var` default, a literal array/object element) keeps the exact
  boxed type existing rule authors and tests may depend on (`VariableTests.testDefaultValue` asserts
  `3.14` as `Double` via `.equals`-based comparison). This has no 1:1 Jackson equivalent in circe and
  needs its own conversion function (`Values.jsonNumberToValue`) and its own new test suite (this isn't
  a ported scenario — it's new logic replacing something Jackson did invisibly). *Costs:* one more
  piece of bespoke logic to maintain, isolated to a single small function. *Scenarios:* S1, S2, S3.
- **A10 (new) — arithmetic/comparison/round operators still normalize through `ToDouble`/`ToBoolean`
  before computing, same as today**, so A9's fidelity only matters for pass-through values, never for
  computed results (those are still always `Double`, or `Boolean`, matching current behavior exactly).
  *Scenarios:* S1, S2, S3.
- **C3 (new) — every operator's per-operator arity-validation behavior is preserved exactly, including
  its inconsistency.** Some operators throw `IllegalArgumentException` on wrong arity
  (`exists`/`not_exists`, all date operators except the two generators); others silently compile to a
  constant fallback (`greatest`/`least` -> `0.0`, `round` -> `0.0`, `contains`/`starts_with`/`ends_with`
  /`regex_match` -> `false`, `in` -> `false`). *if not holds:* a rewrite that "cleans up" this
  inconsistency changes observable behavior for a malformed rule, which is explicitly out of scope
  (behavior parity, not improvement). *Scenarios:* S1, S2, S3.
- **C4 (new) — a chained comparison (3+ args to `>`,`<`,`>=`,`<=`) re-evaluates each interior argument
  twice** (once as the right operand of pair *i*, once as the left operand of pair *i+1*), exactly as
  today. *if not holds:* an argument expression with a side effect readable only via wall-clock state
  (e.g. `now()` used mid-chain) would return a measurably different value under a "smarter"
  evaluate-once rewrite — a real, if obscure, behavior change. Preserved rather than fixed; not this
  branch's call to make. *Scenarios:* S1, S2.
- **G3 (new known gap, carried in as documentation, not fixed by this branch) — the `in` operator's
  `Map`-container branch (`{"in": [key, {"var": "someMap"}]}` -> key membership) has no test in the
  current suite.** Cheap to add a single test for during the port (derisks the rewrite at near-zero
  cost); not required to newly cover every branch this repo has ever left untested. *Scenarios:* S1, S2.

### Numeric-literal conversion (A9) — the algorithm

`Values.jsonNumberToValue(n: io.circe.JsonNumber): AnyRef`, driven off the number's original text
(`n.toString`, which circe/jawn retain verbatim):

1. Text contains `.`, `e`, or `E` -> `java.lang.Double`.
2. Otherwise integral: fits `Int` -> `java.lang.Integer`; else fits `Long` -> `java.lang.Long`; else
   -> `java.math.BigInteger`.

Needs its own new ScalaTest suite (`NumberLiteralFidelitySpec` or similar) — ported suites don't
exercise this since Jackson's equivalent behavior was never itself under test, only relied upon.

### Test migration

24 JUnit files (2029 lines) -> 24 (or 25, for A9/G3's new cases) ScalaTest files, `AnyFunSuite` +
`Matchers`, one-suite-per-concern kept matching today's grouping (production code merges by domain;
tests do not, so `docs/json-logic-compiler-java.md`'s chapter 5 test map stays easy to audit
file-by-file). `TableDrivenPropertyChecks` for the many near-identical one-line assertions
(`EqualityExpressionTests`, `NumericComparisonExpressionTests`, `StringExpressionTests` are the
biggest wins) — this, not file-merging, is where most test-code density comes from.

**Known translation trap:** Scala's `==` (and `shouldEqual`) does numeric-value comparison *across*
boxed numeric types (`(2024: Integer) == (2024L: Long)` is `true` in Scala), unlike Java
`Object.equals` (`false`). Several original tests implicitly assert exact boxed *type*, not just
value, through JUnit's `assertEquals(Object, Object)`:

- `NumberTests.testConvertAllNumericInputToDouble` — the whole point of the test is that `Double`/
  `Float`/`Integer`/`Short`/`Long` come back unchanged.
- `ChronoFieldExtractorOperatorTests` — expects `Integer`, not `Long`.
- `DateDiffOperatorTests` — expects `Long`.
- `MathExpressionTests`/`RoundOperatorTest`/`GreatestLeastOperatorTests` — expect `Double`.

Each of these needs an explicit `result.getClass shouldBe classOf[...]` (or
`result shouldBe a[java.lang.Integer]`) alongside the value check in the port, or a silent type
regression would pass ScalaTest without ever failing. `VariableTests.emptyVarKey_returnsWholeDataObject`
similarly needs ScalaTest's `theSameInstanceAs` (reference equality), not `shouldEqual`, to preserve
the `assertSame` check.

The JMH benchmark (`src/test/java/.../benchmark/JsonLogicBenchmark.java`) stays Java, calling the
Scala-implemented `JsonLogic`/`CompiledExpression` exactly as today — it's a consumer of the public
API, not part of the library, and JMH's annotation-processor-based code generation is safer left on
plain javac. See Open questions for the one build-wiring risk this creates.

### Build & tooling

| Concern | Choice | Version |
|---|---|---|
| Language | Scala 3, LTS line (per scala-lang's own guidance: target LTS when publishing a library) | 3.3.8 |
| Build plugin | `net.alchim31.maven:scala-maven-plugin` | 4.9.3 |
| JSON | `io.circe:circe-core`, `io.circe:circe-parser` (no `circe-generic` — hand-written `Json => Expr`, no case-class derivation needed) | 0.14.16 |
| cats | transitive via circe-core only; not added as a direct dependency unless implementation finds a concrete need | (transitive) |
| Test framework | `org.scalatest:scalatest_3` + `TableDrivenPropertyChecks` | 3.2.20 |
| Test runner | `org.scalatest:scalatest-maven-plugin` bound to `test` phase; `maven-surefire-plugin` execution disabled (no JUnit tests remain in `src/test/scala`; the JMH benchmark isn't run under `mvn test` today either — it's opt-in via `-Pbenchmark`) | 2.2.0 |
| Cache | `com.github.ben-manes.caffeine:caffeine` (replaces Guava) | pin latest 3.x at implementation time |
| Regex | `com.google.re2j:re2j` | unchanged |
| Dropped | `com.google.guava:guava`, `com.google.code.findbugs:jsr305` (unreferenced outside Guava's own annotations — verify at implementation time and drop if confirmed unused) | — |
| JVM target | `-release 17` (Scala 3.3.8 supports it; consuming service runs Java 25, so 17 is a floor, not a ceiling) | — |

CI (`.github/workflows/publish-java.yml`) needs `java-version: '11'` bumped to `'17'` (or higher) to
match; everything else in that workflow (GPG signing, GitHub Packages/Maven Central publish) is
build-tool-agnostic and untouched.

### Verification (this repo only)

1. `mvn -pl . clean verify` — full Scala build + ported ScalaTest suite green.
2. `mvn install` to a local SNAPSHOT version (e.g. `2.0.0-SNAPSHOT`) in `~/.m2`.
3. In a scratch/uncommitted copy of `api-gateways`, bump `json.logic.compiler.version` to that SNAPSHOT
   and run `mvn -pl segmentation-engine-service -am compile` (compile only, no commit, no push) —
   confirms `AssignGroupOperator` and the three `SegmentProcessingService`/`AggregateProcessingService`/
   `MetricProcessingService` call sites still compile untouched. Discard the scratch changes afterward.
4. `mvn -Pbenchmark test-compile exec:exec` once, to confirm the JMH harness still builds and runs
   against the new jar (not a correctness gate, just a build-wiring smoke check).

### Open questions

| Question | Resolution | Answer | Options |
|---|---|---|---|
| Does `scala-maven-plugin`'s mixed-source `testCompile` still run the JMH annotation processor correctly for the one remaining Java test file (the benchmark), now that `src/test/scala` exists alongside `src/test/java`? | resolved | Option (b). Left at its default, scala-maven-plugin silently absorbed `src/test/java` too, compiling the benchmark itself via its own internal javac call — bypassing `maven-compiler-plugin`'s `annotationProcessorPaths` entirely, so JMH failed at runtime with "Unable to find the resource: /META-INF/BenchmarkList". Fixed by explicitly setting `<sourceDir>src/main/scala</sourceDir>`/`<testSourceDir>src/test/scala</testSourceDir>` on scala-maven-plugin, scoping it away from the Java sources. Verified: `mvn -Pbenchmark exec:exec` runs all 10 benchmarks against the Scala-implemented jar. | (a) let scala-maven-plugin's zinc-backed mixed compilation handle both, passing JMH's `annotationProcessorPaths` through `<javacArgs>`; (b) keep `maven-compiler-plugin`'s default-testCompile scoped only to `src/test/java` (as today, benchmark profile only) and scope scala-maven-plugin's `testCompile` only to `src/test/scala`, letting default phase ordering (main compile before test compile) handle the cross-language dependency |
| Exact Caffeine version to pin | unresolved | — | pin latest stable 3.x at implementation time (not looked up as precisely as the other versions above) |
| New `pom.xml` version number | unresolved | — | propose `2.0.0` (major bump signaling the implementation-language change under an unchanged contract); final call belongs to whoever cuts the release, not this branch |
| README.md / NOTICE.md wording | unresolved | — | both need small updates (Java 11 -> Scala 3 install notes; `ToBoolean`'s "derived from json-logic-java" note should point at the Scala port, still logically derived) — small doc chores, not a design risk |

## Changes from revision 1 (this check pass)

- Guava removal was going to be a straight swap for a hand-rolled `synchronized`/`LinkedHashMap` LRU.
  Caught during self-review: that would put a single global lock around every `apply`/`check` call
  across all tenants sharing one of `segmentation-engine-service`'s three static `JsonLogic` instances
  — a real availability risk at this platform's stated throughput targets, not a theoretical one.
  Replaced with Caffeine (A8) — already trusted elsewhere in this org's stack.
- Added A9/A10 and the numeric-literal-conversion algorithm — circe has no built-in equivalent to
  Jackson's default numeric-type inference, and this repo's own tests (`VariableTests.testDefaultValue`)
  depend on getting it right for pass-through literals.
- Added the ScalaTest cross-boxed-numeric-equality trap under Test migration — without it, several
  ported tests (`NumberTests`, the chrono/date-diff/math suites) would silently stop proving what they
  proved in JUnit, while still reporting green.
- Confirmed (via a Java-file spike, not yet an open question) that `Operator`/`CompiledExpression`
  should stay plain `.java` files rather than gambling on Scala-trait-to-Java-SAM compilation; this was
  the one place the "wrappers are OK" allowance is actually used.
- Confirmed A7 (unchecked exception) is a strictly safe change by tracing every call site up two
  levels in `segmentation-engine-service` — no `try/catch JsonProcessingException` exists anywhere in
  that chain.
- Added the local-SNAPSHOT verification recipe, since this repo can't otherwise prove the Java-facing
  contract holds without a real Java compiler pointed at the new jar.
