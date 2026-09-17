package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}
import io.veridia.jsonlogic.helpers.ToDouble

import java.util as ju

/** `>`,`<`,`>=`,`<=` (chained: `{">=": [a,b,c]} == (a>=b) AND (b>=c)`), plus `greatest`/`least`. */
object NumericOps:

  // Preserves an existing quirk deliberately: a chained comparison re-evaluates each interior
  // argument twice (once as the right operand of pair i, once as the left operand of pair i+1) —
  // not an evaluate-once loop. Changing that would be a real (if obscure) behavior change for an
  // argument expression with a side effect, e.g. `now()` used mid-chain.
  private def comparison(sym: String, test: (Double, Double) => Boolean): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      val n = args.size()
      ctx =>
        if n < 2 then java.lang.Boolean.FALSE
        else
          var ok = true
          var i = 0
          while ok && i < n - 1 do
            val left = ToDouble.eval(args.get(i).eval(ctx))
            val right = ToDouble.eval(args.get(i + 1).eval(ctx))
            if !test(left, right) then ok = false
            i += 1
          java.lang.Boolean.valueOf(ok)

  val Gt: Operator = comparison(">", _ > _)
  val Lt: Operator = comparison("<", _ < _)
  val Gte: Operator = comparison(">=", _ >= _)
  val Lte: Operator = comparison("<=", _ <= _)

  private def extremum(sym: String, pick: (Double, Double) => Double): Operator = new Operator:
    def key(): String = sym
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 2 then (_ => java.lang.Double.valueOf(0.0))
      else
        val leftExpr = args.get(0)
        val rightExpr = args.get(1)
        ctx => java.lang.Double.valueOf(pick(ToDouble.eval(leftExpr.eval(ctx)), ToDouble.eval(rightExpr.eval(ctx))))

  val Greatest: Operator = extremum("greatest", Math.max)
  val Least: Operator = extremum("least", Math.min)

  val all: List[Operator] = List(Gt, Lt, Gte, Lte, Greatest, Least)
