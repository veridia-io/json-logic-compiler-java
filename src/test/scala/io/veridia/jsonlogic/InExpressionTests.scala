package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class InExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("string substring membership") {
    jsonLogic.apply("{\"in\": [\"race\", \"racecar\"]}", null) shouldEqual true
  }

  test("string substring non-membership") {
    jsonLogic.apply("{\"in\": [\"race\", \"clouds\"]}", null) shouldEqual false
    jsonLogic.apply("{\"in\": [null, \"clouds\"]}", null) shouldEqual false
  }

  test("array membership") {
    jsonLogic.apply("{\"in\": [1, [1, 2, 3]]}", null) shouldEqual true
    jsonLogic.apply("{\"in\": [4.56, [1, 2, 3, 4.56]]}", null) shouldEqual true
    jsonLogic.apply("{\"in\": [null, [1, 2, 3, null]]}", null) shouldEqual true
  }

  test("array non-membership") {
    jsonLogic.apply("{\"in\": [5, [1, 2, 3]]}", null) shouldEqual false
    jsonLogic.apply("{\"in\": [null, [1, 2, 3]]}", null) shouldEqual false
  }

  test("membership against a var-resolved list") {
    val data = ju.Collections.singletonMap("list", ju.Arrays.asList(1, 2, 3))
    jsonLogic.apply("{\"in\": [2, {\"var\": \"list\"}]}", data) shouldEqual true
  }

  test("non-membership against a var-resolved list, including a null context") {
    val data = ju.Collections.singletonMap("list", ju.Arrays.asList(1, 2, 3))
    jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", data) shouldEqual false
    jsonLogic.apply("{\"in\": [4, {\"var\": \"list\"}]}", null) shouldEqual false
  }

  test("both needle and container resolved via var") {
    val data = new ju.HashMap[String, Object]()
    data.put("list", ju.Arrays.asList(1, 2, 3))
    data.put("value", java.lang.Integer.valueOf(3))
    jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", data) shouldEqual true
    jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, {\"var\": \"list\"}]}", null) shouldEqual false
  }

  test("a single argument is false") {
    jsonLogic.apply("{\"in\": [\"Spring\"]}", null) shouldEqual false
  }

  test("a non-string/list/map second argument is false") {
    jsonLogic.apply("{\"in\": [\"Spring\", 3]}", null) shouldEqual false
  }

  test("cross-numeric-type membership: a Long needle against Integer literal elements") {
    // A Long needle (e.g. any operator's result, not just a JSON literal) against a container of
    // JSON int literals (parsed as Integer) must still match numerically — raw List.contains()
    // would reject 2L against [1, 2, 3] since Long(2).equals(Integer(2)) is false.
    val data = ju.Collections.singletonMap("value", java.lang.Long.valueOf(2L))
    jsonLogic.apply("{\"in\": [{\"var\": \"value\"}, [1, 2, 3]]}", data) shouldEqual true
  }

  test("map-container membership tests key presence, not value presence") {
    val data = ju.Map.of("roles", ju.Map.of("admin", java.lang.Boolean.TRUE))
    jsonLogic.apply("{\"in\": [\"admin\", {\"var\": \"roles\"}]}", data) shouldEqual true
    jsonLogic.apply("{\"in\": [\"guest\", {\"var\": \"roles\"}]}", data) shouldEqual false
  }
