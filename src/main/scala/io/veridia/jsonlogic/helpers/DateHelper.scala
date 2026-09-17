package io.veridia.jsonlogic.helpers

import java.time.{DayOfWeek, Instant, ZoneId, ZoneOffset, ZonedDateTime}
import java.time.temporal.{ChronoField, ChronoUnit, TemporalAdjusters}

object DateHelper:

  def toMillis(v: Any): Long = v match
    case n: Number => n.longValue()
    case _ => 0L

  def resolveZone(tz: Any): ZoneId =
    if tz == null then ZoneOffset.UTC else ZoneId.of(String.valueOf(tz))

  def toZonedDateTime(value: Any, zone: ZoneId): ZonedDateTime =
    Instant.ofEpochMilli(toMillis(value)).atZone(zone)

  def toChronoUnit(unit: String): ChronoUnit = unit match
    case "minute" => ChronoUnit.MINUTES
    case "hour" => ChronoUnit.HOURS
    case "day" => ChronoUnit.DAYS
    case "week" => ChronoUnit.WEEKS
    case "month" => ChronoUnit.MONTHS
    case "year" => ChronoUnit.YEARS
    case other => throw new IllegalArgumentException(s"Unknown chrono unit: $other")

  def toChronoField(component: String): ChronoField = component match
    case "year" => ChronoField.YEAR
    case "month" => ChronoField.MONTH_OF_YEAR
    case "day" => ChronoField.DAY_OF_MONTH
    case "hour" => ChronoField.HOUR_OF_DAY
    case "day_of_week" => ChronoField.DAY_OF_WEEK
    case other => throw new IllegalArgumentException(s"Unknown date_eq component: $other")

  /** Floors `zdt` to the start of `unit`. Unlike add/diff, "week" has no calendar-neutral
    * definition — fixed to Monday (ISO-8601), matching `day_of_week`'s numbering rather than left
    * to derive from `zdt`'s locale.
    */
  def truncateTo(zdt: ZonedDateTime, unit: ChronoUnit): ZonedDateTime = unit match
    case ChronoUnit.MINUTES | ChronoUnit.HOURS | ChronoUnit.DAYS => zdt.truncatedTo(unit)
    case ChronoUnit.WEEKS =>
      zdt.`with`(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)).truncatedTo(ChronoUnit.DAYS)
    case ChronoUnit.MONTHS => zdt.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS)
    case ChronoUnit.YEARS => zdt.`with`(TemporalAdjusters.firstDayOfYear()).truncatedTo(ChronoUnit.DAYS)
    case other => throw new IllegalArgumentException(s"Unsupported truncation unit: $other")
