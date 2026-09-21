package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;
import java.time.OffsetDateTime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DateGeneratorOperatorTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  private static final long T_2024_03_15_14_30_00Z = Instant.parse("2024-03-15T14:30:00Z").toEpochMilli();

  @Test
  public void testNowIsCloseToWallClock() throws JsonProcessingException {
    long before = System.currentTimeMillis();
    Object result = jsonLogic.apply("{\"now\": []}", null);
    long after = System.currentTimeMillis();

    long now = ((Number) result).longValue();
    assertTrue(now >= before && now <= after);
  }

  @Test
  public void testNowIsNotFrozenAcrossRepeatedEvaluationsOfTheSameCachedExpression() throws Exception {
    // The single most safety-critical property of now()/today(): JsonLogic caches a compiled
    // expression indefinitely per instance, keyed by the raw expression string (C1 in
    // docs/json-logic-compiler-java.md) — the SAME "{\"now\": []}" string is deliberately reused
    // here to force a cache hit, proving the value is read inside the lambda, not memoized at
    // compile() time. A regression here would silently freeze "now" forever for every caller
    // sharing this JsonLogic instance.
    long first = ((Number) jsonLogic.apply("{\"now\": []}", null)).longValue();
    Thread.sleep(5);
    long second = ((Number) jsonLogic.apply("{\"now\": []}", null)).longValue();

    assertTrue(second > first);
  }

  @Test
  public void testTodayDefaultsToUtcStartOfDay() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"today\": []}", null);
    long today = ((Number) result).longValue();

    long expected = Instant.now().atZone(java.time.ZoneOffset.UTC)
            .toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
    assertEquals(expected, today);
  }

  @Test
  public void testTodayHonorsExplicitTimezone() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"today\": [\"Asia/Tokyo\"]}", null);
    long today = ((Number) result).longValue();

    long expected = Instant.now().atZone(java.time.ZoneId.of("Asia/Tokyo"))
            .toLocalDate().atStartOfDay(java.time.ZoneId.of("Asia/Tokyo")).toInstant().toEpochMilli();
    assertEquals(expected, today);
  }

  @Test
  public void testDateAddDays() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"date_add\": [" + T_2024_03_15_14_30_00Z + ", 1, \"day\"]}", null);
    assertEquals(Instant.parse("2024-03-16T14:30:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateAddNegativeAmountShiftsBackward() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"date_add\": [" + T_2024_03_15_14_30_00Z + ", -30, \"day\"]}", null);
    assertEquals(Instant.parse("2024-02-14T14:30:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateAddCalendarMonthHandlesOverflow() throws JsonProcessingException {
    long jan31 = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli();
    Object result = jsonLogic.apply("{\"date_add\": [" + jan31 + ", 1, \"month\"]}", null);
    // 2024 is a leap year: Jan 31 + 1 calendar month clamps to Feb 29, not Mar 2.
    assertEquals(Instant.parse("2024-02-29T00:00:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateTruncateToDay() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"date_truncate\": [" + T_2024_03_15_14_30_00Z + ", \"day\"]}", null);
    assertEquals(Instant.parse("2024-03-15T00:00:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateTruncateToWeekIsMondayStart() throws JsonProcessingException {
    // 2024-03-15 is a Friday; the preceding Monday is 2024-03-11.
    Object result = jsonLogic.apply("{\"date_truncate\": [" + T_2024_03_15_14_30_00Z + ", \"week\"]}", null);
    assertEquals(Instant.parse("2024-03-11T00:00:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateTruncateToMonth() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"date_truncate\": [" + T_2024_03_15_14_30_00Z + ", \"month\"]}", null);
    assertEquals(Instant.parse("2024-03-01T00:00:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateTruncateToYear() throws JsonProcessingException {
    Object result = jsonLogic.apply("{\"date_truncate\": [" + T_2024_03_15_14_30_00Z + ", \"year\"]}", null);
    assertEquals(Instant.parse("2024-01-01T00:00:00Z").toEpochMilli(), ((Number) result).longValue());
  }

  @Test
  public void testDateTruncateHonorsExplicitTimezone() throws JsonProcessingException {
    // 14:30 UTC on 2024-03-15 is 23:30 on the same calendar day in Asia/Tokyo (UTC+9, no DST).
    Object result = jsonLogic.apply(
            "{\"date_truncate\": [" + T_2024_03_15_14_30_00Z + ", \"day\", \"Asia/Tokyo\"]}", null);
    long expected = OffsetDateTime.parse("2024-03-15T00:00:00+09:00").toInstant().toEpochMilli();
    assertEquals(expected, ((Number) result).longValue());
  }

  @Test
  public void testMalformedUnitThrowsAtCompileTime() {
    try {
      jsonLogic.apply("{\"date_add\": [" + T_2024_03_15_14_30_00Z + ", 1, \"fortnight\"]}", null);
      throw new AssertionError("expected IllegalArgumentException");
    } catch (IllegalArgumentException expected) {
      // unit is a fixed vocabulary, not a data value — a bad unit is a rule-authoring bug that
      // must surface loudly, unlike a malformed data value (see DateEqOperatorTests re fail-open).
    } catch (JsonProcessingException e) {
      throw new AssertionError(e);
    }
  }
}
