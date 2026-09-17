package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.DateHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

public class DateTruncateOperator implements Operator {
    public static final DateTruncateOperator INSTANCE = new DateTruncateOperator();

    @Override
    public String key() {
        return "date_truncate";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() < 2) {
            throw new IllegalArgumentException("Operator 'date_truncate' expects at least 2 arguments");
        }

        CompiledExpression valueExpr = args.get(0);
        ChronoUnit unit = DateHelper.toChronoUnit(String.valueOf(args.get(1).eval(Collections.emptyMap())));
        CompiledExpression tzExpr = args.size() > 2 ? args.get(2) : null;

        return ctx -> {
            ZoneId zone = DateHelper.resolveZone(tzExpr == null ? null : tzExpr.eval(ctx));
            ZonedDateTime zdt = DateHelper.toZonedDateTime(valueExpr.eval(ctx), zone);
            return DateHelper.truncateTo(zdt, unit).toInstant().toEpochMilli();
        };
    }
}
