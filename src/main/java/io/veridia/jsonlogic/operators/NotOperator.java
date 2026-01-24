package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

import java.util.List;

public class NotOperator implements Operator {
    public static final NotOperator SINGLE = new NotOperator(false);
    public static final NotOperator DOUBLE = new NotOperator(true);

    private final boolean isDoubleNot;
    NotOperator(boolean isDoubleNot) {
        this.isDoubleNot = isDoubleNot;
    }

    @Override
    public String key() {
        return isDoubleNot ? "!!" : "!";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.isEmpty()) return ctx -> !isDoubleNot;

        CompiledExpression valueExpr = args.get(0);

        return ctx -> {
            Object value = valueExpr.eval(ctx);
            return isDoubleNot == ToBoolean.eval(value);
        };
    }
}
