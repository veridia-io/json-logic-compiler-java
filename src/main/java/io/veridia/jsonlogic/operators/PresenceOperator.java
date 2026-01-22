package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;

import java.util.List;

public class PresenceOperator implements Operator {

    public static final PresenceOperator EXISTS =
            new PresenceOperator("exists", false);

    public static final PresenceOperator NOT_EXISTS =
            new PresenceOperator("not_exists", true);

    private final String op;
    private final boolean negate;

    private PresenceOperator(String op, boolean negate) {
        this.op = op;
        this.negate = negate;
    }

    @Override
    public String key() {
        return op;
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 1) {
            throw new IllegalArgumentException(
                    "Operator '" + op + "' expects exactly 1 argument"
            );
        }

        CompiledExpression arg = args.get(0);

        return ctx -> {
            Object value = arg.eval(ctx);

            boolean exists = value != null;

            return negate ? !exists : exists;
        };
    }
}
