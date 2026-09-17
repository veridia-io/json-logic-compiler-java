# Benchmarks

Microbenchmarks for the JSON Logic evaluation hot paths, measured with
[JMH](https://openjdk.org/projects/code-tools/jmh/). Benchmarks live under
`src/test/java/io/veridia/jsonlogic/benchmark/` and are opt-in via a Maven profile, so they never
slow down a normal build.

## Running

```bash
# Default run (JsonLogicBenchmark, JMH annotation defaults: 3 forks, 5 warmup + 10 measurement
# iterations, 2s each — tuned for a stable number without hand-tuned flags; ~1 min/benchmark)
mvn -Pbenchmark test-compile exec:exec

# Quicker, noisier smoke run
mvn -Pbenchmark test-compile exec:exec -Dbenchmark.args="JsonLogicBenchmark -f 1 -wi 2 -i 3"

# Just one benchmark, for chasing down a specific noisy result
mvn -Pbenchmark test-compile exec:exec -Dbenchmark.args="math_compiled -f 5 -i 15"

# Write machine-readable results
mvn -Pbenchmark test-compile exec:exec -Dbenchmark.args="JsonLogicBenchmark -rf json -rff result.json"
```

Each expression is measured two ways:

- **`*_apply`** — the full `JsonLogic.apply(String, Object)` path (cache lookup + eval).
- **`*_compiled`** — a pre-compiled `CompiledExpression.eval(Object)` obtained once via
  `JsonLogic.compile(String)`, the zero-overhead path for high-throughput callers.

## Results

Throughput in **operations per microsecond** (higher is better). The table below is a same-JDK,
same-machine `compiled`-path comparison (JDK 25, macOS arm64, 5 forks × 10 iterations, 1s each)
between this implementation and the pre-rewrite Java/Jackson/Guava one (git commit `0050f76`, via
a worktree, same benchmark class). An older baseline measured on Temurin 21 is not included here —
it made the rewrite look like it regressed `flatVar`/`deepVar` by ~12%, which was entirely a JDK
21-vs-25 artifact, not a real difference; always compare same-JDK, same-machine, not against a
number measured on a different runtime.

| Expression | pre-rewrite (Java) | this version (Scala) | Δ |
|---|---:|---:|---:|
| `flatVar` — `{"var":"age"}` | 284.4 ± 22.8 (noisy) | 258.0 ± 3.5 | ~−9%, but the baseline here isn't trustworthy — a dedicated 1-benchmark run (2s iterations) put both within noise of each other (~261 ops/µs each) |
| `deepVar` — `{"var":"user.profile.address.zip"}` | 51.7 ± 0.2 | 47.3 ± 0.2 | −8.4% (real; `var` resolution doesn't share code with the fix below, so this gap wasn't touched by it) |
| `math` — `{"+":[{"var":"a"},{"*":[{"var":"b"},2]},3]}` | 28.6 ± 1.8 | 28.8 ± 3.9 | flat (both noisy; this expression's `_compiled` throughput is genuinely bimodal on this machine regardless of iteration count — not GC-driven, `gc.alloc.rate.norm` is a rock-solid 48 B/op — likely JIT-tiering or thermal-throttling noise, not a code issue) |
| `logic` — `{"and":[{">":[…]},{"<":[…]},{"==":[…]}]}` | 30.7 ± 0.7 | 33.2 ± 0.3 | **+8.1%** |
| `some` — `{"some":[{"var":"scores"},{">":[{"var":"item"},80]}]}` | 29.4 ± 0.2 | 29.9 ± 0.1 | **+1.9%** |

`logic`/`some` were the ones that mattered: an earlier version of this rewrite built each
operator's argument list via `.asJava` on a Scala `List` (a linked list), which several operators —
every chained/2-arg comparison among them — then indexed with `args.get(i)` *inside* their per-eval
closure, turning an O(1) array access into an O(i) linked-list walk on every evaluation. Fixed by
materializing a real `java.util.ArrayList` once per `compile()` call
(`JsonLogicCompiler.scala`) — `logic`/`some` went from double-digit regressions to matching or
beating the original. `deepVar`'s residual ~8% gap doesn't go through that code path (`var`
resolution is handled directly in `JsonLogicCompiler`, not via `Operator.compile`) and is most
likely the smaller, harder-to-eliminate cost of an extra method-call layer plus pattern-match
dispatch in `PathResolver` versus the original's inlined `instanceof` chain.

For hot loops, compile once and reuse the `CompiledExpression`:

```java
JsonLogic jsonLogic = new JsonLogic();
CompiledExpression rule = jsonLogic.compile("{\"and\":[{\">\":[{\"var\":\"age\"},18]}]}");

for (Map<String, Object> ctx : contexts) {
    Object result = rule.eval(ctx); // no parsing, hashing, or cache lookup per call
}
```
