package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

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
            if (left instanceof Number && right instanceof Number) {
                return Double.valueOf(((Number) left).doubleValue()).equals(((Number) right).doubleValue());
            }

            if (left instanceof Number && right instanceof String) {
                return compareNumberToString((Number) left, (String) right);
            }

            if (left instanceof Number && right instanceof Boolean) {
                return compareNumberToBoolean((Number) left, (Boolean) right);
            }

            // Check string loose equality
            if (left instanceof String && right instanceof String) {
                return left.equals(right);
            }

            if (left instanceof String && right instanceof Number) {
                return compareNumberToString((Number) right, (String) left);
            }

            if (left instanceof String && right instanceof Boolean) {
                return compareStringToBoolean((String) left, (Boolean) right);
            }

            // Check boolean loose equality
            if (left instanceof Boolean && right instanceof Boolean) {
                return ((Boolean) left).booleanValue() == ((Boolean) right).booleanValue();
            }

            if (left instanceof Boolean && right instanceof Number) {
                return compareNumberToBoolean((Number) right, (Boolean) left);
            }

            if (left instanceof Boolean && right instanceof String) {
                return compareStringToBoolean((String) right, (Boolean) left);
            }

            return !ToBoolean.eval(left) && !ToBoolean.eval(right);
        };
    }

    private boolean compareNumberToString(Number left, String right) {
        try {
            if (right.trim().isEmpty()) {
                right = "0";
            }

            return Double.parseDouble(right) == left.doubleValue();
        }
        catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean compareNumberToBoolean(Number left, Boolean right) {
        if (right) {
            return left.doubleValue() == 1.0;
        }

        return left.doubleValue() == 0.0;
    }

    private boolean compareStringToBoolean(String left, Boolean right) {
        return ToBoolean.eval(left) == right;
    }
}
