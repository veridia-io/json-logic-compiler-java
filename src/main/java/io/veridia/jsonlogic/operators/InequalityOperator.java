package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;

import java.util.List;

public class InequalityOperator implements Operator {
    public static final InequalityOperator INSTANCE = new InequalityOperator();

    private static final EqualityOperator delegate = new EqualityOperator();

    @Override
    public String key() {
        return "!=";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        CompiledExpression compiledEquality = delegate.compile(args);

        return ctx -> !((boolean) compiledEquality.eval(ctx));
    }
}
