package io.veridia.jsonlogic;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.time.Instant;

import static org.junit.Assert.assertEquals;

public class ChronoFieldExtractorOperatorTests {
  private static final JsonLogic jsonLogic = new JsonLogic();

  // 2024-03-15T14:30:00Z is a Friday.
  private static final long T_2024_03_15_14_30_00Z = Instant.parse("2024-03-15T14:30:00Z").toEpochMilli();

  @Test
  public void testYear() throws JsonProcessingException {
    assertEquals(2024, jsonLogic.apply("{\"year\": [" + T_2024_03_15_14_30_00Z + "]}", null));
  }

  @Test
  public void testMonthIsOneIndexed() throws JsonProcessingException {
    assertEquals(3, jsonLogic.apply("{\"month\": [" + T_2024_03_15_14_30_00Z + "]}", null));
  }

  @Test
  public void testDay() throws JsonProcessingException {
    assertEquals(15, jsonLogic.apply("{\"day\": [" + T_2024_03_15_14_30_00Z + "]}", null));
  }

  @Test
  public void testHourDefaultsToUtc() throws JsonProcessingException {
    assertEquals(14, jsonLogic.apply("{\"hour\": [" + T_2024_03_15_14_30_00Z + "]}", null));
  }

  @Test
  public void testHourHonorsExplicitTimezone() throws JsonProcessingException {
    // 14:30 UTC is 23:30 in Asia/Tokyo (UTC+9, no DST) — still the 15th there.
    assertEquals(23, jsonLogic.apply(
            "{\"hour\": [" + T_2024_03_15_14_30_00Z + ", \"Asia/Tokyo\"]}", null));
  }

  @Test
  public void testDayOfWeekIsIsoMondayOne() throws JsonProcessingException {
    assertEquals(5, jsonLogic.apply("{\"day_of_week\": [" + T_2024_03_15_14_30_00Z + "]}", null)); // Friday
  }

  @Test
  public void testComposesWithChainedComparisonForHourRange() throws JsonProcessingException {
    // "hour between 9 and 17" — replaces the earlier bespoke in_time_window idea entirely.
    assertEquals(true, jsonLogic.check(
            "{\"<=\": [9, {\"hour\": [" + T_2024_03_15_14_30_00Z + "]}, 17]}", null));
  }

  @Test
  public void testComposesWithInForWeekendSet() throws JsonProcessingException {
    long saturday = Instant.parse("2024-03-16T00:00:00Z").toEpochMilli();
    assertEquals(true, jsonLogic.check(
            "{\"in\": [{\"day_of_week\": [" + saturday + "]}, [6, 7]]}", null));
    assertEquals(false, jsonLogic.check(
            "{\"in\": [{\"day_of_week\": [" + T_2024_03_15_14_30_00Z + "]}, [6, 7]]}", null)); // Friday
  }
}
