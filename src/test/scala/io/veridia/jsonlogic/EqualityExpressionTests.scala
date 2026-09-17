package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class EqualityExpressionTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  private val cases: Seq[(String, String, Boolean)] = Seq(
    ("same value same type", "{\"==\": [1, 1]}", true),
    ("different value different type: [] == false", "{\"==\": [[], false]}", true),
    ("empty string and zero", "{\"==\": [\" \", 0]}", true),
    ("numeric string equals number", "{\"==\": [\"0.0\", 0]}", true),
    ("numeric string equals number 2", "{\"==\": [\"5\", 5]}", true),
    ("numeric string equals number 3", "{\"==\": [\"7.43\", 7.43]}", true),
    ("false equals zero", "{\"==\": [false, 0]}", true),
    ("true equals one", "{\"==\": [true, 1]}", true),
    ("boolean not equals wrong number", "{\"==\": [true, 2]}", false),
    ("string equals string", "{\"==\": [\"foo\", \"foo\"]}", true),
    ("string not equals string", "{\"==\": [\"foo\", \"bar\"]}", false),
    ("null equals zero", "{\"==\": [null, 0]}", true),
    ("null equals false", "{\"==\": [null, false]}", true),
    ("null equals numeric string", "{\"==\": [null, \"0.0\"]}", true),
    ("null not equals non-numeric string", "{\"==\": [null, \"foo\"]}", false),
    ("number not equals non-numeric string", "{\"==\": [5, \"foo\"]}", false),
    ("false not equals non-zero number", "{\"==\": [false, 2]}", false),
    ("numeric string not equals non-numeric string", "{\"==\": [\"0\", \"foo\"]}", false),
    ("null equals null", "{\"==\": [null, null]}", true),
    ("whitespace string numeric coercion", "{\"==\": [\" 7.5 \", 7.5]}", true),
    ("whitespace non-numeric string", "{\"==\": [\" foo \", \"foo\"]}", false),
    ("numeric string equals boolean true", "{\"==\": [\"1\", true]}", true)
  )

  for (name, json, expected) <- cases do
    test(s"== : $name") {
      jsonLogic.apply(json, null) shouldEqual expected
    }
