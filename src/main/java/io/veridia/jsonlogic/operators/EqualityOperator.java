package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;
import io.veridia.jsonlogic.helpers.ToDouble;

import java.util.List;

public class EqualityOperator implements Operator {
    public static final EqualityOperator INSTANCE = new EqualityOperator();

    public String key() {
        return "==";
    }

    public CompiledExpression compile(List<CompiledExpression> args) {
        return ctx -> {
            Object left = args.get(0).eval(ctx);
            if (left == null) {
                left = 0.0;
            }

            Object right = args.get(1).eval(ctx);
            if (right == null) {
                right = 0.0;
            }

            // Check numeric loose equality
            if (
                    (left instanceof Number && right instanceof Number)
                            || (left instanceof Number && right instanceof String)
                            || (left instanceof String && right instanceof Number)
            ) {
                return Double.valueOf(ToDouble.eval(left)).equals(ToDouble.eval(right));
            }

            if (
                    (left instanceof Number && right instanceof Boolean)
                            || (left instanceof Boolean && right instanceof Number)
                            || (left instanceof String && right instanceof Boolean)
                            || (left instanceof Boolean && right instanceof String)
            ) {
                return ToBoolean.eval(left) == ToBoolean.eval(right);
            }

            // Check string loose equality
            if (left instanceof String && right instanceof String) {
                return left.equals(right);
            }

            // Check boolean loose equality
            if (left instanceof Boolean && right instanceof Boolean) {
                return ((Boolean) left).booleanValue() == ((Boolean) right).booleanValue();
            }

            return !ToBoolean.eval(left) && !ToBoolean.eval(right);
        };
    }
}
