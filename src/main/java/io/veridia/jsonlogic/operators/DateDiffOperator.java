package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.DateHelper;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class DateDiffOperator implements Operator {
    public static final DateDiffOperator INSTANCE = new DateDiffOperator();

    @Override
    public String key() {
        return "date_diff";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() != 3) {
            throw new IllegalArgumentException("Operator 'date_diff' expects exactly 3 arguments");
        }

        CompiledExpression aExpr = args.get(0);
        CompiledExpression bExpr = args.get(1);
        ChronoUnit unit = DateHelper.toChronoUnit(String.valueOf(args.get(2).eval(Collections.emptyMap())));

        return ctx -> {
            ZonedDateTime a = DateHelper.toZonedDateTime(aExpr.eval(ctx), ZoneOffset.UTC);
            ZonedDateTime b = DateHelper.toZonedDateTime(bExpr.eval(ctx), ZoneOffset.UTC);
            return unit.between(a, b);
        };
    }
}
