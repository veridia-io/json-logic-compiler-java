package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class GeneralTests extends AnyFunSuite with Matchers:
  private val jsonLogic = new JsonLogic()

  test("null expression") {
    jsonLogic.apply(null, null) shouldBe null
  }

  test("empty expression") {
    val m = ju.Map.of("one", java.lang.Integer.valueOf(1))
    jsonLogic.apply("{\"var\": \"\"}", m) shouldEqual m
    jsonLogic.apply("{\"var\": \"\"}", null) shouldBe null
    jsonLogic.apply("", null) shouldBe null
  }
