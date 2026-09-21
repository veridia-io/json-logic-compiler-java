package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class LogicAndOrTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val cases: Seq[(String, String, Boolean)] = Seq(
    ("and: all true", "{\"and\": [true, 1, \"nonempty\"]}", true),
    ("and: first false", "{\"and\": [false, true, true]}", false),
    ("and: middle false", "{\"and\": [true, false, true]}", false),
    ("and: last false", "{\"and\": [true, true, false]}", false),
    ("or: first truthy", "{\"or\": [true, false, false]}", true),
    ("or: all falsy", "{\"or\": [0, null, \"\"]}", false),
    ("and/or mixed 1", "{\"and\": [ true, {\"or\": [false, true]} ]}", true),
    ("and/or mixed 2", "{\"and\": [ true, {\"or\": [false, false]} ]}", false),
    ("and/or mixed 3", "{\"or\": [ false, {\"and\": [true, true]} ]}", true),
    ("and short-circuits correctly on the last condition", "{\"and\":[true, true, false]}", false)
  )

  for (name, json, expected) <- cases do
    test(name) {
      jsonLogic.apply(json, null) shouldEqual expected
    }
