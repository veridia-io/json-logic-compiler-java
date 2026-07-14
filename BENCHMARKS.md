# Benchmarks

Microbenchmarks for the JSON Logic evaluation hot paths, measured with
[JMH](https://openjdk.org/projects/code-tools/jmh/). Benchmarks live under
`src/test/java/io/veridia/jsonlogic/benchmark/` and are opt-in via a Maven profile, so they never
slow down a normal build.

## Running

```bash
# Default run (JsonLogicBenchmark, JMH annotation defaults: 1 fork, 5 warmup + 5 measurement iters)
mvn -Pbenchmark test-compile exec:exec

# Custom JMH flags
mvn -Pbenchmark test-compile exec:exec -Dbenchmark.args="JsonLogicBenchmark -f 2 -wi 3 -i 5"

# Write machine-readable results
mvn -Pbenchmark test-compile exec:exec -Dbenchmark.args="JsonLogicBenchmark -rf json -rff result.json"
```

Each expression is measured two ways:

- **`*_apply`** — the full `JsonLogic.apply(String, Object)` path (cache lookup + eval).
- **`*_compiled`** — a pre-compiled `CompiledExpression.eval(Object)` obtained once via
  `JsonLogic.compile(String)`, the zero-overhead path for high-throughput callers.

## Results

Throughput in **operations per microsecond** (higher is better). Measured on Temurin 21 (macOS,
arm64), 2 forks × 4 iterations. Absolute numbers are machine-specific; use them for relative
comparison, not as portable guarantees.

| Expression | `apply` (ops/µs) | `compiled` (ops/µs) |
|------------|-----------------:|--------------------:|
| `flatVar` — `{"var":"age"}` | 42.2 ± 0.9 | 298.6 ± 9.4 |
| `deepVar` — `{"var":"user.profile.address.zip"}` | 25.1 ± 0.2 | 51.0 ± 3.5 |
| `math` — `{"+":[{"var":"a"},{"*":[{"var":"b"},2]},3]}` | 16.6 ± 2.5 | 57.2 ± 1.4 |
| `logic` — `{"and":[{">":[…]},{"<":[…]},{"==":[…]}]}` | 19.6 ± 1.3 | 35.6 ± 0.3 |
| `some` — `{"some":[{"var":"scores"},{">":[{"var":"item"},80]}]}` | 18.0 ± 0.6 | 26.5 ± 3.8 |

For hot loops, compile once and reuse the `CompiledExpression`:

```java
JsonLogic jsonLogic = new JsonLogic();
CompiledExpression rule = jsonLogic.compile("{\"and\":[{\">\":[{\"var\":\"age\"},18]}]}");

for (Map<String, Object> ctx : contexts) {
    Object result = rule.eval(ctx); // no parsing, hashing, or cache lookup per call
}
```
