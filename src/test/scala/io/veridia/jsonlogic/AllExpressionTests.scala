package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AllExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("empty array is false") {
    jsonLogic.apply("{\"all\": [[], {\">\": [{\"var\": \"item\"}, 0]}]}", null) shouldEqual false
  }

  test("all") {
    jsonLogic.apply("{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 0]}]}", null) shouldEqual true
    jsonLogic.apply("{\"all\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 1]}]}", null) shouldEqual false
  }
