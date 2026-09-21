package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class GreatestLeastOperatorTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("simple greatest") {
    jsonLogic.apply("{\"greatest\": [10, 3]}", null) shouldEqual 10.0
  }

  test("simple least") {
    jsonLogic.apply("{\"least\": [10, 3]}", null) shouldEqual 3.0
  }

  test("numeric strings coerce") {
    jsonLogic.apply("{\"greatest\": [\"7.5\", \"2\"]}", null) shouldEqual 7.5
  }

  test("null handling") {
    jsonLogic.apply("{\"greatest\": [null, 5]}", null) shouldEqual 5.0
    jsonLogic.apply("{\"least\": [null, 5]}", null) shouldEqual 0.0
  }

  test("boolean values coerce") {
    jsonLogic.apply("{\"greatest\": [true, 3]}", null) shouldEqual 3.0
    jsonLogic.apply("{\"least\": [false, 5]}", null) shouldEqual 0.0
  }

  test("wrong arity falls back to 0") {
    jsonLogic.apply("{\"least\": [5]}", null) shouldEqual 0.0
  }
