package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToDouble;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

public class MathOperator implements Operator {
    public static final MathOperator ADD = new MathOperator("+", Double::sum);
    public static final MathOperator SUBTRACT = new MathOperator("-", (a, b) -> a - b);
    public static final MathOperator MULTIPLY = new MathOperator("*", (a, b) -> a * b);
    public static final MathOperator DIVIDE = new MathOperator("/", (a, b) -> a / b);
    public static final MathOperator MODULO = new MathOperator("%", (a, b) -> a % b);
    public static final MathOperator MIN = new MathOperator("min", Math::min);
    public static final MathOperator MAX = new MathOperator("max", Math::max);

    private final String op;
    private final BiFunction<Double, Double, Double> reducer;

    MathOperator(String op, BiFunction<Double, Double, Double> reducer) {
        this.op = op;
        this.reducer = reducer;
    }

    public String key() {
        return op;
    }

    public CompiledExpression compile(List<CompiledExpression> args) {
        return ctx -> {
            Double[] values = args.stream().map(element -> ToDouble.eval(element.eval(ctx))).toArray(Double[]::new);
            if (values.length == 1) {
                if (op.equals("-")) {
                    return -values[0];
                }

                if (op.equals("/")) {
                    return null;
                }
            }

            if (Objects.equals(op, "/") && values[1] == 0.0)
                return 0.0;

            double accumulator = values[0];

            for (int i = 1; i < values.length; i++) {
                accumulator = reducer.apply(accumulator, values[i]);
            }

            return accumulator;
        };
    }
}
