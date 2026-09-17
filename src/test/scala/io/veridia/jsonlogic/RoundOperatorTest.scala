package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class RoundOperatorTest extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("round to integer") {
    jsonLogic.apply("{\"round\": [5.6, 0]}", null) shouldEqual 6.0
  }

  test("round to two decimals") {
    jsonLogic.apply("{\"round\": [5.6789, 2]}", null) shouldEqual 5.68
  }

  test("round to three decimals") {
    jsonLogic.apply("{\"round\": [3.14159, 3]}", null) shouldEqual 3.142
  }

  test("negative precision rounds to tens/hundreds") {
    jsonLogic.apply("{\"round\": [12.34, -1]}", null) shouldEqual 10.0
    jsonLogic.apply("{\"round\": [123.45, -2]}", null) shouldEqual 100.0
  }

  test("rounds a var-resolved value") {
    val ctx = ju.Map.of("amount", java.lang.Double.valueOf(7.3339))
    jsonLogic.apply("{\"round\": [ {\"var\":\"amount\"}, 2 ]}", ctx) shouldEqual 7.33
  }

  test("null value falls back to 0") {
    jsonLogic.apply("{\"round\": [null, 2]}", null) shouldEqual 0.0
  }

  test("invalid precision coerces to 0 (via ToDouble's fail-open string parsing)") {
    jsonLogic.apply("{\"round\": [5.123, \"bad\"]}", null) shouldEqual 5.0
  }

  test("a nested arg that throws at eval time is caught and falls back to 0.0") {
    // round's try/catch wraps the arg evals themselves, not just the arithmetic: an invalid
    // caller-supplied timezone makes `year` throw ZoneRulesException at eval time, and round's
    // contract is to fail closed rather than let that escape.
    val ctx = ju.Map.of("tz", "Not/AZone")
    noException should be thrownBy jsonLogic.apply("{\"round\": [{\"year\": [0, {\"var\": \"tz\"}]}, 2]}", ctx)
    jsonLogic.apply("{\"round\": [{\"year\": [0, {\"var\": \"tz\"}]}, 2]}", ctx) shouldEqual 0.0
  }
