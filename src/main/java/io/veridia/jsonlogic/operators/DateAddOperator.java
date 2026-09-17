package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.DateHelper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class DateAddOperator implements Operator {
    public static final DateAddOperator INSTANCE = new DateAddOperator();

    @Override
    public String key() {
        return "date_add";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("Operator 'date_add' expects exactly 3 arguments");
        }

        CompiledExpression valueExpr = args.get(0);
        CompiledExpression amountExpr = args.get(1);
        // unit is a fixed vocabulary string, not a per-evaluation value — resolved once here.
        ChronoUnit unit = DateHelper.toChronoUnit(String.valueOf(args.get(2).eval(Collections.emptyMap())));

        return ctx -> {
            long value = DateHelper.toMillis(valueExpr.eval(ctx));
            long amount = DateHelper.toMillis(amountExpr.eval(ctx));

            ZonedDateTime zdt = Instant.ofEpochMilli(value).atZone(ZoneOffset.UTC);
            return zdt.plus(amount, unit).toInstant().toEpochMilli();
        };
    }
}
