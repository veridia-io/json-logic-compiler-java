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

                // Numeric loose equality, matching EqualityOperator: a Long needle (any
                // operator's result, e.g. date_diff) must match an Integer literal element (how
                // Jackson parses a small JSON int) — plain List.contains() uses raw equals() and
                // Long(2).equals(Integer(2)) is false, so a same-typed check alone would miss it.
                if (needle instanceof Number) {
                    for (Object item : list) {
                        if (item instanceof Number && ((Number) needle).doubleValue() == ((Number) item).doubleValue()) {
                            return true;
                        }
                    }
                    return false;
                }

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
