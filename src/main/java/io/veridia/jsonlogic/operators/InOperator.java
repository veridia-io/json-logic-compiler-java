package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

import java.util.List;
import java.util.Map;

public class InOperator implements Operator {
    public static final InOperator INSTANCE = new InOperator();

    @Override
    public String key() {
        return "in";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() < 2) return ctx -> false;

        // args = [needleExpr, containerExpr]
        CompiledExpression needleExpr   = args.get(0);
        CompiledExpression containerExpr = args.get(1);

        return ctx -> {
            Object needle = needleExpr.eval(ctx);
            Object container = containerExpr.eval(ctx);

            if (container == null) {
                return false;
            }

            // Case 1: container is a STRING → substring test
            if (container instanceof String && needle instanceof String) {
                return ((String) container).contains((String) needle);
            }

            // Case 2: container is a LIST → membership test
            if (container instanceof List) {
                List<?> list = (List<?>) container;
                return list.contains(needle);
            }

            // Case 3: container is a MAP → key membership
            if (container instanceof Map) {
                Map<?, ?> map = (Map<?, ?>) container;
                return map.containsKey(needle);
            }

            return false; // everything else → false
        };
    }
}
