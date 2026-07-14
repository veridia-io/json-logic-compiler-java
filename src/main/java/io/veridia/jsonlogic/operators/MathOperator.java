package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToDouble;

import java.util.List;
import java.util.function.DoubleBinaryOperator;

public class MathOperator implements Operator {
    public static final MathOperator ADD = new MathOperator("+", Double::sum);
    public static final MathOperator SUBTRACT = new MathOperator("-", (a, b) -> a - b);
    public static final MathOperator MULTIPLY = new MathOperator("*", (a, b) -> a * b);
    public static final MathOperator DIVIDE = new MathOperator("/", (a, b) -> a / b);
    public static final MathOperator MODULO = new MathOperator("%", (a, b) -> a % b);
    public static final MathOperator MIN = new MathOperator("min", Math::min);
    public static final MathOperator MAX = new MathOperator("max", Math::max);

    private final String op;
    private final boolean isSubtract;
    private final boolean isDivide;
    // Primitive reducer — no autoboxing of intermediate results on the eval hot path.
    private final DoubleBinaryOperator reducer;

    MathOperator(String op, DoubleBinaryOperator reducer) {
        this.op = op;
        this.isSubtract = op.equals("-");
        this.isDivide = op.equals("/");
        this.reducer = reducer;
    }

    @Override
    public String key() {
        return op;
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        // Materialize args once at compile time: indexed array, no per-eval iterator or stream.
        CompiledExpression[] a = args.toArray(new CompiledExpression[0]);

        return ctx -> {
            double first = ToDouble.eval(a[0].eval(ctx));

            if (a.length == 1) {
                if (isSubtract) return -first;
                if (isDivide) return null;
                return first;
            }

            double second = ToDouble.eval(a[1].eval(ctx));
            if (isDivide && second == 0.0) return 0.0;

            double accumulator = reducer.applyAsDouble(first, second);
            for (int i = 2; i < a.length; i++) {
                accumulator = reducer.applyAsDouble(accumulator, ToDouble.eval(a[i].eval(ctx)));
            }

            return accumulator;
        };
    }
}
