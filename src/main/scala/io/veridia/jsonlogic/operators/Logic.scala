package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}
import io.veridia.jsonlogic.helpers.ToBoolean

import java.util as ju

/** `and`/`or` (short-circuiting, always boolean — not JS-style last-value propagation) and `!`/`!!`. */
object Logic:

  private def junction(sym: String, isAnd: Boolean): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      val a = args.toArray(Array.empty[CompiledExpression])
      ctx =>
        var result = true
        var i = 0
        var done = false
        while !done && i < a.length do
          result = ToBoolean.eval(a(i).eval(ctx))
          if (isAnd && !result) || (!isAnd && result) then done = true
          i += 1
        java.lang.Boolean.valueOf(result)

  val And: Operator = junction("and", isAnd = true)
  val Or: Operator = junction("or", isAnd = false)

  private def negation(sym: String, isDoubleNot: Boolean): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.isEmpty then (_ => java.lang.Boolean.valueOf(!isDoubleNot))
      else
        val valueExpr = args.get(0)
        ctx => java.lang.Boolean.valueOf(isDoubleNot == ToBoolean.eval(valueExpr.eval(ctx)))

  val Not: Operator = negation("!", isDoubleNot = false)
  val NotNot: Operator = negation("!!", isDoubleNot = true)

  val all: List[Operator] = List(And, Or, Not, NotNot)
