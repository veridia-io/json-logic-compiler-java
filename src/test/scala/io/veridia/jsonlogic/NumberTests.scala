package io.veridia.jsonlogic

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util as ju

/** Proves A1: `var` returns the raw Java numeric subtype unchanged when the context is a
  * caller-supplied `Map` (not a JSON string) — coercion is deferred to the consuming operator, not
  * performed at resolution time. Uses explicit `getClass` checks, not just value equality: Scala's
  * `==` (unlike Java's `Object.equals`) considers boxed numerics of different types value-equal
  * across types, so a `shouldEqual`-only assertion would silently pass even if the implementation
  * regressed to always returning e.g. `Double`.
  */
class NumberTests extends AnyFunSuite with Matchers:

  test("every numeric subtype survives var resolution unchanged") {
    val jsonLogic = new JsonLogic()
    val numbers = new ju.HashMap[String, Object]()
    numbers.put("double", java.lang.Double.valueOf(1d))
    numbers.put("float", java.lang.Float.valueOf(1f))
    numbers.put("int", java.lang.Integer.valueOf(1))
    numbers.put("short", java.lang.Short.valueOf(1.toShort))
    numbers.put("long", java.lang.Long.valueOf(1L))

    val double = jsonLogic.apply("{\"var\": \"double\"}", numbers)
    double shouldEqual 1d
    double.getClass shouldBe classOf[java.lang.Double]

    val float = jsonLogic.apply("{\"var\": \"float\"}", numbers)
    float shouldEqual 1f
    float.getClass shouldBe classOf[java.lang.Float]

    val int = jsonLogic.apply("{\"var\": \"int\"}", numbers)
    int shouldEqual 1
    int.getClass shouldBe classOf[java.lang.Integer]

    val short = jsonLogic.apply("{\"var\": \"short\"}", numbers)
    short.getClass shouldBe classOf[java.lang.Short]

    val long = jsonLogic.apply("{\"var\": \"long\"}", numbers)
    long shouldEqual 1L
    long.getClass shouldBe classOf[java.lang.Long]
  }
