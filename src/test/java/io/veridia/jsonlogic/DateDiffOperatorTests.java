package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class DateDiffOperatorTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testPositiveWhenBIsLater() throws JsonProcessingException {
    long a = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli();
    assertEquals(30L, jsonLogic.apply("{\"date_diff\": [" + a + ", " + b + ", \"day\"]}", null));
  }

  @Test
  public void testNegativeWhenBIsEarlier() throws JsonProcessingException {
    long a = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    assertEquals(-30L, jsonLogic.apply("{\"date_diff\": [" + a + ", " + b + ", \"day\"]}", null));
  }

  @Test
  public void testWeeks() throws JsonProcessingException {
    long a = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-01-15T00:00:00Z").toEpochMilli();
    assertEquals(2L, jsonLogic.apply("{\"date_diff\": [" + a + ", " + b + ", \"week\"]}", null));
  }

  @Test
  public void testCalendarAwareMonths() throws JsonProcessingException {
    long a = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-03-01T00:00:00Z").toEpochMilli();
    // Jan 31 -> Mar 1 is one full calendar month (to Feb 29, 2024 being a leap year) plus one
    // partial day, so ChronoUnit.MONTHS.between truncates to 1, not 2.
    assertEquals(1L, jsonLogic.apply("{\"date_diff\": [" + a + ", " + b + ", \"month\"]}", null));
  }

  @Test
  public void testComposesWithComparisonForAgeCheck() throws JsonProcessingException {
    long signup = Instant.parse("2023-01-01T00:00:00Z").toEpochMilli();
    long now = Instant.parse("2024-06-01T00:00:00Z").toEpochMilli();
    // "been a customer for at least a year"
    assertEquals(true, jsonLogic.check(
            "{\">=\": [{\"date_diff\": [" + signup + ", " + now + ", \"day\"]}, 365]}", null));
  }
}
