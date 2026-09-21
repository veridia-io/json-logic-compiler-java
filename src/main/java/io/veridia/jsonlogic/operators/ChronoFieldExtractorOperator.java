package io.veridia.jsonlogic.operators;

import io.veridia.jsonlogic.CompiledExpression;
import io.veridia.jsonlogic.Operator;
import io.veridia.jsonlogic.helpers.DateHelper;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.List;

/**
 * One generic operator, parametrized by {@link ChronoField}, registered under five keys —
 * year/month/day/hour/day_of_week all read the same way via {@link ZonedDateTime#get}.
 */
public class ChronoFieldExtractorOperator implements Operator {
    public static final ChronoFieldExtractorOperator YEAR =
            new ChronoFieldExtractorOperator("year", ChronoField.YEAR);
    public static final ChronoFieldExtractorOperator MONTH =
            new ChronoFieldExtractorOperator("month", ChronoField.MONTH_OF_YEAR);
    public static final ChronoFieldExtractorOperator DAY =
            new ChronoFieldExtractorOperator("day", ChronoField.DAY_OF_MONTH);
    public static final ChronoFieldExtractorOperator HOUR =
            new ChronoFieldExtractorOperator("hour", ChronoField.HOUR_OF_DAY);
    // ISO-8601 numbering (Mon=1..Sun=7) — ChronoField.DAY_OF_WEEK's default under ISO chronology.
    public static final ChronoFieldExtractorOperator DAY_OF_WEEK =
            new ChronoFieldExtractorOperator("day_of_week", ChronoField.DAY_OF_WEEK);

    private final String key;
    private final ChronoField field;

    private ChronoFieldExtractorOperator(String key, ChronoField field) {
        this.key = key;
        this.field = field;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public CompiledExpression compile(List<CompiledExpression> args) {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Operator '" + key + "' expects at least 1 argument");
        }

        CompiledExpression valueExpr = args.get(0);
        CompiledExpression tzExpr = args.size() > 1 ? args.get(1) : null;

        return ctx -> {
            ZoneId zone = DateHelper.resolveZone(tzExpr == null ? null : tzExpr.eval(ctx));
            ZonedDateTime zdt = DateHelper.toZonedDateTime(valueExpr.eval(ctx), zone);
            // Integer, not Long: matches both what ZonedDateTime.get(ChronoField) natively
            // returns and what a small JSON literal (e.g. inside an `in` set) parses to via
            // Jackson — InOperator's List.contains uses raw equals(), which a Long/Integer
            // mismatch fails silently (see ChronoFieldExtractorOperatorTests).
            return zdt.get(field);
        };
    }
}
