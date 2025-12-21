package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToDouble;

import java.util.List;

public class GreatestLeastOperator implements Operator {
    public static final GreatestLeastOperator GREATEST = new GreatestLeastOperator(true);
    public static final GreatestLeastOperator LEAST = new GreatestLeastOperator(false);

    private final boolean isGreatest;

    GreatestLeastOperator(boolean isGreatest) {
        this.isGreatest = isGreatest;
    }

    @Override
    public String key() {
        return isGreatest ? "greatest" : "least";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 2) {
            return ctx -> 0.0;
        }

        CompiledExpression leftExpr = args.get(0);
        CompiledExpression rightExpr  = args.get(1);

        return ctx -> {
            Object rawLeft = leftExpr.eval(ctx);
            Object rawRight = rightExpr.eval(ctx);

            double left = ToDouble.eval(rawLeft);
            double right = ToDouble.eval(rawRight);

            return isGreatest ? Math.max(left, right) : Math.min(left, right);
        };
    }
}
