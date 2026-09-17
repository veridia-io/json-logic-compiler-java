package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.DateHelper;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

public class TodayOperator implements Operator {
    public static final TodayOperator INSTANCE = new TodayOperator();

    @Override
    public String key() {
        return "today";
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        CompiledExpression tzExpr = args.isEmpty() ? null : args.get(0);

        // Instant.now() must be read inside the returned lambda — see NowOperator.
        return ctx -> {
            ZoneId zone = DateHelper.resolveZone(tzExpr == null ? null : tzExpr.eval(ctx));
            return Instant.now().atZone(zone).toLocalDate().atStartOfDay(zone).toInstant().toEpochMilli();
        };
    }
}
