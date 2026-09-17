package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.DateHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.Collections;
import java.util.List;

public class DateEqOperator implements Operator {
    public static final DateEqOperator INSTANCE = new DateEqOperator();

    private static final List<Object> DEFAULT_COMPONENTS = List.of("year", "month", "day");

    @Override
    public String key() {
        return "date_eq";
    }

    @Override
    @SuppressWarnings("unchecked")
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.size() < 2) {
            throw new IllegalArgumentException("Operator 'date_eq' expects at least 2 arguments");
        }

        CompiledExpression aExpr = args.get(0);
        CompiledExpression bExpr = args.get(1);

        // components is a literal array, not a per-evaluation value — resolved once here.
        List<Object> rawComponents = args.size() > 2
                ? (List<Object>) args.get(2).eval(Collections.emptyMap())
                : DEFAULT_COMPONENTS;

        ChronoField[] fields = rawComponents.stream()
                .map(String::valueOf)
                .map(DateHelper::toChronoField)
                .toArray(ChronoField[]::new);

        CompiledExpression tzExpr = args.size() > 3 ? args.get(3) : null;

        return ctx -> {
            ZoneId zone = DateHelper.resolveZone(tzExpr == null ? null : tzExpr.eval(ctx));
            ZonedDateTime a = DateHelper.toZonedDateTime(aExpr.eval(ctx), zone);
            ZonedDateTime b = DateHelper.toZonedDateTime(bExpr.eval(ctx), zone);

            for (ChronoField field : fields) {
                if (a.get(field) != b.get(field)) {
                    return false;
                }
            }
            return true;
        };
    }
}
