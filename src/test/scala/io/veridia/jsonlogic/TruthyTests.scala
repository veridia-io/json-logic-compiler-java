package io.veridia.jsonlogic

import io.veridia.jsonlogic.helpers.ToBoolean
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

class TruthyTests extends AnyFunSuite with Matchers:

  test("truthy values") {
    ToBoolean.eval(java.lang.Integer.valueOf(0)) shouldBe false
    ToBoolean.eval(java.lang.Double.valueOf(1.04)) shouldBe true
    ToBoolean.eval(java.lang.Integer.valueOf(-1)) shouldBe true

    ToBoolean.eval(ju.Collections.emptyList()) shouldBe false
    ToBoolean.eval(new Array[Int](0)) shouldBe false

    ToBoolean.eval(ju.Collections.singleton(java.lang.Integer.valueOf(1))) shouldBe true
    ToBoolean.eval(Array(false)) shouldBe true

    ToBoolean.eval("") shouldBe false
    ToBoolean.eval("hello world") shouldBe true
    ToBoolean.eval("0") shouldBe true

    ToBoolean.eval(null) shouldBe false

    ToBoolean.eval(java.lang.Double.NaN) shouldBe false
    ToBoolean.eval(java.lang.Float.NaN) shouldBe false
    ToBoolean.eval(java.lang.Double.POSITIVE_INFINITY) shouldBe true
    ToBoolean.eval(java.lang.Double.NEGATIVE_INFINITY) shouldBe true
    ToBoolean.eval(java.lang.Float.POSITIVE_INFINITY) shouldBe true
    ToBoolean.eval(java.lang.Float.NEGATIVE_INFINITY) shouldBe true
  }
