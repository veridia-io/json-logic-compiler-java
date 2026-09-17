package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

/** New coverage for this rewrite's own responsibility, not a ported scenario: the previous,
  * Jackson-based engine got numeric-literal-type inference for free from `ObjectMapper`. circe has
  * no built-in equivalent, so `Values.jsonNumberToValue` reimplements it, and needs its own proof —
  * a fractional/exponent literal becomes `Double`; an integral literal becomes the smallest of
  * `Integer`/`Long`/`BigInteger` that fits. Exercised through `var` defaults (a pass-through path
  * that returns the literal's exact boxed value unchanged, unlike arithmetic/comparison operators,
  * which always normalize through `ToDouble`).
  */
class NumberLiteralFidelitySpec extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private def defaultOf(literal: String): Object =
    jsonLogic.apply(s"""{"var": ["missing", $literal]}""", null)

  test("a fractional literal becomes java.lang.Double") {
    val result = defaultOf("3.14")
    result.getClass shouldBe classOf[java.lang.Double]
    result shouldEqual 3.14
  }

  test("an exponent literal becomes java.lang.Double") {
    val result = defaultOf("1e3")
    result.getClass shouldBe classOf[java.lang.Double]
    result shouldEqual 1000.0
  }

  test("a small integral literal becomes java.lang.Integer") {
    val result = defaultOf("5")
    result.getClass shouldBe classOf[java.lang.Integer]
    result shouldEqual 5
  }

  test("an integral literal beyond Int range but within Long becomes java.lang.Long") {
    val result = defaultOf("9999999999")
    result.getClass shouldBe classOf[java.lang.Long]
    result shouldEqual 9999999999L
  }

  test("an integral literal beyond Long range becomes java.math.BigInteger") {
    val result = defaultOf("99999999999999999999")
    result.getClass shouldBe classOf[java.math.BigInteger]
    result shouldEqual new java.math.BigInteger("99999999999999999999")
  }

  test("a literal array of mixed numeric shapes preserves each element's own boxed type") {
    val result = jsonLogic.apply("""{"var": ["missing", [1, 2.5, 9999999999]]}""", null)
    val list = result.asInstanceOf[java.util.List[Object]]
    list.get(0).getClass shouldBe classOf[java.lang.Integer]
    list.get(1).getClass shouldBe classOf[java.lang.Double]
    list.get(2).getClass shouldBe classOf[java.lang.Long]
  }

  test("a JSON-string context is converted with the same numeric fidelity") {
    val result = jsonLogic.apply("{\"var\": \"count\"}", "{\"count\": 5}")
    result.getClass shouldBe classOf[java.lang.Integer]
    result shouldEqual 5
  }
