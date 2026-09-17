# Plan: date/timestamp operators (`story/datetime-support`)

Companion branch: `api-gateways`'s `story/datetime-predicates` (`docs/plan.md` there covers
`segmentation-engine-service` and the `JSON Logic` tool doc).

## Modules touched

| Module/Tool | Why touched |
|---|---|
| `json-logic-compiler-java` | Add date/timestamp-aware operators to the default operator set, consumed by `segmentation-engine-service`'s segment/aggregate/metric rule evaluation. |

**Out of scope:** `node-control-plane`/`node-data-plane` — separate repos, not touched by this
branch. See the companion `api-gateways` `docs/plan.md` and `docs/dt-predicates-comparison.md`'s
Node-side awareness section for what was checked there and why it's excluded.

## json-logic-compiler-java

Desired end state: a tenant-authored rule can express date/timestamp predicates — "today is the
user's birthday", a wall-clock-relative comparison — through new operators registered in
`JsonLogicCompiler.registerDefaultOperators`, without breaking `docs/json-logic-compiler-java.md`'s
C1 (no per-evaluation-varying value may be memoized at `compile()` time).

**Representation:** epoch-millis `Long`/`Double`, matching the convention every date field already
uses on the `rpc-contracts` side. No new value type, so no change to `LogicParser`,
`helpers/ToDouble`, `helpers/ToBoolean`, or `EqualityOperator`/`InequalityOperator` — every existing
comparison operator (`>`,`<`,`>=`,`<=`,`==`,`!=`) already works, unmodified, against two millis
values or a millis value and a new generator operator's result. This is what keeps the change
additive-only: new operator classes plus a handful of lines in `registerDefaultOperators`, per the
established pattern (`docs/json-logic-compiler-java.md` R3).

**Operator names, argument order and semantics are deliberately modeled on
`react-awesome-query-builder`'s JsonLogic export** (`packages/core/modules/utils/jsonLogicUtils.js`,
`addRequiredJsonLogicOperations`/`customJsonLogicOperations`, confirmed from source, not docs prose)
— see Change in responsibilities below for exactly what's adopted, what's dropped, and what's ours
only. The concrete payoff of matching their operator *keys* where semantics allow: a rule exported by
that UI (if it's ever adopted as a tenant-facing or internal rule-authoring surface — an
`api-gateways`-side decision, not this repo's) evaluates here with no translation layer, for the
operators that do match. Where our millis representation lets us do better than theirs, we do — we
don't copy a workaround that exists only because of *their* representation choice.

### New or changed scenarios

None. S1 (`check`) and S2 (`apply`) gain a richer default operator set; no new entrypoint. S3
(`registerOperator`) is unaffected — these ship as defaults, not externally-registered custom
operators.

### Change in responsibilities

Built from two small, orthogonal primitive families plus two generators, composed with operators the
compiler already had before this branch — rather than one bespoke operator per named use case. See
A5 below for why orthogonality was chosen over a smaller operator count.

- **Generators** (return a millis value), adopted from `react-awesome-query-builder`'s naming where
  it applies: `now()`, `today(tz?)`, `date_add(value, amount, unit)`, `date_truncate(value, unit)`.
  Collapses their `date_add`/`datetime_add` and `today`/`start_of_today` pairs into one operator
  each — our single millis representation and A3's explicit-`tz`-argument rule remove the reason
  each pair existed. `unit` ∈ `minute`/`hour`/`day`/`week`/`month`/`year` (singular; full chrono-unit
  reference, including the `java.time`/moment.js mapping for each, is in `api-gateways`'s
  `docs/dt-DRAFT.md`).
- **Component extractors** (return a plain `int`, no equivalent in their set at all): `year(value,
  tz?)`, `month(value, tz?)`, `day(value, tz?)`, `hour(value, tz?)`, `day_of_week(value, tz?)`. These
  are what make the family composable — they plug directly into operators R3 already registers by
  default: chained comparison for a range (`{"<=": [9, {"hour": [v]}, 17]}`), `in` for a set
  (`{"in": [{"day_of_week": [v]}, [6, 7]]}`), plain `==`/`and` for a component match.
- **One combinator**, because "compare N calendar components for equality" is common enough to earn
  a name on its own: `date_eq(a, b, components?, tz?)`, where `components` is a JSON literal array
  drawn from the same five names as the extractors above — `year`/`month`/`day`/`hour`/
  `day_of_week` — defaulting to `["year", "month", "day"]` when omitted. This is the operator
  answering "can we abstract the birthday case" — `date_eq(a, b, ["month", "day"])` *is* the
  anniversary check, and `date_eq(a, b)` *is* full calendar-date equality, with no separate operator
  for either.
- `date_diff(a, b, unit)` — a generator, not reducible to extractor-plus-comparison since a diff is a
  signed magnitude, not a component or a boolean. Missing from `react-awesome-query-builder`'s set
  too. Same `unit` vocabulary as `date_add`/`date_truncate`.

**No dedicated `date==`, `is_anniversary`, or `in_time_window`.** Each is now an instance of
composing the primitives above with operators the compiler already had:

| Named case | Composition |
|---|---|
| `date==(a, b)` (calendar-day equality, matches their op) | `{"date_eq": [a, b]}` |
| "is the user's birthday today" (month+day match, ignoring year) | `{"date_eq": [a, b, ["month", "day"]]}` |
| a time-of-day window | `{"<=": [start, {"hour": [v]}, end]}` |
| a wraparound time-of-day window (22:00–06:00) — something a 2-arg bespoke operator can't express at all | `{"or": [{">=": [{"hour": [v]}, 22]}, {"<": [{"hour": [v]}, 6]}]}` |

**Explicitly not adopted from `react-awesome-query-builder`, and why:** `datetime==`/`datetime!=` —
patch a `Date`/string identity-comparison bug our millis representation (A4) doesn't have; plain
`==`/`!=` already work. Their non-date operators (`CALL`, `JSX`, `mergeObjects`, `fromEntries`,
string ops) are out of scope or already covered under different names
(`docs/json-logic-compiler-java.md` R3).

Total: 11 operators (4 generators + 5 extractors + `date_eq` + `date_diff`).

- R4 (`var`/field resolution) is unchanged — `tz` travels as a normal operator argument (typically a
  caller-supplied `{"var": "context.timezone"}`), not a new resolution mechanism, per A1's "no
  ambient state" shape and A3 below.

### Change in assumptions, constraints, or limits

- New assumption A3 — **Timezone is always an explicit operator argument, never ambient/instance
  state.** *Enables:* the library stays free of a `Clock`/`ZoneId` constructor parameter or any other
  mutable per-instance default; a `JsonLogic` instance remains stateless and shareable. *Costs:* every
  call site that wants a non-UTC zone must plumb it into the context itself — this library adds no
  mechanism for discovering "the right" timezone. This is also *why* we don't need
  `react-awesome-query-builder`'s `today()`/`start_of_today()` split — their two ops exist to route
  around an ambient timezone; A3 means we never have one to route around. *Scenarios:* S1, S2.
- New assumption A4 — **A date value is a plain millis `Long`/`Double`, never a boxed date/time
  type.** *Enables:* every existing operator that already coerces via `ToDouble`
  (`NumericComparisonOperator`, `EqualityOperator`/`InequalityOperator`, `MathOperator`) keeps working
  on date values with zero changes, and — concretely — it's why this plan needs no `datetime==`/
  `datetime!=` equivalent: `react-awesome-query-builder` needs that pair only to patch a comparison
  bug specific to its `Date`-object/string representation (`CONFIG.adoc`'s
  `fixJsonLogicDateCompareOp`), a bug class this representation doesn't have. *Costs:* a date value is
  not self-describing on the wire — nothing distinguishes "a millis timestamp" from "a plain number"
  without external (field-definition) knowledge; not addressed by this branch. *Scenarios:* S1, S2.
- New assumption A5 — **A new date predicate is preferred as a composition of extractors and
  existing operators over a new named operator, unless the composition is common enough to justify a
  named combinator.** `date_eq` is the one precedent that clears that bar (component-equality is
  common enough); a bespoke time-window operator was judged not to (a chained comparison over
  `hour()` is no less readable, and composes to a wraparound window via `or` with no further design
  work, which a 2-arg bespoke operator couldn't do at all). *Enables:* the operator surface grows
  sublinearly with the number of predicates it can express; a future date predicate is a
  documentation exercise, not a new-operator branch, unless it's genuinely irreducible (as
  `date_diff` is). *Costs:* a rule author writes a multi-clause expression for a case a bespoke
  operator would have made one call — deliberate. *Scenarios:* S1, S2.
- L2 ("no date/time support") is retired by this branch once folded back — not deleted, superseded;
  see Errata in `docs/rules/process.md`.
- G2 (silent-fail coercion) is **not** closed by this branch. A malformed date argument (e.g. a
  non-numeric string passed to `date_diff`) still needs a decision — fail open (return a sentinel,
  consistent with `ToDouble`'s existing precedent) or fail closed (throw). See Open questions.

### Open questions

| Question | Resolution | Answer | Options |
|---|---|---|---|
| Fail-open (sentinel, matching `ToDouble`'s existing precedent) or fail-closed (throw) when a date operator receives an unparseable argument? | unresolved | — | fail-open (consistent with L3/G2's existing shape) / fail-closed (a wrong date comparison silently passing is worse than a visible error, for a segment-evaluation rule) |
