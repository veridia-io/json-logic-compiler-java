package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.ToBoolean;

import java.util.List;

public class LogicOperator implements Operator {
    public static final LogicOperator AND = new LogicOperator(true);
    public static final LogicOperator OR = new LogicOperator(false);

    private final boolean isAnd;

    LogicOperator(boolean isAnd) {
        this.isAnd = isAnd;
    }

    public String key() {
        return isAnd ? "and" : "or";
    }

    public CompiledExpression compile(List<CompiledExpression> args) {
        return ctx -> {
            for (CompiledExpression a : args) {
                boolean result = ToBoolean.eval(a.eval(ctx));

                if ((isAnd && !result) || (!isAnd && result)) return result;
            }

            return true;
        };
    }
}
