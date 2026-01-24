package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class StringOperator implements Operator {
    public static final StringOperator CONTAINS = new StringOperator("contains", String::contains);
    public static final StringOperator STARTS_WITH = new StringOperator("starts_with", String::startsWith);
    public static final StringOperator ENDS_WITH = new StringOperator("ends_with", String::endsWith);

    private final String op;
    private final BiFunction<String, String, Boolean> reducer;

    StringOperator(String op, BiFunction<String, String, Boolean> reducer) {
        this.op = op;
        this.reducer = reducer;
    }

    @Override
    public String key() {
        return op;
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 2)
            return ctx -> false;

        return ctx -> {
            Object left = args.get(0).eval(ctx);
            Object right = args.get(1).eval(ctx);

            if (left == null || right == null) {
                return false;
            }

            String leftStr = Objects.toString(left, null);
            String rightStr = Objects.toString(right, null);

            if (leftStr == null || rightStr == null) {
                return false;
            }

            return this.reducer.apply(leftStr, rightStr);
        };
    }
}
