package io.veridia.jsonlogic.benchmark;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.JsonLogic;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.CommandLineOptionException;
import org.openjdk.jmh.runner.options.CommandLineOptions;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Microbenchmarks for the JSON Logic evaluation hot paths.
 * <p>
 * Run with: {@code mvn -Pbenchmark test-compile exec:java}
 * <p>
 * Two flavours are measured per expression:
 * <ul>
 *   <li><b>*_apply</b> — the full {@link JsonLogic#apply(String, Object)} path (cache lookup + eval),
 *       exercising the string-keyed cache that replaced per-call murmur hashing.</li>
 *   <li><b>*_compiled</b> — a pre-compiled {@link CompiledExpression#eval(Object)}, the zero-overhead
 *       loop callers should use for millions of executions per second.</li>
 * </ul>
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class JsonLogicBenchmark {

    private static final String FLAT_VAR = "{\"var\":\"age\"}";
    private static final String DEEP_VAR = "{\"var\":\"user.profile.address.zip\"}";
    private static final String MATH = "{\"+\":[{\"var\":\"a\"},{\"*\":[{\"var\":\"b\"},2]},3]}";
    private static final String LOGIC = "{\"and\":[{\">\":[{\"var\":\"age\"},18]},{\"<\":[{\"var\":\"age\"},65]},{\"==\":[{\"var\":\"country\"},\"US\"]}]}";
    private static final String SOME = "{\"some\":[{\"var\":\"scores\"},{\">\":[{\"var\":\"item\"},80]}]}";

    private JsonLogic jsonLogic;
    private Map<String, Object> context;

    private CompiledExpression flatVar;
    private CompiledExpression deepVar;
    private CompiledExpression math;
    private CompiledExpression logic;
    private CompiledExpression some;

    @Setup
    public void setup() throws JsonProcessingException {
        jsonLogic = new JsonLogic();

        Map<String, Object> address = new HashMap<>();
        address.put("zip", "94107");

        Map<String, Object> profile = new HashMap<>();
        profile.put("address", address);

        Map<String, Object> user = new HashMap<>();
        user.put("profile", profile);

        List<Object> scores = new ArrayList<>();
        scores.add(72);
        scores.add(85);
        scores.add(90);

        context = new HashMap<>();
        context.put("age", 30);
        context.put("country", "US");
        context.put("a", 10);
        context.put("b", 5);
        context.put("user", user);
        context.put("scores", scores);

        flatVar = jsonLogic.compile(FLAT_VAR);
        deepVar = jsonLogic.compile(DEEP_VAR);
        math = jsonLogic.compile(MATH);
        logic = jsonLogic.compile(LOGIC);
        some = jsonLogic.compile(SOME);
    }

    @Benchmark
    public Object flatVar_apply() throws JsonProcessingException {
        return jsonLogic.apply(FLAT_VAR, context);
    }

    @Benchmark
    public Object flatVar_compiled() {
        return flatVar.eval(context);
    }

    @Benchmark
    public Object deepVar_apply() throws JsonProcessingException {
        return jsonLogic.apply(DEEP_VAR, context);
    }

    @Benchmark
    public Object deepVar_compiled() {
        return deepVar.eval(context);
    }

    @Benchmark
    public Object math_apply() throws JsonProcessingException {
        return jsonLogic.apply(MATH, context);
    }

    @Benchmark
    public Object math_compiled() {
        return math.eval(context);
    }

    @Benchmark
    public Object logic_apply() throws JsonProcessingException {
        return jsonLogic.apply(LOGIC, context);
    }

    @Benchmark
    public Object logic_compiled() {
        return logic.eval(context);
    }

    @Benchmark
    public Object some_apply() throws JsonProcessingException {
        return jsonLogic.apply(SOME, context);
    }

    @Benchmark
    public Object some_compiled() {
        return some.eval(context);
    }

    public static void main(String[] args) throws RunnerException, CommandLineOptionException {
        // The Maven exec:exec wiring passes all JMH flags as a single argument (e.g.
        // "JsonLogicBenchmark -f 1 -i 3"), so split a lone space-containing arg into tokens.
        String[] jmhArgs = args;
        if (args.length == 1) {
            jmhArgs = args[0].trim().split("\\s+");
        }

        // Honor standard JMH CLI flags when provided; otherwise fall back to the class annotations.
        boolean hasArgs = jmhArgs.length > 0 && !jmhArgs[0].isEmpty();
        Options opt = hasArgs
                ? new CommandLineOptions(jmhArgs)
                : new OptionsBuilder().include(JsonLogicBenchmark.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
