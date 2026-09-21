package io.veridia.jsonlogic.helpers;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class DateHelper {

    private DateHelper() {
    }

    public static long toMillis(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return 0L;
    }

    public static ZoneId resolveZone(Object tz) {
        if (tz == null) {
            return ZoneOffset.UTC;
        }
        return ZoneId.of(String.valueOf(tz));
    }

    public static ZonedDateTime toZonedDateTime(Object value, ZoneId zone) {
        return Instant.ofEpochMilli(toMillis(value)).atZone(zone);
    }

    public static ChronoUnit toChronoUnit(String unit) {
        switch (unit) {
            case "minute": return ChronoUnit.MINUTES;
            case "hour": return ChronoUnit.HOURS;
            case "day": return ChronoUnit.DAYS;
            case "week": return ChronoUnit.WEEKS;
            case "month": return ChronoUnit.MONTHS;
            case "year": return ChronoUnit.YEARS;
            default:
                throw new IllegalArgumentException("Unknown chrono unit: " + unit);
        }
    }

    public static ChronoField toChronoField(String component) {
        switch (component) {
            case "year": return ChronoField.YEAR;
            case "month": return ChronoField.MONTH_OF_YEAR;
            case "day": return ChronoField.DAY_OF_MONTH;
            case "hour": return ChronoField.HOUR_OF_DAY;
            case "day_of_week": return ChronoField.DAY_OF_WEEK;
            default:
                throw new IllegalArgumentException("Unknown date_eq component: " + component);
        }
    }

    /**
     * Floors {@code zdt} to the start of {@code unit}. Unlike add/diff, "week" has no
     * calendar-neutral definition — fixed to Monday (ISO-8601), matching the day_of_week
     * numbering ({@link ChronoField#DAY_OF_WEEK}) rather than left to derive from zdt's locale.
     */
    public static ZonedDateTime truncateTo(ZonedDateTime zdt, ChronoUnit unit) {
        switch (unit) {
            case MINUTES:
            case HOURS:
            case DAYS:
                return zdt.truncatedTo(unit);
            case WEEKS:
                return zdt.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS);
            case MONTHS:
                return zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case YEARS:
                return zdt.with(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS);
            default:
                throw new IllegalArgumentException("Unsupported truncation unit: " + unit);
        }
    }
}
