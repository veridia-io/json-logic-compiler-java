package io.veridia.jsonlogic.operators

import io.veridia.jsonlogic.{CompiledExpression, Operator}
import io.veridia.jsonlogic.helpers.ToDouble

import java.util as ju

/** `+ - * / %`, `min`, `max`, `round`. Every arg normalizes through [[ToDouble]], so the result is
  * always a `Double` (`round`'s bad-precision fallback included) — a literal's exact boxed numeric
  * type never survives arithmetic, unlike a `var` pass-through.
  */
object Arithmetic:

  private def reduceOp(sym: String, reducer: (Double, Double) => Double): Operator =
    val isSubtract = sym == "-"
    val isDivide = sym == "/"
    new Operator:
      def key(): String = sym
      def compile(args: ju.List[CompiledExpression]): CompiledExpression =
        val n = args.size()
        ctx =>
          val first = ToDouble.eval(args.get(0).eval(ctx))
          if n == 1 then
            if isSubtract then java.lang.Double.valueOf(-first)
            else if isDivide then null
            else java.lang.Double.valueOf(first)
          else
            val second = ToDouble.eval(args.get(1).eval(ctx))
            if isDivide && second == 0.0 then java.lang.Double.valueOf(0.0)
            else
              var acc = reducer(first, second)
              var i = 2
              while i < n do
                acc = reducer(acc, ToDouble.eval(args.get(i).eval(ctx)))
                i += 1
              java.lang.Double.valueOf(acc)

  val Add: Operator = reduceOp("+", _ + _)
  val Subtract: Operator = reduceOp("-", _ - _)
  val Multiply: Operator = reduceOp("*", _ * _)
  val Divide: Operator = reduceOp("/", _ / _)
  val Modulo: Operator = reduceOp("%", _ % _)
  val MinOp: Operator = reduceOp("min", Math.min)
  val MaxOp: Operator = reduceOp("max", Math.max)

  val Round: Operator = new Operator:
    def key(): String = "round"
    def compile(args: ju.List[CompiledExpression]): CompiledExpression =
      if args.size() != 2 then (_ => java.lang.Double.valueOf(0.0))
      else
        val valueExpr = args.get(0)
        val precisionExpr = args.get(1)
        ctx =>
          // The try/catch guards the arg evals themselves, not just the arithmetic: a nested
          // expression (e.g. a date operator fed a bad timezone) can throw at eval time, and
          // round's contract is to fail closed to 0.0 rather than let that escape.
          try
            val rawValue = valueExpr.eval(ctx)
            val rawPrecision = precisionExpr.eval(ctx)
            if rawValue == null || rawPrecision == null then java.lang.Double.valueOf(0.0)
            else
              val value = ToDouble.eval(rawValue)
              val precision = ToDouble.eval(rawPrecision).toInt
              val factor = Math.pow(10, precision)
              java.lang.Double.valueOf(Math.round(value * factor) / factor)
          catch case _: Exception => java.lang.Double.valueOf(0.0)

  val all: List[Operator] = List(Add, Subtract, Multiply, Divide, Modulo, MinOp, MaxOp, Round)
