package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.Instant

class DateGeneratorOperatorTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val T_2024_03_15_14_30_00Z = Instant.parse("2024-03-15T14:30:00Z").toEpochMilli

  test("now is close to wall-clock time") {
    val before = System.currentTimeMillis()
    val result = jsonLogic.apply("{\"now\": []}", null)
    val after = System.currentTimeMillis()
    val now = result.asInstanceOf[Number].longValue()
    now should (be >= before and be <= after)
  }

  test("now is not frozen across repeated evaluations of the same cached expression") {
    // The single most safety-critical property of now()/today(): JsonLogic caches a compiled
    // expression indefinitely per instance, keyed by the raw expression string — the SAME
    // "{\"now\": []}" string is deliberately reused here to force a cache hit, proving the value
    // is read inside eval, not memoized at compile() time.
    val first = jsonLogic.apply("{\"now\": []}", null).asInstanceOf[Number].longValue()
    Thread.sleep(5)
    val second = jsonLogic.apply("{\"now\": []}", null).asInstanceOf[Number].longValue()
    second should be > first
  }

  test("today defaults to UTC start of day") {
    val today = jsonLogic.apply("{\"today\": []}", null).asInstanceOf[Number].longValue()
    val expected = Instant.now().atZone(java.time.ZoneOffset.UTC).toLocalDate.atStartOfDay(java.time.ZoneOffset.UTC).toInstant.toEpochMilli
    today shouldEqual expected
  }

  test("today honors an explicit timezone") {
    val today = jsonLogic.apply("{\"today\": [\"Asia/Tokyo\"]}", null).asInstanceOf[Number].longValue()
    val zone = java.time.ZoneId.of("Asia/Tokyo")
    val expected = Instant.now().atZone(zone).toLocalDate.atStartOfDay(zone).toInstant.toEpochMilli
    today shouldEqual expected
  }

  test("date_add: days") {
    val result = jsonLogic.apply(s"""{"date_add": [$T_2024_03_15_14_30_00Z, 1, "day"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-03-16T14:30:00Z").toEpochMilli
  }

  test("date_add: negative amount shifts backward") {
    val result = jsonLogic.apply(s"""{"date_add": [$T_2024_03_15_14_30_00Z, -30, "day"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-02-14T14:30:00Z").toEpochMilli
  }

  test("date_add: calendar month handles leap-year overflow") {
    val jan31 = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli
    val result = jsonLogic.apply(s"""{"date_add": [$jan31, 1, "month"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-02-29T00:00:00Z").toEpochMilli
  }

  test("date_truncate: to day") {
    val result = jsonLogic.apply(s"""{"date_truncate": [$T_2024_03_15_14_30_00Z, "day"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-03-15T00:00:00Z").toEpochMilli
  }

  test("date_truncate: to week is Monday-start") {
    val result = jsonLogic.apply(s"""{"date_truncate": [$T_2024_03_15_14_30_00Z, "week"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-03-11T00:00:00Z").toEpochMilli
  }

  test("date_truncate: to month") {
    val result = jsonLogic.apply(s"""{"date_truncate": [$T_2024_03_15_14_30_00Z, "month"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-03-01T00:00:00Z").toEpochMilli
  }

  test("date_truncate: to year") {
    val result = jsonLogic.apply(s"""{"date_truncate": [$T_2024_03_15_14_30_00Z, "year"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-01-01T00:00:00Z").toEpochMilli
  }

  test("date_truncate: honors an explicit timezone") {
    val result = jsonLogic.apply(s"""{"date_truncate": [$T_2024_03_15_14_30_00Z, "day", "Asia/Tokyo"]}""", null)
    result.asInstanceOf[Number].longValue() shouldEqual Instant.parse("2024-03-15T00:00:00+09:00").toEpochMilli
  }

  test("a malformed unit throws IllegalArgumentException at compile time") {
    an[IllegalArgumentException] should be thrownBy jsonLogic.apply(
      s"""{"date_add": [$T_2024_03_15_14_30_00Z, 1, "fortnight"]}""",
      null
    )
  }
