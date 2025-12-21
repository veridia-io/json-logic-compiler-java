package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToDouble;

import java.util.List;

public class RoundOperator implements Operator {
    public static final RoundOperator INSTANCE = new RoundOperator();

    @Override
    public String key() {
        return "round";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 2) {
            return ctx -> 0.0;
        }

        CompiledExpression valueExpr = args.get(0);
        CompiledExpression precisionExpr  = args.get(1);

        return ctx -> {
            try {
                Object rawValue = valueExpr.eval(ctx);
                Object rawPrecision = precisionExpr.eval(ctx);
                if (rawValue == null || rawPrecision == null) {
                    return 0.0;
                }

                double value = ToDouble.eval(rawValue);
                int precision = (int) ToDouble.eval(rawPrecision);

                double factor = Math.pow(10, precision);

                return Math.round(value * factor) / factor;
            } catch (Exception e) {
                return 0.0;
            }
        };
    }
}
