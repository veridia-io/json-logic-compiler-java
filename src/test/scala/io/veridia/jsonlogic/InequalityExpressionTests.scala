package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class InequalityExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("different value same type") {
    jsonLogic.apply("{\"!=\": [1, 2]}", null) shouldEqual true
  }

  test("same value different type") {
    jsonLogic.apply("{\"!=\": [1.0, \"1\"]}", null) shouldEqual false
  }
