package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

import java.util.List;
import java.util.Map;

public class ArrayHasOperator implements Operator {
    public static final ArrayHasOperator SOME = new ArrayHasOperator(true);
    public static final ArrayHasOperator NONE = new ArrayHasOperator(false);

    private final boolean isSome;

    ArrayHasOperator(boolean isSome) {
        this.isSome = isSome;
    }

    @Override
    public String key() {
        return isSome ? "some" : "none";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        // args: [arrayExpr, conditionExpr]
        CompiledExpression arrayExpr = args.get(0);
        CompiledExpression condExpr  = args.get(1);

        return ctx -> {
            Object arrObj = arrayExpr.eval(ctx);
            if (!(arrObj instanceof List)) return !isSome;   // non-array => false for some

            List<?> list = (List<?>) arrObj;
            if (list.isEmpty()) return !isSome;   // empty array => false for some

            for (Object element : list) {
                // Create child context with this element as data
                Object result = condExpr.eval(Map.of("item", element));
                if (ToBoolean.eval(result)) {
                    return isSome; //
                }
            }

            return !isSome;
        };
    }
}
