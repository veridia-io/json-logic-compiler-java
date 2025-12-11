package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

import java.util.List;
import java.util.Map;

public class AllOperator implements Operator {
    public static final AllOperator INSTANCE = new AllOperator();

    @Override
    public String key() {
        return "all";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        // args: [arrayExpr, conditionExpr]
        CompiledExpression arrayExpr = args.get(0);
        CompiledExpression condExpr  = args.get(1);

        return ctx -> {
            Object arrObj = arrayExpr.eval(ctx);
            if (!(arrObj instanceof List)) return false;   // non-array => false

            List<?> list = (List<?>) arrObj;
            if (list.isEmpty()) return false;

            for (Object element : list) {
                // Create child context with this element as data
                Object result = condExpr.eval(Map.of("item", element));
                if (!ToBoolean.eval(result)) {
                    return false;
                }
            }

            return true;
        };
    }
}
