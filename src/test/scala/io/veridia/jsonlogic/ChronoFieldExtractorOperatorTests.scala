package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.time.Instant

/** Chrono extractors must return `java.lang.Integer`, not `Long` — asserted explicitly via
  * `getClass`, since Scala's `==`/`shouldEqual` is numeric-value-equal across boxed number types
  * and would not catch a regression to `Long` the way a type-sensitive check does.
  */
class ChronoFieldExtractorOperatorTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  // 2024-03-15T14:30:00Z is a Friday.
  private val T_2024_03_15_14_30_00Z = Instant.parse("2024-03-15T14:30:00Z").toEpochMilli

  private def assertInt(result: Object, expected: Int): Unit =
    result.getClass shouldBe classOf[java.lang.Integer]
    result shouldEqual expected

  test("year") {
    assertInt(jsonLogic.apply(s"""{"year": [$T_2024_03_15_14_30_00Z]}""", null), 2024)
  }

  test("month is one-indexed") {
    assertInt(jsonLogic.apply(s"""{"month": [$T_2024_03_15_14_30_00Z]}""", null), 3)
  }

  test("day") {
    assertInt(jsonLogic.apply(s"""{"day": [$T_2024_03_15_14_30_00Z]}""", null), 15)
  }

  test("hour defaults to UTC") {
    assertInt(jsonLogic.apply(s"""{"hour": [$T_2024_03_15_14_30_00Z]}""", null), 14)
  }

  test("hour honors an explicit timezone") {
    // 14:30 UTC is 23:30 in Asia/Tokyo (UTC+9, no DST) — still the 15th there.
    assertInt(jsonLogic.apply(s"""{"hour": [$T_2024_03_15_14_30_00Z, "Asia/Tokyo"]}""", null), 23)
  }

  test("day_of_week is ISO Monday=1") {
    assertInt(jsonLogic.apply(s"""{"day_of_week": [$T_2024_03_15_14_30_00Z]}""", null), 5) // Friday
  }

  test("composes with chained comparison for an hour range") {
    jsonLogic.check(s"""{"<=": [9, {"hour": [$T_2024_03_15_14_30_00Z]}, 17]}""", null) shouldEqual true
  }

  test("composes with in for a weekend set") {
    val saturday = Instant.parse("2024-03-16T00:00:00Z").toEpochMilli
    jsonLogic.check(s"""{"in": [{"day_of_week": [$saturday]}, [6, 7]]}""", null) shouldEqual true
    jsonLogic.check(s"""{"in": [{"day_of_week": [$T_2024_03_15_14_30_00Z]}, [6, 7]]}""", null) shouldEqual false // Friday
  }
