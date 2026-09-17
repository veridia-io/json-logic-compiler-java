package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;

import java.time.Instant;
import java.util.List;

public class NowOperator implements Operator {
    public static final NowOperator INSTANCE = new NowOperator();

    @Override
    public String key() {
        return "now";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        // Instant.now() must be read inside the returned lambda, never here: JsonLogic caches
        // this CompiledExpression indefinitely per unique expression string, so a value computed
        // at compile() time would be frozen for the cache entry's lifetime.
        return ctx -> Instant.now().toEpochMilli();
    }
}
