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

    @Override
    public String key() {
        return isAnd ? "and" : "or";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        // Materialize once at compile time: an indexed loop over an array allocates no iterator per eval.
        CompiledExpression[] a = args.toArray(new CompiledExpression[0]);

        return ctx -> {
            boolean result = true;

            for (int i = 0; i < a.length; i++) {
                result = ToBoolean.eval(a[i].eval(ctx));

                if ((isAnd && !result) || (!isAnd && result)) return result;
            }

            return result;
        };
    }
}
