# Review: date/timestamp operators (`story/datetime-support`)

Reviewed: working tree (uncommitted) against `main` (`03f0d1b`) — one section below, this repo's
only module.

**Verdict:** ship. One High finding surfaced during this pass (`InOperator`'s numeric-membership
gap) — fixed, with a zero-checked regression test. A second `check` pass found nothing further.

## Modules touched

| Module/Tool | Why touched |
|---|---|
| `json-logic-compiler-java` | Implements the 11 operators from `docs/plan.md`: 4 generators (`now`, `today`, `date_add`, `date_truncate`), 5 component extractors (`year`, `month`, `day`, `hour`, `day_of_week`), `date_eq`, `date_diff`. |

## json-logic-compiler-java

### New terms

| Term | Meaning | Scope |
|---|---|---|
| generator (date) | An operator returning a millis timestamp: `now`, `today`, `date_add`, `date_truncate`. | shared |
| component extractor | An operator returning one calendar/clock field as a plain `Integer`: `year`, `month`, `day`, `hour`, `day_of_week`. | shared |
| chrono unit | The shared, singular string vocabulary (`minute`/`hour`/`day`/`week`/`month`/`year`) taken by `date_add`/`date_truncate`/`date_diff`. | shared |

### New or changed scenarios

None. S1 (`check`) and S2 (`apply`) gain the operators above as new defaults; no new entrypoint,
matching `docs/plan.md`.

### Assumptions and limitations touched

- A3, A4, A5 (new, per `docs/plan.md`) — confirmed as implemented: `helpers/DateHelper.resolveZone`
  treats a missing `tz` argument as UTC and reads no ambient state (A3); every date value crossing
  an operator boundary is a plain `Long`/`Integer`, never a boxed date type (A4); `date_eq` and the
  extractors compose with pre-existing operators (`==`, chained comparison, `in`, `and`/`or`, `!`)
  rather than adding one bespoke operator per named case (A5) — `date==`, `is_anniversary`, and
  `in_time_window` all resolved to compositions, none became a separate operator.
- L2 ("no date/time support of any kind") is retired by this branch — superseded, not deleted; see
  Errata in `docs/rules/process.md`.

### Gaps in this change (chapter 3, Design)

| Severity | Ref | Scenario | Gap | Threatens |
|---|---|---|---|---|
| Medium | `helpers/DateHelper.java` (`toMillis`) | S1, S2 | Continues G2 (silent-fail coercion): a non-`Number` value passed where a date is expected coerces to `0` rather than raising an error — same fail-open shape as the existing `ToDouble`, deliberately left unresolved (the fail-open-vs-closed choice is still an open question in `docs/plan.md`). By contrast, `toChronoUnit`/`toChronoField` throw `IllegalArgumentException` for an unrecognized string on purpose: a fixed-vocabulary argument is a rule-authoring mistake, not a data-quality issue, and (`DateGeneratorOperatorTests.testMalformedUnitThrowsAtCompileTime`) proves it surfaces at compile time. | G2 |
| Low | `operators/DateAddOperator.java`, `operators/DateDiffOperator.java` | S1, S2 | `date_add`/`date_diff` do UTC-only calendar arithmetic for `month`/`year` (no `tz` argument on either operator, unlike `today`/`date_truncate`/the extractors) — a deliberate implementation choice made while coding, not a data point `docs/plan.md` states anywhere. Worth folding into that doc's operator table so it isn't only discoverable by reading the source. | — |

### Known issues (chapter 4, Implementation)

| Issue | Severity | Ref | Gap | Threatens | Scenario | Proof |
|---|---|---|---|---|---|---|
| **I2 — `InOperator`'s list-membership test used raw `equals()`, silently rejecting a numerically-equal cross-type pair** (e.g. a `Long` needle against `Integer`-parsed JSON literals). Surfaced by this branch's own operators: `date_diff` returns `Long`, and `{"in": [{"date_diff": [...]}, [1,2,3]]}` returned `false` for an actual diff of `2` before the fix, since `Long(2).equals(Integer(2))` is `false`. Not hypothetical — the day-of-week/hour-range compositions this branch's own docs advertise as the headline "stackable primitives" pattern (`{"in": [{"day_of_week": [...]}, [6,7]]}`) hit the identical class of bug had the extractors returned `Long` instead of `Integer`. Fixed: `InOperator`'s list branch now does numeric loose-equality (`doubleValue()`) when the needle is a `Number`, matching `EqualityOperator`'s existing cross-numeric-type handling. | High | `operators/InOperator.java` | — | A1 (dynamic, duck-typed evaluation) | S1, S2 | `InExpressionTests.testCrossNumericTypeMembership` — reproduced failing before the fix (via a throwaway `date_diff`-based repro, since removed), passing after |

### Test coverage map (chapter 5, Tests)

| Test | Scenario | File | Covers | vs. before |
|---|---|---|---|---|
| T1–T13 | S1, S2 | `DateGeneratorOperatorTests.java` | `now`/`today`/`date_add`/`date_truncate`: UTC default, explicit `tz`, negative-amount shift, calendar-month-overflow clamping, week/month/year truncation boundaries, malformed-unit compile-time throw, and — added during the `check` pass — that `now()` reads the clock fresh on every evaluation of the *same cached* compiled expression, not just once across separate expression strings (C1's most safety-critical property) | new |
| T1–T8 | S1, S2 | `ChronoFieldExtractorOperatorTests.java` | All 5 extractors; ISO day-of-week numbering; composition with pre-existing chained comparison and `in` | new |
| T1–T5 | S1, S2 | `DateDiffOperatorTests.java` | Sign convention, `week` unit, calendar-aware `month` counting, composition with `>=` | new |
| T1–T5 | S1, S2 | `DateEqOperatorTests.java` | Default vs. explicit `components`, the birthday case, negation via the existing `!`, explicit-`tz` day-boundary crossing | new |
| T1 (`testCrossNumericTypeMembership`) | S1, S2 | `InExpressionTests.java` | I2's fix | new, on a pre-existing suite |
