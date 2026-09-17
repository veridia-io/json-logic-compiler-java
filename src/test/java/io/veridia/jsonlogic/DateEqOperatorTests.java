package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class DateEqOperatorTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  @Test
  public void testDefaultComponentsAreFullCalendarDate() throws JsonProcessingException {
    long a = Instant.parse("2024-06-15T08:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-06-15T23:00:00Z").toEpochMilli(); // same day, different time
    assertEquals(true, jsonLogic.check("{\"date_eq\": [" + a + ", " + b + "]}", null));

    long c = Instant.parse("2024-06-16T08:00:00Z").toEpochMilli(); // next day
    assertEquals(false, jsonLogic.check("{\"date_eq\": [" + a + ", " + c + "]}", null));
  }

  @Test
  public void testMonthDayComponentsIgnoreYear_theBirthdayCase() throws JsonProcessingException {
    long birthdate = Instant.parse("1990-06-15T00:00:00Z").toEpochMilli();
    long todayIsTheBirthday = Instant.parse("2024-06-15T00:00:00Z").toEpochMilli();
    long todayIsNot = Instant.parse("2024-06-16T00:00:00Z").toEpochMilli();

    assertEquals(true, jsonLogic.check(
            "{\"date_eq\": [" + birthdate + ", " + todayIsTheBirthday + ", [\"month\", \"day\"]]}", null));
    assertEquals(false, jsonLogic.check(
            "{\"date_eq\": [" + birthdate + ", " + todayIsNot + ", [\"month\", \"day\"]]}", null));
    // Same month/day but different year — full-date default equality must reject it, proving
    // the two component sets are genuinely different checks, not just an alias.
    assertEquals(false, jsonLogic.check(
            "{\"date_eq\": [" + birthdate + ", " + todayIsTheBirthday + "]}", null));
  }

  @Test
  public void testSingleComponentSameMonthAnyYear() throws JsonProcessingException {
    long a = Instant.parse("1990-06-01T00:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-06-30T00:00:00Z").toEpochMilli();
    assertEquals(true, jsonLogic.check("{\"date_eq\": [" + a + ", " + b + ", [\"month\"]]}", null));
  }

  @Test
  public void testNegationViaExistingNotOperator() throws JsonProcessingException {
    // No dedicated date_ne — {"!": [{"date_eq": [...]}]} is the negation, using the compiler's
    // existing generic `!`.
    long a = Instant.parse("2024-06-15T00:00:00Z").toEpochMilli();
    long b = Instant.parse("2024-06-16T00:00:00Z").toEpochMilli();
    assertEquals(true, jsonLogic.check("{\"!\": [{\"date_eq\": [" + a + ", " + b + "]}]}", null));
  }

  @Test
  public void testHonorsExplicitTimezoneAtDayBoundary() throws JsonProcessingException {
    // 2024-06-15T23:30Z is already 2024-06-16 in Asia/Tokyo (UTC+9).
    long a = Instant.parse("2024-06-15T23:30:00Z").toEpochMilli();
    long b = Instant.parse("2024-06-16T01:00:00Z").toEpochMilli();

    assertEquals(false, jsonLogic.check("{\"date_eq\": [" + a + ", " + b + "]}", null)); // UTC default
    assertEquals(true, jsonLogic.check(
            "{\"date_eq\": [" + a + ", " + b + ", [\"year\",\"month\",\"day\"], \"Asia/Tokyo\"]}", null));
  }
}
