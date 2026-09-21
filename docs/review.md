# Review: Scala 3 rewrite (`rewrite/scala`)

Reviewed: working tree (uncommitted) against `main` (`0050f76`) — one section below, this repo's
only module. Implements `docs/plan.md` revision 2.

**Verdict:** ship. `mvn clean verify` is green (198/198 ScalaTest cases across 24 suites, jar/
sources/javadoc all build); the `-Pbenchmark` JMH profile runs against the new jar; a scratch,
uncommitted local-SNAPSHOT compile of `segmentation-engine-service` (in `api-gateways`) against
this jar succeeds with **zero source changes**, including `AssignGroupOperator`, the one external
custom `Operator`. An independent adversarial pass (fresh agent, `git show`-diffing every operator
against its pre-rewrite Java source) surfaced two real findings (I1, I2) — both fixed, with
zero-checked regression tests, below. A third finding (I3) surfaced afterward from a reported CI
failure: a pre-existing, JDK-11-specific test bug (predating this branch, carried forward from the
pre-rewrite Java suite), not a rewrite regression — also fixed below. Three Low-severity, deliberate
behavior divergences also surfaced during the port, all for input shapes the original library's own
README already documents as unsupported/undefined and none exercised by any existing test —
recorded below rather than silently dropped.

## Modules touched

| Module/Tool | Why touched |
|---|---|
| `json-logic-compiler-java` | Full rewrite of `src/main` from Java 11 to Scala 3 (circe, ADTs, pattern matching); ~41 default operators regrouped from 23 files into 7 domain objects; Guava replaced by Caffeine; all 22 JUnit test classes ported to ScalaTest plus one new suite. |

## json-logic-compiler-java

### New or changed scenarios

None. S1 (`check`), S2 (`apply`), S3 (`registerOperator`) — see `docs/json-logic-compiler-java.md`
chapter 1 — keep their signatures and behavior.

### Assumptions and limitations touched

- A7 (plan) confirmed: `apply`/`check`/`compile` now throw unchecked `JsonLogicException` instead
  of checked `JsonProcessingException`. Verified safe by tracing every `segmentation-engine-service`
  call site two levels up — none catches the checked type; `PipelineProcessingService`'s
  `throws IOException` becomes a harmless unused declaration.
- A8 (plan) confirmed: the compiled-expression cache and the regex pattern cache are Caffeine-backed
  (`cache/BoundedCache.scala`, `operators/Strings.scala`), same 1000/1024-entry bound, no time-based
  eviction — L1 in `docs/json-logic-compiler-java.md` still holds unchanged.
- A9/A10 (plan) confirmed: `values/Values.scala`'s `jsonNumberToValue` mimics Jackson's numeric-type
  inference (fractional/exponent -> `Double`; integral -> smallest of `Integer`/`Long`/`BigInteger`);
  proven by the new `NumberLiteralFidelitySpec`, not by any ported test (Jackson's equivalent
  behavior was relied upon, never itself under test).
- C3/C4 (plan) confirmed: every operator's individual arity-validation behavior (throw vs.
  silent-fallback) and the chained-comparison double-evaluation of interior arguments are preserved
  exactly — verified by the ported suites, which assert both.

### Gaps in this change (chapter 3, Design)

| Severity | Ref | Scenario | Gap | Threatens |
|---|---|---|---|---|
| Low | `LogicParser.scala` (`parseField`, literal-array branch) | S1, S2 | A literal array containing an operator-shaped object (e.g. `[1, {"var": "x"}]` as a literal, not an operator's argument list) is now a plain nested literal (the object itself, converted via `Values.jsonToValue`), where the old Jackson-based parser collapsed `{"var": "x"}` specifically to the string `"x"` (an accidental side effect of routing every literal element through the full operator-aware `parse`+`exprToLiteral`, not a documented behavior). The library's own README states "arrays inside literals must contain literals only," so this input shape was already unsupported; no test exercises either the old or new behavior. | — |
| Low | `LogicParser.scala` (`parseVar`, default-value branch) | S1, S2 | A `var` default that is itself an operator-shaped object (e.g. `{"var": ["a", {"+": [1,2]}]}`) is now treated as a literal object, not silently evaluated to `null` the way the old parser's `parse(...).constant` roundabout produced (since an `Op` node's `.constant` field was always `null`). Not a documented feature of either version; no test exercises it. | — |
| Low | `PathResolver.scala` (`step`, `Array[AnyRef]` case) | S1, S2 | A `var` path segment navigating into a **primitive** array context value (`int[]`, `boolean[]`, etc. — not `Object[]`/`String[]`) now resolves to `null` instead of the old code's `ClassCastException` (`(Object[]) current` on a primitive array throws in Java; Scala's `case a: Array[AnyRef]` simply doesn't match a primitive array, falling through to the `null` case). No test in either version passes a primitive array as a context value for path navigation. | — |

### Known issues (chapter 4, Implementation)

I1/I2 found by the independent adversarial pass (a second, differently-scoped reviewer than the
self-review that produced the gaps above); I3 found via a CI failure reported after this section's
original "ship" verdict. All three fixed in this branch, not merely recorded.

| Issue | Severity | Ref | Threatens | Scenario | Proof |
|---|---|---|---|---|---|
| **I1 — `round`'s exception firewall was dropped.** The original `RoundOperator` wraps its entire per-eval body — including evaluating its own two argument sub-expressions, not just the arithmetic — in `catch (Exception e) { return 0.0; }`. The port initially reproduced only `ToDouble`'s narrow `NumberFormatException` handling, so an exception thrown by a *nested* sub-expression (e.g. `year`/`date_eq`/etc. fed an invalid caller-supplied timezone string, which throws `ZoneRulesException`) propagated out of `apply`/`check` uncaught instead of failing closed to `0.0`. Fixed: restored the same blanket per-eval try/catch. | High | `operators/Arithmetic.scala` (`Round`) | round's documented fail-open contract | S1, S2 | `RoundOperatorTest.scala` ("a nested arg that throws at eval time is caught and falls back to 0.0") — reproduced failing before the fix (traced by hand, not committed failing), passing after |
| **I2 — string operators lost a null-after-`toString()` re-check.** `contains`/`starts_with`/`ends_with`/`regex_match` convert their operands via `String.valueOf`, which returns whatever a *non-null* object's own `toString()` returns — including `null`, for a pathological override. The original code re-checks for `null` after this conversion (`Objects.toString(x, null)` then a second null guard) and returns `false`; the port skipped straight to invoking the string operation, throwing `NullPointerException` for this case. Fixed: restored the post-conversion null check. | Low | `operators/Strings.scala` (`stringOp`, `RegexMatch`) | none (pathological-input edge case, not a documented contract) | S1, S2 | `StringExpressionTests.scala` ("a non-null value whose own toString() returns null is false, not an NPE", ×2: `contains`, `regex_match`) — reproduced failing before the fix, passing after |
| **I3 — `date_truncate`'s explicit-timezone test computed its expected value with a JDK-version-sensitive `Instant.parse` call, not a production-code defect.** `DateGeneratorOperatorTests`'s "honors an explicit timezone" case asserted against `Instant.parse("2024-03-15T00:00:00+09:00")`; `Instant.parse` uses `DateTimeFormatter.ISO_INSTANT`, which throws `DateTimeParseException` for any offset other than `Z` on JDK 11 (`main`'s current CI `java-version`, pre-dating this branch's bump to 17 — see A7-adjacent build note) but parses it without error on JDK 17/21/25 (verified locally against all three). The CI trace reported (`DateGeneratorOperatorTests.java:111`, method `testDateTruncateHonorsExplicitTimezone`) matches the pre-rewrite Java test at `0050f76:src/test/java/io/veridia/jsonlogic/DateGeneratorOperatorTests.java:111` exactly — this bug predates the rewrite and was carried forward verbatim into the Scala port (`DateGeneratorOperatorTests.scala:83`) rather than introduced by it. The `date_truncate` operator itself (`operators/DateTime.scala`) is unaffected; only the test's own expected-value computation was JDK-version-fragile. Fixed: expected value now computed via `OffsetDateTime.parse(...).toInstant`, which parses an explicit offset on every JDK version. | Medium | `DateGeneratorOperatorTests.scala:83` | none (test-only; no production `assumption`/`constraint`) | S1, S2 | `DateGeneratorOperatorTests.scala` ("date_truncate: honors an explicit timezone") — reproduced failing on JDK 11 per the reported CI trace (not reproducible locally, no JDK 11 available in this environment; confirmed passing unchanged on JDK 17/21/25 before and after the fix, and the fix removes the JDK-11-specific failure mode by construction) |

### Test coverage map (chapter 5, Tests)

All 22 original JUnit suites ported 1:1 by file (production code was regrouped by domain; tests
were not, so this table stays easy to audit against the file list above). vs. before: **ported**
(behavior-preserving translation) unless noted.

| Test file | Scenario | vs. before |
|---|---|---|
| `GeneralTests`, `VariableTests`, `TruthyTests`, `EqualityExpressionTests`, `InequalityExpressionTests`, `NotExpressionTests`, `LogicAndOrTests`, `NumericComparisonExpressionTests`, `MathExpressionTests`, `GreatestLeastOperatorTests`, `AllExpressionTests`, `ArrayHasExpressionTests`, `PresenceExpressionTests`, `DateEqOperatorTests` | S1, S2 | ported |
| `DateGeneratorOperatorTests` | S1, S2 | ported, **fixed**: I3's JDK-11-sensitive `Instant.parse` expected-value computation replaced with `OffsetDateTime.parse` |
| `RoundOperatorTest` | S1, S2 | ported, **expanded**: added the zero-checked regression test for I1 |
| `StringExpressionTests` | S1, S2 | ported, **expanded**: added the two zero-checked regression tests for I2 |
| `NumberTests` | S1, S2 | ported, **expanded**: added explicit `getClass` checks — Scala's `==`/`shouldEqual` is numeric-value-equal across boxed types (unlike JUnit's `assertEquals`), so a value-only port would silently stop proving the test's actual point (exact numeric subtype preservation) |
| `ChronoFieldExtractorOperatorTests`, `DateDiffOperatorTests` | S1, S2 | ported, expanded with the same explicit type check (`Integer` vs. `Long`), for the same reason |
| `InExpressionTests` | S1, S2 | ported, **expanded**: added a `Map`-container membership case (G3 in the pre-rewrite module doc — untested in the original suite; cheap to close during the port) |
| `ExpandabilityTests` | S3 | ported (its own local test-only `AssignGroupOperator`, not `segmentation-engine-service`'s, re-implemented in Scala against the `Operator`/`CompiledExpression` Java SPI) |
| `NumberLiteralFidelitySpec` | S1, S2 | **new** — proves A9, which has no equivalent in the pre-rewrite suite (Jackson's numeric-type inference was relied upon, never tested directly) |

LOC, main lib only (excludes the 2 unchanged Java SPI files, per the "except wrappers" scope): 1678
-> 850, a 49.3% reduction. Tests: 1866 -> 1081 (42.1%), while gaining the coverage above.
