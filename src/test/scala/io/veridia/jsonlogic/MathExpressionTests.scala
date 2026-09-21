package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class MathExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val cases: Seq[(String, String, Double)] = Seq(
    ("add", "{\"+\":[4,2]}", 6.0),
    ("multi-add", "{\"+\":[2,2,2,2,2]}", 10.0),
    ("add coerces a non-numeric string operand to 0", "{\"+\" : [1, \"foo\"]}", 1.0),
    ("subtract", "{\"-\":[4,2]}", 2.0),
    ("unary subtract on a number", "{\"-\": 2 }", -2.0),
    ("unary subtract on a numeric string", "{\"-\": \"2\" }", -2.0),
    ("multiply", "{\"*\":[4,2]}", 8.0),
    ("multi-multiply", "{\"*\":[2,2,2,2,2]}", 32.0),
    ("multiply with a nested array coerces to 0", "{\"*\":[2,[[3, 4], 5]]}", 0.0),
    ("multiply with an empty array coerces to 0", "{\"*\":[2,[]]}", 0.0),
    ("divide", "{\"/\":[4,2]}", 2.0),
    ("divide by zero fails open to 0", "{\"/\":[4,0]}", 0.0),
    ("modulo", "{\"%\": [101,2]}", 1.0),
    ("min", "{\"min\":[1,2,3]}", 1.0),
    ("max", "{\"max\":[1,2,3]}", 3.0)
  )

  for (name, json, expected) <- cases do
    test(name) {
      jsonLogic.apply(json, null) shouldEqual expected
    }

  test("unary divide returns null, not 0") {
    jsonLogic.apply("{\"/\": [0]}", null) shouldBe null
  }
