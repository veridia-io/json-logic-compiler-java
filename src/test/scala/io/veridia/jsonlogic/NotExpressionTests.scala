package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class NotExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val cases: Seq[(String, String, Boolean)] = Seq(
    ("! boolean", "{\"!\": false}", true),
    ("! number", "{\"!\": 0}", true),
    ("! string", "{\"!\": \"\"}", true),
    ("! array", "{\"!\": []}", true),
    ("!! boolean", "{\"!!\": false}", false),
    ("!! number", "{\"!!\": 0}", false),
    ("!! string", "{\"!!\": \"\"}", false),
    ("!! array", "{\"!!\": [[]]}", false)
  )

  for (name, json, expected) <- cases do
    test(name) {
      jsonLogic.apply(json, null) shouldEqual expected
    }
