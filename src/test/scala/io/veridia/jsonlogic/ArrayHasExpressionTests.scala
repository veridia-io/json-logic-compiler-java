package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class ArrayHasExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("some over a null array (via var) is false") {
    jsonLogic.apply(
      "{\"and\":[{\"some\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"item\"},[\"apple\"]]}]}]}",
      "{\"fruits\":null}"
    ) shouldEqual false
  }

  test("some over an empty array is false") {
    jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"item\"}, 0]}]}", null) shouldEqual false
  }

  test("some") {
    jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 3]}]}", null) shouldEqual false
    jsonLogic.apply("{\"some\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 1]}]}", null) shouldEqual true
  }

  test("none over a null array (via var) is true") {
    jsonLogic.apply(
      "{\"and\":[{\"none\":[{\"var\":\"fruits\"},{\"in\":[{\"var\":\"item\"},[\"apple\"]]}]}]}",
      "{\"fruits\":null}"
    ) shouldEqual true
  }

  test("none over an empty array (matches jsonlogic.com's `some` semantics, not `none`'s)") {
    jsonLogic.apply("{\"some\": [[], {\">\": [{\"var\": \"item\"}, 0]}]}", null) shouldEqual false
  }

  test("none") {
    jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 3]}]}", null) shouldEqual true
    jsonLogic.apply("{\"none\": [[1, 2, 3], {\">\": [{\"var\": \"item\"}, 2]}]}", null) shouldEqual false
  }
