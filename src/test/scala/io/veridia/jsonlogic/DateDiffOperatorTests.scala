package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.Instant

/** `date_diff` must return `java.lang.Long`, not `Integer` — see [[ChronoFieldExtractorOperatorTests]]
  * for why this needs an explicit type check, not just a value check.
  */
class DateDiffOperatorTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private def assertLong(result: Object, expected: Long): Unit =
    result.getClass shouldBe classOf[java.lang.Long]
    result shouldEqual expected

  test("positive when b is later") {
    val a = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli
    val b = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli
    assertLong(jsonLogic.apply(s"""{"date_diff": [$a, $b, "day"]}""", null), 30L)
  }

  test("negative when b is earlier") {
    val a = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli
    val b = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli
    assertLong(jsonLogic.apply(s"""{"date_diff": [$a, $b, "day"]}""", null), -30L)
  }

  test("weeks") {
    val a = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli
    val b = Instant.parse("2024-01-15T00:00:00Z").toEpochMilli
    assertLong(jsonLogic.apply(s"""{"date_diff": [$a, $b, "week"]}""", null), 2L)
  }

  test("calendar-aware months") {
    val a = Instant.parse("2024-01-31T00:00:00Z").toEpochMilli
    val b = Instant.parse("2024-03-01T00:00:00Z").toEpochMilli
    // Jan 31 -> Mar 1 is one full calendar month (to Feb 29, a leap year) plus one partial day, so
    // ChronoUnit.MONTHS.between truncates to 1, not 2.
    assertLong(jsonLogic.apply(s"""{"date_diff": [$a, $b, "month"]}""", null), 1L)
  }

  test("composes with comparison for an age check") {
    val signup = Instant.parse("2023-01-01T00:00:00Z").toEpochMilli
    val now = Instant.parse("2024-06-01T00:00:00Z").toEpochMilli
    jsonLogic.check(s"""{">=": [{"date_diff": [$signup, $now, "day"]}, 365]}""", null) shouldEqual true
  }
