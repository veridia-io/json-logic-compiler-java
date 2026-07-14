package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

import java.util.HashMap;
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

            // One reusable child context per eval; the element slot is overwritten each iteration
            // rather than allocating a fresh map per element.
            Map<String, Object> childCtx = new HashMap<>(2);
            for (Object element : list) {
                childCtx.put("item", element);
                Object result = condExpr.eval(childCtx);
                if (!ToBoolean.eval(result)) {
                    return false;
                }
            }

            return true;
        };
    }
}
