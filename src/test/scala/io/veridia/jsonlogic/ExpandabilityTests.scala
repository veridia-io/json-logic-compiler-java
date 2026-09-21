package io.veridia.jsonlogic

import io.veridia.jsonlogic.helpers.ToDouble
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import java.util as ju
import java.util.zip.CRC32

/** Proves R3/C1: a custom operator can be added via `registerOperator` with zero changes to any
  * core file, and (via `groupsCount < 1`) that a wrong-shaped custom operator can eagerly `eval`
  * its own compile-time-constant arguments safely, unlike a time-varying value (see `DateTime`'s
  * `now`/`today`, which must not repeat this pattern).
  */
class ExpandabilityTests extends AnyFunSuite with Matchers:

  class AssignGroupOperator extends Operator:
    def key(): String = "assignGroup"

    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 2 then (_ => java.lang.Long.valueOf(1L))
      else
        val groupsCountExpr = args.get(0)
        val experimentNameExpr = args.get(1)
        try
          val groupsCount = ToDouble.eval(groupsCountExpr.eval(ju.Collections.emptyMap())).toLong
          val experimentName = experimentNameExpr.eval(ju.Collections.emptyMap())
          if groupsCount < 1 then (_ => java.lang.Long.valueOf(1L))
          else
            ctx =>
              try
                val ctxMap = ctx.asInstanceOf[ju.Map[String, Object]].get("context").asInstanceOf[ju.Map[String, Object]]
                val canonicalId = ctxMap.get("canonicalId").toString
                java.lang.Long.valueOf(crc32mod(canonicalId, String.valueOf(experimentName), groupsCount) + 1L)
              catch case _: Exception => java.lang.Long.valueOf(1L)
        catch case _: Exception => (_ => java.lang.Long.valueOf(1L))

    private def crc32mod(input: String, nonce: String, mod: Long): Long =
      val crc = new CRC32()
      crc.update(input.getBytes(StandardCharsets.UTF_8))
      crc.update(nonce.getBytes(StandardCharsets.UTF_8))
      crc.getValue % mod

  private val jsonLogic = new JsonLogic().registerOperator(new AssignGroupOperator())

  private def ctxFor(canonicalId: String): ju.Map[String, Object] =
    ju.Map.of("context", ju.Map.of("canonicalId", canonicalId))

  test("empty args falls back to 1") {
    jsonLogic.apply("{\"assignGroup\": []}", ctxFor("1234")) shouldEqual 1L
  }

  test("single arg falls back to 1") {
    jsonLogic.apply("{\"assignGroup\": [1]}", ctxFor("1234")) shouldEqual 1L
  }

  test("groupsCount of 1 always returns 1") {
    jsonLogic.apply("{\"assignGroup\": [1, \"\"]}", ctxFor("1234")) shouldEqual 1L
  }

  test("deterministic CRC32-based bucket assignment") {
    jsonLogic.apply("{\"assignGroup\": [2, \"new1\"]}", ctxFor("1234")) shouldEqual 1L
    jsonLogic.apply("{\"assignGroup\": [2, \"new1\"]}", ctxFor("12345")) shouldEqual 2L
    jsonLogic.apply("{\"assignGroup\": [2, \"new4\"]}", ctxFor("12345")) shouldEqual 1L
    jsonLogic.apply("{\"assignGroup\": [2, 3]}", ctxFor("12345")) shouldEqual 1L
  }
