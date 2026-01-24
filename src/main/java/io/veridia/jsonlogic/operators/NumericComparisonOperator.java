package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToDouble;

import java.util.List;

public class NumericComparisonOperator implements Operator {
    public static final NumericComparisonOperator GT = new NumericComparisonOperator(">");
    public static final NumericComparisonOperator LT = new NumericComparisonOperator("<");
    public static final NumericComparisonOperator GTE = new NumericComparisonOperator(">=");
    public static final NumericComparisonOperator LTE = new NumericComparisonOperator("<=");

    private final String op;

    NumericComparisonOperator(String op) {
        this.op = op;
    }

    @Override
    public String key() {
        return op;
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        return ctx -> {
            // JSON-Logic requires at least 2 arguments for comparison
            if (args.size() < 2) {
                return false;
            }

            // Compare each adjacent pair
            for (int i = 0; i < args.size() - 1; i++) {
                double left  = ToDouble.eval(args.get(i).eval(ctx));
                double right = ToDouble.eval(args.get(i + 1).eval(ctx));

                boolean result;

                switch (op) {
                    case ">":
                        result = left > right;
                        break;
                    case "<":
                        result = left < right;
                        break;
                    case ">=":
                        result = left >= right;
                        break;
                    case "<=":
                        result = left <= right;
                        break;
                    default:
                        return false;
                }

                // If any comparison fails → whole chain must fail
                if (!result) {
                    return false;
                }
            }

            // All comparisons succeeded
            return true;
        };
    }
}
