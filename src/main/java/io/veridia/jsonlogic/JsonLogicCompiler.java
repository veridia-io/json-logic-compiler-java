package io.veridia.jsonlogic;

import io.veridia.jsonlogic.LogicParser.Expr;
import io.veridia.jsonlogic.operators.*;

import java.util.List;
import java.util.stream.Collectors;

public class JsonLogicCompiler {

    private final OperatorRegistry registry = new OperatorRegistry();

    public JsonLogicCompiler() {
        registerDefaultOperators();
    }

    public CompiledExpression compile(Expr expr) {
        // constant
        if (expr.op == null && expr.variable == null) {
            Object value = expr.constant;
            return ctx -> value;
        }

        // var
        if ("var".equals(expr.op)) {
            String var = expr.variable;
            return ctx -> {
                Object resolved = PathResolver.resolve(ctx, var);

                return resolved != null ? resolved : expr.variableDefault;
            };
        }

        // operator
        Operator operator = registry.get(expr.op);
        List<CompiledExpression> compiledArgs = expr.args.stream().map(this::compile).collect(Collectors.toList());

        return operator.compile(compiledArgs);
    }

    private void registerDefaultOperators() {
        registry.register(EqualityOperator.INSTANCE);
        registry.register(InequalityOperator.INSTANCE);

        registry.register(NotOperator.SINGLE);
        registry.register(NotOperator.DOUBLE);
        registry.register(LogicOperator.AND);
        registry.register(LogicOperator.OR);

        registry.register(NumericComparisonOperator.GT);
        registry.register(NumericComparisonOperator.LT);
        registry.register(NumericComparisonOperator.GTE);
        registry.register(NumericComparisonOperator.LTE);

        registry.register(MathOperator.ADD);
        registry.register(MathOperator.SUBTRACT);
        registry.register(MathOperator.MULTIPLY);
        registry.register(MathOperator.DIVIDE);
        registry.register(MathOperator.MODULO);
        registry.register(MathOperator.MIN);
        registry.register(MathOperator.MAX);
        registry.register(RoundOperator.INSTANCE);

        registry.register(GreatestLeastOperator.GREATEST);
        registry.register(GreatestLeastOperator.LEAST);

        registry.register(AllOperator.INSTANCE);
        registry.register(ArrayHasOperator.SOME);
        registry.register(ArrayHasOperator.NONE);

        registry.register(InOperator.INSTANCE);

        registry.register(StringOperator.CONTAINS);
        registry.register(StringOperator.STARTS_WITH);
        registry.register(StringOperator.ENDS_WITH);

        registry.register(StringRegexOperator.INSTANCE);

        registry.register(PresenceOperator.EXISTS);
        registry.register(PresenceOperator.NOT_EXISTS);
    }

    public void registerOperator(Operator impl) {
        registry.register(impl);
    }
}
