package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class NumericComparisonExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val cases: Seq[(String, String, Boolean)] = Seq(
    ("less than", "{\"<\" : [1, 2]}", true),
    ("less than or equal", "{\"<=\" : [1, 1]}", true),
    ("greater than", "{\">\" : [2, 1]}", true),
    ("greater than or equal", "{\">=\" : [1, 1]}", true),
    ("chained < exclusive", "{\"<\" : [1, 2, 3]}", true),
    ("chained <= inclusive", "{\"<=\" : [1, 1, 3]}", true),
    ("chained > exclusive", "{\">\" : [3, 2, 1]}", true),
    ("chained >= inclusive", "{\">=\" : [3, 1, 1]}", true),
    ("chained >= 4 args all satisfied", "{\">=\" : [3, 1, 1, 1]}", true),
    ("chained >= 4 args one pair fails", "{\">=\" : [3, 1, 3, 1]}", false)
  )

  for (name, json, expected) <- cases do
    test(name) {
      jsonLogic.apply(json, null) shouldEqual expected
    }
